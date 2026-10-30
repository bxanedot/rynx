package dev.bxane.rynx.ui

/**
 * Honest device-local "For You" material.
 * Favorites and listening history are the only personalization signals available;
 * remote catalog songs are not labeled personal without an account-based feed.
 */
internal fun forYouTracks(state: PlayerUiState): List<TrackItem> {
    val favorite = state.favoriteTracks.filter { it.id in state.favoriteIds }
    val localFavorites = state.tracks.filter { it.id in state.favoriteIds }
    val recent = state.recentTracks
    return (favorite + localFavorites + recent).distinctBy(TrackItem::id).take(12)
}
