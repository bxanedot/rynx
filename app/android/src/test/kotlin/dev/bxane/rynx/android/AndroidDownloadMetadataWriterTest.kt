package dev.bxane.rynx.android

import dev.bxane.rynx.playback.download.DownloadBrandingMetadata
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AndroidDownloadMetadataWriterTest {
    @get:Rule val temporary=TemporaryFolder()

    @Test fun writesAttributionBesideDownloadWithoutChangingAudioBytes() {
        val original=byteArrayOf(0x49,0x44,0x33,0x04,0x00)
        val mediaFile=temporary.newFile("track.m4a").apply {writeBytes(original)}
        val sidecar=AndroidDownloadMetadataWriter.write(mediaFile,DownloadBrandingMetadata(
            mediaId="yt:abc123",
            title="Example track",
            artist="Example artist",
            provider="YouTube Music",
            downloadedAtEpochMillis=42,
        ))

        assertEquals("track.m4a.rynx.json",sidecar.name)
        assertTrue(sidecar.isFile)
        assertArrayEquals(original,mediaFile.readBytes())
        assertTrue(sidecar.readText().contains("\"comment\":\"Downloaded by Rynx\""))
        assertTrue(sidecar.readText().contains("\"applicationId\":\"dev.bxane.rynx\""))
    }
}
