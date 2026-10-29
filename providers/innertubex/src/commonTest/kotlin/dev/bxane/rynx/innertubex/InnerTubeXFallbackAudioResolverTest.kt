package dev.bxane.rynx.innertubex

import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.ExtractedStream
import com.metrolist.innertubex.extraction.StreamExtractor
import com.metrolist.innertubex.extraction.StreamResolveException
import dev.bxane.rynx.playback.catalog.AudioResolutionChain
import dev.bxane.rynx.playback.catalog.AudioStreamResolver
import dev.bxane.rynx.playback.catalog.ProviderAccessRefusalException
import dev.bxane.rynx.playback.catalog.ResolvedAudioStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InnerTubeXFallbackAudioResolverTest {
    private class FakeExtractor(
        private val stream: ExtractedStream?,
        private val failure: Exception? = null,
    ) : StreamExtractor {
        var receivedHints: ContentHints? = null
            private set
        var receivedQuality: AudioQuality? = null
            private set

        override suspend fun prewarm() = Unit

        override suspend fun extract(
            videoId: String,
            hints: ContentHints,
            excludedClients: Set<String>,
            audioQuality: AudioQuality,
            clientPlaybackNonce: String,
        ): ExtractedStream? {
            receivedHints = hints
            receivedQuality = audioQuality
            failure?.let { throw it }
            return stream
        }
    }

    private fun stream(
        url: String = "https://media.example/audio",
        mimeType: String? = "audio/webm; codecs=opus",
        rangeChunks: Boolean = false,
        boundedRange: Boolean = false,
    ) = ExtractedStream(
        videoId = "abc12345678",
        audioUrl = url,
        headers = mapOf("User-Agent" to "Rynx test"),
        loudnessDb = null,
        expiresAt = null,
        contentLengthBytes = 1_000_000,
        itag = 251,
        mimeType = mimeType,
        codecs = "opus",
        bitrate = 160_000,
        sampleRate = 48_000,
        clientName = "WEB",
        profileId = "web-default",
        requireBoundedRange = boundedRange,
        rangeChunkSizeBytes = 1_048_576,
        useRangeChunks = rangeChunks,
    )

    @Test
    fun requestsDirectAudioAndMapsOnlyPlayerCompatibleStreamData() = runSuspend {
        val extractor = FakeExtractor(stream())
        val resolver = InnerTubeXFallbackAudioResolver(extractor)

        val resolved = resolver.resolveAudio("abc12345678")

        assertEquals("abc12345678", resolved?.videoId)
        assertEquals("https://media.example/audio", resolved?.url)
        assertEquals(mapOf("User-Agent" to "Rynx test"), resolved?.headers)
        assertEquals(160, resolved?.bitrateKbps)
        assertEquals("audio/webm", resolved?.mimeType)
        assertEquals("innertubex-web-default", resolved?.profileId)
        assertEquals(AudioQuality.HIGH, extractor.receivedQuality)
        assertFalse(extractor.receivedHints!!.allowHls)
        assertFalse(extractor.receivedHints!!.allowSabr)
        assertFalse(extractor.receivedHints!!.allowBoundedRange)
    }

    @Test
    fun refusesStreamsOutsideTheCurrentPlayerContract() = runSuspend {
        val hls = InnerTubeXFallbackAudioResolver(FakeExtractor(stream(mimeType = "application/x-mpegURL")))
        val chunked = InnerTubeXFallbackAudioResolver(FakeExtractor(stream(rangeChunks = true)))
        val bounded = InnerTubeXFallbackAudioResolver(FakeExtractor(stream(boundedRange = true)))
        val nonHttps = InnerTubeXFallbackAudioResolver(FakeExtractor(stream(url = "http://media.example/audio")))

        assertNull(hls.resolveAudio("abc12345678"))
        assertNull(chunked.resolveAudio("abc12345678"))
        assertNull(bounded.resolveAudio("abc12345678"))
        assertNull(nonHttps.resolveAudio("abc12345678"))
    }

    @Test
    fun rejectsInvalidVideoIdsBeforeStartingExtraction() = runSuspend {
        var started = false
        val extractor = object : StreamExtractor {
            override suspend fun prewarm() = Unit

            override suspend fun extract(
                videoId: String,
                hints: ContentHints,
                excludedClients: Set<String>,
                audioQuality: AudioQuality,
                clientPlaybackNonce: String,
            ): ExtractedStream? {
                started = true
                return null
            }
        }

        val failure = runCatching { InnerTubeXFallbackAudioResolver(extractor).resolveAudio("bad-id") }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertFalse(started)
    }

    @Test
    fun explicitAgeRestrictionIsNotReclassifiedAsFallbackEligible() = runSuspend {
        val resolver = InnerTubeXFallbackAudioResolver(
            FakeExtractor(
                stream = null,
                failure = StreamResolveException(
                    reason = StreamResolveException.Reason.AGE_RESTRICTED,
                    message = "restricted",
                ),
            ),
        )

        assertFailsWith<ProviderAccessRefusalException> { resolver.resolveAudio("abc12345678") }
    }

    @Test
    fun missingMusicVideoMetadataAllowsTheConfiguredFallbackResolver() = runSuspend {
        val primary = InnerTubeXFallbackAudioResolver(
            FakeExtractor(
                stream = null,
                failure = StreamResolveException(
                    reason = StreamResolveException.Reason.NO_MUSIC_VIDEO,
                    message = "No music video metadata",
                ),
            ),
        )
        var fallbackCalled = false
        val fallback = AudioStreamResolver { videoId ->
            fallbackCalled = true
            ResolvedAudioStream(
                videoId = videoId,
                url = "https://media.example/audio",
                headers = emptyMap(),
                bitrateKbps = 128,
                mimeType = "audio/mp4",
                profileId = "fallback-test",
            )
        }

        val stream = AudioResolutionChain(primary, fallback).resolveAudio("abc12345678")

        assertTrue(fallbackCalled)
        assertEquals("fallback-test", stream?.profileId)
    }
}

private fun runSuspend(block: suspend () -> Unit) {
    kotlinx.coroutines.runBlocking { block() }
}
