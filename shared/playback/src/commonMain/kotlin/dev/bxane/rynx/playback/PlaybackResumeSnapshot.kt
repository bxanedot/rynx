package dev.bxane.rynx.playback

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Durable metadata for resuming user-owned local media. Resolved network URLs are never represented. */
data class PlaybackResumeTrack(
    val mediaId: String,
    val title: String,
    val artist: String,
)

/** A paused checkpoint for the existing player queue, not a second playback queue. */
data class PlaybackResumeSnapshot(
    val tracks: List<PlaybackResumeTrack>,
    val currentIndex: Int,
    val positionMillis: Long,
    val repeatMode: RepeatMode,
    val shuffleEnabled: Boolean,
    /** Timeline indexes in traversal order when shuffle is enabled. */
    val playbackOrder: List<Int> = emptyList(),
)

/** Versioned, bounded storage format. Unknown or malformed data fails closed. */
object PlaybackResumeCodec {
    private const val SCHEMA = 1
    const val MAX_TRACKS = 500
    private const val MAX_MEDIA_ID_LENGTH = 4_096
    private const val MAX_TEXT_LENGTH = 512
    private const val MAX_ENCODED_LENGTH = 512 * 1024
    private const val MAX_POSITION_MILLIS = 7L * 24 * 60 * 60 * 1_000
    private val json = Json

    fun isPersistableQueue(tracks: List<PlaybackResumeTrack>): Boolean =
        tracks.size in 1..MAX_TRACKS && tracks.all { track ->
            track.mediaId.length <= MAX_MEDIA_ID_LENGTH &&
                track.mediaId.startsWith("content://", ignoreCase = true) &&
                track.title.length <= MAX_TEXT_LENGTH &&
                track.artist.length <= MAX_TEXT_LENGTH
        }

    fun encode(snapshot: PlaybackResumeSnapshot): String? {
        if (!isValid(snapshot)) return null
        val encoded = buildJsonObject {
            put("schema", JsonPrimitive(SCHEMA))
            put("tracks", buildJsonArray {
                snapshot.tracks.forEach { track ->
                    add(buildJsonObject {
                        put("mediaId", JsonPrimitive(track.mediaId))
                        put("title", JsonPrimitive(track.title))
                        put("artist", JsonPrimitive(track.artist))
                    })
                }
            })
            put("currentIndex", JsonPrimitive(snapshot.currentIndex))
            put("positionMillis", JsonPrimitive(snapshot.positionMillis))
            put("repeatMode", JsonPrimitive(snapshot.repeatMode.name))
            put("shuffleEnabled", JsonPrimitive(snapshot.shuffleEnabled))
            put("playbackOrder", buildJsonArray {
                snapshot.playbackOrder.forEach { add(JsonPrimitive(it)) }
            })
        }.toString()
        return encoded.takeIf { it.length <= MAX_ENCODED_LENGTH }
    }

    fun decode(encoded: String?): PlaybackResumeSnapshot? = runCatching {
        require(!encoded.isNullOrBlank() && encoded.length <= 512 * 1024)
        val root = json.parseToJsonElement(encoded).jsonObject
        require(root["schema"]?.jsonPrimitive?.intOrNull == SCHEMA)
        val tracks = root["tracks"]?.jsonArray?.map { raw ->
            val item = raw.jsonObject
            PlaybackResumeTrack(
                mediaId = item.requiredString("mediaId"),
                title = item.requiredString("title"),
                artist = item.requiredString("artist"),
            )
        } ?: error("Missing tracks")
        val repeat = when (root["repeatMode"]?.jsonPrimitive?.content) {
            RepeatMode.Off.name -> RepeatMode.Off
            RepeatMode.All.name -> RepeatMode.All
            RepeatMode.One.name -> RepeatMode.One
            else -> error("Unknown repeat mode")
        }
        val snapshot = PlaybackResumeSnapshot(
            tracks = tracks,
            currentIndex = root["currentIndex"]?.jsonPrimitive?.intOrNull ?: error("Missing index"),
            positionMillis = root["positionMillis"]?.jsonPrimitive?.longOrNull ?: error("Missing position"),
            repeatMode = repeat,
            shuffleEnabled = root["shuffleEnabled"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                ?: error("Missing shuffle state"),
            playbackOrder = root["playbackOrder"]?.jsonArray?.map { value ->
                value.jsonPrimitive.intOrNull ?: error("Invalid shuffle index")
            } ?: emptyList(),
        )
        require(isValid(snapshot))
        snapshot
    }.getOrNull()

    private fun isValid(snapshot: PlaybackResumeSnapshot): Boolean {
        if (!isPersistableQueue(snapshot.tracks)) return false
        if (snapshot.currentIndex !in snapshot.tracks.indices) return false
        if (snapshot.positionMillis !in 0..MAX_POSITION_MILLIS) return false
        val naturalOrder = snapshot.tracks.indices.toList()
        if (snapshot.shuffleEnabled) {
            if (snapshot.playbackOrder.size != snapshot.tracks.size) return false
            if (snapshot.playbackOrder.sorted() != naturalOrder) return false
        } else if (snapshot.playbackOrder.isNotEmpty() && snapshot.playbackOrder != naturalOrder) {
            return false
        }
        return true
    }

    private fun kotlinx.serialization.json.JsonObject.requiredString(key: String): String {
        val primitive = this[key]?.jsonPrimitive ?: error("Missing $key")
        require(primitive.isString)
        return primitive.content
    }
}
