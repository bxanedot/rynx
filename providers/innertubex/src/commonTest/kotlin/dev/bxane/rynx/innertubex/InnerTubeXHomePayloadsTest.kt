package dev.bxane.rynx.innertubex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InnerTubeXHomePayloadsTest {
    @Test fun realHomeCarouselProducesActionableSongsWithoutMisrepresentingPlaylists() {
        val data = """{"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{"sectionListRenderer":{"contents":[{"musicCarouselShelfRenderer":{"header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Quick Picks"}]}}},"contents":[{"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Neon Sky"}]},"subtitle":{"runs":[{"text":"Aria"}]},"navigationEndpoint":{"watchEndpoint":{"videoId":"B1c2D3e4F5g"}},"thumbnailRenderer":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"https://lh3.googleusercontent.com/sample=w544-h544","width":544,"height":544}]}}}}},{"musicTwoRowItemRenderer":{"title":{"runs":[{"text":"Playlist not playable"}]},"navigationEndpoint":{"browseEndpoint":{"browseId":"VLPL123"}}}}]}}]}}}}]}}}"""
        val sections=InnerTubeXHomePayloads.parse(data)
        assertEquals(1,sections.size)
        assertEquals("Quick Picks",sections.single().title)
        assertEquals(1,sections.single().tracks.size)
        val song=sections.single().tracks.single()
        assertEquals("Neon Sky",song.title)
        assertEquals("Aria",song.artist)
        assertEquals("B1c2D3e4F5g",song.playbackId)
        assertEquals("https://music.youtube.com/watch?v=B1c2D3e4F5g",song.sourceUrl)
        assertEquals("https://lh3.googleusercontent.com/sample=w544-h544",song.artworkUrl)
    }

    @Test fun emptyHomeIsNeverFakedFromNonSongItems() {
        assertTrue(InnerTubeXHomePayloads.parse("""{"contents":{}}""").isEmpty())
        assertTrue(InnerTubeXHomePayloads.parse("""{"contents":{"musicCarouselShelfRenderer":{"header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Empty"}]}}},"contents":[]}}}""").isEmpty())
    }
}
