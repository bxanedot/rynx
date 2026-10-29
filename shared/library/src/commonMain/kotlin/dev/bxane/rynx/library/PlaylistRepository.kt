package dev.bxane.rynx.library

import kotlinx.coroutines.flow.Flow

/** Persistence contract shared by Android, desktop and future Apple platform adapters. */
interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylists(): List<Playlist>
    suspend fun getPlaylist(id: Long): Playlist?
    suspend fun createPlaylist(name: String): Playlist
    /** Adds imported playlists and entries in one transaction; existing playlists are left untouched. */
    suspend fun importPlaylists(playlists: List<PlaylistImportDraft>): List<Playlist>
    suspend fun renamePlaylist(id: Long, name: String): Playlist?
    suspend fun deletePlaylist(id: Long): Boolean
    suspend fun addEntry(playlistId: Long, entry: PlaylistEntryDraft): PlaylistEntry
    suspend fun removeEntry(playlistId: Long, entryId: Long): Boolean
    suspend fun moveEntry(playlistId: Long, entryId: Long, toIndex: Int): Boolean
}
