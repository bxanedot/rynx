package dev.bxane.rynx.apple

import dev.bxane.rynx.playback.discovery.*
import kotlinx.coroutines.*
import kotlin.time.Clock

/** Discovery orchestration and original recommendations, separate from extraction/playback. */
class AppleHomeRepository(private val client:AppleMetadataClient) {
    suspend fun load(region:String,signals:List<ListeningSignal>,favorites:Set<String>,previousRanks:List<MusicMetadata>):DiscoveryState=supervisorScope {
        client.beginRefresh()
        val location=Regions.all.firstOrNull {it.code==region}?:Regions.all.first()
        suspend fun <T> safely(block:suspend ()->T):T?=try {block()} catch(cancelled:CancellationException){throw cancelled} catch(_:Exception){null}
        val genres=async {safely {client.genres(location.code)}}
        val global=async {safely {client.playlist(AppleMetadataClient.Global,location.code)}}
        val local=async {safely {client.topSongs(location.code)}}
        val neighbors=location.neighbors.take(3).map {code->async {code to safely {client.topSongs(code)}}}
        val fresh=async {safely {client.playlist(AppleMetadataClient.NewMusicDaily,location.code)}}
        val cities=async {safely {client.cityCharts(location.code)}}
        val tours=async {safely {client.tourSetLists(location.code)}}
        val globalChart=global.await();val localChart=local.await();val neighboring=neighbors.awaitAll()
        val newSongs=fresh.await();val cityLinks=cities.await().orEmpty();val tourLinks=tours.await().orEmpty()
        val candidates=(localChart?.tracks.orEmpty()+globalChart?.tracks.orEmpty()+newSongs?.tracks.orEmpty()+neighboring.flatMap {it.second?.tracks.orEmpty()}).distinctBy {it.id}
        val now=Clock.System.now().toEpochMilliseconds()
        val recommendations=RynxRecommendations.rank(candidates,signals,favorites,now)
        val thisWeek=newSongs?.tracks.orEmpty().filter {t->t.releaseDate?.take(10)?.let {runCatching {kotlin.time.Instant.parse("${it}T00:00:00Z").toEpochMilliseconds()}.getOrNull()}?.let {now-it in 0..7*86_400_000L}==true}
        val sections=buildList {
            add(HomeSection("for-you","Made for you",if(signals.isEmpty())"Start listening to shape your Rynx mix" else "Your listening, new discoveries",HomeSectionKind.Recommendations,recommendations.map {it.track},unavailableReason=if(candidates.isEmpty())"Connect to refresh your discovery mix." else null))
            add(HomeSection("global","Daily 100 · Global","Apple Music's Top 100: Global",HomeSectionKind.Daily100,globalChart?.tracks.orEmpty(),AppleMetadataClient.Global.url,if(globalChart==null)"The global chart could not be loaded. Try refreshing." else null,availabilityNote=globalChart?.coverageNote()))
            add(HomeSection("region","Daily 100 · ${location.name}","Apple storefront most-played · a daily Rynx snapshot",HomeSectionKind.Daily100,localChart?.tracks.orEmpty(),localChart?.sourceUrl,if(localChart==null)"This region's chart is unavailable." else null))
            neighboring.forEach {(code,chart)->add(HomeSection("neighbor:$code","Daily 100 · ${Regions.name(code)}","Nearby countries · Apple storefront most-played",HomeSectionKind.Daily100,chart?.tracks.orEmpty(),chart?.sourceUrl,if(chart==null)"The chart could not be loaded." else null))}
            add(HomeSection("new-songs","Best new songs","From Apple Music's New Music Daily",HomeSectionKind.NewSongs,newSongs?.tracks.orEmpty(),AppleMetadataClient.NewMusicDaily.url,if(newSongs==null)"New Music Daily could not be loaded." else null,availabilityNote=newSongs?.coverageNote()))
            add(HomeSection("this-week","New this week","Released in the last seven days · New Music Daily",HomeSectionKind.ThisWeek,thisWeek,AppleMetadataClient.NewMusicDaily.url,if(thisWeek.isEmpty())"No dated releases from this week are available in the current selection." else null))
            val trending=ChartTrends.rising(localChart?.tracks.orEmpty(),previousRanks)
            add(HomeSection("trending","Trending songs","Rising since your previous daily regional chart",HomeSectionKind.Trending,trending,localChart?.sourceUrl,if(trending.isEmpty())"A second daily chart is needed to identify genuine rising songs." else null))
            add(HomeSection("city-charts","City Charts","Apple Music's city-by-city Top 25",HomeSectionKind.CityCharts,sourceUrl="https://music.apple.com/${location.code}/new/top-charts/city-charts",unavailableReason=if(cityLinks.isEmpty())"City charts could not be loaded." else null,collections=cityLinks.map {ChartCollection(it.id,it.title,emptyList(),it.url)}))
            add(HomeSection("on-tour","New on tour","Tour set lists from Apple Music · not concert dates",HomeSectionKind.OnTour,sourceUrl="https://music.apple.com/${location.code}/concerts",unavailableReason=if(tourLinks.isEmpty())"No current tour set lists are available for this storefront." else null,collections=tourLinks.map {ChartCollection(it.id,it.title,emptyList(),it.url)}))
        }
        DiscoveryState(true,false,location.code,Regions.all,genres.await().orEmpty(),sections,
            error=if(candidates.isEmpty())"Apple discovery is unavailable. You can still search YouTube Music." else null,
            stale=client.usedStaleCache,updatedMillis=now)
    }
}
object Regions {
    val all=listOf(
        DiscoveryRegion("us","United States",listOf("ca","mx")),DiscoveryRegion("gb","United Kingdom",listOf("ie","fr")),
        DiscoveryRegion("de","Germany",listOf("at","fr","pl")),DiscoveryRegion("in","India",listOf("np","pk","bd")),
        DiscoveryRegion("ca","Canada",listOf("us")),DiscoveryRegion("mx","Mexico",listOf("us","gt")),
        DiscoveryRegion("fr","France",listOf("be","de","es")),DiscoveryRegion("es","Spain",listOf("pt","fr")),
        DiscoveryRegion("it","Italy",listOf("fr","at","ch")),DiscoveryRegion("at","Austria",listOf("de","it","ch")),
        DiscoveryRegion("ch","Switzerland",listOf("fr","de","it")),DiscoveryRegion("pl","Poland",listOf("de","cz","sk")),
        DiscoveryRegion("nl","Netherlands",listOf("be","de")),DiscoveryRegion("be","Belgium",listOf("nl","fr","de")),
        DiscoveryRegion("ie","Ireland",listOf("gb")),DiscoveryRegion("pt","Portugal",listOf("es")),
        DiscoveryRegion("br","Brazil",listOf("ar","co","pe")),DiscoveryRegion("ar","Argentina",listOf("br","cl","uy")),
        DiscoveryRegion("jp","Japan"),DiscoveryRegion("kr","South Korea"),DiscoveryRegion("au","Australia"),DiscoveryRegion("nz","New Zealand"),
        DiscoveryRegion("za","South Africa",listOf("na","bw","mz")),DiscoveryRegion("ng","Nigeria",listOf("ne","cm","bj")),
        DiscoveryRegion("se","Sweden",listOf("no","fi")),DiscoveryRegion("no","Norway",listOf("se","fi")),
        DiscoveryRegion("fi","Finland",listOf("se","no")),DiscoveryRegion("tr","Türkiye",listOf("gr","bg","ge")),
    )
    private val extra=mapOf("np" to "Nepal","pk" to "Pakistan","bd" to "Bangladesh","gt" to "Guatemala","cz" to "Czechia","sk" to "Slovakia","co" to "Colombia","pe" to "Peru","cl" to "Chile","uy" to "Uruguay","na" to "Namibia","bw" to "Botswana","mz" to "Mozambique","ne" to "Niger","cm" to "Cameroon","bj" to "Benin","gr" to "Greece","bg" to "Bulgaria","ge" to "Georgia")
    fun name(code:String)=all.firstOrNull {it.code==code}?.name?:extra[code]?:code.uppercase()
}
