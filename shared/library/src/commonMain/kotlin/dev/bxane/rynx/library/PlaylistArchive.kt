package dev.bxane.rynx.library

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A decoded user backup. Stable media IDs and display metadata are retained, not resolved stream URLs. */
data class PlaylistImportDraft(
    val name: String,
    val entries: List<PlaylistEntryDraft>,
)

class InvalidPlaylistArchive(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

/** Versioned, bounded JSON format for moving Rynx playlist definitions between installs. */
object PlaylistArchive {
    const val MIME_TYPE = "application/json"
    const val DEFAULT_FILE_NAME = "rynx-playlists.json"
    const val MAX_ARCHIVE_BYTES = 1_000_000
    const val MAX_ARCHIVE_CHARACTERS = MAX_ARCHIVE_BYTES
    const val MAX_PLAYLISTS = 100
    const val MAX_ENTRIES = 10_000
    private const val FORMAT = "rynx-playlists"
    private const val VERSION = 1

    fun encode(playlists: List<Playlist>, exportedAtEpochMillis: Long): String {
        require(exportedAtEpochMillis >= 0) { "export time cannot be negative" }
        require(playlists.size <= MAX_PLAYLISTS) { "too many playlists to export" }
        require(playlists.sumOf { it.entries.size } <= MAX_ENTRIES) { "too many playlist entries to export" }

        val root = buildJsonObject {
            put("format", JsonPrimitive(FORMAT))
            put("schemaVersion", JsonPrimitive(VERSION))
            put("application", JsonPrimitive("Rynx"))
            put("applicationId", JsonPrimitive("dev.bxane.rynx"))
            put("attribution", JsonPrimitive("Exported by Rynx"))
            put("exportedAtEpochMillis", JsonPrimitive(exportedAtEpochMillis))
            put("playlists", buildJsonArray {
                playlists.forEach { playlist ->
                    add(buildJsonObject {
                        put("name", JsonPrimitive(PlaylistRules.normalizeName(playlist.name)))
                        put("entries", buildJsonArray {
                            playlist.entries.forEach { entry ->
                                add(buildJsonObject {
                                    put("mediaId", JsonPrimitive(entry.mediaId))
                                    put("title", JsonPrimitive(entry.title))
                                    put("artist", JsonPrimitive(entry.artist))
                                    put("artworkUrl", entry.artworkUrl?.let(::JsonPrimitive) ?: JsonNull)
                                    put("source", JsonPrimitive(entry.source))
                                })
                            }
                        })
                    })
                }
            })
        }.toString()
        require(root.encodeToByteArray().size <= MAX_ARCHIVE_BYTES) { "playlist archive exceeds the export limit" }
        return root
    }

    fun decode(contents: String): List<PlaylistImportDraft> {
        if (contents.encodeToByteArray().size > MAX_ARCHIVE_BYTES) {
            throw InvalidPlaylistArchive("Playlist archive is larger than the 1 MB limit")
        }
        try {
            val root = kotlinx.serialization.json.Json.parseToJsonElement(contents).jsonObject
            if (root.requiredString("format", 32) != FORMAT) throw InvalidPlaylistArchive("This is not a Rynx playlist archive")
            val version = root["schemaVersion"]?.jsonPrimitive?.intOrNull
                ?: throw InvalidPlaylistArchive("Playlist archive has no schema version")
            if (version != VERSION) throw InvalidPlaylistArchive("Playlist archive version $version is not supported")
            val playlists = root.requiredArray("playlists")
            if (playlists.size > MAX_PLAYLISTS) throw InvalidPlaylistArchive("Playlist archive contains too many playlists")
            var totalEntries = 0
            return playlists.map { element ->
                val playlist = element as? JsonObject ?: throw InvalidPlaylistArchive("Playlist entry is malformed")
                val name = PlaylistRules.normalizeName(playlist.requiredString("name", PlaylistRules.MAX_NAME_LENGTH * 4))
                val entries = playlist.requiredArray("entries")
                totalEntries += entries.size
                if (totalEntries > MAX_ENTRIES) throw InvalidPlaylistArchive("Playlist archive contains too many songs")
                PlaylistImportDraft(name, entries.map { rawEntry ->
                    val entry = rawEntry as? JsonObject ?: throw InvalidPlaylistArchive("Song entry is malformed")
                    val mediaId = entry.requiredString("mediaId", 4096)
                    val title = entry.requiredString("title", 1024)
                    val artist = entry.requiredString("artist", 1024)
                    val source = entry.requiredString("source", 128)
                    val artwork = entry["artworkUrl"]?.let { value ->
                        if (value == JsonNull) null else value.stringValue("artworkUrl", 4096).also {
                            if (it.isNotEmpty() && !it.startsWith("https://")) {
                                throw InvalidPlaylistArchive("Artwork links must use HTTPS")
                            }
                        }
                    }
                    // Imported document URIs have no persisted Android read grant on another install.
                    PlaylistEntryDraft(mediaId, title, artist, artwork, source, localUri = null)
                })
            }
        } catch (error: InvalidPlaylistArchive) {
            throw error
        } catch (error: Exception) {
            throw InvalidPlaylistArchive("Playlist archive is invalid or incomplete", error)
        }
    }

    private fun JsonObject.requiredString(name: String, maxLength: Int): String =
        (this[name] ?: throw InvalidPlaylistArchive("Playlist archive is missing $name"))
            .stringValue(name, maxLength)

    private fun JsonElement.stringValue(name: String, maxLength: Int): String {
        val primitive = this as? JsonPrimitive
            ?: throw InvalidPlaylistArchive("Playlist archive field $name must be text")
        if (!primitive.isString) throw InvalidPlaylistArchive("Playlist archive field $name must be text")
        val value = primitive.content
        if (value.length > maxLength || (name != "artworkUrl" && value.isBlank())) {
            throw InvalidPlaylistArchive("Playlist archive field $name is empty or too long")
        }
        return value
    }

    private fun JsonObject.requiredArray(name: String): JsonArray =
        this[name] as? JsonArray ?: throw InvalidPlaylistArchive("Playlist archive field $name must be a list")
}
