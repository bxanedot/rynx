package dev.bxane.rynx.android

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import dev.bxane.rynx.library.PlaylistEntryDraft
import dev.bxane.rynx.library.SqlDelightPlaylistRepository
import dev.bxane.rynx.library.db.RynxLibraryDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PlaylistPersistenceTest {
    @Test
    fun androidSqliteDriverRetainsPlaylistAndTrackOrderAcrossReopen() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val databaseName="rynx-playlist-${UUID.randomUUID()}.db"
        val firstDriver=AndroidSqliteDriver(RynxLibraryDatabase.Schema,context,databaseName)
        val firstRepository=SqlDelightPlaylistRepository(RynxLibraryDatabase(firstDriver),Dispatchers.IO){1_000L}
        val playlist=firstRepository.createPlaylist("Device persistence")
        val first=firstRepository.addEntry(playlist.id,entry("one"))
        val second=firstRepository.addEntry(playlist.id,entry("two"))
        assertEquals(true,firstRepository.moveEntry(playlist.id,second.id,0))
        firstDriver.close()

        val reopenedDriver=AndroidSqliteDriver(RynxLibraryDatabase.Schema,context,databaseName)
        val reopenedRepository=SqlDelightPlaylistRepository(RynxLibraryDatabase(reopenedDriver),Dispatchers.IO){2_000L}
        val saved=reopenedRepository.getPlaylist(playlist.id)
        assertEquals("Device persistence",saved?.name)
        assertEquals(listOf(second.id,first.id),saved?.entries?.map { it.id })
        assertEquals(listOf(0L,1L),saved?.entries?.map { it.position })
        reopenedDriver.close()
        context.deleteDatabase(databaseName)
    }

    private fun entry(id:String)=PlaylistEntryDraft(
        mediaId="content://test/$id",title="Track $id",artist="Test artist",source="local",localUri="content://test/$id",
    )
}
