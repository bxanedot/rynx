package dev.bxane.rynx.playback.discovery

/** Factual catalog metadata; playback identity remains separate from its metadata provider. */
enum class MetadataSource { AppleMusic, YouTubeMusic }
data class MusicGenre(val id:String,val name:String,val children:List<MusicGenre> = emptyList())
data class MusicMetadata(
    val id:String,val title:String,val artist:String,
    val source:MetadataSource=MetadataSource.AppleMusic,
    val album:String?=null,val artworkUrl:String?=null,val genreIds:List<String> = emptyList(),
    val genreNames:List<String> = emptyList(),val releaseDate:String?=null,val durationMillis:Long?=null,
    val sourceUrl:String?=null,val chartRank:Int?=null,val playbackId:String?=null,
)
data class ChartCollection(val id:String,val title:String,val tracks:List<MusicMetadata>,val sourceUrl:String,val updated:String?=null,val expectedTrackCount:Int=tracks.size)
data class DiscoveryRegion(val code:String,val name:String,val neighbors:List<String> = emptyList())
enum class HomeSectionKind { Recommendations, Daily100, NewSongs, ThisWeek, Recent, Trending, CityCharts, OnTour }
data class HomeSection(val id:String,val title:String,val subtitle:String,val kind:HomeSectionKind,
    val tracks:List<MusicMetadata> = emptyList(),val sourceUrl:String?=null,val unavailableReason:String?=null,
    val collections:List<ChartCollection> = emptyList(),val availabilityNote:String?=null)
data class DiscoveryState(
    val enabled:Boolean=false,val loading:Boolean=false,val region:String="us",val regions:List<DiscoveryRegion> = emptyList(),
    val genres:List<MusicGenre> = emptyList(),val sections:List<HomeSection> = emptyList(),val error:String?=null,
    val stale:Boolean=false,val updatedMillis:Long?=null,
)
data class ListeningSignal(val trackId:String,val artist:String,val genreIds:List<String>,val playedAtMillis:Long,val skipped:Boolean=false)
data class RecommendedTrack(val track:MusicMetadata,val score:Double,val reason:String)

fun ChartCollection.coverageNote():String? = if(tracks.size<expectedTrackCount)"${tracks.size} of $expectedTrackCount recordings available in this storefront" else null
