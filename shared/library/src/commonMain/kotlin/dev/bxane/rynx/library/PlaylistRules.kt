package dev.bxane.rynx.library

/** Deterministic rules shared by persistent stores and every platform UI. */
object PlaylistRules {
    const val MAX_NAME_LENGTH = 80

    fun normalizeName(value: String): String {
        val name = value.trim()
        if (name.isEmpty()) throw InvalidPlaylistName("Playlist names cannot be blank")
        if (name.length > MAX_NAME_LENGTH) {
            throw InvalidPlaylistName("Playlist names must be $MAX_NAME_LENGTH characters or fewer")
        }
        return name
    }

    fun move(entries: List<PlaylistEntry>, entryId: Long, toIndex: Int): List<PlaylistEntry> {
        val fromIndex = entries.indexOfFirst { it.id == entryId }
        if (fromIndex < 0 || entries.isEmpty()) return entries

        val targetIndex = toIndex.coerceIn(0, entries.lastIndex)
        if (fromIndex == targetIndex) return entries

        val reordered = entries.toMutableList().apply {
            add(targetIndex, removeAt(fromIndex))
        }
        return withContiguousPositions(reordered)
    }

    fun remove(entries: List<PlaylistEntry>, entryId: Long): List<PlaylistEntry> {
        val remaining = entries.filterNot { it.id == entryId }
        if (remaining.size == entries.size) return entries
        return withContiguousPositions(remaining)
    }

    fun withContiguousPositions(entries: List<PlaylistEntry>): List<PlaylistEntry> =
        entries.mapIndexed { index, entry -> entry.copy(position = index.toLong()) }
}
