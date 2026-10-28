package dev.bxane.rynx.vyonex

import com.vyonex.AppleMusicSong
import com.vyonex.VyonExThumbnail
import kotlin.test.*

class AppleMusicMapperTest {
    @Test fun preservesAppleIdentityAndArtworkWithoutPretendingItIsPlayableAudio() {
        val song=AppleMusicSong(123,"Moonlight","bxane",10,"Night",20,180000,1,10,1,1,"2026-10-28","Pop",false,
            listOf(VyonExThumbnail("https://example.test/small.jpg",100,100),VyonExThumbnail("https://example.test/cover.jpg",1200,1200)),"https://music.apple.com/us/song/moonlight/123")
        val metadata=VyonExAppleMetadataMapper.song(song)!!
        assertEquals("apple:123",metadata.id);assertEquals("https://example.test/cover.jpg",metadata.artworkUrl)
        assertNull(metadata.playbackId);assertEquals(180000,metadata.durationMillis)
    }
}
