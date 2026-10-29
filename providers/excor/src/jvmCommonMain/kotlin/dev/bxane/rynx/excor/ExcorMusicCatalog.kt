package dev.bxane.rynx.excor

import com.bxane.excor.Excor
import com.bxane.excor.ExcorConfigStore
import com.bxane.excor.ExcorHost
import com.bxane.excor.ExcorLogLevel
import com.bxane.excor.ExcorPlaylist
import com.bxane.excor.ExcorSession
import com.bxane.excor.ExcorUnplayableException
import okhttp3.OkHttpClient
import dev.bxane.rynx.playback.catalog.AudioStreamResolver
import dev.bxane.rynx.playback.catalog.CatalogPage
import dev.bxane.rynx.playback.catalog.CatalogSearchRepository
import dev.bxane.rynx.playback.catalog.MusicPlaybackCatalog
import dev.bxane.rynx.playback.catalog.ProviderAccessRefusalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Desktop/JVM adapter around the approved Excor engine. It never performs extraction itself. */
class ExcorMusicCatalog(
    host: ExcorHost,
    private val releaseHost: () -> Unit = {},
) : MusicPlaybackCatalog, AudioStreamResolver {
    private val engine = Excor(host)

    fun init() = engine.init()
    override fun initialize() = init()

    override suspend fun searchMusic(query: String): CatalogPage = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        require(normalized.isNotEmpty()) { "Search text must not be empty" }
        engine.searchMusic(normalized).let { page ->
            CatalogPage(
                items = page.results.map(ExcorCatalogMapper::mapSearchResult),
                continuation = page.continuation,
            )
        }
    }

    override suspend fun searchMore(continuation: String): CatalogPage = withContext(Dispatchers.IO) {
        require(continuation.isNotBlank()) { "Search continuation must not be empty" }
        engine.searchMore(continuation).let { page ->
            CatalogPage(
                items = page.results.map(ExcorCatalogMapper::mapSearchResult),
                continuation = page.continuation,
            )
        }
    }

    suspend fun playlist(playlistId: String): CatalogPage? {
        require(playlistId.isNotBlank()) { "Playlist ID must not be empty" }
        val playlist: ExcorPlaylist = engine.playlist(playlistId) ?: return null
        return ExcorCatalogMapper.mapListPage(playlist.firstPage)
    }

    suspend fun playlistMore(continuation: String): CatalogPage {
        require(continuation.isNotBlank()) { "Playlist continuation must not be empty" }
        return ExcorCatalogMapper.mapListPage(engine.playlistMore(continuation))
    }

    override suspend fun searchAppleMusic(query:String,country:String?,limit:Int):List<dev.bxane.rynx.playback.discovery.MusicMetadata> =
        withContext(Dispatchers.IO) { engine.appleMusic.searchSongs(query,country,limit).mapNotNull(ExcorAppleMetadataMapper::song) }

    override suspend fun appleSong(id:Long,country:String?):dev.bxane.rynx.playback.discovery.MusicMetadata? =
        withContext(Dispatchers.IO) { engine.appleMusic.song(id,country)?.let(ExcorAppleMetadataMapper::song) }

    suspend fun appleArtistImages(id:Long,country:String?):List<com.bxane.excor.ExcorThumbnail> =
        withContext(Dispatchers.IO) { engine.appleMusic.artistImages(id,country) }

    override suspend fun resolveAudio(videoId: String): ResolvedAudioStream? = resolveAudio(videoId, 160)

    suspend fun resolveAudio(videoId: String, maxKbps: Int): ResolvedAudioStream? = withContext(Dispatchers.IO) {
        require(videoId.isNotBlank()) { "Video ID must not be empty" }
        require(maxKbps > 0) { "Maximum bitrate must be positive" }
        val stream = try {
            engine.extract(videoId, maxKbps)
        } catch (refusal: ExcorUnplayableException) {
            throw ProviderAccessRefusalException(refusal)
        } ?: return@withContext null
        ResolvedAudioStream(
            videoId = stream.videoId,
            url = stream.url,
            headers = stream.headers.toMap(),
            bitrateKbps = stream.kbps,
            mimeType = stream.mimeType,
            profileId = stream.profileId,
        )
    }

    /** Call when the native media client reports a refused stream. Excor owns client failover. */
    fun onRefused(stream: ResolvedAudioStream): String? = engine.onRefused(stream.url)

    fun onSessionChanged() = engine.onSessionChanged()

    override fun close() {
        engine.close()
        releaseHost()
    }
}

/** Default logged-out host. Session credentials and the engine's player cache are disabled. */
fun createLoggedOutExcorMusicCatalog(): ExcorMusicCatalog {
    val host = LoggedOutExcorHost()
    return ExcorMusicCatalog(host, host::close)
}

/** Catalog-only factory for hosts that do not need direct stream resolution. */
fun createLoggedOutExcorCatalog(): CatalogSearchRepository = createLoggedOutExcorMusicCatalog()

private class LoggedOutExcorHost : ExcorHost, AutoCloseable {
    override val httpClient = OkHttpClient.Builder().build()
    override val configStore: ExcorConfigStore = object : ExcorConfigStore {
        override val enabled = false
        override fun getString(key: String): String? = null
        override fun getLong(key: String): Long? = null
        override fun putString(key: String, value: String) = Unit
        override fun putLong(key: String, value: Long) = Unit
    }
    private val locale = Locale.getDefault()

    override suspend fun session() = ExcorSession(
        cookie = null,
        visitorData = null,
        region = locale.country.takeIf { it.length == 2 }?.uppercase(Locale.ROOT) ?: "US",
        language = locale.language.takeIf { it.isNotBlank() } ?: "en",
    )

    override fun nowMs(): Long = System.currentTimeMillis()
    override fun log(level: ExcorLogLevel, message: String, trackId: String?) = Unit

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.cache?.close()
    }
}
