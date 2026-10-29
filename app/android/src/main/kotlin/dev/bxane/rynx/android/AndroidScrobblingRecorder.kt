package dev.bxane.rynx.android

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.bxane.rynx.playback.scrobble.LastFmClient
import dev.bxane.rynx.playback.scrobble.LastFmSession
import dev.bxane.rynx.playback.scrobble.ListenBrainzClient
import dev.bxane.rynx.playback.scrobble.ScrobbleAccountStore
import dev.bxane.rynx.playback.scrobble.ScrobblePlaybackSample
import dev.bxane.rynx.playback.scrobble.ScrobbleProvider
import dev.bxane.rynx.playback.scrobble.ScrobbleProgressTracker
import dev.bxane.rynx.playback.scrobble.ScrobbleSettingsState
import dev.bxane.rynx.playback.scrobble.ScrobbleSubmission
import dev.bxane.rynx.playback.scrobble.ScrobbleTrackMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Service-owned scrobbling; failures only update account status and never affect Media3 playback. */
internal class AndroidScrobblingRecorder(
    context: Context,
    private val player: Player,
    private val accountStore: ScrobbleAccountStore,
    private val lastFm: LastFmClient,
    private val listenBrainz: ListenBrainzClient,
) : Player.Listener, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val accountPreferences = context.applicationContext.getSharedPreferences("scrobbling-accounts", Context.MODE_PRIVATE)
    private var lastAccountRefresh = -ACCOUNT_REFRESH_MILLIS
    private val accountPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key?.startsWith("lastfm.") == true || key?.startsWith("listenbrainz.") == true) {
            lastAccountRefresh = SystemClock.elapsedRealtime() - ACCOUNT_REFRESH_MILLIS
        }
    }
    private val tracker = ScrobbleProgressTracker()
    private var polling: Job? = null
    private var accountState = ScrobbleSettingsState()
    private var lastFmSession: LastFmSession? = null
    private var listenBrainzToken: String? = null
    private var activeMediaId: String? = null
    private var attemptNumber = 0L
    private var pendingScrobble: ScrobbleSubmission? = null
    private val attemptedProviders = mutableSetOf<ScrobbleProvider>()
    private var lastFmNowPlayingKey: String? = null
    private var listenBrainzNowPlayingKey: String? = null

    init {
        accountPreferences.registerOnSharedPreferenceChangeListener(accountPreferencesListener)
        player.addListener(this)
        polling = scope.launch {
            while (isActive) {
                try {
                    refreshAccountsIfNeeded()
                    samplePlayer()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A transient preference/provider error must not stop subsequent samples.
                }
                delay(POLL_MILLIS)
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        startAttempt(mediaItem?.mediaId, force = true)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            lastFmNowPlayingKey = null
            listenBrainzNowPlayingKey = null
        }
    }

    private suspend fun refreshAccountsIfNeeded() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAccountRefresh < ACCOUNT_REFRESH_MILLIS) return
        lastAccountRefresh = now
        val snapshot = withContext(Dispatchers.IO) {
            Triple(accountStore.load(), accountStore.lastFmSession(), accountStore.listenBrainzToken())
        }
        val previousLastFm = lastFmSession?.username
        val previousListenBrainz = accountState.listenBrainz.username
        accountState = snapshot.first
        lastFmSession = snapshot.second
        listenBrainzToken = snapshot.third
        if (previousLastFm != lastFmSession?.username) lastFmNowPlayingKey = null
        if (previousListenBrainz != accountState.listenBrainz.username) listenBrainzNowPlayingKey = null
    }

    private suspend fun samplePlayer() {
        val item = player.currentMediaItem
        val id = item?.mediaId
        if (id != activeMediaId) startAttempt(id)
        val duration = player.duration.takeIf { it > 0L && it != C.TIME_UNSET }
        val position = player.currentPosition.coerceAtLeast(0L)
        val sample = ScrobblePlaybackSample(
            trackId = id,
            playing = player.isPlaying,
            positionMillis = position,
            durationMillis = duration,
            elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            wallClockMillis = System.currentTimeMillis(),
        )
        val submission = tracker.observe(sample)
        if (submission != null) pendingScrobble = submission

        val metadata = item?.toScrobbleMetadata(duration)
        if (metadata != null && player.isPlaying) sendNowPlayingIfNeeded(id.orEmpty(), metadata)
        val pending = pendingScrobble
        if (pending != null && metadata != null) submitToEnabledProviders(pending, metadata)
    }

    private fun startAttempt(mediaId: String?, force: Boolean = false) {
        if (force || mediaId != activeMediaId) {
            activeMediaId = mediaId
            attemptNumber++
            tracker.beginNewPlaybackAttempt()
            pendingScrobble = null
            attemptedProviders.clear()
            lastFmNowPlayingKey = null
            listenBrainzNowPlayingKey = null
        }
    }

    private fun sendNowPlayingIfNeeded(mediaId: String, track: ScrobbleTrackMetadata) {
        val session = lastFmSession
        if (accountState.lastFm.connected && accountState.lastFm.enabled && session != null) {
            val key = "$mediaId:$attemptNumber:${session.username}"
            if (key != lastFmNowPlayingKey) {
                lastFmNowPlayingKey = key
                sendProviderRequest(ScrobbleProvider.LastFm, "Last.fm could not update now-playing. Check your connection.") {
                    lastFm.updateNowPlaying(session, track)
                }
            }
        } else lastFmNowPlayingKey = null

        val token = listenBrainzToken
        if (accountState.listenBrainz.connected && accountState.listenBrainz.enabled && token != null) {
            val key = "$mediaId:$attemptNumber:${accountState.listenBrainz.username}"
            if (key != listenBrainzNowPlayingKey) {
                listenBrainzNowPlayingKey = key
                sendProviderRequest(ScrobbleProvider.ListenBrainz, "ListenBrainz could not update now-playing. Check your connection.") {
                    listenBrainz.updateNowPlaying(token, track)
                }
            }
        } else listenBrainzNowPlayingKey = null
    }

    private fun submitToEnabledProviders(submission: ScrobbleSubmission, track: ScrobbleTrackMetadata) {
        val session = lastFmSession
        if (accountState.lastFm.connected && accountState.lastFm.enabled && session != null && attemptedProviders.add(ScrobbleProvider.LastFm)) {
            sendProviderRequest(ScrobbleProvider.LastFm, "Last.fm could not save this listen. Check your connection.") {
                lastFm.scrobble(session, track, submission.listenedAtEpochSeconds)
            }
        }
        val token = listenBrainzToken
        if (accountState.listenBrainz.connected && accountState.listenBrainz.enabled && token != null && attemptedProviders.add(ScrobbleProvider.ListenBrainz)) {
            sendProviderRequest(ScrobbleProvider.ListenBrainz, "ListenBrainz could not save this listen. Check your connection.") {
                listenBrainz.submitListen(token, track, submission.listenedAtEpochSeconds)
            }
        }
    }

    private fun sendProviderRequest(provider: ScrobbleProvider, onFailure: String, action: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                recordFailure(provider, onFailure)
            }
        }
    }

    private suspend fun recordFailure(provider: ScrobbleProvider, message: String) {
        try {
            accountStore.setError(provider, message)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Scrobbling status storage is best-effort; playback remains unaffected.
        }
    }

    private fun MediaItem.toScrobbleMetadata(durationMillis: Long?): ScrobbleTrackMetadata? {
        val title = mediaMetadata.title?.toString()?.trim().orEmpty()
        val artist = mediaMetadata.artist?.toString()?.trim().orEmpty()
        if (title.isBlank() || artist.isBlank() || title == "Unknown track" || artist == "Unknown artist") return null
        val album = mediaMetadata.albumTitle?.toString()?.trim()?.takeIf(String::isNotBlank)
        return ScrobbleTrackMetadata(title, artist, album, durationMillis)
    }

    override fun close() {
        polling?.cancel()
        polling = null
        player.removeListener(this)
        accountPreferences.unregisterOnSharedPreferenceChangeListener(accountPreferencesListener)
        scope.cancel()
    }

    private companion object {
        const val POLL_MILLIS = 1_000L
        const val ACCOUNT_REFRESH_MILLIS = 5_000L
    }
}
