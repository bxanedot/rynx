package dev.bxane.rynx.playback.scrobble

enum class ScrobbleProvider { LastFm, ListenBrainz }

data class ScrobbleTrackMetadata(
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMillis: Long? = null,
)

data class LastFmSession(val username: String, val sessionKey: String)
data class ListenBrainzAccount(val username: String)

data class ScrobbleProviderState(
    val connected: Boolean = false,
    val enabled: Boolean = false,
    val username: String? = null,
    val pendingApproval: Boolean = false,
    val error: String? = null,
)

data class ScrobbleSettingsState(
    val lastFmConfigured: Boolean = false,
    val lastFm: ScrobbleProviderState = ScrobbleProviderState(),
    val listenBrainz: ScrobbleProviderState = ScrobbleProviderState(),
)

/** Non-secret account preferences; credentials live behind a platform secure-store adapter. */
interface ScrobbleAccountStore {
    suspend fun load(): ScrobbleSettingsState
    suspend fun setEnabled(provider: ScrobbleProvider, enabled: Boolean)
    suspend fun setError(provider: ScrobbleProvider, message: String?)
    suspend fun connectLastFm(session: LastFmSession)
    suspend fun disconnectLastFm()
    suspend fun connectListenBrainz(account: ListenBrainzAccount, token: String)
    suspend fun disconnectListenBrainz()
    suspend fun lastFmSession(): LastFmSession?
    suspend fun listenBrainzToken(): String?
}

open class ScrobbleException(message: String) : Exception(message)
class ScrobbleAuthenticationException(message: String = "The account could not be verified.") : ScrobbleException(message)
class ScrobbleNetworkException(message: String) : ScrobbleException(message)
class ScrobbleProtocolException(message: String) : ScrobbleException(message)
