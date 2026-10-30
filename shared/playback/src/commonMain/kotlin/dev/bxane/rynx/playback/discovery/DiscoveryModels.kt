package dev.bxane.rynx.playback.discovery

/** Factual catalog metadata; playback identity remains separate from its metadata provider. */
enum class MetadataSource { YouTubeMusic }
data class MusicGenre(val id:String,val name:String,val children:List<MusicGenre> = emptyList(),val browseUrl:String?=null)
data class MusicMetadata(
    val id:String,val title:String,val artist:String,
    val source:MetadataSource=MetadataSource.YouTubeMusic,
    val album:String?=null,val artworkUrl:String?=null,val genreIds:List<String> = emptyList(),
    val genreNames:List<String> = emptyList(),val releaseDate:String?=null,val durationMillis:Long?=null,
    val sourceUrl:String?=null,val chartRank:Int?=null,val playbackId:String?=null,
)
data class ChartCollection(val id:String,val title:String,val tracks:List<MusicMetadata>,val sourceUrl:String,val updated:String?=null,val expectedTrackCount:Int=tracks.size)
data class DiscoveryRegion(val code:String,val name:String,val neighbors:List<String> = emptyList())

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
enum class HomeSectionKind { Recommendations, Daily100, NewSongs, ThisWeek, Recent, Trending, CityCharts, OnTour }
data class HomeSection(val id:String,val title:String,val subtitle:String,val kind:HomeSectionKind,
    val tracks:List<MusicMetadata> = emptyList(),val sourceUrl:String?=null,val unavailableReason:String?=null,
    val collections:List<ChartCollection> = emptyList(),val availabilityNote:String?=null)
data class DiscoveryState(
    val enabled:Boolean=false,val loading:Boolean=false,val region:String="us",val regions:List<DiscoveryRegion> = emptyList(),
    val genres:List<MusicGenre> = emptyList(),val sections:List<HomeSection> = emptyList(),val error:String?=null,
    val stale:Boolean=false,val updatedMillis:Long?=null,val browseGenres:List<MusicGenre> = emptyList(),
)
data class ListeningSignal(val trackId:String,val artist:String,val genreIds:List<String>,val playedAtMillis:Long,val skipped:Boolean=false)
data class RecommendedTrack(val track:MusicMetadata,val score:Double,val reason:String)

fun ChartCollection.coverageNote():String? = if(tracks.size<expectedTrackCount)"${tracks.size} of $expectedTrackCount recordings available" else null

/** UTC daily snapshots refresh while visible; failures retry with a five-minute backoff. */
fun shouldRefreshDiscovery(feed:DiscoveryState,lastAttemptMillis:Long?,nowMillis:Long):Boolean {
    if(!feed.enabled || feed.loading)return false
    val due=feed.updatedMillis==null || feed.updatedMillis/86_400_000L!=nowMillis/86_400_000L || feed.error!=null
    return due && (lastAttemptMillis==null || nowMillis-lastAttemptMillis>=300_000L)
}
