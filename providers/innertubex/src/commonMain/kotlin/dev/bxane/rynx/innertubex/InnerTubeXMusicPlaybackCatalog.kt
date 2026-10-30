package dev.bxane.rynx.innertubex

import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.models.YouTubeClient
import dev.bxane.rynx.playback.catalog.CatalogItem
import dev.bxane.rynx.playback.catalog.CatalogItemType
import dev.bxane.rynx.playback.catalog.CatalogPage
import dev.bxane.rynx.playback.catalog.CatalogProvider
import dev.bxane.rynx.playback.catalog.MusicPlaybackCatalog
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** YouTube Music catalog via InnerTubeX. HTTP client is caller-owned. */
class InnerTubeXMusicPlaybackCatalog internal constructor(
    private val innerTube: InnerTube,
) : MusicPlaybackCatalog {
    override suspend fun searchMusic(query: String): CatalogPage {
        require(query.isNotBlank()) { "Music search query must not be empty" }
        val response = innerTube.search(YouTubeClient.WEB_REMIX, query = query, setLogin = false)
        val body = response.bodyAsText()
        val page = InnerTubeXSearchPayloads.parse(body)
        // A nonempty provider error or unsupported payload must not look like a healthy
        // catalog containing no songs. Keep the failure visible so users can retry.
        if (page.items.isEmpty() && InnerTubeXSearchPayloads.hasCatalogProblem(body)) {
            throw IllegalStateException("YouTube Music returned an unavailable or unsupported catalog response")
        }
        return page
    }

    /** Unauthenticated, actual YouTube Music Home feed, not a synthetic search result. */
    suspend fun browseHome(): List<dev.bxane.rynx.playback.discovery.HomeSection> {
        val response = innerTube.browse(YouTubeClient.WEB_REMIX, browseId = "FEmusic_home", setLogin = false)
        return InnerTubeXHomePayloads.parse(response.bodyAsText())
    }

    override suspend fun searchMore(continuation: String): CatalogPage {
        require(continuation.isNotBlank()) { "Search continuation must not be empty" }
        val response = innerTube.search(YouTubeClient.WEB_REMIX, continuation = continuation, setLogin = false)
        return InnerTubeXSearchPayloads.parse(response.bodyAsText())
    }

    override fun close() { innerTube.close() }
}

fun createInnerTubeXMusicPlaybackCatalog(httpClient: HttpClient): InnerTubeXMusicPlaybackCatalog =
    InnerTubeXMusicPlaybackCatalog(InnerTube(httpClient))

internal object InnerTubeXSearchPayloads {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): CatalogPage {
        val root = json.parseToJsonElement(body)
        val renderers = mutableListOf<JsonObject>()
        fun collect(element: JsonElement) {
            when (element) {
                is JsonArray -> element.forEach(::collect)
                is JsonObject -> {
                    element["musicResponsiveListItemRenderer"]?.jsonObjectOrNull()?.let(renderers::add)
                    element.values.forEach(::collect)
                }
                else -> Unit
            }
        }
        collect(root)

        val items = renderers.mapNotNull(::track).distinctBy(CatalogItem::mediaId)
        return CatalogPage(items, continuation(root))
    }

    /** Provider responses sometimes put the watch endpoint inside a flex-column text run. */
    private fun track(renderer: JsonObject): CatalogItem? {
        val id = renderer.obj("playlistItemData").text("videoId")
            ?: renderer.obj("navigationEndpoint").obj("watchEndpoint").text("videoId")
            ?: findWatchVideoId(renderer)
            ?: return null
        if (!id.matches(Regex("^[A-Za-z0-9_-]{11}$"))) return null

        val columns = renderer.array("flexColumns").mapNotNull { value ->
            value.jsonObjectOrNull()?.obj("musicResponsiveListItemFlexColumnRenderer")?.obj("text")?.let(::runs)
        }
        val title = columns.firstOrNull()?.takeIf(String::isNotBlank) ?: return null
        val artist = columns.getOrNull(1)?.takeIf(String::isNotBlank)
        val duration = renderer.array("fixedColumns").asSequence()
            .mapNotNull { it.jsonObjectOrNull()?.obj("musicResponsiveListItemFlexColumnRenderer")?.obj("text")?.let(::runs) }
            .mapNotNull(::durationSeconds).firstOrNull()

        return CatalogItem(
            provider = CatalogProvider.YouTubeMusic,
            type = CatalogItemType.Track,
            mediaId = id,
            title = title,
            artist = artist,
            durationSeconds = duration,
            artworkUrl = largestThumbnail(renderer),
        )
    }

    private fun largestThumbnail(renderer: JsonObject): String? {
        val images = mutableListOf<Triple<String, Int, Int>>()
        fun visit(element: JsonElement) {
            when (element) {
                is JsonArray -> element.forEach(::visit)
                is JsonObject -> {
                    val url = element.text("url")
                    if (url?.startsWith("https://") == true &&
                        (url.contains("googleusercontent.com") || url.contains("ytimg.com"))) {
                        images += Triple(url, element.int("width") ?: 0, element.int("height") ?: 0)
                    }
                    element.values.forEach(::visit)
                }
                else -> Unit
            }
        }
        renderer["thumbnail"]?.let(::visit)
        return images.maxByOrNull { it.second.toLong() * it.third }?.first
    }

    /** Accept only watch endpoints, not arbitrary browse or playlist identifiers. */
    private fun findWatchVideoId(renderer: JsonObject): String? {
        fun visit(element: JsonElement): String? = when(element) {
            is JsonArray -> element.firstNotNullOfOrNull(::visit)
            is JsonObject -> {
                val endpoint = element["watchEndpoint"] as? JsonObject
                endpoint?.text("videoId")
                    ?: element.values.firstNotNullOfOrNull(::visit)
            }
            else -> null
        }
        return visit(renderer)
    }

    /** Detect a provider failure without leaking upstream response content to the UI. */
    fun hasCatalogProblem(body: String): Boolean {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return true
        if(root["error"] != null || root["playabilityStatus"] != null) return true
        // Only classify an entirely unrecognized response as failure. A valid empty
        // search may contain a section-list renderer with no matching songs.
        val expected = listOf("contents", "continuationContents", "onResponseReceivedCommands")
        return expected.none { it in root }
    }

    private fun continuation(root: JsonElement): String? {
        fun find(element: JsonElement): String? = when (element) {
            is JsonArray -> element.firstNotNullOfOrNull(::find)
            is JsonObject -> {
                element.obj("continuationCommand").text("token")
                    ?: element.obj("nextContinuationData").text("continuation")
                    ?: element.values.firstNotNullOfOrNull(::find)
            }
            else -> null
        }
        return find(root)
    }

    private fun runs(text: JsonObject): String = text.text("simpleText") ?: text.array("runs")
        .mapNotNull { it.jsonObjectOrNull()?.text("text") }
        .joinToString("")

    private fun durationSeconds(value: String): Long? {
        val parts = value.split(':').map(String::toLongOrNull)
        if (parts.size !in 2..3 || parts.any { it == null || it < 0 }) return null
        val digits = parts.filterNotNull()
        return if (digits.size == 2) digits[0] * 60 + digits[1] else digits[0] * 3600 + digits[1] * 60 + digits[2]
    }

    private fun JsonObject.obj(key: String): JsonObject = this[key]?.jsonObjectOrNull() ?: JsonObject(emptyMap())
    private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())
    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject
}
