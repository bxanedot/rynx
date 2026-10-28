package dev.bxane.rynx.vyonex

import dev.bxane.rynx.playback.catalog.CatalogItemType
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.net.URI
import kotlin.test.*

/** Explicit network probe of the primary engine; it never prints signed URLs or stream headers. */
class LiveYouTubeEngineTest {
    @Test fun primaryEngineSearchesMusicAndResolvesPublicAudio() {
        assumeTrue(System.getenv("RYNX_YOUTUBE_LIVE")=="1")
        System.getenv("HTTPS_PROXY")?.takeIf {it.isNotBlank()}?.let {value->
            val proxy=URI(value);System.setProperty("https.proxyHost",proxy.host);System.setProperty("https.proxyPort",proxy.port.toString())
        }
        System.getenv("RYNX_TEST_TRUSTSTORE")?.let {System.setProperty("javax.net.ssl.trustStore",it)}
        runBlocking {
            val catalog=createLoggedOutVyonExMusicCatalog()
            try {
                catalog.initialize()
                val songs=catalog.searchMusic("NCS electronic music").items.filter {it.type==CatalogItemType.Track}
                assertTrue(songs.isNotEmpty(),"Primary engine must return actual music results")
                val stream=catalog.resolveAudio(songs.first().mediaId)
                assertNotNull(stream,"Primary engine must resolve public audio")
                assertEquals("https",runCatching {URI(stream.url).scheme}.getOrNull())
                assertTrue(stream.bitrateKbps>0)
                println("LIVE VYONEX YOUTUBE: actual music search and typed public audio resolution")
            } finally {catalog.close()}
        }
    }
}
