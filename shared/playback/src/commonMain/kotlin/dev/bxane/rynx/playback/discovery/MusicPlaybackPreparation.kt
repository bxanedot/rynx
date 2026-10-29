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
    private val region: () -> String,
) {
    suspend fun prepareDiscovery(track: MusicMetadata): PreparedMusic {
        val enriched = if (
            (track.artworkUrl == null || track.durationMillis == null) && track.id.startsWith("apple:")
        ) {
            try {
                track.id.removePrefix("apple:").toLongOrNull()?.let { catalog.appleSong(it, region()) } ?: track
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                track
            }
        } else {
            track
        }

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
            MetadataMatching.bestPlaybackMatch(enriched, results.items) ?: throw NoMatchingRecordingException()
        }

        val metadata = enriched.copy(genreIds = track.genreIds.ifEmpty { enriched.genreIds })
        return PreparedMusic(item, metadata, streamResolver.resolveAudio(item.mediaId))
    }

    suspend fun prepareCatalog(item: CatalogItem): PreparedMusic {
        val metadata = try {
            val results = catalog.searchAppleMusic(
                query = "${item.artist.orEmpty()} ${item.title}",
                country = region(),
                limit = 5,
            )
            MetadataMatching.bestMetadataMatch(item, results)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return PreparedMusic(item, metadata, streamResolver.resolveAudio(item.mediaId))
    }
}
