package dev.bxane.rynx.android

import android.content.ComponentName
import android.content.ContentValues
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlaybackResumeTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()

    @Test fun serviceRestoresLocalQueuePausedAtSavedPositionAndOrder() {
        val context=instrumentation.targetContext
        val first=insertAudio("Rynx resume one")
        val second=insertAudio("Rynx resume two")
        forceStopApp(context)
        var expectedOrder:List<Int> = emptyList()
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val controller=connectController(context)
                try {
                    instrumentation.runOnMainSync {
                        val items=listOf(first,second).mapIndexed { index,uri ->
                            MediaItem.Builder()
                                .setMediaId(uri.toString())
                                .setUri(uri)
                                .setMediaMetadata(MediaMetadata.Builder()
                                    .setTitle("Resume ${index+1}")
                                    .setArtist("Rynx test")
                                    .build())
                                .build()
                        }
                        controller.setMediaItems(items,1,1_200)
                        controller.repeatMode=androidx.media3.common.Player.REPEAT_MODE_ALL
                        controller.shuffleModeEnabled=true
                        controller.prepare()
                        controller.pause()
                    }
                    awaitQueue(controller,2)
                    awaitTimeline(controller,2)
                    instrumentation.runOnMainSync {
                        expectedOrder=playbackOrder(controller.currentTimeline,true)
                    }
                    // Let PlaybackService's normal checkpoint loop write the queue before simulating
                    // process death. The test does not seed the service's private preferences itself.
                    SystemClock.sleep(2_500)
                    instrumentation.runOnMainSync {
                        assertEquals(2,controller.mediaItemCount)
                        assertEquals(second.toString(),controller.currentMediaItem?.mediaId)
                        assertTrue(controller.shuffleModeEnabled)
                        assertFalse(controller.playWhenReady)
                    }
                } finally {
                    instrumentation.runOnMainSync { controller.release() }
                }
            }

            forceStopApp(context)
            ActivityScenario.launch(MainActivity::class.java).use {
                val controller=connectController(context)
                try {
                    awaitQueue(controller,2)
                    awaitTimeline(controller,2)
                    instrumentation.runOnMainSync {
                        assertEquals(2,controller.mediaItemCount)
                        assertEquals(second.toString(),controller.currentMediaItem?.mediaId)
                        assertEquals(1,controller.currentMediaItemIndex)
                        assertEquals(androidx.media3.common.Player.REPEAT_MODE_ALL,controller.repeatMode)
                        assertTrue(controller.shuffleModeEnabled)
                        assertEquals(expectedOrder,playbackOrder(controller.currentTimeline,true))
                        assertFalse("Process restoration must not start audio unexpectedly",controller.playWhenReady)
                        assertTrue("Restored seek position should remain near the saved point",controller.currentPosition in 900..2_000)
                    }
                } finally {
                    instrumentation.runOnMainSync { controller.release() }
                }
            }
        } finally {
            forceStopApp(context)
            AndroidPlaybackResumeStore(context).saveDurably(null)
            context.contentResolver.delete(first,null,null)
            context.contentResolver.delete(second,null,null)
        }
    }

    private fun connectController(context:android.content.Context):MediaController {
        lateinit var future:com.google.common.util.concurrent.ListenableFuture<MediaController>
        instrumentation.runOnMainSync {
            future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()
        }
        return future.get(20,TimeUnit.SECONDS)
    }

    private fun awaitQueue(controller:MediaController,count:Int) {
        val deadline=SystemClock.elapsedRealtime()+10_000
        while(mediaItemCount(controller)!=count && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(50)
        assertEquals("The Media3 service did not expose the expected queue",count,mediaItemCount(controller))
    }

    private fun awaitTimeline(controller:MediaController,count:Int) {
        val deadline=SystemClock.elapsedRealtime()+10_000
        while(timelineWindowCount(controller)!=count && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(50)
        assertEquals("The Media3 timeline did not prepare the expected queue",count,timelineWindowCount(controller))
    }

    private fun mediaItemCount(controller:MediaController):Int {
        var count=0
        instrumentation.runOnMainSync { count=controller.mediaItemCount }
        return count
    }

    private fun timelineWindowCount(controller:MediaController):Int {
        var count=0
        instrumentation.runOnMainSync { count=controller.currentTimeline.windowCount }
        return count
    }

    private fun insertAudio(title:String):Uri {
        val resolver=instrumentation.targetContext.contentResolver
        val values=ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,"$title.wav")
            put(MediaStore.MediaColumns.MIME_TYPE,"audio/wav")
            put(MediaStore.MediaColumns.RELATIVE_PATH,"Music/RynxResumeTest")
        }
        val uri=resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)
            ?:throw FileNotFoundException("Unable to create Android media test fixture")
        resolver.openOutputStream(uri,"w")!!.use { output -> output.write(wav()) }
        return uri
    }

    private fun forceStopApp(context:android.content.Context) {
        val descriptor=instrumentation.uiAutomation.executeShellCommand("am force-stop ${context.packageName}")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        instrumentation.waitForIdleSync()
    }

    private fun wav():ByteArray {
        val pcm=ByteArray(8_000*2*10)
        val header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray());putInt(36+pcm.size);put("WAVEfmt ".toByteArray());putInt(16)
            putShort(1);putShort(1);putInt(8_000);putInt(16_000);putShort(2);putShort(16)
            put("data".toByteArray());putInt(pcm.size)
        }.array()
        return header+pcm
    }
}
