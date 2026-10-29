package dev.bxane.rynx.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackResumeSnapshotTest {
    private val valid = PlaybackResumeSnapshot(
        tracks = listOf(
            PlaybackResumeTrack("content://media/external/audio/1", "First", "Aster"),
            PlaybackResumeTrack("content://media/external/audio/2", "Second 🎧", "Juniper"),
            PlaybackResumeTrack("content://media/external/audio/3", "Third", "Vale"),
        ),
        currentIndex = 1,
        positionMillis = 42_250,
        repeatMode = RepeatMode.All,
        shuffleEnabled = true,
        playbackOrder = listOf(1, 2, 0),
    )

    @Test fun codecRoundTripsQueuePositionAndPlaybackModes() {
        val encoded = PlaybackResumeCodec.encode(valid)

        assertTrue(encoded != null)
        assertEquals(valid, PlaybackResumeCodec.decode(encoded))
    }

    @Test fun rejectsRemoteAndNonContentQueuesBeforePersistence() {
        assertNull(PlaybackResumeCodec.encode(valid.copy(tracks = valid.tracks + PlaybackResumeTrack("yt:abc", "Remote", "Artist"))))
        assertNull(PlaybackResumeCodec.encode(valid.copy(tracks = listOf(PlaybackResumeTrack("https://signed.example/audio", "Remote", "Artist")))))
        assertTrue(PlaybackResumeCodec.isPersistableQueue(valid.tracks))
        assertFalse(PlaybackResumeCodec.isPersistableQueue(emptyList()))
    }

    @Test fun rejectsMalformedVersionsPositionsAndShuffleOrders() {
        assertNull(PlaybackResumeCodec.decode("not-json"))
        assertNull(PlaybackResumeCodec.decode("""{"schema": 99}"""))
        assertNull(PlaybackResumeCodec.encode(valid.copy(currentIndex = 3)))
        assertNull(PlaybackResumeCodec.encode(valid.copy(positionMillis = -1)))
        assertNull(PlaybackResumeCodec.encode(valid.copy(playbackOrder = listOf(1, 1, 0))))
        assertNull(PlaybackResumeCodec.encode(valid.copy(playbackOrder = listOf(1, 2))))
    }

    @Test fun refusesOversizedQueuesAndMetadata() {
        val tooMany = valid.copy(tracks = List(501) { PlaybackResumeTrack("content://media/$it", "Title", "Artist") })
        assertNull(PlaybackResumeCodec.encode(tooMany))
        assertNull(PlaybackResumeCodec.encode(valid.copy(tracks = listOf(valid.tracks.first().copy(title = "x".repeat(513))))))
        val tooLarge = valid.copy(tracks = List(200) {
            PlaybackResumeTrack("content://" + "x".repeat(4_086), "Title", "Artist")
        })
        assertNull(PlaybackResumeCodec.encode(tooLarge))
    }
}
