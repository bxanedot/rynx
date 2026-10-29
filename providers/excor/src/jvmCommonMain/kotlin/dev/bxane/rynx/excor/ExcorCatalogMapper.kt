package dev.bxane.rynx.excor

import com.bxane.excor.ExcorListItem
import com.bxane.excor.ExcorResultKind
import com.bxane.excor.ExcorSearchResult
import com.bxane.excor.ExcorThumbnail
import com.bxane.excor.ExcorListPage
import dev.bxane.rynx.playback.catalog.CatalogItem
import dev.bxane.rynx.playback.catalog.CatalogItemType
import dev.bxane.rynx.playback.catalog.CatalogPage
import dev.bxane.rynx.playback.catalog.CatalogProvider

/** Maps documented Excor search/list results into provider-neutral playback catalog data. */
object ExcorCatalogMapper {
    fun mapSearchResult(result: ExcorSearchResult): CatalogItem = CatalogItem(
        provider = CatalogProvider.YouTubeMusic,
        type = result.kind.toCatalogType(),
        mediaId = result.id,
        title = result.title?.takeIf { it.isNotBlank() } ?: "Untitled item",
        artist = result.channelName?.takeIf { it.isNotBlank() },
        durationSeconds = result.durationSeconds.nonNegativeOrNull(),
        artworkUrl = result.thumbnails.largestArtwork(),
    )

    fun mapListItem(item: ExcorListItem): CatalogItem = CatalogItem(
        provider = CatalogProvider.YouTubeMusic,
        type = CatalogItemType.Track,
        mediaId = item.videoId,
        title = item.title?.takeIf { it.isNotBlank() } ?: "Untitled item",
        artist = item.channelName?.takeIf { it.isNotBlank() },
        durationSeconds = item.durationSeconds.nonNegativeOrNull(),
        artworkUrl = item.thumbnails.largestArtwork(),
    )

    fun mapListPage(page: ExcorListPage): CatalogPage = CatalogPage(
        items = page.items.map(::mapListItem),
        continuation = page.continuation,
    )

    private fun ExcorResultKind.toCatalogType(): CatalogItemType = when (this) {
        ExcorResultKind.VIDEO -> CatalogItemType.Track
        ExcorResultKind.PLAYLIST -> CatalogItemType.Playlist
        ExcorResultKind.ALBUM -> CatalogItemType.Album
        ExcorResultKind.ARTIST -> CatalogItemType.Artist
        ExcorResultKind.CHANNEL -> CatalogItemType.Channel
    }

    private fun Long?.nonNegativeOrNull() = this?.takeIf { it >= 0 }

    private fun List<ExcorThumbnail>.largestArtwork(): String? =
        filter { it.url.isNotBlank() && it.width > 0 && it.height > 0 }
            .maxByOrNull { it.width.toLong() * it.height.toLong() }
            ?.url
}
