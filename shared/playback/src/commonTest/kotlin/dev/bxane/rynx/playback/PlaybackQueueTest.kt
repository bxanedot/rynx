package dev.bxane.rynx.playback

import kotlin.test.*

class PlaybackQueueTest {
    private val a = QueueEntry("entry-a", "local:a")
    private val b = QueueEntry("entry-b", "local:b")
    private val c = QueueEntry("entry-c", "local:a")

    @Test fun startsAtRequestedEntryAndAdvancesOnEnd() {
        val queue = PlaybackQueue(listOf(a, b, c), 1)
        assertEquals(b, queue.current)
        assertEquals(c, queue.advance(AdvanceReason.Ended).current)
        assertNull(queue.advance(AdvanceReason.Ended).advance(AdvanceReason.Ended).current)
    }
    @Test fun repeatOneAppliesToTrackEndButNotExplicitSkip() {
        val queue = PlaybackQueue(listOf(a, b), repeat = RepeatMode.One)
        assertEquals(a, queue.advance(AdvanceReason.Ended).current)
        assertEquals(b, queue.advance(AdvanceReason.UserSkip).current)
    }
    @Test fun repeatAllWrapsWithoutLosingDuplicateMediaEntries() {
        val queue = PlaybackQueue(listOf(a, b, c), 2, RepeatMode.All)
        assertEquals(a, queue.advance(AdvanceReason.Ended).current)
        assertEquals(3, queue.entries.size)
    }
    @Test fun removingCurrentSelectsSuccessorOrPredecessorAtTail() {
        val queue = PlaybackQueue(listOf(a, b, c), 1)
        assertEquals(c, queue.remove(b.id).current)
        assertEquals(b, PlaybackQueue(listOf(a, b, c), 2).remove(c.id).current)
        assertNull(PlaybackQueue(listOf(a)).remove(a.id).current)
    }
    @Test fun reorderAndRemovalBeforeCurrentPreserveCurrentIdentity() {
        val queue = PlaybackQueue(listOf(a, b, c), 1)
        assertEquals(b, queue.move(b.id, 0).current)
        assertEquals(b, queue.remove(a.id).current)
        assertEquals(listOf(b, a, c), queue.move(b.id, 0).entries)
    }
    @Test fun emptyQueueStaysEmptyAndInvalidStatesAreRejected() {
        assertNull(PlaybackQueue().advance(AdvanceReason.Ended).current)
        assertFailsWith<IllegalArgumentException> { PlaybackQueue(listOf(a), 5) }
        assertFailsWith<IllegalArgumentException> { PlaybackQueue(listOf(a, a)) }
        assertFailsWith<IllegalArgumentException> { QueueEntry("", "local:a") }
        assertFailsWith<IllegalArgumentException> { PlaybackQueue(listOf(a)).move(a.id, -1) }
    }
    @Test fun snapshotsDoNotAliasCallerMutableList() {
        val source = mutableListOf(a, b)
        val queue = PlaybackQueue(source)
        source.clear()
        assertEquals(listOf(a, b), queue.entries)
        assertEquals(a, queue.current)
    }
    @Test fun previousDoesNotWrapUnlessRepeatAll() {
        assertEquals(a, PlaybackQueue(listOf(a, b)).previous().current)
        assertEquals(b, PlaybackQueue(listOf(a, b), repeat = RepeatMode.All).previous().current)
        assertEquals(a, PlaybackQueue(listOf(a, b), 1).previous().current)
    }
    @Test fun unknownEditsAreNoOpsAndFinishedQueueCanBeSelectedAgain() {
        val queue = PlaybackQueue(listOf(a, b))
        assertEquals(queue.entries, queue.remove("missing").entries)
        val ended = queue.advance(AdvanceReason.UserSkip).advance(AdvanceReason.UserSkip)
        assertNull(ended.current)
        assertNull(ended.advance(AdvanceReason.Ended).current)
        assertEquals(a, ended.select(a.id).current)
        assertNull(ended.remove(b.id).current)
    }
}
