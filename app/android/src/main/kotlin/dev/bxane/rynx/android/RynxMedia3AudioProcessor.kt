package dev.bxane.rynx.android

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import dev.bxane.rynx.playback.audio.AudioDspSettings
import dev.bxane.rynx.playback.audio.RynxAudioDspRuntime
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import kotlin.math.roundToInt

/** Media3's PCM hook. DSP state and scratch storage are prepared during configure/settings changes. */
@UnstableApi
internal class RynxMedia3AudioProcessor(initialSettings: AudioDspSettings) : AudioProcessor {
    private val settings = AtomicReference(initialSettings.normalized())
    private val runtime = AtomicReference<RynxAudioDspRuntime?>(null)
    // Retain the previous filter state for a short, allocation-free old->new transition.
    private val transitionFrom = AtomicReference<RynxAudioDspRuntime?>(null)
    private val requestedFadeFrames = AtomicInteger(0)
    private var fadeRemainingFrames = 0
    private var fadeLengthFrames = 1
    private var inputFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputStorage = ByteBuffer.allocateDirect(INITIAL_OUTPUT_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var floatScratch = FloatArray(MAX_WORKING_SAMPLES)
    private var dryScratch = FloatArray(MAX_WORKING_SAMPLES)
    private var inputEnded = false
    private var bigEndian = false
    // Audio-thread-owned xorshift state: no allocations or random-service calls during PCM conversion.
    private var ditherState = 0x6A09E667

    fun updateSettings(value: AudioDspSettings) {
        val safe = value.normalized()
        settings.set(safe)
        val currentFormat = inputFormat
        if (currentFormat != AudioProcessor.AudioFormat.NOT_SET && isSupported(currentFormat.encoding)) {
            val updatedRuntime = newRuntime(safe, currentFormat)
            val oldRuntime = runtime.getAndSet(updatedRuntime)
            // Pure Mode must bypass immediately; otherwise blend between the actual effects,
            // not between dry PCM and the replacement effects.
            transitionFrom.set(if (safe.pureMode || (oldRuntime?.isBypass == true && updatedRuntime.isBypass)) null else oldRuntime)
            AudioEngineDiagnostics.recordPcmFormat(currentFormat, !safe.pureMode && (!updatedRuntime.isBypass || safe.ditherEnabled))
            requestedFadeFrames.set(if (safe.pureMode) 0 else (currentFormat.sampleRate / 40).coerceAtLeast(1))
        }
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        inputFormat = inputAudioFormat
        if (floatScratch.size < MAX_WORKING_SAMPLES) floatScratch = FloatArray(MAX_WORKING_SAMPLES)
        if (dryScratch.size < MAX_WORKING_SAMPLES) dryScratch = FloatArray(MAX_WORKING_SAMPLES)
        if (inputAudioFormat.sampleRate <= 0 || inputAudioFormat.channelCount <= 0 || !isSupported(inputAudioFormat.encoding)) {
            outputFormat = AudioProcessor.AudioFormat.NOT_SET
            runtime.set(null)
            AudioEngineDiagnostics.recordPcmFormat(null, false)
            return outputFormat
        }
        bigEndian = inputAudioFormat.encoding in BIG_ENDIAN_ENCODINGS
        val oneSecondCapacity=(inputAudioFormat.sampleRate.toLong()*inputAudioFormat.bytesPerFrame)
            .coerceIn(INITIAL_OUTPUT_BYTES.toLong(),MAX_OUTPUT_BYTES.toLong()).toInt()
        if(outputStorage.capacity()<oneSecondCapacity)
            outputStorage=ByteBuffer.allocateDirect(oneSecondCapacity).order(ByteOrder.LITTLE_ENDIAN)
        outputFormat = inputAudioFormat
        val configuredSettings = settings.get()
        val configuredRuntime = newRuntime(configuredSettings, inputAudioFormat)
        runtime.set(configuredRuntime)
        transitionFrom.set(null)
        fadeRemainingFrames = 0
        AudioEngineDiagnostics.recordPcmFormat(inputAudioFormat, !configuredSettings.pureMode && (!configuredRuntime.isBypass || configuredSettings.ditherEnabled))
        val initialFade = if (configuredSettings.pureMode) 0 else (inputAudioFormat.sampleRate / 40).coerceAtLeast(1)
        requestedFadeFrames.set(initialFade)
        return outputFormat
    }

    override fun isActive(): Boolean = outputFormat != AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive() || !inputBuffer.hasRemaining() || outputBuffer.hasRemaining()) return
        val channels = inputFormat.channelCount
        val frameBytes = inputFormat.bytesPerFrame
        if (frameBytes <= 0) return
        val availableFrames = inputBuffer.remaining() / frameBytes
        val frames = min(availableFrames, (MAX_OUTPUT_BYTES / frameBytes).coerceAtLeast(1))
        val consumedBytes = frames * frameBytes
        if (frames <= 0) return
        ensureOutputCapacity(consumedBytes)
        outputStorage.clear()
        val activeRuntime = runtime.get()
        val previousRuntime = transitionFrom.get()
        val appliedSettings = settings.get()
        val shouldDither = !appliedSettings.pureMode && appliedSettings.ditherEnabled
        if (appliedSettings.pureMode || activeRuntime == null ||
            (activeRuntime.isBypass && !shouldDither && previousRuntime == null)) {
            val originalLimit=inputBuffer.limit()
            inputBuffer.limit(inputBuffer.position()+consumedBytes)
            outputStorage.put(inputBuffer)
            inputBuffer.limit(originalLimit)
            outputBuffer = outputStorage.apply { flip() }
            return
        }

        val blockFade = requestedFadeFrames.getAndSet(0)
        if (blockFade > 0) {
            fadeLengthFrames = blockFade
            fadeRemainingFrames = blockFade
        }
        val byteOrder=if (bigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        inputBuffer.order(byteOrder)
        outputStorage.order(byteOrder)
        val maxChunkFrames = min(MAX_WORKING_SAMPLES / channels, activeRuntime.maximumFrames).coerceAtLeast(1)
        var frameOffset = 0
        while (frameOffset < frames) {
            val chunkFrames = min(maxChunkFrames, frames - frameOffset)
            val sampleCount = chunkFrames * channels
            for (sampleIndex in 0 until sampleCount) {
                val value = readSample(inputBuffer)
                floatScratch[sampleIndex] = value
                if (fadeRemainingFrames > 0) dryScratch[sampleIndex] = value
            }
            val startFade = fadeRemainingFrames
            if (startFade > 0 && previousRuntime != null) previousRuntime.process(dryScratch, chunkFrames)
            activeRuntime.process(floatScratch, chunkFrames)
            for (frame in 0 until chunkFrames) {
                val mix = if (fadeRemainingFrames > 0) 1f - fadeRemainingFrames.toFloat() / fadeLengthFrames else 1f
                val offset = frame * channels
                for (channel in 0 until channels) {
                    val index = offset + channel
                    val processed = if (startFade > 0 && frame < startFade) {
                        dryScratch[index] + (floatScratch[index] - dryScratch[index]) * mix
                    } else floatScratch[index]
                    writeSample(outputStorage, processed, shouldDither)
                }
                if (fadeRemainingFrames > 0) fadeRemainingFrames--
            }
            frameOffset += chunkFrames
        }
        // The PCM input is frame-aligned by Media3. Any malformed trailing bytes are consumed unchanged only
        // after complete frames have been processed; supported sink formats do not emit this case.
        if (frames == availableFrames && inputBuffer.hasRemaining()) {
            while (inputBuffer.hasRemaining()) outputStorage.put(inputBuffer.get())
        }
        if (fadeRemainingFrames == 0 && previousRuntime != null) transitionFrom.compareAndSet(previousRuntime, null)
        outputBuffer = outputStorage.apply { flip() }
    }

    override fun queueEndOfStream() { inputEnded = true }

    override fun getOutput(): ByteBuffer {
        val result = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return result
    }

    override fun isEnded(): Boolean = inputEnded && !outputBuffer.hasRemaining()

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        runtime.get()?.reset()
        transitionFrom.set(null)
        fadeRemainingFrames = 0
        requestedFadeFrames.set(0)
    }

    override fun reset() {
        flush()
        inputFormat = AudioProcessor.AudioFormat.NOT_SET
        outputFormat = AudioProcessor.AudioFormat.NOT_SET
        runtime.set(null)
        AudioEngineDiagnostics.recordPcmFormat(null, false)
        floatScratch = FloatArray(0)
        dryScratch = FloatArray(0)
        outputStorage = ByteBuffer.allocateDirect(0)
        bigEndian = false
    }

    private fun newRuntime(settings: AudioDspSettings, format: AudioProcessor.AudioFormat) =
        RynxAudioDspRuntime(settings, format.sampleRate, format.channelCount, MAX_RUNTIME_FRAMES)

    private fun ensureOutputCapacity(requiredBytes: Int) {
        if (outputStorage.capacity() >= requiredBytes) return
        var capacity = outputStorage.capacity().coerceAtLeast(INITIAL_OUTPUT_BYTES)
        while (capacity < requiredBytes) capacity = (capacity * 2).coerceAtMost(MAX_OUTPUT_BYTES)
        if (capacity < requiredBytes) error("PCM block exceeds the configured Rynx output buffer limit")
        outputStorage = ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun readSample(input: ByteBuffer): Float = when (baseEncoding(inputFormat.encoding)) {
        C.ENCODING_PCM_8BIT -> ((input.get().toInt() and 0xff) - 128) / 128f
        C.ENCODING_PCM_16BIT -> input.short / 32768f
        C.ENCODING_PCM_24BIT -> readPcm24(input) / 8_388_608f
        C.ENCODING_PCM_32BIT -> (input.int.toDouble() / 2_147_483_648.0).toFloat()
        C.ENCODING_PCM_FLOAT -> input.float.takeIf(Float::isFinite) ?: 0f
        else -> 0f
    }

    private fun writeSample(output: ByteBuffer, source: Float, dither: Boolean) {
        val sample = if (source.isFinite()) source else 0f
        if (sample < -1f || sample > 1f) AudioEngineDiagnostics.recordClip()
        val safe = sample.coerceIn(-1f, 1f)
        when (baseEncoding(outputFormat.encoding)) {
            // TPDF dither is applied in units of the target integer format's least significant bit.
            // Float PCM and 32-bit integer PCM are not dithered, avoiding needless noise there.
            C.ENCODING_PCM_8BIT -> output.put((safe * 128f + if (dither) tpdfLsb() else 0f).roundToInt().coerceIn(-128, 127).plus(128).toByte())
            C.ENCODING_PCM_16BIT -> output.putShort((safe * 32768f + if (dither) tpdfLsb() else 0f).roundToInt().coerceIn(-32768, 32767).toShort())
            C.ENCODING_PCM_24BIT -> writePcm24(output, (safe * 8_388_608f + if (dither) tpdfLsb() else 0f).roundToInt().coerceIn(-8_388_608, 8_388_607))
            C.ENCODING_PCM_32BIT -> output.putInt((safe.toDouble() * 2_147_483_648.0).toLong().coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt())
            C.ENCODING_PCM_FLOAT -> output.putFloat(safe)
        }
    }

    private fun tpdfLsb(): Float = nextUniform() - nextUniform()

    private fun nextUniform(): Float {
        var value = ditherState
        value = value xor (value shl 13)
        value = value xor (value ushr 17)
        value = value xor (value shl 5)
        ditherState = value
        return (value ushr 8) / 16_777_216f
    }

    private fun readPcm24(input: ByteBuffer): Int {
        val a = input.get().toInt() and 0xff
        val b = input.get().toInt() and 0xff
        val c = input.get().toInt() and 0xff
        val unsigned = if (bigEndian) (a shl 16) or (b shl 8) or c else (c shl 16) or (b shl 8) or a
        return if (unsigned and 0x800000 != 0) unsigned or -0x1000000 else unsigned
    }

    private fun writePcm24(output: ByteBuffer, signed: Int) {
        val value = signed and 0x00ffffff
        val low = (value and 0xff).toByte()
        val middle = ((value shr 8) and 0xff).toByte()
        val high = ((value shr 16) and 0xff).toByte()
        if (bigEndian) { output.put(high); output.put(middle); output.put(low) }
        else { output.put(low); output.put(middle); output.put(high) }
    }

    private fun baseEncoding(encoding: Int): Int = when (encoding) {
        C.ENCODING_PCM_16BIT_BIG_ENDIAN -> C.ENCODING_PCM_16BIT
        C.ENCODING_PCM_24BIT_BIG_ENDIAN -> C.ENCODING_PCM_24BIT
        C.ENCODING_PCM_32BIT_BIG_ENDIAN -> C.ENCODING_PCM_32BIT
        C.ENCODING_PCM_FLOAT_BIG_ENDIAN -> C.ENCODING_PCM_FLOAT
        else -> encoding
    }

    private fun isSupported(encoding: Int): Boolean = baseEncoding(encoding) in SUPPORTED_ENCODINGS

    private companion object {
        const val MAX_WORKING_SAMPLES = 262_144
        const val MAX_RUNTIME_FRAMES = 32_768
        const val INITIAL_OUTPUT_BYTES = 1_048_576
        const val MAX_OUTPUT_BYTES = 16 * 1024 * 1024
        val SUPPORTED_ENCODINGS = setOf(C.ENCODING_PCM_8BIT, C.ENCODING_PCM_16BIT, C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT)
        val BIG_ENDIAN_ENCODINGS = setOf(C.ENCODING_PCM_16BIT_BIG_ENDIAN, C.ENCODING_PCM_24BIT_BIG_ENDIAN, C.ENCODING_PCM_32BIT_BIG_ENDIAN, C.ENCODING_PCM_FLOAT_BIG_ENDIAN)
    }
}

internal data class ActiveDspPcmFormat(
    val sampleRateHz: Int,
    val channelCount: Int,
    val encoding: Int,
    val processingEnabled: Boolean,
)

internal object AudioEngineDiagnostics {
    private val clipped = AtomicLong(0)
    private val pcm = AtomicReference<ActiveDspPcmFormat?>(null)
    val clippingCount: Long get() = clipped.get()
    val pcmFormat: ActiveDspPcmFormat? get() = pcm.get()
    fun recordClip() { clipped.incrementAndGet() }
    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    fun recordPcmFormat(format: AudioProcessor.AudioFormat?, processingEnabled: Boolean) {
        pcm.set(format?.let { ActiveDspPcmFormat(it.sampleRate, it.channelCount, it.encoding, processingEnabled) })
    }
}
