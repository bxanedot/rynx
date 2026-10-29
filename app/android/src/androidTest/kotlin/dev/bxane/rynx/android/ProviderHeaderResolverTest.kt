package dev.bxane.rynx.android

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class ProviderHeaderResolverTest {
    @Test
    fun providerHeadersApplyOnlyToTheirResolvedUrlWithoutMutatingTheOriginalSpec() {
        val uri = "https://media.example/audio"
        val headers = mapOf("Origin" to "https://music.youtube.com", "X-Session" to "ephemeral")
        val registry = StreamRequestHeaders().apply { register(uri, headers) }
        val original = DataSpec.Builder()
            .setUri(uri)
            .setHttpRequestHeaders(mapOf("X-Caller" to "preserved"))
            .build()

        val resolved = ProviderHeaderResolver(registry::headersFor).resolveDataSpec(original)

        assertEquals(headers, resolved.httpRequestHeaders)
        assertEquals(mapOf("X-Caller" to "preserved"), original.httpRequestHeaders)
        assertEquals(uri, resolved.uri.toString())
    }

    @Test
    fun unregisteredUrlKeepsTheOriginalMedia3DataSpec() {
        val original = DataSpec.Builder()
            .setUri("https://media.example/other")
            .setHttpRequestHeaders(mapOf("X-Caller" to "preserved"))
            .build()

        val resolved = ProviderHeaderResolver { emptyMap() }.resolveDataSpec(original)

        assertSame(original, resolved)
        assertEquals(mapOf("X-Caller" to "preserved"), resolved.httpRequestHeaders)
    }
}
