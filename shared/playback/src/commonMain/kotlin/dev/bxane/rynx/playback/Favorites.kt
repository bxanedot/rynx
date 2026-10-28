package dev.bxane.rynx.playback

/** Returns a new favorite set with [id] toggled; blank media identities are rejected. */
fun toggleFavoriteId(favorites: Set<String>, id: String): Set<String> {
    require(id.isNotBlank())
    return if (id in favorites) favorites - id else favorites + id
}
