package dev.bxane.rynx.playback.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class AudioEqualizerLayout { TEN_BAND, THIRTY_ONE_BAND, PARAMETRIC }
enum class AudioFilterShape { PEAKING, LOW_SHELF, HIGH_SHELF, HIGH_PASS, LOW_PASS }

private fun Float.finiteOrZero() = if (isFinite()) this else 0f
private fun Float.finiteOr(fallback: Float) = if (isFinite()) this else fallback

data class AudioEqualizerBand(
    val frequencyHz: Float,
    val gainDb: Float = 0f,
    val q: Float = 1f,
    val shape: AudioFilterShape = AudioFilterShape.PEAKING,
    val enabled: Boolean = true,
)

/** User preferences for Rynx's playback DSP. Instances are immutable snapshots. */
data class AudioDspSettings(
    val pureMode: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val equalizerLayout: AudioEqualizerLayout = AudioEqualizerLayout.TEN_BAND,
    val equalizerBands: List<AudioEqualizerBand> = defaultBands(AudioEqualizerLayout.TEN_BAND),
    val bassDb: Float = 0f,
    val trebleDb: Float = 0f,
    val preampDb: Float = 0f,
    val autoHeadroom: Boolean = true,
    val stereoWidth: Float = 1f,
    val stereoBalance: Float = 0f,
    val crossfeed: Float = 0f,
    val compressorEnabled: Boolean = false,
    val compressorThresholdDb: Float = -18f,
    val compressorRatio: Float = 2f,
    val compressorAttackMs: Float = 10f,
    val compressorReleaseMs: Float = 120f,
    val limiterEnabled: Boolean = false,
    val limiterCeiling: Float = .98f,
    /** Optional triangular dither for the integer PCM conversion after DSP; Pure Mode bypasses it. */
    val ditherEnabled: Boolean = false,
) {
    fun normalized(): AudioDspSettings {
        val count = when (equalizerLayout) {
            AudioEqualizerLayout.TEN_BAND -> 10
            AudioEqualizerLayout.THIRTY_ONE_BAND -> 31
            AudioEqualizerLayout.PARAMETRIC -> 12
        }
        val normalizedBands = equalizerBands.take(count).map { band ->
            band.copy(
                frequencyHz = band.frequencyHz.takeIf { it.isFinite() }?.coerceIn(20f, 20_000f) ?: 1_000f,
                gainDb = band.gainDb.finiteOrZero().coerceIn(-18f, 18f),
                q = band.q.finiteOr(1f).coerceIn(.1f, 12f),
            )
        }.let { bands ->
            // Parametric EQ is a variable-length filter bank (1..12). Do not pad it to 12:
            // padding made the Sound sheet's Add filter action permanently disabled.
            if (equalizerLayout == AudioEqualizerLayout.PARAMETRIC) {
                bands.ifEmpty { defaultBands(AudioEqualizerLayout.PARAMETRIC) }
            } else if (bands.size == count) bands else bands + defaultBands(equalizerLayout).drop(bands.size)
        }
        return copy(
            equalizerBands = normalizedBands,
            bassDb = bassDb.finiteOrZero().coerceIn(-12f, 12f),
            trebleDb = trebleDb.finiteOrZero().coerceIn(-12f, 12f),
            preampDb = preampDb.finiteOrZero().coerceIn(-24f, 12f),
            stereoWidth = stereoWidth.finiteOr(1f).coerceIn(0f, 2f),
            stereoBalance = stereoBalance.finiteOrZero().coerceIn(-1f, 1f),
            crossfeed = crossfeed.finiteOrZero().coerceIn(0f, 1f),
            compressorThresholdDb = compressorThresholdDb.finiteOr(-18f).coerceIn(-60f, 0f),
            compressorRatio = compressorRatio.finiteOr(2f).coerceIn(1f, 20f),
            compressorAttackMs = compressorAttackMs.finiteOr(10f).coerceIn(.1f, 200f),
            compressorReleaseMs = compressorReleaseMs.finiteOr(120f).coerceIn(5f, 2_000f),
            limiterCeiling = limiterCeiling.finiteOr(.98f).coerceIn(.1f, 1f),
        )
    }

    fun withLayout(layout: AudioEqualizerLayout): AudioDspSettings {
        val source = equalizerBands
        val template = when (layout) {
            AudioEqualizerLayout.PARAMETRIC -> listOf(
                AudioEqualizerBand(100f), AudioEqualizerBand(400f), AudioEqualizerBand(1_600f), AudioEqualizerBand(6_400f),
            )
            else -> defaultBands(layout)
        }
        val mapped = template.map { target ->
            val nearest = source.minByOrNull { band -> kotlin.math.abs(kotlin.math.ln(band.frequencyHz.coerceAtLeast(20f) / target.frequencyHz) ) }
            if (nearest != null && kotlin.math.abs(kotlin.math.ln(nearest.frequencyHz.coerceAtLeast(20f) / target.frequencyHz)) < .18f)
                target.copy(gainDb=nearest.gainDb,q=nearest.q,shape=nearest.shape,enabled=nearest.enabled)
            else target
        }
        return copy(equalizerLayout=layout,equalizerBands=mapped)
    }

    companion object {
        fun forLayout(layout: AudioEqualizerLayout) = AudioDspSettings().withLayout(layout)
    }
}

fun defaultBands(layout: AudioEqualizerLayout): List<AudioEqualizerBand> = when (layout) {
    AudioEqualizerLayout.TEN_BAND -> listOf(31.5f, 63f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f)
    AudioEqualizerLayout.THIRTY_ONE_BAND -> listOf(
        20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f, 125f, 160f, 200f, 250f, 315f, 400f, 500f, 630f,
        800f, 1_000f, 1_250f, 1_600f, 2_000f, 2_500f, 3_150f, 4_000f, 5_000f, 6_300f, 8_000f, 10_000f,
        12_500f, 16_000f, 20_000f,
    )
    AudioEqualizerLayout.PARAMETRIC -> listOf(100f, 400f, 1_600f, 6_400f)
}.map(::AudioEqualizerBand)

/** Stateful, allocation-free PCM block processor. Create/reconfigure it off the audio callback. */
class RynxAudioDspRuntime(
    settings: AudioDspSettings,
    private val sampleRateHz: Int,
    private val channelCount: Int,
    val maximumFrames: Int = 65_536,
) {
    private val settings = settings.normalized()
    private val filters: Array<ChannelFilter>
    private val crossfeedFilter: BiquadCoefficients
    private val crossfeedState: Array<BiquadState>
    private val limiterSegmentGain = FloatArray(maximumFrames.coerceAtLeast(1))
    private var limiterReleaseGain = 1f
    private var compressorGain = 1f
    private val inputIsNeutral: Boolean
    val isBypass: Boolean get() = inputIsNeutral
    private val preampLinear: Float
    private val crossfeedLinear: Float
    private val compressorThresholdLinear: Float
    private val compressorExponent: Float
    private val compressorAttackCoefficient: Float
    private val compressorReleaseCoefficient: Float

    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(channelCount > 0) { "channelCount must be positive" }
        val activeBands = if (this.settings.equalizerEnabled && !this.settings.pureMode) this.settings.equalizerBands.filter { it.enabled } else emptyList()
        val specs = ArrayList<AudioEqualizerBand>(activeBands.size + 2)
        if (!this.settings.pureMode && this.settings.bassDb != 0f) specs += AudioEqualizerBand(120f, this.settings.bassDb, .707f, AudioFilterShape.LOW_SHELF)
        specs.addAll(activeBands)
        if (!this.settings.pureMode && this.settings.trebleDb != 0f) specs += AudioEqualizerBand(8_000f, this.settings.trebleDb, .707f, AudioFilterShape.HIGH_SHELF)
        filters = Array(specs.size) { index ->
            ChannelFilter(coefficients(specs[index], sampleRateHz), Array(channelCount) { BiquadState() })
        }
        crossfeedFilter = coefficients(AudioEqualizerBand(700f, 0f, .707f, AudioFilterShape.LOW_PASS), sampleRateHz)
        crossfeedState = Array(min(channelCount, 2)) { BiquadState() }
        inputIsNeutral = this.settings.pureMode || (
            filters.isEmpty() && this.settings.preampDb == 0f && this.settings.stereoWidth == 1f &&
                this.settings.stereoBalance == 0f && this.settings.crossfeed == 0f &&
                (!this.settings.compressorEnabled || this.settings.compressorRatio <= 1f) && !this.settings.limiterEnabled
            )
        val maximumBoostDb = if (this.settings.autoHeadroom && !this.settings.pureMode) maximumBoostDb(filters, sampleRateHz) else 0f
        val requiredAttenuation = max(0f, maximumBoostDb + this.settings.preampDb)
        preampLinear = dbToLinear(this.settings.preampDb - requiredAttenuation)
        crossfeedLinear = this.settings.crossfeed * .22f
        compressorThresholdLinear = dbToLinear(this.settings.compressorThresholdDb)
        compressorExponent = 1f / this.settings.compressorRatio - 1f
        compressorAttackCoefficient = exp(-1f / (sampleRateHz * this.settings.compressorAttackMs / 1_000f))
        compressorReleaseCoefficient = exp(-1f / (sampleRateHz * this.settings.compressorReleaseMs / 1_000f))
    }

    /** Processes interleaved float PCM in-place without allocating on the calling thread. */
    fun process(samples: FloatArray, frameCount: Int) {
        require(frameCount >= 0 && frameCount <= maximumFrames) { "frameCount exceeds configured audio block capacity" }
        require(samples.size >= frameCount * channelCount) { "PCM block is smaller than frameCount * channelCount" }
        if (frameCount == 0 || inputIsNeutral) return

        if (channelCount >= 2 && crossfeedLinear > 0f) {
            for (frame in 0 until frameCount) {
                val offset = frame * channelCount
                val left = samples[offset]
                val right = samples[offset + 1]
                val lowRight = crossfeedFilter.process(right, crossfeedState[0])
                val lowLeft = crossfeedFilter.process(left, crossfeedState[1])
                samples[offset] = left + lowRight * crossfeedLinear
                samples[offset + 1] = right + lowLeft * crossfeedLinear
            }
        }

        if (channelCount >= 2 && (settings.stereoWidth != 1f || settings.stereoBalance != 0f)) {
            val leftBalance = if (settings.stereoBalance > 0f) 1f - settings.stereoBalance else 1f
            val rightBalance = if (settings.stereoBalance < 0f) 1f + settings.stereoBalance else 1f
            for (frame in 0 until frameCount) {
                val offset = frame * channelCount
                val left = samples[offset]
                val right = samples[offset + 1]
                val mid = (left + right) * .5f
                val side = (left - right) * .5f * settings.stereoWidth
                samples[offset] = (mid + side) * leftBalance
                samples[offset + 1] = (mid - side) * rightBalance
            }
        }

        if (filters.isNotEmpty() || preampLinear != 1f) {
            for (frame in 0 until frameCount) {
                val offset = frame * channelCount
                for (channel in 0 until channelCount) {
                    var value = samples[offset + channel]
                    for (filter in filters) value = filter.coefficients.process(value, filter.states[channel])
                    samples[offset + channel] = value * preampLinear
                }
            }
        }

        if (settings.compressorEnabled && settings.compressorRatio > 1f) compressLinked(samples, frameCount)
        if (settings.limiterEnabled) limitTruePeakAware(samples, frameCount)
    }

    /** Clears filter delay and limiter envelope when Media3 flushes between tracks. */
    fun reset() {
        filters.forEach { filter -> filter.states.forEach { state -> state.z1 = 0f; state.z2 = 0f } }
        crossfeedState.forEach { state -> state.z1 = 0f; state.z2 = 0f }
        limiterReleaseGain = 1f
        compressorGain = 1f
    }

    /** Peak-detecting, stereo-linked compressor with smoothed gain reduction. */
    private fun compressLinked(samples: FloatArray, frameCount: Int) {
        for (frame in 0 until frameCount) {
            val offset = frame * channelCount
            var peak = 0f
            for (channel in 0 until channelCount) peak = max(peak, abs(samples[offset + channel]))
            val targetGain = if (peak <= compressorThresholdLinear || peak <= 0f) 1f
                else (peak / compressorThresholdLinear).pow(compressorExponent).coerceIn(0f, 1f)
            val coefficient = if (targetGain < compressorGain) compressorAttackCoefficient else compressorReleaseCoefficient
            compressorGain = targetGain + (compressorGain - targetGain) * coefficient
            for (channel in 0 until channelCount) samples[offset + channel] *= compressorGain
        }
    }

    private fun limitTruePeakAware(samples: FloatArray, frameCount: Int) {
        val ceiling = settings.limiterCeiling
        for (frame in 0 until frameCount) {
            var peak = 0f
            for (channel in 0 until channelCount) {
                val p0 = samples[(max(0, frame - 1)) * channelCount + channel]
                val p1 = samples[frame * channelCount + channel]
                val p2 = samples[(min(frameCount - 1, frame + 1)) * channelCount + channel]
                val p3 = samples[(min(frameCount - 1, frame + 2)) * channelCount + channel]
                peak = max(peak, cubicSegmentPeak(p0, p1, p2, p3))
            }
            // Small allowance covers coefficient and finite-precision error in the 4x interpolator.
            limiterSegmentGain[frame] = min(1f, ceiling / (peak * 1.01f).coerceAtLeast(1e-9f))
        }
        val release = exp(-1f / (sampleRateHz * .060f))
        for (frame in 0 until frameCount) {
            val target = min(limiterSegmentGain[frame], if (frame == 0) limiterSegmentGain[frame] else limiterSegmentGain[frame - 1])
            limiterReleaseGain = if (target < limiterReleaseGain) target else 1f - (1f - limiterReleaseGain) * release
            val offset = frame * channelCount
            for (channel in 0 until channelCount) samples[offset + channel] *= limiterReleaseGain
        }
    }

    private data class ChannelFilter(val coefficients: BiquadCoefficients, val states: Array<BiquadState>)
    private class BiquadState(var z1: Float = 0f, var z2: Float = 0f)
    private data class BiquadCoefficients(val b0: Float, val b1: Float, val b2: Float, val a1: Float, val a2: Float) {
        fun process(sample: Float, state: BiquadState): Float {
            val output = b0 * sample + state.z1
            state.z1 = b1 * sample - a1 * output + state.z2
            state.z2 = b2 * sample - a2 * output
            return output
        }
    }

    private fun cubicSegmentPeak(p0: Float, p1: Float, p2: Float, p3: Float): Float {
        var peak = max(abs(p1), abs(p2))
        for (step in 1..3) {
            val t = step * .25f
            val t2 = t * t
            val t3 = t2 * t
            val value = .5f * (
                2f * p1 + (-p0 + p2) * t + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                    (-p0 + 3f * p1 - 3f * p2 + p3) * t3
                )
            peak = max(peak, abs(value))
        }
        return peak
    }

    private fun maximumBoostDb(filters: Array<ChannelFilter>, sampleRateHz: Int): Float {
        if (filters.isEmpty()) return 0f
        val nyquist = min(20_000f, sampleRateHz * .45f)
        var maximum = 0f
        // Log-frequency scan compiles conservative headroom once, outside the real-time callback.
        for (index in 0..256) {
            val fraction = index / 256f
            val frequency = 20f * (nyquist / 20f).pow(fraction)
            val omega = 2.0 * PI * frequency / sampleRateHz
            val cosine = cos(omega).toFloat()
            val sine = sin(omega).toFloat()
            val cosine2 = cos(2.0 * omega).toFloat()
            val sine2 = sin(2.0 * omega).toFloat()
            var responseDb = 0f
            for (filter in filters) {
                val c = filter.coefficients
                val nr = c.b0 + c.b1 * cosine + c.b2 * cosine2
                val ni = -c.b1 * sine - c.b2 * sine2
                val dr = 1f + c.a1 * cosine + c.a2 * cosine2
                val di = -c.a1 * sine - c.a2 * sine2
                val numerator = nr * nr + ni * ni
                val denominator = dr * dr + di * di
                responseDb += (10.0 * ln((numerator / denominator).toDouble().coerceAtLeast(1e-12)) / ln(10.0)).toFloat()
            }
            maximum = max(maximum, responseDb)
        }
        return maximum.coerceAtLeast(0f)
    }

    private fun coefficients(band: AudioEqualizerBand, sampleRateHz: Int): BiquadCoefficients {
        val frequency = band.frequencyHz.coerceIn(10f, sampleRateHz * .45f)
        val omega = 2.0 * PI * frequency / sampleRateHz
        val c = cos(omega).toFloat()
        val s = sin(omega).toFloat()
        val q = band.q.coerceIn(.1f, 12f)
        val alpha = s / (2f * q)
        val a = 10.0.pow(band.gainDb / 40.0).toFloat()
        val sqrtA = sqrt(a)
        val (b0, b1, b2, a0, a1, a2) = when (band.shape) {
            AudioFilterShape.PEAKING -> Triple(1f + alpha * a, -2f * c, 1f - alpha * a).let { (b0, b1, b2) ->
                Six(b0, b1, b2, 1f + alpha / a, -2f * c, 1f - alpha / a)
            }
            AudioFilterShape.LOW_SHELF -> {
                val beta = 2f * sqrtA * alpha
                Six(a * ((a + 1f) - (a - 1f) * c + beta), 2f * a * ((a - 1f) - (a + 1f) * c), a * ((a + 1f) - (a - 1f) * c - beta),
                    (a + 1f) + (a - 1f) * c + beta, -2f * ((a - 1f) + (a + 1f) * c), (a + 1f) + (a - 1f) * c - beta)
            }
            AudioFilterShape.HIGH_SHELF -> {
                val beta = 2f * sqrtA * alpha
                Six(a * ((a + 1f) + (a - 1f) * c + beta), -2f * a * ((a - 1f) + (a + 1f) * c), a * ((a + 1f) + (a - 1f) * c - beta),
                    (a + 1f) - (a - 1f) * c + beta, 2f * ((a - 1f) - (a + 1f) * c), (a + 1f) - (a - 1f) * c - beta)
            }
            AudioFilterShape.HIGH_PASS -> Six((1f + c) / 2f, -(1f + c), (1f + c) / 2f, 1f + alpha, -2f * c, 1f - alpha)
            AudioFilterShape.LOW_PASS -> Six((1f - c) / 2f, 1f - c, (1f - c) / 2f, 1f + alpha, -2f * c, 1f - alpha)
        }
        return BiquadCoefficients(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    private data class Six(val b0: Float, val b1: Float, val b2: Float, val a0: Float, val a1: Float, val a2: Float)

    private fun dbToLinear(db: Float) = 10.0.pow(db / 20.0).toFloat()
}
