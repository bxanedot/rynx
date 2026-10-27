package dev.bxane.rynx.desktop

import java.io.ByteArrayInputStream
import java.nio.file.Files
import javax.sound.sampled.*
import kotlin.math.*
import kotlin.test.*

class PcmDecoderTest {
    private fun wave(): java.io.File {
        val file = Files.createTempFile("rynx-test-", ".wav").toFile().apply { deleteOnExit() }
        val samples = ByteArray(1600) { i -> if (i % 2 == 0) (sin(i / 2.0 * 2 * PI * 440 / 8000) * 40).toInt().toByte() else 0 }
        val format = AudioFormat(8000f, 16, 1, true, false)
        AudioInputStream(ByteArrayInputStream(samples), format, 800).use {
            AudioSystem.write(it, AudioFileFormat.Type.WAVE, file)
        }
        return file
    }
    @Test fun decodesRealWaveAndComputesDurationWithoutAudioDevice() {
        val file = wave()
        try {
            val decoded = PcmDecoder.decode(file)
            assertEquals(100L, decoded.durationMillis)
            assertEquals(1600, decoded.bytes.size)
            assertEquals(1, decoded.format.channels)
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, decoded.format.encoding)
        } finally { file.delete() }
    }
    @Test fun rejectsDecodeBeyondMemoryBudget() {
        val file = wave()
        try { assertFailsWith<IllegalArgumentException> { PcmDecoder.decode(file, maxDecodedBytes = 100) } }
        finally { file.delete() }
    }
    @Test fun rejectsNonAudioInput() {
        val file = Files.createTempFile("rynx-invalid-", ".wav").toFile()
        try {
            file.writeText("not audio")
            assertFailsWith<UnsupportedAudioFileException> { PcmDecoder.decode(file) }
        } finally { file.delete() }
    }
}
