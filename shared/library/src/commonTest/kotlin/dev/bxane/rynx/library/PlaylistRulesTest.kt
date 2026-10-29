package dev.bxane.rynx.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaylistRulesTest {
    @Test
    fun playlistNamesAreTrimmedAndValidated() {
        assertEquals("Road Trip", PlaylistRules.normalizeName("  Road Trip  "))
        assertFailsWith<InvalidPlaylistName> { PlaylistRules.normalizeName(" \t ") }
        assertFailsWith<InvalidPlaylistName> { PlaylistRules.normalizeName("x".repeat(81)) }
    }

    @Test
    fun movingAnEntryUsesItsIdentityAndKeepsContiguousPositions() {
        val entries = listOf(entry(10, "a", 0), entry(11, "b", 1), entry(12, "c", 2))

        val moved = PlaylistRules.move(entries, entryId = 12, toIndex = 0)

        assertEquals(listOf(12L, 10L, 11L), moved.map(PlaylistEntry::id))
        assertEquals(listOf(0L, 1L, 2L), moved.map(PlaylistEntry::position))
    }

    @Test
    fun removingOneDuplicateEntryPreservesItsSiblingAndRenumbers() {
        val entries = listOf(entry(10, "same-media", 0), entry(11, "same-media", 1), entry(12, "other", 2))

        val remaining = PlaylistRules.remove(entries, entryId = 10)

        assertEquals(listOf(11L, 12L), remaining.map(PlaylistEntry::id))
        assertEquals(listOf(0L, 1L), remaining.map(PlaylistEntry::position))
    }

    @Test
    fun moveClampsAtListBoundariesAndIgnoresUnknownEntry() {
        val entries = listOf(entry(10, "a", 0), entry(11, "b", 1), entry(12, "c", 2))

        assertEquals(listOf(12L, 10L, 11L), PlaylistRules.move(entries, 12, -20).map(PlaylistEntry::id))
        assertEquals(entries, PlaylistRules.move(entries, 99, 0))
    }

    private fun entry(id: Long, mediaId: String, position: Long) = PlaylistEntry(
        id = id,
        playlistId = 1,
        mediaId = mediaId,
        title = mediaId,
        artist = "Artist",
        artworkUrl = null,
        source = "local",
        localUri = null,
        position = position,
    )
}
