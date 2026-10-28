package dev.bxane.rynx.vyonex

import com.vyonex.*
import dev.bxane.rynx.playback.catalog.*
import kotlin.test.*

class VyonExMapperTest {
    @Test fun mapsEveryMusicSearchKindAndKeepsProviderIdentity() {
        val inputs = listOf(
            VyonExSearchResult(VyonExResultKind.VIDEO,"track-id","Track","Artist",null,211,listOf(VyonExThumbnail("small",100,100),VyonExThumbnail("large",500,500))),
            VyonExSearchResult(VyonExResultKind.PLAYLIST,"playlist-id","Playlist","Curator",null,null,emptyList()),
            VyonExSearchResult(VyonExResultKind.ALBUM,"MPREb_album","Album","Artist",null,null,emptyList()),
            VyonExSearchResult(VyonExResultKind.ARTIST,"UC_artist","Artist",null,null,null,emptyList()),
            VyonExSearchResult(VyonExResultKind.CHANNEL,"UC_channel","Channel",null,null,null,emptyList()),
        )
        val mapped=inputs.map(VyonExCatalogMapper::mapSearchResult)
        assertEquals(listOf(CatalogItemType.Track,CatalogItemType.Playlist,CatalogItemType.Album,CatalogItemType.Artist,CatalogItemType.Channel),mapped.map{it.type})
        assertEquals(listOf("track-id","playlist-id","MPREb_album","UC_artist","UC_channel"),mapped.map{it.mediaId})
        assertEquals(CatalogProvider.YouTubeMusic,mapped.first().provider)
        assertEquals("large",mapped.first().artworkUrl)
        assertEquals(211L,mapped.first().durationSeconds)
    }

    @Test fun mapsPlaylistRowsAndPagesWithoutLosingContinuation() {
        val result=VyonExCatalogMapper.mapListPage(VyonExListPage(
            listOf(VyonExListItem("video","Song","Artist","channel",90,listOf(VyonExThumbnail("cover",300,300)))),"opaque-continuation"))
        assertEquals("opaque-continuation",result.continuation)
        assertEquals("video",result.items.single().mediaId)
        assertEquals("Artist",result.items.single().artist)
        assertEquals(90L,result.items.single().durationSeconds)
    }

    @Test fun absentProviderFieldsStayAbsentAndAreNeverInvented() {
        val mapped=VyonExCatalogMapper.mapSearchResult(VyonExSearchResult(VyonExResultKind.VIDEO,"video-id",null,null,null,-4,emptyList()))
        assertEquals("Untitled item",mapped.title)
        assertNull(mapped.artist)
        assertNull(mapped.durationSeconds)
        assertNull(mapped.artworkUrl)
    }

    @Test fun resolvedSignedUrlsAndHeadersAreRedactedFromString() {
        val secret="https://r1.googlevideo.com/videoplayback?token=secret-value"
        val stream=ResolvedAudioStream("video-id",secret,mapOf("Cookie" to "secret-cookie"),128,"audio/mp4","profile")
        assertFalse(stream.toString().contains("secret-value"))
        assertFalse(stream.toString().contains("secret-cookie"))
        assertTrue(stream.toString().contains("video-id"))
    }

    @Test fun mapsRefusalWithoutExposingUrlOrProviderReasonText() {
        val failure=VyonExFailureMapper.classify(VyonExUnplayableException("video-id",VyonExUnplayableReason.GEO_RESTRICTED,"UNPLAYABLE","private provider detail"))
        assertEquals(YouTubePlaybackFailure.UnavailableInRegion,failure)
        assertFalse(failure.toString().contains("private provider detail"))
    }
}
