package dev.bxane.rynx.playback.catalog

enum class CatalogProvider { YouTubeMusic }
enum class CatalogItemType { Track, Playlist, Album, Artist, Channel }

data class CatalogItem(
    val provider: CatalogProvider,
    val type: CatalogItemType,
    val mediaId: String,
    val title: String,
    val artist: String? = null,
    val durationSeconds: Long? = null,
    val artworkUrl: String? = null,
) {
    init {
        require(mediaId.isNotBlank())
        require(title.isNotBlank())
        require(durationSeconds == null || durationSeconds >= 0)
    }
}

data class CatalogPage(val items: List<CatalogItem>, val continuation: String?)

/** Search and pagination boundary shared by catalog providers and application state. */
interface CatalogSearchRepository {
    fun initialize() {}
    suspend fun searchMusic(query: String): CatalogPage
    suspend fun searchMore(continuation: String): CatalogPage
    fun close() {}
}
