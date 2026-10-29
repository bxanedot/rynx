package dev.bxane.rynx.playback.scrobble

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScrobbleProgressTrackerTest {
    @Test fun knownTrackScrobblesAtHalfDuration() {
        val tracker = ScrobbleProgressTracker()
        assertNull(tracker.observe(sample(0, 0, duration = 120_000)))
        assertNull(tracker.observe(sample(30_000, 30_000, duration = 120_000)))
        assertNotNull(tracker.observe(sample(60_000, 60_000, duration = 120_000)))
        assertNull(tracker.observe(sample(90_000, 90_000, duration = 120_000)))
    }

    @Test fun longAndUnknownTracksUseFourMinuteLimit() {
        val long = ScrobbleProgressTracker()
        assertNull(long.observe(sample(0, 0, duration = 900_000)))
        assertNull(long.observe(sample(239_000, 239_000, duration = 900_000)))
        assertNotNull(long.observe(sample(240_000, 240_000, duration = 900_000)))

        val unknown = ScrobbleProgressTracker()
        assertNull(unknown.observe(sample(0, 0, duration = null)))
        assertNull(unknown.observe(sample(239_000, 239_000, duration = null)))
        assertNotNull(unknown.observe(sample(240_000, 240_000, duration = null)))
    }

    @Test fun tracksShorterThanThirtySecondsAreNeverScrobbled() {
        val tracker = ScrobbleProgressTracker()
        assertNull(tracker.observe(sample(0, 0, duration = 29_999)))
        assertNull(tracker.observe(sample(40_000, 40_000, duration = 29_999)))
    }

    @Test fun pausedTimeAndSeekJumpsDoNotCount() {
        val tracker = ScrobbleProgressTracker()
        assertNull(tracker.observe(sample(0, 0, duration = 120_000)))
        assertNull(tracker.observe(sample(10_000, 10_000, duration = 120_000)))
        assertNull(tracker.observe(sample(40_000, 10_000, playing = false, duration = 120_000)))
        assertNull(tracker.observe(sample(100_000, 20_000, duration = 120_000))) // seek while paused
        assertNull(tracker.observe(sample(110_000, 30_000, duration = 120_000)))
        assertNull(tracker.observe(sample(120_000, 40_000, duration = 120_000)))
        assertNull(tracker.observe(sample(130_000, 90_000, duration = 120_000))) // seek while playing
        assertNull(tracker.observe(sample(140_000, 100_000, duration = 120_000)))
        assertNotNull(tracker.observe(sample(160_000, 120_000, duration = 120_000)))
    }

    @Test fun changingTrackStartsANewPlaybackAttempt() {
        val tracker = ScrobbleProgressTracker()
        tracker.observe(sample(0, 0, id = "a", duration = 60_000))
        assertNull(tracker.observe(sample(10_000, 10_000, id = "a", duration = 60_000)))
        assertNull(tracker.observe(sample(20_000, 20_000, id = "b", duration = 60_000)))
        assertNull(tracker.observe(sample(30_000, 30_000, id = "b", duration = 60_000)))
        assertNull(tracker.observe(sample(40_000, 40_000, id = "b", duration = 60_000)))
        assertNotNull(tracker.observe(sample(50_000, 50_000, id = "b", duration = 60_000)))
    }

    @Test fun timestampUsesTheEstimatedTrackStartAndPlaybackAttemptEmitsOnce() {
        val tracker = ScrobbleProgressTracker()
        assertNull(tracker.observe(ScrobblePlaybackSample("track", true, 20_000, 40_000, 100_000, 1_700_000_020_000)))
        val scrobble = assertNotNull(tracker.observe(ScrobblePlaybackSample("track", true, 40_000, 40_000, 120_000, 1_700_000_040_000)))
        assertEquals(1_700_000_000L, scrobble.listenedAtEpochSeconds)
        assertNull(tracker.observe(ScrobblePlaybackSample("track", true, 41_000, 40_000, 121_000, 1_700_000_041_000)))
    }

    private fun sample(
        elapsed: Long,
        position: Long,
        id: String = "track",
        playing: Boolean = true,
        duration: Long?,
    ) = ScrobblePlaybackSample(id, playing, position, duration, elapsed, elapsed)
}
