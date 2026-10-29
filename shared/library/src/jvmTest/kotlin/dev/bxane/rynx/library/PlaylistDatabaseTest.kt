package dev.bxane.rynx.library

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.bxane.rynx.library.db.RynxLibraryDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistDatabaseTest {
    @Test
    fun createRenameAndDeleteArePersistedWithValidatedNames() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        RynxLibraryDatabase.Schema.create(driver)
        val repository = SqlDelightPlaylistRepository(RynxLibraryDatabase(driver)) { 100L }

        val created = repository.createPlaylist("  Road Trip  ")
        assertEquals("Road Trip", created.name)
        assertEquals(100L, created.createdAtMillis)

        val now = 200L
        val timedRepository = SqlDelightPlaylistRepository(RynxLibraryDatabase(driver)) { now }
        val renamed = timedRepository.renamePlaylist(created.id, "Night Drive")
        assertEquals("Night Drive", renamed?.name)
        assertEquals(200L, renamed?.updatedAtMillis)

        var failure: Throwable? = null
        try {
            timedRepository.createPlaylist("   ")
        } catch (error: Throwable) {
            failure = error
        }
        assertTrue(failure is InvalidPlaylistName)
        assertEquals(1, timedRepository.getPlaylists().size)

        timedRepository.addEntry(created.id,localEntry("content://media/cascade"))
        assertTrue(timedRepository.deletePlaylist(created.id))
        assertFalse(timedRepository.deletePlaylist(created.id))
        assertTrue(timedRepository.getPlaylists().isEmpty())
        assertTrue(timedRepository.getPlaylist(created.id)==null)
        assertTrue(RynxLibraryDatabase(driver).playlistQueries.selectEntries(created.id).executeAsList().isEmpty())
        driver.close()
    }

    @Test
    fun duplicateMediaCanBeAddedAndRemovingOneEntryKeepsTheOther() = runBlocking {
        val (driver, repository) = inMemoryRepository()
        val playlist = repository.createPlaylist("Favorites for the road")
        val first = repository.addEntry(playlist.id, localEntry("content://media/track-1"))
        val duplicate = repository.addEntry(playlist.id, localEntry("content://media/track-1"))
        val third = repository.addEntry(playlist.id, localEntry("content://media/track-2"))

        assertTrue(repository.removeEntry(playlist.id, first.id))

        val saved = repository.getPlaylists().single().entries
        assertEquals(listOf(duplicate.id, third.id), saved.map(PlaylistEntry::id))
        assertEquals(listOf(0L, 1L), saved.map(PlaylistEntry::position))
        assertEquals(listOf("content://media/track-1", "content://media/track-2"), saved.map(PlaylistEntry::mediaId))
        driver.close()
    }

    @Test
    fun moveUsesEntryIdentityAndPersistsContiguousPositions() = runBlocking {
        val (driver, repository) = inMemoryRepository()
        val playlist = repository.createPlaylist("Order")
        val first = repository.addEntry(playlist.id, localEntry("a"))
        val second = repository.addEntry(playlist.id, localEntry("b"))
        val third = repository.addEntry(playlist.id, localEntry("c"))

        assertTrue(repository.moveEntry(playlist.id, third.id, -10))
        val moved = repository.getPlaylists().single().entries
        assertEquals(listOf(third.id, first.id, second.id), moved.map(PlaylistEntry::id))
        assertEquals(listOf(0L, 1L, 2L), moved.map(PlaylistEntry::position))
        assertFalse(repository.moveEntry(playlist.id, 999, 0))
        driver.close()
    }

    @Test
    fun playlistAndEntryOrderSurviveClosingAndReopeningTheDatabase() = runBlocking {
        val file = Files.createTempFile("rynx-playlists", ".db").toFile()
        val firstDriver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        RynxLibraryDatabase.Schema.create(firstDriver)
        val firstRepository = SqlDelightPlaylistRepository(RynxLibraryDatabase(firstDriver)) { 1_000L }
        val playlist = firstRepository.createPlaylist("Reopen me")
        val first = firstRepository.addEntry(playlist.id, localEntry("a"))
        val second = firstRepository.addEntry(playlist.id, localEntry("b"))
        firstRepository.moveEntry(playlist.id, second.id, 0)
        firstDriver.close()

        val reopenedDriver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        val reopened = SqlDelightPlaylistRepository(RynxLibraryDatabase(reopenedDriver)) { 2_000L }
            .getPlaylists().single()

        assertEquals(playlist.id, reopened.id)
        assertEquals("Reopen me", reopened.name)
        assertEquals(listOf(second.id, first.id), reopened.entries.map(PlaylistEntry::id))
        assertEquals(listOf(0L, 1L), reopened.entries.map(PlaylistEntry::position))
        reopenedDriver.close()
        assertTrue(file.delete() || !file.exists())
    }

    @Test
    fun observePlaylistsReturnsThePersistedCollections() = runBlocking {
        val (driver, repository) = inMemoryRepository()
        val created = repository.createPlaylist("Observable")

        assertEquals(created, repository.observePlaylists().first().single())
        driver.close()
    }

    @Test
    fun observationEmitsAfterPlaylistChangesCommit() = runBlocking {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        RynxLibraryDatabase.Schema.create(driver)
        val repository=SqlDelightPlaylistRepository(RynxLibraryDatabase(driver)){1_000L}
        val initial=CompletableDeferred<Unit>()
        val changed=CompletableDeferred<List<Playlist>>()
        val observation=repository.observePlaylists().onEach { rows ->
            if(rows.isEmpty()) initial.complete(Unit) else changed.complete(rows)
        }.launchIn(this)
        initial.await()

        val created=repository.createPlaylist("Observable update")
        val snapshot=withTimeout(2_000){changed.await()}

        assertEquals(created,snapshot.single())
        observation.cancel()
        driver.close()
    }

    @Test
    fun importedPlaylistsAreAddedAtomicallyAndKeepEntryOrder() = runBlocking {
        val (driver, repository) = inMemoryRepository()
        val existing = repository.createPlaylist("Keep me")
        val imports = listOf(
            PlaylistImportDraft(
                "Road trip",
                listOf(localEntry("provider:one"), localEntry("provider:two")),
            ),
            PlaylistImportDraft("Empty list", emptyList()),
        )

        val imported = repository.importPlaylists(imports)

        assertEquals(listOf("Keep me", "Road trip", "Empty list"), repository.getPlaylists().sortedBy { it.id }.map { it.name })
        assertEquals(listOf("provider:one", "provider:two"), imported.first().entries.map { it.mediaId })
        assertEquals(listOf(0L, 1L), imported.first().entries.map { it.position })
        assertEquals(existing.id, repository.getPlaylist(existing.id)?.id)

        var failure: Throwable? = null
        try {
            repository.importPlaylists(
                listOf(
                    PlaylistImportDraft("Would roll back", emptyList()),
                    PlaylistImportDraft("Invalid", listOf(localEntry(" "))),
                ),
            )
        } catch (error: Throwable) {
            failure = error
        }
        assertTrue(failure is IllegalArgumentException)
        assertEquals(listOf("Keep me", "Road trip", "Empty list"), repository.getPlaylists().sortedBy { it.id }.map { it.name })
        driver.close()
    }

    @Test
    fun addingAnEntryToAnUnknownPlaylistFailsWithoutCreatingOrphanRows() = runBlocking {
        val (driver,repository)=inMemoryRepository()

        var failure:Throwable?=null
        try {
            repository.addEntry(99,localEntry("content://media/orphan"))
        } catch(error:Throwable) {
            failure=error
        }

        assertTrue(failure is PlaylistNotFound)
        assertTrue(repository.getPlaylists().isEmpty())
        driver.close()
    }

    private fun inMemoryRepository(): Pair<JdbcSqliteDriver, SqlDelightPlaylistRepository> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        RynxLibraryDatabase.Schema.create(driver)
        return driver to SqlDelightPlaylistRepository(RynxLibraryDatabase(driver)) { 1_000L }
    }

    private fun localEntry(uri: String) = PlaylistEntryDraft(
        mediaId = uri,
        title = "Track ${uri.substringAfterLast('/')}",
        artist = "Test Artist",
        source = "local",
        localUri = uri,
    )
}
