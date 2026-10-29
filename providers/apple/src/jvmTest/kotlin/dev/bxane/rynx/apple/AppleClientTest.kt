package dev.bxane.rynx.apple

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlin.test.*

class AppleClientTest {
    @Test fun rejectsUntrustedHostsAndNeverUsesStaleCacheForCancellation()=runBlocking {
        val cache=object:AppleDocumentCache {
            override fun read(url:String)=CachedAppleDocument("saved",0)
            override fun write(url:String,value:CachedAppleDocument){}
        }
        val client=AppleMetadataClient(HttpClient(MockEngine {throw CancellationException("cancelled")}),cache,{10_000})
        try {
            assertFailsWith<IllegalArgumentException>{client.document("https://evil.test/metadata")}
            assertFailsWith<CancellationException>{client.document("https://itunes.apple.com/lookup?id=1",0)}
            Unit
        } finally {client.close()}
    }
    @Test fun lookupMapsByIdInsteadOfDependingOnResponseOrder()=runBlocking {
        val client=AppleMetadataClient(HttpClient(MockEngine {respond("""{"results":[{"kind":"song","trackId":2,"trackName":"Two","artistName":"B"},{"kind":"song","trackId":1,"trackName":"One","artistName":"A"}]}""",headers=headersOf(HttpHeaders.ContentType,"application/json"))}))
        try {assertEquals(setOf("apple:1","apple:2"),client.lookup(listOf("1","2"),"us").map {it.id}.toSet())} finally {client.close()}
    }
    @Test fun songSearchUsesApplePublicEndpointAndKeepsRealArtwork()=runBlocking {
        var url:Url?=null
        val client=AppleMetadataClient(HttpClient(MockEngine {request->
            url=request.url
            respond("""{"results":[{"kind":"song","trackId":4,"trackName":"Blue","artistName":"Artist","artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/a/100x100bb.jpg"}]}""",headers=headersOf(HttpHeaders.ContentType,"application/json"))
        }))
        try {
            val result=client.searchSongs("blue & gold","gb",55).single()
            assertEquals("itunes.apple.com",url?.host)
            assertEquals("blue & gold",url?.parameters?.get("term"))
            assertEquals("song",url?.parameters?.get("entity"))
            assertEquals("gb",url?.parameters?.get("country"))
            assertEquals("55",url?.parameters?.get("limit"))
            assertEquals("https://is1-ssl.mzstatic.com/image/thumb/a/1200x1200bb.jpg",result.artworkUrl)
        } finally {client.close()}
    }
    @Test fun mustRevalidateAndNoStoreAreRespected()=runBlocking {
        var written:CachedAppleDocument?=null
        val cache=object:AppleDocumentCache {
            override fun read(url:String)=written
            override fun write(url:String,value:CachedAppleDocument){written=value}
        }
        var calls=0
        val http=HttpClient(MockEngine {
            calls++
            if(calls>1)respondError(HttpStatusCode.ServiceUnavailable)
            else respond("fresh",headers=headersOf(HttpHeaders.CacheControl,"max-age=0, must-revalidate"))
        })
        val client=AppleMetadataClient(http,cache,{10000})
        try {
            assertEquals("fresh",client.document("https://itunes.apple.com/test"))
            assertTrue(written!!.mustRevalidate)
            assertFails {client.document("https://itunes.apple.com/test")}
            Unit
        } finally {client.close()}
    }

    @Test fun emptyStorefrontEnrichmentIsUnavailableRatherThanAnEmptySuccess()=runBlocking {
        val markup="""<script type="application/ld+json">{"@type":"MusicPlaylist","name":"Chart","track":[{"name":"Song","url":"https://music.apple.com/us/song/song/123"}]}</script>"""
        val client=AppleMetadataClient(HttpClient(MockEngine {request->respond(if(request.url.host=="itunes.apple.com")"{\"results\":[]}" else markup)}))
        try {assertFails {client.playlist(AppleMetadataClient.Global,"us")};Unit} finally {client.close()}
    }

}
