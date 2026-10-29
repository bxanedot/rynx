package dev.bxane.rynx.android

import android.content.ComponentName
import android.content.ContentValues
import android.app.ActivityManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.bxane.rynx.playback.PlaybackResumeSnapshot
import dev.bxane.rynx.playback.PlaybackResumeTrack
import dev.bxane.rynx.playback.RepeatMode
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
        stopExistingPlaybackService(context)
        val store=AndroidPlaybackResumeStore(context)
        val snapshot=PlaybackResumeSnapshot(
            tracks=listOf(
                PlaybackResumeTrack(first.toString(),"Resume one","Rynx test"),
                PlaybackResumeTrack(second.toString(),"Resume two","Rynx test"),
            ),
            currentIndex=1,
            positionMillis=1_200,
            repeatMode=RepeatMode.All,
            shuffleEnabled=true,
            playbackOrder=listOf(1,0),
        )
        assertTrue("The seeded queue must reach disk before the service starts",store.saveDurably(snapshot))
        assertEquals("The local resume snapshot must be stored before starting the service",2,store.load()?.tracks?.size)
        try {
            lateinit var future:com.google.common.util.concurrent.ListenableFuture<MediaController>
            instrumentation.runOnMainSync {
                future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()
            }
            val controller=future.get(20,TimeUnit.SECONDS)
            try {
                val deadline=SystemClock.elapsedRealtime()+10_000
                while(mediaItemCount(controller)!=2 && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(50)
                instrumentation.runOnMainSync {
                    assertEquals(
                        "The Media3 service should restore two items; persisted count=${store.load()?.tracks?.size ?: 0}",
                        2,
                        controller.mediaItemCount,
                    )
                    assertEquals("Saved current item must be restored",second.toString(),controller.currentMediaItem?.mediaId)
                    assertEquals("Saved item position must be restored",1,controller.currentMediaItemIndex)
                    assertTrue("Restore must preserve repeat mode",controller.repeatMode==androidx.media3.common.Player.REPEAT_MODE_ALL)
                    assertTrue("Restore must preserve shuffle",controller.shuffleModeEnabled)
                    assertEquals(listOf(1,0),playbackOrder(controller.currentTimeline,true))
                    assertFalse("Process restoration must not start audio unexpectedly",controller.playWhenReady)
                    assertTrue("Restored seek position should remain near the saved point",controller.currentPosition in 900..2_000)
                }
            } finally {
                instrumentation.runOnMainSync { controller.release() }
            }
        } finally {
            stopExistingPlaybackService(context)
            store.saveDurably(null)
            context.contentResolver.delete(first,null,null)
            context.contentResolver.delete(second,null,null)
        }
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

    private fun mediaItemCount(controller:MediaController):Int {
        var count=0
        instrumentation.runOnMainSync { count=controller.mediaItemCount }
        return count
    }

    @Suppress("DEPRECATION")
    private fun stopExistingPlaybackService(context:android.content.Context) {
        val descriptor=instrumentation.uiAutomation.executeShellCommand("am force-stop ${context.packageName}")
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        instrumentation.waitForIdleSync()
        val activityManager=context.getSystemService(ActivityManager::class.java)
        val deadline=SystemClock.elapsedRealtime()+5_000
        while(SystemClock.elapsedRealtime()<deadline) {
            val running=activityManager.getRunningServices(100).any { it.service.className==PlaybackService::class.java.name }
            if(!running)return
            SystemClock.sleep(50)
        }
        assertFalse("An earlier playback service must stop before the test seeds its snapshot",
            activityManager.getRunningServices(100).any {it.service.className==PlaybackService::class.java.name})
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
