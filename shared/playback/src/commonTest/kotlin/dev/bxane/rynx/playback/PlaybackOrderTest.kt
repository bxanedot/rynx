package dev.bxane.rynx.playback

import kotlin.test.*

class PlaybackOrderTest {
    @Test fun shuffledEditsPreserveTimelineIdentitiesAndThePlayingPrefix() {
        assertEquals(listOf(2,1,0,3),moveUpcomingQueueEntry(listOf(2,0,1,3),2,1,0))
        assertEquals(listOf(2,1,3,0),moveUpcomingQueueEntry(listOf(2,0,1,3),2,0,3))
    }
    @Test fun staleOrNonUpcomingMovesAreRejected() {
        val order=listOf(2,0,1,3)
        assertNull(moveUpcomingQueueEntry(order,0,2,1))
        assertNull(moveUpcomingQueueEntry(order,0,1,2))
        assertNull(moveUpcomingQueueEntry(order,0,99,1))
        assertNull(moveUpcomingQueueEntry(order,99,1,3))
        assertNull(moveUpcomingQueueEntry(listOf(0,1,1),0,1,1))
    }
    @Test fun incompleteOrInvalidProviderOrdersFallBackToTheFullTimeline() {
        assertEquals(listOf(2,0,1),normalizedPlaybackOrder(3,listOf(2,0,1)))
        assertEquals(listOf(0,1,2),normalizedPlaybackOrder(3,listOf(2,0)))
        assertEquals(listOf(0,1,2),normalizedPlaybackOrder(3,listOf(2,0,5)))
        assertEquals(listOf(0,1,2),normalizedPlaybackOrder(3,listOf(0,0,2)))
        assertEquals(emptyList(),normalizedPlaybackOrder(0,emptyList()))
    }
}
