package dev.bxane.rynx.innertubex

import kotlin.test.Test
import kotlin.test.assertEquals

class InnerTubeXSearchPayloadsTest {
    @Test fun mapsYoutubeMusicTrackMetadataArtworkDurationAndContinuation() {
        val page = InnerTubeXSearchPayloads.parse(
            """{"musicResponsiveListItemRenderer":{"playlistItemData":{"videoId":"abcdefghijk"},"flexColumns":[{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Glass"}]}}},{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"bxane"}]}}}],"fixedColumns":[{"musicResponsiveListItemFlexColumnRenderer":{"text":{"simpleText":"3:42"}}}],"thumbnail":{"thumbnails":[{"url":"https://lh3.googleusercontent.com/cover=w120-h120","width":120,"height":120},{"url":"https://lh3.googleusercontent.com/cover=w544-h544","width":544,"height":544}]}},"continuations":[{"nextContinuationData":{"continuation":"next-page-token"}}]}""",
        )

        val track = page.items.single()
        assertEquals("abcdefghijk", track.mediaId)
        assertEquals("Glass", track.title)
        assertEquals("bxane", track.artist)
        assertEquals(222L, track.durationSeconds)
        assertEquals("https://lh3.googleusercontent.com/cover=w544-h544", track.artworkUrl)
        assertEquals("next-page-token", page.continuation)
    }

    @Test fun ignoresNonTrackRowsAndInvalidContinuation() {
        val page = InnerTubeXSearchPayloads.parse("""{"contents":{}}""")
        assertEquals(emptyList(), page.items)
        assertEquals(null, page.continuation)
    }
}
