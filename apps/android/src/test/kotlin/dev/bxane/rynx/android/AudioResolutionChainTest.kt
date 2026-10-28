package dev.bxane.rynx.android

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AudioResolutionChainTest {
    @Test fun successfulPrimaryResolutionDoesNotInvokeFallback() = runBlocking {
        var fallbackCalls=0
        val chain=AudioResolutionChain<String>({ "vyonex-stream" },{ fallbackCalls++; "newpipe-stream" })

        assertEquals("vyonex-stream",chain.resolve("video-id"))
        assertEquals(0,fallbackCalls)
    }

    @Test fun fallbackResolvesMissingOrFailedPrimaryStreams() = runBlocking {
        var fallbackCalls=0
        val chain=AudioResolutionChain<String>({ null },{ fallbackCalls++; "newpipe-stream" })
        assertEquals("newpipe-stream",chain.resolve("video-id"))
        assertEquals(1,fallbackCalls)

        val failedPrimary=AudioResolutionChain<String>({ error("primary provider unavailable") },{ "recovered-stream" })
        assertEquals("recovered-stream",failedPrimary.resolve("video-id"))
    }

    @Test fun cancellationDoesNotStartFallback() {
        var fallbackCalls=0
        val chain=AudioResolutionChain<String>({ throw CancellationException("cancel search") },{ fallbackCalls++; "newpipe-stream" })

        assertThrows(CancellationException::class.java) { runBlocking { chain.resolve("video-id") } }
        assertEquals(0,fallbackCalls)
    }
}
