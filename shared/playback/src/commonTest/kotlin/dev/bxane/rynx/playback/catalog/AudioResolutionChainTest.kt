package dev.bxane.rynx.playback.catalog

import kotlin.coroutines.cancellation.CancellationException
import dev.bxane.rynx.playback.runSuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AudioResolutionChainTest {
    private class FakeResolver(
        private val result: ResolvedAudioStream? = null,
        private val failure: Exception? = null,
    ) : AudioStreamResolver {
        var calls = 0
            private set

        override suspend fun resolveAudio(videoId: String): ResolvedAudioStream? {
            calls++
            failure?.let { throw it }
            return result
        }
    }

    private fun stream(id: String) = ResolvedAudioStream(
        videoId = id,
        url = "https://media.example/$id",
        headers = emptyMap(),
        bitrateKbps = 128,
        mimeType = "audio/mp4",
        profileId = "test",
    )

    @Test
    fun successfulPrimaryResolutionDoesNotInvokeFallback() = runSuspend {
        val primary = FakeResolver(stream("primary"))
        val fallback = FakeResolver(stream("fallback"))

        val result = AudioResolutionChain(primary, fallback).resolveAudio("video-id")

        assertEquals("primary", result?.videoId)
        assertEquals(1, primary.calls)
        assertEquals(0, fallback.calls)
    }

    @Test
    fun missingOrFailedPrimaryUsesFallback() = runSuspend {
        val fallback = FakeResolver(stream("fallback"))

        val missing = AudioResolutionChain(FakeResolver(), fallback).resolveAudio("missing")
        val failed = AudioResolutionChain(
            FakeResolver(failure = IllegalStateException("primary unavailable")), fallback,
        ).resolveAudio("failed")

        assertEquals("fallback", missing?.videoId)
        assertEquals("fallback", failed?.videoId)
        assertEquals(2, fallback.calls)
    }

    @Test
    fun cancellationNeverStartsFallback() = runSuspend {
        val fallback = FakeResolver(stream("fallback"))

        assertFailsWith<CancellationException> {
            AudioResolutionChain(
                FakeResolver(failure = CancellationException("selection cancelled")), fallback,
            ).resolveAudio("video-id")
        }

        assertEquals(0, fallback.calls)
    }

    @Test
    fun twoMissingResolversReturnNull() = runSuspend {
        assertNull(AudioResolutionChain(FakeResolver(), FakeResolver()).resolveAudio("video-id"))
    }
}
