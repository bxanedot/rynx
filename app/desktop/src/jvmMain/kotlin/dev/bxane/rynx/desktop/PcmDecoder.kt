package dev.bxane.rynx.desktop

import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

internal data class PcmAudio(val bytes: ByteArray, val format: AudioFormat) {
    val durationMillis: Long get() = (bytes.size.toDouble() / format.frameSize / format.frameRate * 1000).toLong()
}

/** Bounded desktop baseline decoder; supported formats depend on the installed Java Sound codecs. */
internal object PcmDecoder {
    fun decode(file: File, maxDecodedBytes: Int = 128 * 1024 * 1024): PcmAudio {
        require(maxDecodedBytes > 0)
        require(file.length() <= 64L * 1024 * 1024) { "This baseline player supports files up to 64 MiB" }
        AudioSystem.getAudioInputStream(file).use { input ->
            val source = input.format
            require(source.sampleRate.isFinite() && source.sampleRate > 0 && source.channels in 1..8) { "Unsupported audio format" }
            val pcm = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, source.sampleRate, 16, source.channels, source.channels * 2, source.sampleRate, false)
            AudioSystem.getAudioInputStream(pcm, input).use { decoded ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                while(true) {
                    val read = decoded.read(buffer)
                    if(read < 0) break
                    require(output.size().toLong() + read <= maxDecodedBytes) { "Decoded audio exceeds memory budget" }
                    output.write(buffer,0,read)
                }
                require(output.size() > 0) { "Audio file contains no samples" }
                return PcmAudio(output.toByteArray(),pcm)
            }
        }
    }
}
