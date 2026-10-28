package dev.bxane.rynx.vyonex

import com.vyonex.VyonExListItem
import com.vyonex.VyonExResultKind
import com.vyonex.VyonExSearchResult
import com.vyonex.VyonExThumbnail
import com.vyonex.VyonExListPage
import dev.bxane.rynx.playback.catalog.CatalogItem
import dev.bxane.rynx.playback.catalog.CatalogItemType
import dev.bxane.rynx.playback.catalog.CatalogPage
import dev.bxane.rynx.playback.catalog.CatalogProvider

/** Maps documented VyonEx search/list results into provider-neutral playback catalog data. */
object VyonExCatalogMapper {
    fun mapSearchResult(result: VyonExSearchResult): CatalogItem = CatalogItem(
        provider = CatalogProvider.YouTubeMusic,
        type = result.kind.toCatalogType(),
        mediaId = result.id,
        title = result.title?.takeIf { it.isNotBlank() } ?: "Untitled item",
        artist = result.channelName?.takeIf { it.isNotBlank() },
        durationSeconds = result.durationSeconds.nonNegativeOrNull(),
        artworkUrl = result.thumbnails.largestArtwork(),
    )

    fun mapListItem(item: VyonExListItem): CatalogItem = CatalogItem(
        provider = CatalogProvider.YouTubeMusic,
        type = CatalogItemType.Track,
        mediaId = item.videoId,
        title = item.title?.takeIf { it.isNotBlank() } ?: "Untitled item",
        artist = item.channelName?.takeIf { it.isNotBlank() },
        durationSeconds = item.durationSeconds.nonNegativeOrNull(),
        artworkUrl = item.thumbnails.largestArtwork(),
    )

    fun mapListPage(page: VyonExListPage): CatalogPage = CatalogPage(
        items = page.items.map(::mapListItem),
        continuation = page.continuation,
    )

    private fun VyonExResultKind.toCatalogType(): CatalogItemType = when (this) {
        VyonExResultKind.VIDEO -> CatalogItemType.Track
        VyonExResultKind.PLAYLIST -> CatalogItemType.Playlist
        VyonExResultKind.ALBUM -> CatalogItemType.Album
        VyonExResultKind.ARTIST -> CatalogItemType.Artist
        VyonExResultKind.CHANNEL -> CatalogItemType.Channel
    }

    private fun Long?.nonNegativeOrNull() = this?.takeIf { it >= 0 }

    private fun List<VyonExThumbnail>.largestArtwork(): String? =
        filter { it.url.isNotBlank() && it.width > 0 && it.height > 0 }
            .maxByOrNull { it.width.toLong() * it.height.toLong() }
            ?.url
}
