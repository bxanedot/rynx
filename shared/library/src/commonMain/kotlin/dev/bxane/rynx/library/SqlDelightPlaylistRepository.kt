package dev.bxane.rynx.library

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.bxane.rynx.library.db.RynxLibraryDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/** SQLDelight-backed implementation. It stores stable references, never expiring stream URLs. */
class SqlDelightPlaylistRepository(
    private val database: RynxLibraryDatabase,
    private val queryDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nowMillis: () -> Long,
) : PlaylistRepository {
    private val queries get() = database.playlistQueries

    override fun observePlaylists(): Flow<List<Playlist>> {
        val playlists = queries.selectAllPlaylists().asFlow().mapToList(queryDispatcher)
        val entries = queries.selectAllEntries().asFlow().mapToList(queryDispatcher)
        return combine(playlists, entries) { playlistRows, entryRows ->
            val entriesByPlaylist = entryRows.groupBy { it.playlistId }
            playlistRows.map { playlist ->
                Playlist(
                    id = playlist.id,
                    name = playlist.name,
                    createdAtMillis = playlist.createdAtMillis,
                    updatedAtMillis = playlist.updatedAtMillis,
                    entries = entriesByPlaylist[playlist.id].orEmpty().map(::toDomain),
                )
            }
        }
    }

    override suspend fun getPlaylists(): List<Playlist> = withContext(queryDispatcher) {
        queries.selectAllPlaylists().executeAsList().map(::toDomain)
    }

    override suspend fun getPlaylist(id: Long): Playlist? = withContext(queryDispatcher) {
        queries.selectPlaylist(id).executeAsOneOrNull()?.let(::toDomain)
    }

    override suspend fun createPlaylist(name: String): Playlist = withContext(queryDispatcher) {
        val normalizedName = PlaylistRules.normalizeName(name)
        val createdAt = nowMillis()
        database.transactionWithResult {
            queries.insertPlaylist(normalizedName, createdAt, createdAt).value
            playlistById(queries.lastInsertedId().executeAsOne())
                ?: error("Inserted playlist could not be read back")
        }
    }

    override suspend fun importPlaylists(playlists: List<PlaylistImportDraft>): List<Playlist> =
        withContext(queryDispatcher) {
            require(playlists.size <= PlaylistArchive.MAX_PLAYLISTS) { "Too many playlists to import" }
            require(playlists.sumOf { it.entries.size } <= PlaylistArchive.MAX_ENTRIES) { "Too many playlist entries to import" }
            playlists.forEach { playlist ->
                PlaylistRules.normalizeName(playlist.name)
                playlist.entries.forEach { entry ->
                    require(entry.mediaId.isNotBlank() && entry.mediaId.length <= 4096) { "Invalid media identity" }
                    require(entry.title.isNotBlank() && entry.title.length <= 1024) { "Invalid track title" }
                    require(entry.artist.isNotBlank() && entry.artist.length <= 1024) { "Invalid artist name" }
                    require(entry.source.isNotBlank() && entry.source.length <= 128) { "Invalid media source" }
                    require(entry.artworkUrl == null || entry.artworkUrl.startsWith("https://")) { "Artwork links must use HTTPS" }
                }
            }
            database.transactionWithResult {
                playlists.map { imported ->
                    val timestamp = nowMillis()
                    queries.insertPlaylist(PlaylistRules.normalizeName(imported.name), timestamp, timestamp).value
                    val playlistId = queries.lastInsertedId().executeAsOne()
                    imported.entries.forEachIndexed { index, entry ->
                        queries.insertEntry(
                            playlistId = playlistId,
                            mediaId = entry.mediaId,
                            title = entry.title,
                            artist = entry.artist,
                            artworkUrl = entry.artworkUrl,
                            source = entry.source,
                            // Android document-provider grants belong to the source device. Re-imported
                            // local references remain visible, but must be selected again if inaccessible.
                            localUri = null,
                            position = index.toLong(),
                        ).value
                    }
                    playlistById(playlistId) ?: error("Imported playlist could not be read back")
                }
            }
        }

    override suspend fun renamePlaylist(id: Long, name: String): Playlist? = withContext(queryDispatcher) {
        val normalizedName = PlaylistRules.normalizeName(name)
        database.transactionWithResult {
            if (queries.selectPlaylist(id).executeAsOneOrNull() == null) return@transactionWithResult null
            queries.renamePlaylist(normalizedName, nowMillis(), id).value
            playlistById(id)
        }
    }

    override suspend fun deletePlaylist(id: Long): Boolean = withContext(queryDispatcher) {
        database.transactionWithResult {
            val exists = queries.selectPlaylist(id).executeAsOneOrNull() != null
            if (exists) {
                // Keep delete semantics identical on drivers that do not enable FK pragmas.
                queries.deleteEntries(id).value
                queries.deletePlaylist(id).value
            }
            exists
        }
    }

    override suspend fun addEntry(playlistId: Long, entry: PlaylistEntryDraft): PlaylistEntry =
        withContext(queryDispatcher) {
            database.transactionWithResult {
                if (queries.selectPlaylist(playlistId).executeAsOneOrNull() == null) {
                    throw PlaylistNotFound(playlistId)
                }
                val position = queries.countEntries(playlistId).executeAsOne()
                queries.insertEntry(
                    playlistId = playlistId,
                    mediaId = entry.mediaId,
                    title = entry.title,
                    artist = entry.artist,
                    artworkUrl = entry.artworkUrl,
                    source = entry.source,
                    localUri = entry.localUri,
                    position = position,
                ).value
                val entryId = queries.lastInsertedEntryId().executeAsOne()
                queries.touchPlaylist(nowMillis(), playlistId).value
                queries.selectEntry(playlistId, entryId).executeAsOne().let(::toDomain)
            }
        }

    override suspend fun removeEntry(playlistId: Long, entryId: Long): Boolean =
        withContext(queryDispatcher) {
            database.transactionWithResult {
                val previous = queries.selectEntries(playlistId).executeAsList().map(::toDomain)
                if (previous.none { it.id == entryId }) return@transactionWithResult false

                queries.deleteEntry(playlistId, entryId).value
                persistOrder(playlistId, PlaylistRules.remove(previous, entryId))
                queries.touchPlaylist(nowMillis(), playlistId).value
                true
            }
        }

    override suspend fun moveEntry(playlistId: Long, entryId: Long, toIndex: Int): Boolean =
        withContext(queryDispatcher) {
            database.transactionWithResult {
                val previous = queries.selectEntries(playlistId).executeAsList().map(::toDomain)
                if (previous.none { it.id == entryId }) return@transactionWithResult false

                val reordered = PlaylistRules.move(previous, entryId, toIndex)
                if (reordered != previous) {
                    persistOrder(playlistId, reordered)
                    queries.touchPlaylist(nowMillis(), playlistId).value
                }
                true
            }
        }

    private fun playlistById(id: Long): Playlist? =
        queries.selectPlaylist(id).executeAsOneOrNull()?.let(::toDomain)

    private fun persistOrder(playlistId: Long, entries: List<PlaylistEntry>) {
        entries.forEach { entry ->
            queries.updateEntryPosition(entry.position, playlistId, entry.id).value
        }
    }

    private fun toDomain(row: dev.bxane.rynx.library.db.Playlist): Playlist = Playlist(
        id = row.id,
        name = row.name,
        createdAtMillis = row.createdAtMillis,
        updatedAtMillis = row.updatedAtMillis,
        entries = queries.selectEntries(row.id).executeAsList().map(::toDomain),
    )

    private fun toDomain(row: dev.bxane.rynx.library.db.PlaylistEntry): PlaylistEntry = PlaylistEntry(
        id = row.id,
        playlistId = row.playlistId,
        mediaId = row.mediaId,
        title = row.title,
        artist = row.artist,
        artworkUrl = row.artworkUrl,
        source = row.source,
        localUri = row.localUri,
        position = row.position,
    )
}
