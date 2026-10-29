package dev.bxane.rynx.playback.download

import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadBrandingMetadataTest {
    @Test fun sidecarRecordsRynxAttributionAndEscapesTrackMetadata() {
        val metadata=DownloadBrandingMetadata(
            mediaId="track:42",
            title="A \"song\"\nline",
            artist="Artist \\ One",
            provider="Apple Music",
            downloadedAtEpochMillis=1_798_000_000_000,
        )

        assertEquals("Downloaded by Rynx",metadata.comment)
        assertEquals(
            """{"schemaVersion":1,"application":"Rynx","applicationId":"dev.bxane.rynx","comment":"Downloaded by Rynx","mediaId":"track:42","title":"A \"song\"\nline","artist":"Artist \\ One","provider":"Apple Music","downloadedAtEpochMillis":1798000000000}""",
            metadata.toSidecarJson(),
        )
    }
}
