package dev.bxane.rynx.playback.discovery

import dev.bxane.rynx.playback.catalog.*
import kotlin.test.*

class DiscoveryRankingTest {
    private fun song(id:String,artist:String="Artist",genres:List<String> = listOf("14"),rank:Int=10)=MusicMetadata(id,"Song $id",artist,genreIds=genres,chartRank=rank)
    @Test fun affinityBeatsUnrelatedPopularityButRecentReplayIsPenalized() {
        val a=song("a","Favorite",rank=50);val b=song("b","Other",rank=1);val c=song("c","Favorite",rank=5)
        val history=listOf(ListeningSignal("old","Favorite",listOf("14"),1000),ListeningSignal("c","Favorite",listOf("14"),1900))
        val result=RynxRecommendations.rank(listOf(a,b,c),history,emptySet(),nowMillis=2000)
        assertEquals("a",result.first().track.id)
        assertTrue(result.indexOfFirst {it.track.id=="c"}>0)
        assertTrue(result.first().reason.isNotBlank())
    }
    @Test fun dailyRankingIsDeterministicUniqueAndDiverse() {
        val tracks=(1..10).map {song("$it",if(it<6)"Same" else "Other $it",rank=it)}
        val result=RynxRecommendations.rank(tracks+tracks,emptyList(),emptySet(),0)
        assertEquals(10,result.size);assertEquals(result,RynxRecommendations.rank(tracks,emptyList(),emptySet(),0))
        assertTrue(result.take(4).map {it.track.artist}.distinct().size>1)
    }
    @Test fun matchingRejectsDifferentArtistCoversAndDurationMismatch() {
        val target=MusicMetadata("apple:1","Moonlight","bxane",durationMillis=180000)
        fun candidate(id:String,title:String,artist:String,duration:Long)=CatalogItem(CatalogProvider.YouTubeMusic,CatalogItemType.Track,id,title,artist,duration)
        val valid=candidate("ok","Moonlight (Official Audio)","bxane",180)
        assertEquals(valid,MetadataMatching.bestPlaybackMatch(target,listOf(candidate("cover","Moonlight cover","Other",180),valid)))
        assertNull(MetadataMatching.bestPlaybackMatch(target,listOf(candidate("wrong","Moonlight","bxane",900))))
        assertNull(MetadataMatching.bestPlaybackMatch(target,listOf(candidate("cover","Moonlight","Other",180))))
    }
    @Test fun trendingUsesActualPreviousRanksAndNewReleasesRespectDateWindow() {
        val previous=listOf(song("a",rank=1),song("b",rank=50))
        val current=listOf(song("a",rank=2),song("b",rank=10),song("new",rank=3))
        assertEquals(listOf("b"),ChartTrends.rising(current,previous).map {it.id})
        assertTrue(ChartTrends.rising(current,emptyList()).isEmpty())
    }
    @Test fun alternateRecordingQualifiersCannotSilentlyChangeTheRequestedSong() {
        val target=MusicMetadata("apple:1","Dancing in the Dark","Artist")
        for(version in listOf("Acoustic","Radio Edit","Piano Version","Unplugged","Extended Mix")) {
            val candidate=CatalogItem(CatalogProvider.YouTubeMusic,CatalogItemType.Track,"variant","Dancing in the Dark ($version)","Artist")
            assertNull(MetadataMatching.bestPlaybackMatch(target,listOf(candidate)),version)
        }
    }
    @Test fun listeningDoesNotEraseAnAvailableYoutubeFallbackShelf() {
        val fallback=MusicMetadata("yt:a","Song","Artist",source=MetadataSource.YouTubeMusic,playbackId="a")
        val feed=DiscoveryState(enabled=true,sections=listOf(HomeSection("for-you","Discover","Fallback",HomeSectionKind.Recommendations,listOf(fallback))))
        val updated=rerankDiscovery(feed,listOf(ListeningSignal("yt:a","Artist",emptyList(),1000)),2000)
        assertEquals(listOf(fallback),updated.sections.single().tracks)
    }

    @Test fun dailyChartsRefreshAcrossDaysAndFailuresBackOffInsteadOfLooping() {
        val day=86_400_000L
        val yesterday=DiscoveryState(enabled=true,updatedMillis=day)
        assertTrue(shouldRefreshDiscovery(yesterday,day,2*day))
        assertFalse(shouldRefreshDiscovery(yesterday,2*day-1000,2*day))
        assertFalse(shouldRefreshDiscovery(yesterday.copy(loading=true),day,2*day))
        assertFalse(shouldRefreshDiscovery(yesterday.copy(updatedMillis=2*day),day,2*day+1000))
        assertTrue(shouldRefreshDiscovery(yesterday.copy(updatedMillis=2*day,error="offline"),day,2*day+1000))
    }

}
