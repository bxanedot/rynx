package dev.bxane.rynx.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.bxane.rynx.ui.TrackItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackMetadataStorageAlbumTest {
    @Test fun taggedAlbumSurvivesSavingRecentOrFavoriteTrack() {
        val expected = TrackItem(
            id = "content://audio/library/11",
            title = "First Light",
            subtitle = "Morning Band",
            artworkUrl = null,
            albumTitle = "Sunrise EP",
        )
        val decoded = TrackMetadataStorage.decode(TrackMetadataStorage.encode(listOf(expected)))
        assertEquals(listOf(expected), decoded)
    }

    @Test fun olderFavoritesWithoutAlbumStillDecodeWithoutInventingOne() {
        val legacy = """[{"id":"content://audio/library/22","title":"Untitled","subtitle":"Artist"}]"""
        val track = TrackMetadataStorage.decode(legacy).single()
        assertNull(track.albumTitle)
        val explicitNull = """[{"id":"content://audio/library/22","title":"Untitled","subtitle":"Artist","albumTitle":null}]"""
        assertNull(TrackMetadataStorage.decode(explicitNull).single().albumTitle)
    }
}
