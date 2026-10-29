package dev.bxane.rynx.innertubex

import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.StreamExtractor
import com.metrolist.innertubex.extraction.StreamResolveException
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import dev.bxane.rynx.playback.catalog.AudioStreamResolver
import dev.bxane.rynx.playback.catalog.ProviderAccessRefusalException
import dev.bxane.rynx.playback.catalog.ResolvedAudioStream
import io.ktor.client.HttpClient

/**
 * Primary Rynx resolver backed by InnerTubeX. Rynx's current Media3 path accepts ordinary HTTPS media
 * streams only, so HLS, SABR, and bounded-range variants are deliberately declined for the next
 * provider in the chain to try.
 */
class InnerTubeXFallbackAudioResolver internal constructor(
    private val extractor: StreamExtractor,
    private val closeSession: () -> Unit = {},
) : AudioStreamResolver, AutoCloseable {
    override suspend fun resolveAudio(videoId: String): ResolvedAudioStream? {
        require(videoId.matches(YOUTUBE_ID)) { "Expected a YouTube video ID" }
        val stream = try {
            extractor.extract(
                videoId = videoId,
                hints = ContentHints(wantVideo = false)
                    .withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = false),
                audioQuality = AudioQuality.HIGH,
            )
        } catch (refusal: StreamResolveException) {
            if (refusal.reason in NO_FALLBACK_REASONS) throw ProviderAccessRefusalException(refusal)
            throw refusal
        } ?: return null

        if (stream.videoId != videoId || stream.requireBoundedRange || stream.useRangeChunks || stream.sabrBootstrap != null) {
            return null
        }
        val url = stream.audioUrl.takeIf { it.startsWith("https://") } ?: return null
        val mimeType = stream.mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.takeIf { it.startsWith("audio/") }
            ?: return null

        return ResolvedAudioStream(
            videoId = stream.videoId,
            url = url,
            headers = stream.headers.toMap(),
            bitrateKbps = stream.bitrate.toKbps(),
            mimeType = mimeType,
            profileId = "innertubex-${stream.profileId}",
        )
    }

    /** Releases InnerTubeX's session work. The caller remains responsible for its HttpClient. */
    override fun close() = closeSession()

    private fun Int?.toKbps(): Int = this?.takeIf { it > 0 }?.let { (it / 1_000).coerceAtLeast(1) } ?: 0

    private companion object {
        val YOUTUBE_ID = Regex("^[A-Za-z0-9_-]{11}$")
        val NO_FALLBACK_REASONS = setOf(
            StreamResolveException.Reason.EXPLICIT_UNSUPPORTED,
            StreamResolveException.Reason.UNAVAILABLE,
            StreamResolveException.Reason.AGE_RESTRICTED,
            StreamResolveException.Reason.NO_MUSIC_VIDEO,
        )
    }
}

/** The supplied HttpClient remains owned by the caller and should be shared across requests. */
fun createInnerTubeXFallbackAudioResolver(httpClient: HttpClient): InnerTubeXFallbackAudioResolver {
    val innerTube = InnerTube(httpClient)
    val configStore = RemotePlayerConfigStore(httpClient, PlayerConfigRepository.disabled())
    val cipherService = YouTubeCipherService(httpClient, configStore)
    val parser = YtConfigParserImpl(httpClient, innerTube, configStore, cipherService = cipherService)
    return InnerTubeXFallbackAudioResolver(
        extractor = InnerTubeExtractor(parser, cipherService, innerTube),
        closeSession = innerTube::close,
    )
}
