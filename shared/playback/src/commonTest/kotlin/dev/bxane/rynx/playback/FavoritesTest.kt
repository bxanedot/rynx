package dev.bxane.rynx.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FavoritesTest {
    @Test fun togglesWithoutMutatingTheOriginalSnapshot() {
        val original=setOf("track-a")
        assertEquals(setOf("track-a","track-b"),toggleFavoriteId(original,"track-b"))
        assertEquals(setOf("track-a"),original)
        assertEquals(emptySet(),toggleFavoriteId(original,"track-a"))
    }

    @Test fun rejectsBlankMediaIdentity() {
        assertFailsWith<IllegalArgumentException>{toggleFavoriteId(emptySet()," ")}
    }
}
