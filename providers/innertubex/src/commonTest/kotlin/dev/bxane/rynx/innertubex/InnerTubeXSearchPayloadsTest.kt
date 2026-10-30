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

    @Test fun nestedFlexColumnWatchEndpointIsAPlayableTrackAndAcceptsYtimgArtwork() {
        // Real-world WEB_REMIX rows can carry the watch endpoint on the title's run,
        // not in playlistItemData or the top-level navigationEndpoint.
        val json = """{"contents":[{"musicResponsiveListItemRenderer":{"flexColumns":[{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Evening Light","navigationEndpoint":{"watchEndpoint":{"videoId":"a1B2c3D4e5F"}}}]}}},{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Sample Artist"}]}}}],"thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/a1B2c3D4e5F/hqdefault.jpg","width":480,"height":360}]}}}]}"""
        val result=InnerTubeXSearchPayloads.parse(json)
        assertEquals("a1B2c3D4e5F",result.items.single().mediaId)
        assertEquals("Evening Light",result.items.single().title)
        assertEquals("Sample Artist",result.items.single().artist)
        assertEquals("https://i.ytimg.com/vi/a1B2c3D4e5F/hqdefault.jpg",result.items.single().artworkUrl)
    }

    @Test fun providerErrorIsNotAValidEmptyCatalog() {
        kotlin.test.assertTrue(InnerTubeXSearchPayloads.hasCatalogProblem("""{"error":{"code":403}}"""))
        kotlin.test.assertTrue(InnerTubeXSearchPayloads.hasCatalogProblem("not JSON"))
        kotlin.test.assertFalse(InnerTubeXSearchPayloads.hasCatalogProblem("""{"contents":{"sectionListRenderer":{"contents":[]}}}"""))
    }

    @Test fun ignoresNonTrackRowsAndInvalidContinuation() {
        val page = InnerTubeXSearchPayloads.parse("""{"contents":{}}""")
        assertEquals(emptyList(), page.items)
        assertEquals(null, page.continuation)
    }
}
