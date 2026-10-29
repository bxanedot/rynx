package dev.bxane.rynx.android

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.bxane.rynx.library.PlaylistRepository
import dev.bxane.rynx.library.SqlDelightPlaylistRepository
import dev.bxane.rynx.library.db.RynxLibraryDatabase
import kotlinx.coroutines.Dispatchers

/** Android supplies the SQLite driver; schema and repository behavior remain shared KMP code. */
internal class AndroidLibraryDatabase(context: Context) : AutoCloseable {
    private val driver = AndroidSqliteDriver(
        schema = RynxLibraryDatabase.Schema,
        context = context.applicationContext,
        name = DATABASE_NAME,
    )

    val playlists: PlaylistRepository = SqlDelightPlaylistRepository(
        database = RynxLibraryDatabase(driver),
        queryDispatcher = Dispatchers.IO,
    ) { System.currentTimeMillis() }

    override fun close() = driver.close()

    private companion object {
        const val DATABASE_NAME = "rynx-library.db"
    }
}
