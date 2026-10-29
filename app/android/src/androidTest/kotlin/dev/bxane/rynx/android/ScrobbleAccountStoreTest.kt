package dev.bxane.rynx.android

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import dev.bxane.rynx.playback.scrobble.LastFmSession
import dev.bxane.rynx.playback.scrobble.ListenBrainzAccount
import dev.bxane.rynx.playback.scrobble.ScrobbleProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrobbleAccountStoreTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun successfulLoginsEnableScrobblingAndUserOptOutPersists() = runBlocking {
        val store = AndroidScrobbleAccountStore(context, lastFmConfigured = true)
        try {
            store.connectLastFm(LastFmSession("lastfm-user", "session-secret"))
            store.connectListenBrainz(ListenBrainzAccount("listenbrainz-user"), "lb-secret")
            var state = store.load()
            assertTrue(state.lastFm.connected)
            assertTrue(state.lastFm.enabled)
            assertEquals("lastfm-user", state.lastFm.username)
            assertTrue(state.listenBrainz.connected)
            assertTrue(state.listenBrainz.enabled)
            assertEquals("listenbrainz-user", state.listenBrainz.username)

            store.setEnabled(ScrobbleProvider.LastFm, false)
            state = AndroidScrobbleAccountStore(context, true).load()
            assertFalse(state.lastFm.enabled)
            assertTrue(state.lastFm.connected)
        } finally {
            store.disconnectLastFm()
            store.disconnectListenBrainz()
        }
    }

    @Test fun secureStoreDoesNotPersistTokensAsPlaintextAndDeletesOnDisconnect() = runBlocking {
        val secure = AndroidKeystoreSecretStore(context)
        val accountStore = AndroidScrobbleAccountStore(context, lastFmConfigured = false, secretStore = secure)
        val secrets = context.getSharedPreferences("scrobbling-secrets", Context.MODE_PRIVATE)
        try {
            val token = "private-listenbrainz-token"
            secure.write("test-token", token)
            assertEquals(token, secure.read("test-token"))
            assertFalse(secrets.getString("test-token", "").orEmpty().contains(token))
            secure.remove("test-token")
            assertNull(secure.read("test-token"))

            accountStore.connectListenBrainz(ListenBrainzAccount("user"), token)
            assertEquals(token, accountStore.listenBrainzToken())
            assertTrue(accountStore.load().listenBrainz.enabled)
            accountStore.disconnectListenBrainz()
            assertNull(accountStore.listenBrainzToken())
            assertFalse(accountStore.load().listenBrainz.enabled)
        } finally {
            accountStore.disconnectLastFm()
            accountStore.disconnectListenBrainz()
            secure.remove("test-token")
        }
    }

    @Test fun lastFmIsReportedUnavailableWhenBuildCredentialsAreMissing() = runBlocking {
        val store = AndroidScrobbleAccountStore(context, lastFmConfigured = false)
        assertFalse(store.load().lastFmConfigured)
        assertNull(store.lastFmSession())
    }
}
