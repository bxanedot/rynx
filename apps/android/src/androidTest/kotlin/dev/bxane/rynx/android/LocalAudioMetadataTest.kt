package dev.bxane.rynx.android

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalAudioMetadataTest {
    @Test fun embeddedTitleArtistAndCoverReachThePlayerModel() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val fixture=File(context.cacheDir,"tagged-silence.mp3")
        instrumentation.context.assets.open("tagged-silence.mp3").use { input -> fixture.outputStream().use(input::copyTo) }
        try {
            val track=LocalAudioMetadata(context).read(Uri.fromFile(fixture),"Filename.mp3")
            assertEquals("Evening walk",track.title)
            assertEquals("bxane",track.subtitle)
            assertNotNull(track.artworkUrl)
            val image=BitmapFactory.decodeFile(Uri.parse(track.artworkUrl).path)
            assertNotNull("The player needs a decoded cover, not just a URI string",image)
            assertEquals(32,image.width)
            image.recycle()
        } finally {fixture.delete()}
    }
    @Test fun unreadableAudioRetainsItsDisplayNameWithoutInventingMetadata() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val track=LocalAudioMetadata(context).read(Uri.fromFile(File(context.cacheDir,"missing-audio")),"Rain.wav")
        assertEquals("Rain.wav",track.title)
        assertEquals("Local audio",track.subtitle)
        assertNull(track.artworkUrl)
    }
}
