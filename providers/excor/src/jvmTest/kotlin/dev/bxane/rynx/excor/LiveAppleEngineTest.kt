package dev.bxane.rynx.excor

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import java.net.URI
import kotlin.test.*

class LiveAppleEngineTest {
    @Test fun approvedEngineReturnsAppleMetadataAndImages() {
        assumeTrue(System.getenv("RYNX_APPLE_LIVE")=="1")
        System.getenv("HTTPS_PROXY")?.takeIf {it.isNotBlank()}?.let {value->
            val proxy=URI(value);System.setProperty("https.proxyHost",proxy.host);System.setProperty("https.proxyPort",proxy.port.toString())
        }
        System.getenv("RYNX_TEST_TRUSTSTORE")?.let {System.setProperty("javax.net.ssl.trustStore",it)}
        runBlocking {
            val catalog=createLoggedOutExcorMusicCatalog()
            try {
                catalog.initialize()
                val songs=catalog.searchAppleMusic("Billie Eilish Wildflower","us",5)
                val match=songs.first {it.artist.contains("Billie Eilish",true)}
                assertTrue(match.title.isNotBlank());assertTrue(match.artworkUrl?.startsWith("https://")==true)
                val id=match.id.removePrefix("apple:").toLong()
                assertEquals(match.id,catalog.appleSong(id,"us")?.id)
                println("LIVE EXCOR APPLE: verified search, typed lookup and cover artwork")
            } finally {catalog.close()}
        }
    }
}
