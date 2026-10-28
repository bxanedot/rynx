package dev.bxane.rynx.playback.discovery

import kotlin.test.*

class ListeningTrackerTest {
    @Test fun seekingPastThirtySecondsDoesNotCountAsListening() {
        val tracker=ListeningSessionTracker()
        assertFalse(tracker.sample("a",true,0,0))
        tracker.discontinuity()
        assertFalse(tracker.sample("a",true,90000,1000))
        for(i in 2..29)assertFalse(tracker.sample("a",true,90000+(i-1)*1000L,i*1000L))
        assertTrue(tracker.sample("a",true,120000,31000))
    }
    @Test fun serviceSamplesCountWhileTheActivityIsAbsentAndRepeatsFormNewSessions() {
        val tracker=ListeningSessionTracker()
        assertFalse(tracker.sample("a",true,0,0))
        for(i in 1..29)assertFalse(tracker.sample("a",true,i*1000L,i*1000L))
        assertTrue(tracker.sample("a",true,30000,30000))
        assertFalse(tracker.sample("a",true,31000,31000))
        tracker.newSession()
        assertFalse(tracker.sample("a",true,0,32000))
        for(i in 1..29)assertFalse(tracker.sample("a",true,i*1000L,32000+i*1000L))
        assertTrue(tracker.sample("a",true,30000,62000))
    }
    @Test fun pausedTimeNeverContributesToListening() {
        val tracker=ListeningSessionTracker()
        tracker.sample("a",true,0,0)
        tracker.sample("a",true,10000,10000)
        tracker.sample("a",false,10000,11000)
        assertFalse(tracker.sample("a",false,10000,100000))
        assertFalse(tracker.sample("a",true,10000,100001))
    }
}
