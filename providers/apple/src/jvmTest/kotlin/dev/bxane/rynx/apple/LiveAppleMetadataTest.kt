package dev.bxane.rynx.apple

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.*

/** Explicit opt-in live contract checks; deterministic fixtures remain the default CI coverage. */
class LiveAppleMetadataTest {
    @Test fun actualPublicGenresChartsAndStructuredPlaylists() {
        assumeTrue(System.getenv("RYNX_APPLE_LIVE")=="1")
        System.getenv("RYNX_TEST_TRUSTSTORE")?.let {System.setProperty("javax.net.ssl.trustStore",it)}
        runBlocking {
            val http=HttpClient(OkHttp) {engine {System.getenv("HTTPS_PROXY")?.takeIf {it.isNotBlank()}?.let {proxy=ProxyBuilder.http(Url(it))}}}
            val client=AppleMetadataClient(http)
            try {
                val genres=client.genres("us");assertTrue(genres.any {it.name=="Pop"});assertTrue(genres.size>20)
                val browse=client.browseGenres("us");assertTrue(browse.size>=50);assertTrue(browse.any {it.name=="Acoustic" && it.id.startsWith("curator:")})
                val chart=client.topSongs("us");assertEquals(100,chart.tracks.size);assertEquals(1,chart.tracks.first().chartRank)
                val global=client.playlist(AppleMetadataClient.Global,"us");assertTrue(global.tracks.size>=90);assertTrue(global.tracks.all {it.artist.isNotBlank()})
                val cities=client.cityCharts("us");assertTrue(cities.any {it.title.contains("New York",true)})
                val pop=client.genreChart(genres.first {it.name=="Pop"},"us");assertTrue(pop.tracks.size>=40)
                println("LIVE APPLE: ${genres.size} genres; ${browse.size} browse categories; ${chart.tracks.size} storefront songs; ${global.tracks.size} global songs; ${cities.size} cities; ${pop.tracks.size} Pop songs")
            } finally {client.close();http.close()}
        }
    }
}
