package dev.bxane.rynx.playback.discovery

import kotlin.math.*

/** Original local content-based ranking. No account data or private provider algorithm is used. */
object RynxRecommendations {
    fun rank(candidates:List<MusicMetadata>,history:List<ListeningSignal>,favoriteIds:Set<String>,nowMillis:Long,limit:Int=30):List<RecommendedTrack> {
        val artists=mutableMapOf<String,Double>();val genres=mutableMapOf<String,Double>()
        history.takeLast(500).forEach {event->
            val ageDays=((nowMillis-event.playedAtMillis).coerceAtLeast(0)/86_400_000.0)
            val weight=exp(-ageDays/21.0)*(if(event.skipped)-.6 else 1.0)
            val artist=event.artist.lowercase().trim()
            if(artist.isNotBlank())artists[artist]=(artists[artist]?:0.0)+weight
            event.genreIds.forEach {genres[it]=(genres[it]?:0.0)+weight}
        }
        val maxArtist=artists.values.maxOrNull()?.coerceAtLeast(1.0)?:1.0
        val maxGenre=genres.values.maxOrNull()?.coerceAtLeast(1.0)?:1.0
        val remaining=candidates.distinctBy {it.id}.map {track->
            val affinity=(artists[track.artist.lowercase().trim()]?:0.0)/maxArtist
            val genre=track.genreIds.map {(genres[it]?:0.0)/maxGenre}.maxOrNull()?:0.0
            val popularity=1.0/sqrt((track.chartRank?:100).coerceAtLeast(1).toDouble())
            val last=history.filter {it.trackId==track.id}.maxOfOrNull {it.playedAtMillis}
            val replay=last?.let {1.8*exp(-((nowMillis-it).coerceAtLeast(0)/86_400_000.0)/2.0)}?:0.0
            val release=track.releaseDate?.take(10)?.let {runCatching {kotlin.time.Instant.parse("${it}T00:00:00Z").toEpochMilliseconds()}.getOrNull()}
            val age=release?.let {(nowMillis-it)/86_400_000.0}
            val fresh=if(age!=null && age>=0 && age<=28).12*(1-age/29) else 0.0
            RecommendedTrack(track,affinity*1.2+genre*.25+popularity*.3+fresh+(if(track.id in favoriteIds).3 else 0.0)-replay,
                when {affinity>.15->"Because you listen to ${track.artist}";genre>.15->"From genres you enjoy";fresh>0->"A new release to discover";else->"Popular in your discovery mix"})
        }.toMutableList()
        return buildList {
            while(remaining.isNotEmpty() && size<limit.coerceIn(0,100)) {
                val recentArtists=takeLast(3).map {it.track.artist.lowercase()}
                val next=remaining.sortedWith(compareByDescending<RecommendedTrack> {candidate->
                    candidate.score-recentArtists.count {it==candidate.track.artist.lowercase()}*.8
                }.thenBy {it.track.id}).first()
                add(next);remaining.remove(next)
            }
        }
    }
}
object ChartTrends {
    fun rising(current:List<MusicMetadata>,previous:List<MusicMetadata>):List<MusicMetadata> {
        val ranks=previous.mapNotNull {t->t.chartRank?.let {t.id to it}}.toMap()
        return current.filter {t->t.chartRank!=null && ranks[t.id]?.let {it>t.chartRank}==true}
            .sortedByDescending {ranks.getValue(it.id)-(it.chartRank?:0)}
    }
}

/** Update the recommendation shelf from independent discovery candidates. */
fun rerankDiscovery(feed:DiscoveryState,signals:List<ListeningSignal>,nowMillis:Long):DiscoveryState {
    val candidates=feed.sections.filter {it.kind!=HomeSectionKind.Recommendations}.flatMap {it.tracks}.distinctBy {it.id}
    if(candidates.isEmpty())return feed
    val ranked=RynxRecommendations.rank(candidates,signals,emptySet(),nowMillis)
    return feed.copy(sections=feed.sections.map {if(it.id=="for-you")it.copy(tracks=ranked.map {r->r.track},subtitle="Your listening, new discoveries") else it})
}
