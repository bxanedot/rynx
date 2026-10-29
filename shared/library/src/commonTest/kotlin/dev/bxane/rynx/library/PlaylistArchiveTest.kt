package dev.bxane.rynx.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlaylistArchiveTest {
    @Test
    fun roundTripKeepsPlaylistAndEntryOrderWithoutDeviceStreamState() {
        val source = listOf(
            Playlist(
                id = 7,
                name = "Late night",
                createdAtMillis = 10,
                updatedAtMillis = 20,
                entries = listOf(
                    PlaylistEntry(2, 7, "yt:abc", "First", "Artist", "https://img.example/cover.jpg", "youtube-music", null, 0),
                    PlaylistEntry(3, 7, "local:one", "Second", "Another", null, "local", "content://private/device-uri", 1),
                    PlaylistEntry(4, 7, "yt:abc", "First again", "Artist", null, "youtube-music", null, 2),
                ),
            ),
        )

        val json = PlaylistArchive.encode(source, exportedAtEpochMillis = 99)
        val restored = PlaylistArchive.decode(json)

        assertEquals("Late night", restored.single().name)
        assertEquals(listOf("yt:abc", "local:one", "yt:abc"), restored.single().entries.map { it.mediaId })
        assertEquals(listOf(0, 1, 2), restored.single().entries.indices.toList())
        assertEquals("https://img.example/cover.jpg", restored.single().entries.first().artworkUrl)
        assertTrue("private/device-uri" !in json)
    }

    @Test
    fun rejectsUnknownVersionAndMalformedOrExcessiveArchives() {
        assertFailsWith<InvalidPlaylistArchive> {
            PlaylistArchive.decode("""{"format":"rynx-playlists","schemaVersion":9,"playlists":[]}""")
        }
        assertFailsWith<InvalidPlaylistArchive> {
            PlaylistArchive.decode("""{"format":"other","schemaVersion":1,"playlists":[]}""")
        }
        assertFailsWith<InvalidPlaylistArchive> { PlaylistArchive.decode("not-json") }
        assertFailsWith<InvalidPlaylistArchive> {
            PlaylistArchive.decode("""{"format":"rynx-playlists","schemaVersion":1,"playlists":[{"name":"Bad","entries":[{"title":"Missing identity"}]}]}""")
        }
        assertFailsWith<InvalidPlaylistArchive> {
            PlaylistArchive.decode(" ".repeat(PlaylistArchive.MAX_ARCHIVE_CHARACTERS + 1))
        }
    }

    @Test
    fun enforcesUtf8ByteLimitForMultibyteTrackNames() {
        val entries = (0 until 300).map { index ->
            PlaylistEntry(index.toLong() + 1, 1, "media:$index", "🎵".repeat(900), "Artist", null, "local", null, index.toLong())
        }
        val playlist = Playlist(1, "Unicode", 0, 0, entries)

        assertFailsWith<IllegalArgumentException> { PlaylistArchive.encode(listOf(playlist), exportedAtEpochMillis = 1) }
    }
}
