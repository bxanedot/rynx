package dev.bxane.rynx.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryCollectionsTest {
    @Test fun artistsAndAlbumsUseRealTagsAndKeepDifferentArtistsSeparate() {
        val tracks = listOf(
            TrackItem("a", "Opener", "Artist A", albumTitle = "Sessions"),
            TrackItem("b", "Interlude", "Artist A", albumTitle = "Sessions"),
            TrackItem("c", "Closer", "Artist B", albumTitle = "Sessions"),
            TrackItem("d", "Standalone", "Artist A"),
            TrackItem("e", "Untagged", "Local audio"),
        )
        val albums = libraryAlbums(tracks)
        assertEquals(2, albums.size)
        assertEquals(listOf("a","b"), albums.first { it.artist == "Artist A" }.tracks.map(TrackItem::id))
        assertEquals(listOf("c"), albums.first { it.artist == "Artist B" }.tracks.map(TrackItem::id))
        val artists = libraryArtists(tracks)
        assertEquals(listOf("Artist A","Artist B"), artists.map(LibraryArtist::name))
        assertEquals(3, artists.first().tracks.size)
        assertEquals(1, artists.first().albums.size)
        assertTrue(artists.none { it.name == "Local audio" })
    }

    @Test fun emptyAlbumsAndDuplicateQueueItemsDoNotCreateFakeCollections() {
        val tracks = listOf(
            TrackItem("a", "Song A", "Unknown artist", albumTitle = ""),
            TrackItem("b", "Song B", "Artist", albumTitle = "  "),
            TrackItem("c", "Song C", "Artist", albumTitle = null),
            TrackItem("d", "Song D", "Artist", albumTitle = "Real Album"),
            TrackItem("d", "Song D duplicate", "Artist", albumTitle = "Real Album"),
        )
        val albums = libraryAlbums(tracks)
        assertEquals(1, albums.size)
        assertEquals("Real Album", albums.single().title)
        assertEquals(listOf("d"), albums.single().tracks.map(TrackItem::id))
        assertEquals(3, libraryArtists(tracks).single().tracks.size)
    }

    @Test fun unknownArtistAlbumIsClearlyLabeledAsUnknownWithoutInventingArtistProfile() {
        val track = TrackItem("x", "Untitled", "Unknown artist", albumTitle = "Mystery")
        assertEquals("Unknown Artist", libraryAlbums(listOf(track)).single().artist)
        assertTrue(libraryArtists(listOf(track)).isEmpty())
    }
}
