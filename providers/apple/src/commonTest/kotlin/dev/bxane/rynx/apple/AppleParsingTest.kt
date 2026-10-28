package dev.bxane.rynx.apple

import kotlin.test.*

class AppleParsingTest {
    @Test fun feedPreservesRankAndGenreFactsWithoutReusingAppleArt() {
        val result=ApplePayloads.feed("""{"feed":{"title":"Top Songs","country":"us","updated":"2026-10-28","results":[{"id":"100","name":"Moonlight","artistName":"bxane","artworkUrl100":"https://example.test/apple.jpg","genres":[{"genreId":"14","name":"Pop"}],"url":"https://music.apple.com/us/song/100"}]}}""","us")
        assertEquals("apple:100",result.tracks.single().id);assertEquals(1,result.tracks.single().chartRank)
        assertEquals(listOf("14"),result.tracks.single().genreIds);assertNull(result.tracks.single().artworkUrl)
    }
    @Test fun genresComeFromAppleHierarchyAndMalformedNodesAreIgnored() {
        val genres=ApplePayloads.genres("""{"34":{"name":"Music","subgenres":{"14":{"id":"14","name":"Pop","subgenres":{"15":{"id":"15","name":"Dream Pop"}}},"broken":{"id":"","name":""}}}}""")
        assertEquals("Pop",genres.single().name);assertEquals("Dream Pop",genres.single().children.single().name)
    }
    @Test fun publicPlaylistUsesOnlyJsonLdNotPrivateScriptData() {
        val html="""<script>window.private={"track":[{"name":"Wrong"}]}</script><script type="application/ld+json">{"@type":"MusicPlaylist","name":"Top 100: Global","url":"https://music.apple.com/us/playlist/global/pl.test","track":[{"@type":"MusicRecording","name":"Moon &amp; stars","url":"https://music.apple.com/us/song/moon/123","duration":"PT3M2S"}]}</script>"""
        val playlist=ApplePayloads.playlist(html,"pl.test","https://music.apple.com/us/playlist/global/pl.test")
        assertEquals("Top 100: Global",playlist.title);assertEquals("apple:123",playlist.tracks.single().id)
        assertEquals(182000,playlist.tracks.single().durationMillis)
        assertFails {ApplePayloads.playlist("<script>{\"@type\":\"MusicPlaylist\"}</script>","x","https://music.apple.com")}
    }
    @Test fun publicLinksOnlyAcceptActualApplePlaylistAnchors() {
        val html="""<script>"href=\"/us/playlist/bad/pl.hidden\""</script><a href="https://evil.test/us/playlist/hack/pl.bad">Bad</a><a href="/us/playlist/top-25-city/pl.city">City</a>"""
        assertEquals(listOf("https://music.apple.com/us/playlist/top-25-city/pl.city"),ApplePayloads.playlistLinks(html).map {it.url})
    }
}
