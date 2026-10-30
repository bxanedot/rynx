package dev.bxane.rynx.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.bxane.rynx.playback.audio.AudioDspSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioDspSettingsPersistenceTest {
    @Test fun ditherAndPureModeSurviveStoreRecreationWithoutASecondSettingsStore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidAudioDspSettingsStore(context)
        val original = store.load()
        try {
            val withDither = original.copy(ditherEnabled = true, skipSilenceEnabled = true, pureMode = false)
            store.save(withDither)
            assertEquals(withDither.normalized(), AndroidAudioDspSettingsStore(context).load())

            val pure = withDither.copy(pureMode = true)
            store.save(pure)
            assertEquals(pure.normalized(), AndroidAudioDspSettingsStore(context).load())
        } finally {
            store.save(original)
        }
    }
}
