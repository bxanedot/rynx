package dev.bxane.rynx.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dev.bxane.rynx.playback.scrobble.LastFmSession
import dev.bxane.rynx.playback.scrobble.ListenBrainzAccount
import dev.bxane.rynx.playback.scrobble.ScrobbleAccountStore
import dev.bxane.rynx.playback.scrobble.ScrobbleProvider
import dev.bxane.rynx.playback.scrobble.ScrobbleProviderState
import dev.bxane.rynx.playback.scrobble.ScrobbleSettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface AndroidSecretStore {
    fun read(name: String): String?
    fun write(name: String, value: String)
    fun remove(name: String)
}

/** Account tokens are encrypted with an AES-GCM key protected by Android Keystore. */
internal class AndroidKeystoreSecretStore(context: Context) : AndroidSecretStore {
    private val preferences = context.applicationContext.getSharedPreferences(SECRET_PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized override fun read(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        val fields = encoded.split(':', limit = 3)
        if (fields.size != 3 || fields[0] != FORMAT_VERSION) return null
        return runCatching {
            val iv = Base64.decode(fields[1], Base64.NO_WRAP)
            val cipherText = Base64.decode(fields[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(cipherText).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    @Synchronized override fun write(name: String, value: String) {
        require(name.isNotBlank() && value.isNotBlank())
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val stored = listOf(
            FORMAT_VERSION,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP),
        ).joinToString(":")
        preferences.edit { putString(name, stored) }
    }

    @Synchronized override fun remove(name: String) {
        preferences.edit { remove(name) }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "dev.bxane.rynx.scrobbling.credentials.v1"
        const val SECRET_PREFERENCES = "scrobbling-secrets"
        const val FORMAT_VERSION = "v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}

internal class AndroidScrobbleAccountStore(
    context: Context,
    private val lastFmConfigured: Boolean,
    private val secretStore: AndroidSecretStore = AndroidKeystoreSecretStore(context),
) : ScrobbleAccountStore {
    private val preferences = context.applicationContext.getSharedPreferences(ACCOUNT_PREFERENCES, Context.MODE_PRIVATE)

    override suspend fun load(): ScrobbleSettingsState = withContext(Dispatchers.IO) {
        ScrobbleSettingsState(
            lastFmConfigured = lastFmConfigured,
            lastFm = ScrobbleProviderState(
                connected = preferences.getBoolean(LAST_FM_CONNECTED, false) && secretStore.read(LAST_FM_SESSION_KEY) != null,
                enabled = preferences.getBoolean(LAST_FM_ENABLED, false),
                username = preferences.getString(LAST_FM_USERNAME, null),
                pendingApproval = preferences.getBoolean(LAST_FM_PENDING, false),
                error = preferences.getString(LAST_FM_ERROR, null),
            ).normalized(),
            listenBrainz = ScrobbleProviderState(
                connected = preferences.getBoolean(LISTEN_BRAINZ_CONNECTED, false) && secretStore.read(LISTEN_BRAINZ_TOKEN) != null,
                enabled = preferences.getBoolean(LISTEN_BRAINZ_ENABLED, false),
                username = preferences.getString(LISTEN_BRAINZ_USERNAME, null),
                pendingApproval = preferences.getBoolean(LISTEN_BRAINZ_PENDING, false),
                error = preferences.getString(LISTEN_BRAINZ_ERROR, null),
            ).normalized(),
        )
    }

    override suspend fun setEnabled(provider: ScrobbleProvider, enabled: Boolean) {
        val connected = when (provider) {
            ScrobbleProvider.LastFm -> lastFmSession() != null
            ScrobbleProvider.ListenBrainz -> listenBrainzToken() != null
        }
        val preference = if (provider == ScrobbleProvider.LastFm) LAST_FM_ENABLED else LISTEN_BRAINZ_ENABLED
        preferences.edit { putBoolean(preference, enabled && connected) }
    }

    override suspend fun setError(provider: ScrobbleProvider, message: String?) = withContext(Dispatchers.IO) {
        val preference = if (provider == ScrobbleProvider.LastFm) LAST_FM_ERROR else LISTEN_BRAINZ_ERROR
        preferences.edit { if (message.isNullOrBlank()) remove(preference) else putString(preference, message.take(MAX_ERROR_LENGTH)) }
    }

    override suspend fun connectLastFm(session: LastFmSession) = withContext(Dispatchers.IO) {
        secretStore.write(LAST_FM_SESSION_KEY, session.sessionKey)
        secretStore.remove(LAST_FM_PENDING_TOKEN)
        preferences.edit {
            putBoolean(LAST_FM_CONNECTED, true)
            putBoolean(LAST_FM_ENABLED, true)
            putString(LAST_FM_USERNAME, session.username)
            remove(LAST_FM_ERROR)
            remove(LAST_FM_PENDING_TOKEN)
            putBoolean(LAST_FM_PENDING, false)
        }
    }

    override suspend fun disconnectLastFm() = withContext(Dispatchers.IO) {
        secretStore.remove(LAST_FM_SESSION_KEY)
        secretStore.remove(LAST_FM_PENDING_TOKEN)
        preferences.edit {
            putBoolean(LAST_FM_CONNECTED, false)
            putBoolean(LAST_FM_ENABLED, false)
            remove(LAST_FM_USERNAME)
            remove(LAST_FM_ERROR)
            remove(LAST_FM_PENDING_TOKEN)
            putBoolean(LAST_FM_PENDING, false)
        }
    }

    override suspend fun connectListenBrainz(account: ListenBrainzAccount, token: String) = withContext(Dispatchers.IO) {
        secretStore.write(LISTEN_BRAINZ_TOKEN, token)
        preferences.edit {
            putBoolean(LISTEN_BRAINZ_CONNECTED, true)
            putBoolean(LISTEN_BRAINZ_ENABLED, true)
            putBoolean(LISTEN_BRAINZ_PENDING, false)
            putString(LISTEN_BRAINZ_USERNAME, account.username)
            remove(LISTEN_BRAINZ_ERROR)
        }
    }

    override suspend fun disconnectListenBrainz() = withContext(Dispatchers.IO) {
        secretStore.remove(LISTEN_BRAINZ_TOKEN)
        preferences.edit {
            putBoolean(LISTEN_BRAINZ_CONNECTED, false)
            putBoolean(LISTEN_BRAINZ_ENABLED, false)
            putBoolean(LISTEN_BRAINZ_PENDING, false)
            remove(LISTEN_BRAINZ_USERNAME)
            remove(LISTEN_BRAINZ_ERROR)
        }
    }

    override suspend fun lastFmSession(): LastFmSession? = withContext(Dispatchers.IO) {
        if (!preferences.getBoolean(LAST_FM_CONNECTED, false)) return@withContext null
        val username = preferences.getString(LAST_FM_USERNAME, null)?.takeIf(String::isNotBlank) ?: return@withContext null
        val key = secretStore.read(LAST_FM_SESSION_KEY)?.takeIf(String::isNotBlank) ?: return@withContext null
        LastFmSession(username, key)
    }

    override suspend fun listenBrainzToken(): String? = withContext(Dispatchers.IO) {
        if (!preferences.getBoolean(LISTEN_BRAINZ_CONNECTED, false)) return@withContext null
        secretStore.read(LISTEN_BRAINZ_TOKEN)?.takeIf(String::isNotBlank)
    }

    fun beginLastFmApproval(token: String) {
        secretStore.write(LAST_FM_PENDING_TOKEN, token)
        preferences.edit { putBoolean(LAST_FM_PENDING, true) }
    }

    fun pendingLastFmToken(): String? = if (preferences.getBoolean(LAST_FM_PENDING, false)) secretStore.read(LAST_FM_PENDING_TOKEN) else null

    fun beginListenBrainzTokenEntry() {
        preferences.edit { putBoolean(LISTEN_BRAINZ_PENDING, true) }
    }

    private fun ScrobbleProviderState.normalized() = copy(
        connected = connected,
        enabled = connected && enabled,
        username = username?.takeIf { connected && it.isNotBlank() },
        pendingApproval = pendingApproval && !connected,
    )

    private companion object {
        const val ACCOUNT_PREFERENCES = "scrobbling-accounts"
        const val LAST_FM_CONNECTED = "lastfm.connected"
        const val LAST_FM_ENABLED = "lastfm.enabled"
        const val LAST_FM_USERNAME = "lastfm.username"
        const val LAST_FM_PENDING_TOKEN = "lastfm.pending-token"
        const val LAST_FM_PENDING = "lastfm.pending"
        const val LAST_FM_ERROR = "lastfm.error"
        const val LAST_FM_SESSION_KEY = "lastfm.session-key"
        const val LISTEN_BRAINZ_CONNECTED = "listenbrainz.connected"
        const val LISTEN_BRAINZ_ENABLED = "listenbrainz.enabled"
        const val LISTEN_BRAINZ_USERNAME = "listenbrainz.username"
        const val LISTEN_BRAINZ_PENDING = "listenbrainz.pending"
        const val LISTEN_BRAINZ_ERROR = "listenbrainz.error"
        const val LISTEN_BRAINZ_TOKEN = "listenbrainz.token"
        const val MAX_ERROR_LENGTH = 240
    }
}
