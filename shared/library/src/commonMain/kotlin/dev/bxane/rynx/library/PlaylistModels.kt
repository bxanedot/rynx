package dev.bxane.rynx.library

/** A user-owned collection. The entry list is always returned in playback order. */
data class Playlist(
    val id: Long,
    val name: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val entries: List<PlaylistEntry> = emptyList(),
)

/** A stable media reference plus display metadata; resolved stream data is deliberately absent. */
data class PlaylistEntry(
    val id: Long,
    val playlistId: Long,
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val source: String,
    val localUri: String?,
    val position: Long,
)

data class PlaylistEntryDraft(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val source: String,
    val localUri: String? = null,
)

class InvalidPlaylistName(message: String) : IllegalArgumentException(message)

class PlaylistNotFound(id: Long) : IllegalArgumentException("Playlist $id does not exist")
