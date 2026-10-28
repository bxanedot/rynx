package dev.bxane.rynx.android

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamRequestHeadersTest {
    @Test fun headersAreLookedUpByTheResolvedStreamUrl() {
        val registry = StreamRequestHeaders(maxEntries = 2)
        registry.register("https://media.example/one", mapOf("Origin" to "https://music.youtube.com"))

        assertEquals(
            mapOf("Origin" to "https://music.youtube.com"),
            registry.headersFor("https://media.example/one"),
        )
        assertEquals(emptyMap<String, String>(), registry.headersFor("https://media.example/other"))
    }

    @Test fun recentlyResolvedStreamsStayAvailableAndOldEntriesAreEvicted() {
        val registry = StreamRequestHeaders(maxEntries = 2)
        registry.register("https://media.example/one", mapOf("X-Stream" to "one"))
        registry.register("https://media.example/two", mapOf("X-Stream" to "two"))
        registry.register("https://media.example/three", mapOf("X-Stream" to "three"))

        assertEquals(emptyMap<String, String>(), registry.headersFor("https://media.example/one"))
        assertEquals(mapOf("X-Stream" to "two"), registry.headersFor("https://media.example/two"))
        assertEquals(mapOf("X-Stream" to "three"), registry.headersFor("https://media.example/three"))
    }
}
