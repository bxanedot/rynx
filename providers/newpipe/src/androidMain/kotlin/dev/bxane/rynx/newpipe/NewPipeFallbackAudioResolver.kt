package dev.bxane.rynx.newpipe

import okhttp3.OkHttpClient
import okhttp3.Request as HttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as ExtractorRequest
import org.schabi.newpipe.extractor.downloader.Response as ExtractorResponse
import dev.bxane.rynx.playback.catalog.AudioStreamResolver
import dev.bxane.rynx.playback.catalog.ResolvedAudioStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android-only fallback when the primary InnerTubeX resolver cannot provide a supported stream. */
object NewPipeFallbackAudioResolver : AudioStreamResolver {
    private val downloader = OkHttpNewPipeDownloader()

    @Volatile private var initialized = false

    override suspend fun resolveAudio(videoId: String): ResolvedAudioStream? = withContext(Dispatchers.IO) {
        require(videoId.matches(YOUTUBE_ID.toRegex())) { "Expected a YouTube video ID" }
        initializeOnce()
        val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()
        val audio = extractor.audioStreams
            .filter { it.content.isNotBlank() }
            .maxByOrNull { it.averageBitrate }
            ?: return@withContext null

        val format = audio.format
        val bitrate = audio.averageBitrate.coerceAtLeast(0)
        ResolvedAudioStream(
            videoId = videoId,
            url = audio.content,
            headers = emptyMap(),
            bitrateKbps = if (bitrate > 100_000) (bitrate / 1_000).coerceAtLeast(1) else bitrate,
            mimeType = format?.mimeType?.takeIf(String::isNotBlank) ?: "audio/mp4",
            profileId = "newpipe-${format?.name ?: "audio"}",
        )
    }

    @Synchronized
    private fun initializeOnce() {
        if (initialized) return
        NewPipe.init(downloader)
        initialized = true
    }

    private const val YOUTUBE_ID = "^[A-Za-z0-9_-]{11}$"
}

/** Supplies NewPipe's documented Downloader contract using the app's OkHttp client. */
private class OkHttpNewPipeDownloader : Downloader() {
    private val client = OkHttpClient.Builder().build()

    override fun execute(request: ExtractorRequest): ExtractorResponse {
        val builder = HttpRequest.Builder().url(request.url())
        request.headers().orEmpty().forEach { (name, values) ->
            values.forEach { value -> builder.addHeader(name, value) }
        }
        val bytes = request.dataToSend()
        val method = request.httpMethod()
        val body = bytes?.toRequestBody()
            ?: if (method !in setOf("GET", "HEAD")) ByteArray(0).toRequestBody() else null
        val outbound = builder.method(method, body).build()
        client.newCall(outbound).execute().use { response ->
            return ExtractorResponse(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body?.string(),
                response.request.url.toString(),
            )
        }
    }
}
