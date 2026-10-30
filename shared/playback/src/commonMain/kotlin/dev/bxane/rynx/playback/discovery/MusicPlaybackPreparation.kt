package dev.bxane.rynx.playback.discovery

import dev.bxane.rynx.playback.catalog.AudioStreamResolver
import dev.bxane.rynx.playback.catalog.CatalogItem
import dev.bxane.rynx.playback.catalog.CatalogItemType
import dev.bxane.rynx.playback.catalog.CatalogProvider
import dev.bxane.rynx.playback.catalog.MusicPlaybackCatalog
import dev.bxane.rynx.playback.catalog.ResolvedAudioStream
import kotlin.coroutines.cancellation.CancellationException

data class PreparedMusic(
    val item: CatalogItem,
    val metadata: MusicMetadata?,
    val stream: ResolvedAudioStream?,
)

class NoMatchingRecordingException : Exception()

/** Provider-neutral selection and resolution policy shared by all application platforms. */
class MusicPlaybackPreparer(
    private val catalog: MusicPlaybackCatalog,
    private val streamResolver: AudioStreamResolver,
) {
    suspend fun prepareDiscovery(track: MusicMetadata): PreparedMusic {
        val item = if (track.source == MetadataSource.YouTubeMusic && track.playbackId != null) {
            CatalogItem(
                provider = CatalogProvider.YouTubeMusic,
                type = CatalogItemType.Track,
                mediaId = track.playbackId,
                title = track.title,
                artist = track.artist,
                durationSeconds = track.durationMillis?.div(1_000),
                artworkUrl = track.artworkUrl,
            )
        } else {
            val results = catalog.searchMusic("${track.artist} ${track.title}")
            MetadataMatching.bestPlaybackMatch(track, results.items) ?: throw NoMatchingRecordingException()
        }
        return PreparedMusic(item, track, streamResolver.resolveAudio(item.mediaId))
    }

    suspend fun prepareCatalog(item: CatalogItem): PreparedMusic =
        PreparedMusic(item, null, streamResolver.resolveAudio(item.mediaId))
}
