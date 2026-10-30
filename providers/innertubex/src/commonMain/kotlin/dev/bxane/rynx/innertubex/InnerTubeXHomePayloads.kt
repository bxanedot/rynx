package dev.bxane.rynx.innertubex

import dev.bxane.rynx.playback.discovery.HomeSection
import dev.bxane.rynx.playback.discovery.HomeSectionKind
import dev.bxane.rynx.playback.discovery.MusicMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parses actual WEB_REMIX FEmusic_home shelves. Only rows with playable watch
 * endpoints are represented as songs. Playlist/album links are not invented as tracks.
 */
internal object InnerTubeXHomePayloads {
    private val json = Json { ignoreUnknownKeys = true }
    private val videoId = Regex("^[A-Za-z0-9_-]{11}$")

    fun parse(body: String): List<HomeSection> {
        val root = json.parseToJsonElement(body)
        val shelves = mutableListOf<JsonObject>()

        fun collect(value: JsonElement) {
            when(value) {
                is JsonArray -> value.forEach(::collect)
                is JsonObject -> {
                    value["musicCarouselShelfRenderer"]?.let { (it as? JsonObject)?.let(shelves::add) }
                    value["musicShelfRenderer"]?.let { (it as? JsonObject)?.let(shelves::add) }
                    value.values.forEach(::collect)
                }
                else -> Unit
            }
        }
        collect(root)

        return shelves.mapIndexedNotNull { index,shelf ->
            val title = when {
                "musicCarouselShelfBasicHeaderRenderer" in (shelf.obj("header") ?: emptyMap<String,JsonElement>()) ->
                    text(shelf.obj("header")?.obj("musicCarouselShelfBasicHeaderRenderer")?.get("title"))
                else -> text(shelf["title"])
            }?.takeIf(String::isNotBlank) ?: return@mapIndexedNotNull null
            val entries = (shelf["contents"] as? JsonArray).orEmpty()
            val tracks = entries.mapNotNull { entry ->
                val obj = entry as? JsonObject ?: return@mapNotNull null
                val renderer = obj.obj("musicTwoRowItemRenderer")
                    ?: obj.obj("musicResponsiveListItemRenderer")
                    ?: return@mapNotNull null
                track(renderer)
            }.distinctBy(MusicMetadata::playbackId).take(20)
            if(tracks.isEmpty()) null
            else HomeSection(
                id="ytm-home-$index",
                title=title,
                subtitle="YouTube Music",
                kind=HomeSectionKind.Trending,
                tracks=tracks,
            )
        }
    }

    private fun track(renderer: JsonObject): MusicMetadata? {
        val id = watchId(renderer)?.takeIf(videoId::matches) ?: return null
        val columns = (renderer["flexColumns"] as? JsonArray).orEmpty()
        val songTitle = text(renderer["title"])
            ?: text((columns.getOrNull(0) as? JsonObject)?.obj("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
            ?: return null
        val artist = text(renderer["subtitle"])
            ?: text((columns.getOrNull(1) as? JsonObject)?.obj("musicResponsiveListItemFlexColumnRenderer")?.get("text"))
            ?: "YouTube Music"
        val art = largestArtwork(renderer)
        return MusicMetadata(
            id="yt:$id", title=songTitle, artist=artist, artworkUrl=art,
            playbackId=id, sourceUrl="https://music.youtube.com/watch?v=$id",
        )
    }

    private fun watchId(root: JsonElement): String? {
        fun find(value: JsonElement): String? = when(value) {
            is JsonArray -> value.firstNotNullOfOrNull(::find)
            is JsonObject -> {
                (value["watchEndpoint"] as? JsonObject)?.let { (it["videoId"] as? JsonPrimitive)?.contentOrNull }
                    ?: value.values.firstNotNullOfOrNull(::find)
            }
            else -> null
        }
        return find(root)
    }

    private fun largestArtwork(root: JsonElement): String? {
        val covers=mutableListOf<Triple<String,Int,Int>>()
        fun visit(value: JsonElement) {
            when(value) {
                is JsonArray -> value.forEach(::visit)
                is JsonObject -> {
                    val url=(value["url"] as? JsonPrimitive)?.contentOrNull
                    if(url?.startsWith("https://")==true &&
                        (url.contains("googleusercontent.com") || url.contains("ytimg.com"))) {
                        val w=(value["width"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0
                        val h=(value["height"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0
                        covers += Triple(url,w,h)
                    }
                    value.values.forEach(::visit)
                }
                else -> Unit
            }
        }
        visit(root)
        return covers.maxByOrNull { it.second.toLong()*it.third }?.first
    }

    private fun text(element: JsonElement?): String? {
        val obj=element as? JsonObject ?: return (element as? JsonPrimitive)?.contentOrNull
        val simple=(obj["simpleText"] as? JsonPrimitive)?.contentOrNull
        if(!simple.isNullOrBlank()) return simple
        val runs=obj["runs"] as? JsonArray
        return runs?.mapNotNull { (it as? JsonObject)?.get("text")?.let { txt ->
            (txt as? JsonPrimitive)?.contentOrNull
        } }?.joinToString("")?.takeIf(String::isNotBlank)
    }

    private fun JsonObject.obj(key:String): JsonObject? = this[key] as? JsonObject
}
