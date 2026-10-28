package dev.bxane.rynx.vyonex

import com.vyonex.AppleMusicSong
import dev.bxane.rynx.playback.discovery.MusicMetadata

/** Thin typed adapter to the owner's Apple metadata API; never resolves Apple media streams. */
object VyonExAppleMetadataMapper {
    fun song(song:AppleMusicSong):MusicMetadata? {
        val title=song.title?.takeIf {it.isNotBlank()}?:return null
        val artist=song.artist?.takeIf {it.isNotBlank()}?:return null
        val artwork=song.artwork.filter {it.width<=1200 && it.width>0 && it.height>0}.maxByOrNull {it.width.toLong()*it.height}?:song.artwork.firstOrNull()
        return MusicMetadata("apple:${song.id}",title,artist,album=song.album,artworkUrl=artwork?.url,
            genreNames=listOfNotNull(song.genre),releaseDate=song.releaseDate,durationMillis=song.durationMs,sourceUrl=song.url)
    }
}
