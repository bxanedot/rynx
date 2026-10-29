package dev.bxane.rynx.playback.discovery

import dev.bxane.rynx.playback.catalog.AudioStreamResolver
import dev.bxane.rynx.playback.catalog.CatalogItem
import dev.bxane.rynx.playback.catalog.CatalogItemType
import dev.bxane.rynx.playback.catalog.MusicPlaybackCatalog
import dev.bxane.rynx.playback.catalog.CatalogPage
import dev.bxane.rynx.playback.catalog.CatalogProvider
import dev.bxane.rynx.playback.catalog.ResolvedAudioStream
import dev.bxane.rynx.playback.runSuspend
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MusicPlaybackPreparationTest {
    private class FakeCatalog : MusicPlaybackCatalog {
        var searchResults = CatalogPage(emptyList(), null)
        var appleMatches = emptyList<MusicMetadata>()
        var appleSong: MusicMetadata? = null
        var appleSearchFailure: Exception? = null
        var appleSongFailure: Exception? = null
        var searchCalls = 0
        var appleSearchCalls = 0
        var appleSongCalls = 0

        override suspend fun searchMusic(query: String): CatalogPage {
            searchCalls++
            return searchResults
        }

        override suspend fun searchMore(continuation: String) = CatalogPage(emptyList(), null)

        override suspend fun searchAppleMusic(query: String, country: String?, limit: Int): List<MusicMetadata> {
            appleSearchCalls++
            appleSearchFailure?.let { throw it }
            return appleMatches
        }

        override suspend fun appleSong(id: Long, country: String?): MusicMetadata? {
            appleSongCalls++
            appleSongFailure?.let { throw it }
            return appleSong
        }
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
    fun appleSelectionEnrichesMetadataMatchesAndResolvesTheChosenRecording() = runSuspend {
        val catalog = FakeCatalog().apply {
            searchResults = CatalogPage(listOf(track()), null)
            appleSong = MusicMetadata("apple:42", "Moonlight", "bxane", artworkUrl = "https://art.example/cover")
        }
        val resolver = FakeResolver(stream("video-id"))
        val selection = MusicMetadata("apple:42", "Moonlight", "bxane", durationMillis = 180_000)

        val prepared = MusicPlaybackPreparer(catalog, resolver) { "us" }.prepareDiscovery(selection)

        assertEquals("apple:42", prepared.metadata?.id)
        assertEquals("https://art.example/cover", prepared.metadata?.artworkUrl)
        assertSame(catalog.searchResults.items.single(), prepared.item)
        assertEquals(listOf("video-id"), resolver.resolvedIds)
        assertEquals(1, catalog.appleSongCalls)
        assertTrue(prepared.stream?.toString()?.contains("private-header") == false)
        assertTrue(prepared.stream.toString().contains("sig=private") == false)
    }

    @Test
    fun youtubeMusicMetadataUsesItsPlaybackIdWithoutSearchingAgain() = runSuspend {
        val catalog = FakeCatalog()
        val resolver = FakeResolver(stream("youtube-id"))
        val selection = MusicMetadata(
            id = "ytm:youtube-id", title = "Moonlight", artist = "bxane",
            source = MetadataSource.YouTubeMusic, playbackId = "youtube-id",
        )

        val prepared = MusicPlaybackPreparer(catalog, resolver) { "us" }.prepareDiscovery(selection)

        assertEquals("youtube-id", prepared.item.mediaId)
        assertEquals(0, catalog.searchCalls)
        assertEquals(listOf("youtube-id"), resolver.resolvedIds)
    }

    @Test
    fun unmatchedAppleRecordingFailsBeforeStreamResolution() = runSuspend {
        val catalog = FakeCatalog().apply { searchResults = CatalogPage(listOf(track(title = "Moonlight cover")), null) }
        val resolver = FakeResolver(stream("must-not-play"))

        assertFailsWith<NoMatchingRecordingException> {
            MusicPlaybackPreparer(catalog, resolver) { "us" }
                .prepareDiscovery(MusicMetadata("apple:42", "Moonlight", "bxane"))
        }

        assertTrue(resolver.resolvedIds.isEmpty())
    }

    @Test
    fun catalogMetadataFailureDoesNotPreventStreamPreparation() = runSuspend {
        val catalog = FakeCatalog().apply { appleSearchFailure = IllegalStateException("offline") }
        val resolver = FakeResolver(stream("video-id"))

        val prepared = MusicPlaybackPreparer(catalog, resolver) { "gb" }.prepareCatalog(track())

        assertNull(prepared.metadata)
        assertEquals("video-id", prepared.stream?.videoId)
        assertEquals(1, catalog.appleSearchCalls)
        assertEquals(listOf("video-id"), resolver.resolvedIds)
    }

    @Test
    fun appleSongEnrichmentFailureKeepsOriginalMetadataAndContinuesMatching() = runSuspend {
        val catalog = FakeCatalog().apply {
            searchResults = CatalogPage(listOf(track()), null)
            appleSongFailure = IllegalStateException("offline")
        }
        val selection = MusicMetadata("apple:42", "Moonlight", "bxane")

        val prepared = MusicPlaybackPreparer(catalog, FakeResolver(stream("video-id"))) { "us" }
            .prepareDiscovery(selection)

        assertEquals(selection, prepared.metadata)
    }

    @Test
    fun appleSearchCancellationPropagatesWithoutResolvingAudio() = runSuspend {
        val catalog = FakeCatalog().apply {
            appleSearchFailure = CancellationException("selection cancelled")
        }
        val resolver = FakeResolver(stream("must-not-play"))

        assertFailsWith<CancellationException> {
            MusicPlaybackPreparer(catalog, resolver) { "us" }
                .prepareCatalog(track())
        }

        assertTrue(resolver.resolvedIds.isEmpty())
    }
}
