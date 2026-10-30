package dev.bxane.rynx.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomePersonalizationTest {
    @Test fun favoritesAndRecentlyPlayedBuildRealForYouWithoutDuplicates() {
        val liked = TrackItem("liked", "Favorite", "Artist A")
        val recent = TrackItem("recent", "Recent", "Artist B")
        val state=PlayerUiState(
            tracks=listOf(liked),
            favoriteIds=setOf("liked"),
            favoriteTracks=listOf(liked),
            recentTracks=listOf(recent, liked),
        )
        assertEquals(listOf("liked","recent"),forYouTracks(state).map(TrackItem::id))
    }

    @Test fun firstInstallWithoutHistoryDoesNotFakeRecommendations() {
        assertTrue(forYouTracks(PlayerUiState()).isEmpty())
        assertTrue(forYouTracks(PlayerUiState(tracks=listOf(TrackItem("local","Local file")))).isEmpty())
    }
}
