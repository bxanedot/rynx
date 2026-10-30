package dev.bxane.rynx.playback.discovery

import dev.bxane.rynx.playback.catalog.AudioStreamResolver
import dev.bxane.rynx.playback.catalog.CatalogItem
import dev.bxane.rynx.playback.catalog.CatalogItemType
import dev.bxane.rynx.playback.catalog.MusicPlaybackCatalog
import dev.bxane.rynx.playback.catalog.CatalogPage
import dev.bxane.rynx.playback.catalog.CatalogProvider
import dev.bxane.rynx.playback.catalog.ResolvedAudioStream
import dev.bxane.rynx.playback.runSuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MusicPlaybackPreparationTest {
    private class FakeCatalog : MusicPlaybackCatalog {
        var searchResults = CatalogPage(emptyList(), null)
        var searchCalls = 0

        override suspend fun searchMusic(query: String): CatalogPage {
            searchCalls++
            return searchResults
        }

        override suspend fun searchMore(continuation: String) = CatalogPage(emptyList(), null)
    }

    private class FakeResolver(
        private val stream: ResolvedAudioStream? = null,
        private val failure: Exception? = null,
    ) : AudioStreamResolver {
        var resolvedIds = mutableListOf<String>()

        override suspend fun resolveAudio(videoId: String): ResolvedAudioStream? {
            resolvedIds += videoId
            failure?.let { throw it }
            return stream
        }
    }

    private fun track(id: String = "video-id", title: String = "Moonlight", artist: String = "bxane") =
        CatalogItem(CatalogProvider.YouTubeMusic, CatalogItemType.Track, id, title, artist, 180)

    private fun stream(videoId: String) = ResolvedAudioStream(
        videoId = videoId,
        url = "https://media.example/$videoId?sig=private",
        headers = mapOf("Authorization" to "private-header"),
        bitrateKbps = 128,
        mimeType = "audio/mp4",
        profileId = "test",
    )

    @Test
    fun youtubeMusicMetadataUsesItsPlaybackIdWithoutSearchingAgain() = runSuspend {
        val catalog = FakeCatalog()
        val resolver = FakeResolver(stream("youtube-id"))
        val selection = MusicMetadata(
            id = "ytm:youtube-id", title = "Moonlight", artist = "bxane",
            source = MetadataSource.YouTubeMusic, playbackId = "youtube-id",
        )

        val prepared = MusicPlaybackPreparer(catalog, resolver).prepareDiscovery(selection)

        assertEquals("youtube-id", prepared.item.mediaId)
        assertEquals(0, catalog.searchCalls)
        assertEquals(listOf("youtube-id"), resolver.resolvedIds)
    }

    @Test
    fun discoveryWithoutPlaybackIdSearchesAndMatchesChosenRecording() = runSuspend {
        val catalog = FakeCatalog().apply {
            searchResults = CatalogPage(listOf(track()), null)
        }
        val resolver = FakeResolver(stream("video-id"))
        val selection = MusicMetadata("yt:search-track", "Moonlight", "bxane")

        val prepared = MusicPlaybackPreparer(catalog, resolver).prepareDiscovery(selection)

        assertSame(catalog.searchResults.items.single(), prepared.item)
        assertEquals(listOf("video-id"), resolver.resolvedIds)
        assertEquals(1, catalog.searchCalls)
    }

    @Test
    fun unmatchedRecordingFailsBeforeStreamResolution() = runSuspend {
        val catalog = FakeCatalog().apply { searchResults = CatalogPage(listOf(track(title = "Moonlight cover")), null) }
        val resolver = FakeResolver(stream("must-not-play"))

        assertFailsWith<NoMatchingRecordingException> {
            MusicPlaybackPreparer(catalog, resolver)
                .prepareDiscovery(MusicMetadata("yt:42", "Moonlight", "bxane"))
        }

        assertTrue(resolver.resolvedIds.isEmpty())
    }

    @Test
    fun prepareCatalogResolvesStreamDirectly() = runSuspend {
        val catalog = FakeCatalog()
        val resolver = FakeResolver(stream("video-id"))

        val prepared = MusicPlaybackPreparer(catalog, resolver).prepareCatalog(track())

        assertNull(prepared.metadata)
        assertEquals("video-id", prepared.stream?.videoId)
        assertEquals(listOf("video-id"), resolver.resolvedIds)
    }
}
