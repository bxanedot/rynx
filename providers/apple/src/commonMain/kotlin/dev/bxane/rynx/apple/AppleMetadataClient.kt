package dev.bxane.rynx.apple

import dev.bxane.rynx.playback.discovery.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Clock

/** Only public metadata. No Apple audio preview, borrowed web token or extraction protocol. */
data class CachedAppleDocument(val body:String,val savedMillis:Long,val maxAgeMillis:Long=Long.MAX_VALUE,val mustRevalidate:Boolean=false)
interface AppleDocumentCache {
    fun read(url:String):CachedAppleDocument?
    fun write(url:String,value:CachedAppleDocument)
}
class AppleMetadataClient(
    http:HttpClient,
    private val cache:AppleDocumentCache?=null,
    private val now:()->Long={Clock.System.now().toEpochMilliseconds()},
) {
    private val owner=http
    private val http=http.config {install(HttpTimeout)}
    private val permits=Semaphore(3)
    var usedStaleCache:Boolean=false;private set
    suspend fun document(url:String,ttlMillis:Long=6*60*60*1000L):String {
        val target=Url(url)
        require(target.protocol==URLProtocol.HTTPS && target.host in setOf("music.apple.com","itunes.apple.com","rss.applemarketingtools.com","rss.marketingtools.apple.com"))
        val saved=cache?.read(url)
        if(saved!=null && now()-saved.savedMillis in 0 until minOf(ttlMillis,saved.maxAgeMillis))return saved.body
        return permits.withPermit {
            try {
                val response=http.get(url) {timeout {requestTimeoutMillis=20_000;connectTimeoutMillis=10_000;socketTimeoutMillis=15_000}}
                check(response.status.isSuccess()){ "Apple metadata is unavailable (${response.status.value})" }
                require((response.contentLength()?:0)<=5_000_000){"Apple metadata document exceeds limit"}
                val body=response.bodyAsText();require(body.length<=5_000_000){"Apple metadata document exceeds limit"}
                val directives=response.headers[HttpHeaders.CacheControl].orEmpty().lowercase()
                val maxAge=if("no-cache" in directives)0L else Regex("max-age=(\\d+)").find(directives)?.groupValues?.get(1)?.toLongOrNull()?.coerceAtMost(7*86400)?.times(1000)?:Long.MAX_VALUE
                if("no-store" !in directives)cache?.write(url,CachedAppleDocument(body,now(),maxAge,"must-revalidate" in directives || "no-cache" in directives))
                body
            } catch(cancelled:CancellationException){throw cancelled}
            catch(error:Exception) {
                if(saved!=null && !saved.mustRevalidate && now()-saved.savedMillis in 0..7*86_400_000L) {usedStaleCache=true;saved.body} else throw error
            }
        }
    }
    fun beginRefresh(){usedStaleCache=false}
    suspend fun genres(region:String):List<MusicGenre> = ApplePayloads.genres(document("https://itunes.apple.com/WebObjects/MZStoreServices.woa/ws/genres?id=34&cc=${country(region)}",7*86_400_000L))
    suspend fun topSongs(region:String):ChartCollection=ApplePayloads.feed(document("https://rss.marketingtools.apple.com/api/v2/${country(region)}/music/most-played/100/songs.json"),country(region))
    suspend fun lookup(ids:List<String>,region:String):List<MusicMetadata> = ids.distinct().filter {it.isNotBlank()&&it.all(Char::isDigit)}.chunked(50).flatMap {batch->
        if(batch.isEmpty())emptyList() else ApplePayloads.lookup(document("https://itunes.apple.com/lookup?id=${batch.joinToString(",")}&country=${country(region)}",86_400_000L))
    }
    suspend fun playlist(link:ApplePlaylistLink,region:String):ChartCollection {
        val playlist=ApplePayloads.playlist(document(link.url),link.id,link.url)
        val metadata=lookup(playlist.tracks.map {it.id.removePrefix("apple:")},region).associateBy {it.id}
        val available=playlist.tracks.map {track->metadata[track.id]?.copy(chartRank=track.chartRank,sourceUrl=track.sourceUrl)?:track}.filter {it.artist.isNotBlank()}
        check(available.isNotEmpty()) {"No usable recordings are available in this Apple storefront"}
        return playlist.copy(tracks=available)
    }
    suspend fun genreChart(genre:MusicGenre,region:String):ChartCollection {
        require(genre.id.isNotBlank()&&genre.id.all(Char::isDigit))
        return ApplePayloads.genreFeed(document("https://itunes.apple.com/${country(region)}/rss/topsongs/limit=50/genre=${genre.id}/json"),country(region),genre)
    }
    suspend fun browseGenres(region:String):List<MusicGenre> = ApplePayloads.browseGenres(document("https://music.apple.com/${country(region)}/room/6456176470",86_400_000L))
    suspend fun genreCollections(genre:MusicGenre):List<ChartCollection> {
        val url=genre.browseUrl?:error("This category has no curator page")
        require(genre.id.startsWith("curator:") && url.contains("/curator/"))
        return ApplePayloads.playlistLinks(document(url)).map {ChartCollection(it.id,it.title,emptyList(),it.url)}
    }
    suspend fun cityCharts(region:String):List<ApplePlaylistLink> = ApplePayloads.playlistLinks(document("https://music.apple.com/${country(region)}/new/top-charts/city-charts",86_400_000L)).filter {it.url.contains("top-25-")}
    suspend fun tourSetLists(region:String):List<ApplePlaylistLink> = ApplePayloads.playlistLinks(document("https://music.apple.com/${country(region)}/new",86_400_000L)).filter {it.url.contains("set-list")}
    fun close(){http.close();owner.close()}
    private fun country(code:String):String=code.lowercase().also {require(it.length==2&&it.all {c->c in 'a'..'z'})}
    companion object {
        val Global=ApplePlaylistLink("pl.d25f5d1181894928af76c85c967f8f31","Top 100: Global","https://music.apple.com/us/playlist/top-100-global/pl.d25f5d1181894928af76c85c967f8f31")
        val NewMusicDaily=ApplePlaylistLink("pl.2b0e6e332fdf4b7a91164da3162127b5","New Music Daily","https://music.apple.com/us/playlist/new-music-daily/pl.2b0e6e332fdf4b7a91164da3162127b5")
    }
}
