package dev.bxane.rynx.android

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.bxane.rynx.ui.TrackItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @Test fun favoriteMetadataRoundTripsForRemotePlayback() {
        val expected=listOf(TrackItem("yt:track-1","Song, \"live\"","Artist\nname","https://example.test/art.jpg"))
        assertEquals(expected,TrackMetadataStorage.decode(TrackMetadataStorage.encode(expected)))
    }

    @Test fun phoneActivityLaunchesAndSurvivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { assertEquals("rynx", it.title.toString()) }
        }
    }
}
