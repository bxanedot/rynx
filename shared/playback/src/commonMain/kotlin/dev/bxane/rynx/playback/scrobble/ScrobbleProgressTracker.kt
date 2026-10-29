package dev.bxane.rynx.playback.scrobble

/** A point-in-time sample from the existing player, with monotonic and wall-clock time. */
data class ScrobblePlaybackSample(
    val trackId: String?,
    val playing: Boolean,
    val positionMillis: Long,
    val durationMillis: Long?,
    val elapsedRealtimeMillis: Long,
    val wallClockMillis: Long,
)

/** One provider submission opportunity for one playback attempt. */
data class ScrobbleSubmission(
    val trackId: String,
    val listenedAtEpochSeconds: Long,
    val durationMillis: Long?,
)

/** Counts advancing player position rather than wall time, so pauses and seek jumps do not count. */
class ScrobbleProgressTracker(
    private val maximumScrobbleMillis: Long = MAX_SCROBBLE_MILLIS,
    private val minimumTrackMillis: Long = MIN_TRACK_MILLIS,
) {
    private var trackId: String? = null
    private var durationMillis: Long? = null
    private var previousPosition: Long? = null
    private var previousElapsed: Long? = null
    private var startedAtEpochSeconds: Long? = null
    private var listenedMillis = 0L
    private var submitted = false

    /** Returns a submission once after the threshold is met; callers must not retry it automatically. */
    fun observe(sample: ScrobblePlaybackSample): ScrobbleSubmission? {
        val id = sample.trackId?.takeIf(String::isNotBlank) ?: run {
            beginNewPlaybackAttempt()
            return null
        }
        if (trackId != id) beginNewPlaybackAttempt()
        trackId = id
        durationMillis = sample.durationMillis?.takeIf { it > 0L } ?: durationMillis

        if (!sample.playing || submitted) {
            clearPreviousSample()
            return null
        }

        if (startedAtEpochSeconds == null) {
            startedAtEpochSeconds = (sample.wallClockMillis - sample.positionMillis.coerceAtLeast(0L)) / 1_000L
        }

        val positionDelta = previousPosition?.let { sample.positionMillis - it }
        val elapsedDelta = previousElapsed?.let { sample.elapsedRealtimeMillis - it }
        if (positionDelta != null && elapsedDelta != null && elapsedDelta > 0L && positionDelta > 0L) {
            // A forward discontinuity is a seek, not listened audio. Small clock rounding is allowed.
            if (positionDelta <= elapsedDelta + POSITION_JITTER_ALLOWANCE_MILLIS) {
                listenedMillis += minOf(positionDelta, elapsedDelta)
            }
        }
        previousPosition = sample.positionMillis
        previousElapsed = sample.elapsedRealtimeMillis

        val duration = durationMillis
        if (duration != null && duration < minimumTrackMillis) return null
        val threshold = duration?.let { minOf(it / 2L, maximumScrobbleMillis) } ?: maximumScrobbleMillis
        if (!submitted && listenedMillis >= threshold) {
            submitted = true
            return ScrobbleSubmission(id, startedAtEpochSeconds ?: sample.wallClockMillis / 1_000L, duration)
        }
        return null
    }

    /** Starts a fresh attempt even if a repeat mode replays the same media ID. */
    fun beginNewPlaybackAttempt() {
        trackId = null
        durationMillis = null
        listenedMillis = 0L
        submitted = false
        startedAtEpochSeconds = null
        clearPreviousSample()
    }

    private fun clearPreviousSample() {
        previousPosition = null
        previousElapsed = null
    }

    private companion object {
        const val MAX_SCROBBLE_MILLIS = 4 * 60 * 1_000L
        const val MIN_TRACK_MILLIS = 30 * 1_000L
        const val POSITION_JITTER_ALLOWANCE_MILLIS = 500L
    }
}
