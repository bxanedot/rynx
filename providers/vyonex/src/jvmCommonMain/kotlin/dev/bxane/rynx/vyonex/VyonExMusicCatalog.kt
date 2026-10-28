package dev.bxane.rynx.vyonex

import com.vyonex.VyonEx
import com.vyonex.VyonExConfigStore
import com.vyonex.VyonExHost
import com.vyonex.VyonExLogLevel
import com.vyonex.VyonExPlaylist
import com.vyonex.VyonExSession
import okhttp3.OkHttpClient
import dev.bxane.rynx.playback.catalog.CatalogPage
import dev.bxane.rynx.playback.catalog.CatalogSearchRepository
import java.util.Locale

/** Desktop/JVM adapter around the approved VyonEx engine. It never performs extraction itself. */
class VyonExMusicCatalog(
    host: VyonExHost,
    private val releaseHost: () -> Unit = {},
) : CatalogSearchRepository {
    private val engine = VyonEx(host)

    fun init() = engine.init()
    override fun initialize() = init()

    override suspend fun searchMusic(query: String): CatalogPage {
        val normalized = query.trim()
        require(normalized.isNotEmpty()) { "Search text must not be empty" }
        return engine.searchMusic(normalized).let { page ->
            CatalogPage(
                items = page.results.map(VyonExCatalogMapper::mapSearchResult),
                continuation = page.continuation,
            )
        }
    }

    override suspend fun searchMore(continuation: String): CatalogPage {
        require(continuation.isNotBlank()) { "Search continuation must not be empty" }
        return engine.searchMore(continuation).let { page ->
            CatalogPage(
                items = page.results.map(VyonExCatalogMapper::mapSearchResult),
                continuation = page.continuation,
            )
        }
    }

    suspend fun playlist(playlistId: String): CatalogPage? {
        require(playlistId.isNotBlank()) { "Playlist ID must not be empty" }
        val playlist: VyonExPlaylist = engine.playlist(playlistId) ?: return null
        return VyonExCatalogMapper.mapListPage(playlist.firstPage)
    }

    suspend fun playlistMore(continuation: String): CatalogPage {
        require(continuation.isNotBlank()) { "Playlist continuation must not be empty" }
        return VyonExCatalogMapper.mapListPage(engine.playlistMore(continuation))
    }

    suspend fun searchAppleMusic(query:String,country:String?,limit:Int=30):List<dev.bxane.rynx.playback.discovery.MusicMetadata> =
        engine.appleMusic.searchSongs(query,country,limit).mapNotNull(VyonExAppleMetadataMapper::song)

    suspend fun appleSong(id:Long,country:String?):dev.bxane.rynx.playback.discovery.MusicMetadata? =
        engine.appleMusic.song(id,country)?.let(VyonExAppleMetadataMapper::song)

    suspend fun appleArtistImages(id:Long,country:String?):List<com.vyonex.VyonExThumbnail> = engine.appleMusic.artistImages(id,country)

    suspend fun resolveAudio(videoId: String, maxKbps: Int = 160): ResolvedAudioStream? {
        require(videoId.isNotBlank()) { "Video ID must not be empty" }
        require(maxKbps > 0) { "Maximum bitrate must be positive" }
        val stream = engine.extract(videoId, maxKbps) ?: return null
        return ResolvedAudioStream(
            videoId = stream.videoId,
            url = stream.url,
            headers = stream.headers.toMap(),
            bitrateKbps = stream.kbps,
            mimeType = stream.mimeType,
            profileId = stream.profileId,
        )
    }

    /** Call when the native media client reports a refused stream. VyonEx owns client failover. */
    fun onRefused(stream: ResolvedAudioStream): String? = engine.onRefused(stream.url)

    fun onSessionChanged() = engine.onSessionChanged()

    override fun close() {
        engine.close()
        releaseHost()
    }
}

/** Default logged-out host. Session credentials and the engine's player cache are disabled. */
fun createLoggedOutVyonExMusicCatalog(): VyonExMusicCatalog {
    val host = LoggedOutVyonExHost()
    return VyonExMusicCatalog(host, host::close)
}

/** Catalog-only factory for hosts that do not need direct stream resolution. */
fun createLoggedOutVyonExCatalog(): CatalogSearchRepository = createLoggedOutVyonExMusicCatalog()

private class LoggedOutVyonExHost : VyonExHost, AutoCloseable {
    override val httpClient = OkHttpClient.Builder().build()
    override val configStore: VyonExConfigStore = object : VyonExConfigStore {
        override val enabled = false
        override fun getString(key: String): String? = null
        override fun getLong(key: String): Long? = null
        override fun putString(key: String, value: String) = Unit
        override fun putLong(key: String, value: Long) = Unit
    }
    private val locale = Locale.getDefault()

    override suspend fun session() = VyonExSession(
        cookie = null,
        visitorData = null,
        region = locale.country.takeIf { it.length == 2 }?.uppercase(Locale.ROOT) ?: "US",
        language = locale.language.takeIf { it.isNotBlank() } ?: "en",
    )

    override fun nowMs(): Long = System.currentTimeMillis()
    override fun log(level: VyonExLogLevel, message: String, trackId: String?) = Unit

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.cache?.close()
    }
}
