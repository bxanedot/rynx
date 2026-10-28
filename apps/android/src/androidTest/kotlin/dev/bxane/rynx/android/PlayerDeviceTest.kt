package dev.bxane.rynx.android

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Exercises Rynx accessibility actions through the activity and actual Media3 service. */
@RunWith(AndroidJUnit4::class)
class PlayerDeviceTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun eventually(message:String,test:()->Boolean) {
        val deadline=SystemClock.elapsedRealtime()+30_000
        while(SystemClock.elapsedRealtime()<deadline) { if(test())return;SystemClock.sleep(100) }
        fail(message)
    }
    private fun findNode(label:String):AccessibilityNodeInfo? {
        fun visit(node:AccessibilityNodeInfo):AccessibilityNodeInfo? {
            if(node.contentDescription?.toString()==label)return node
            for(index in 0 until node.childCount) { val child=node.getChild(index) ?: continue;visit(child)?.let {return it} }
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
    }
    private fun control(label:String):AccessibilityNodeInfo {
        var found:AccessibilityNodeInfo?=null
        eventually("No accessible player control: $label") { found=findNode(label);found!=null }
        return found!!
    }
    private fun click(label:String) { assertTrue("Could not click $label",control(label).performAction(AccessibilityNodeInfo.ACTION_CLICK)) }
    private fun capture(name:String) {
        val screenshot=instrumentation.uiAutomation.takeScreenshot()
        assertNotNull("A rendered device frame is required",screenshot)
        File(instrumentation.targetContext.filesDir,name).outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
    }

    @Test fun playerActionsReachMedia3AndLyricsRestoreForTheSelectedTrack() {
        val context=instrumentation.targetContext
        val audio=context.getSystemService(AudioManager::class.java)
        val originalVolume=audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val wave=File(context.cacheDir,"rynx-player-device-test.wav")
        val lyricFile=File(context.cacheDir,"rynx-player-device-test.lrc")
        val samples=ByteArray(16_000*20)
        val header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray());putInt(36+samples.size);put("WAVEfmt ".toByteArray());putInt(16)
            putShort(1);putShort(1);putInt(8000);putInt(16000);putShort(2);putShort(16)
            put("data".toByteArray());putInt(samples.size)
        }.array()
        wave.outputStream().use {it.write(header);it.write(samples)}
        lyricFile.writeText("[00:00]Evening begins\n[00:05]Moonlit road\n[00:15]Home again")
        runBlocking { AndroidTimedLyricsStore(context).import("device-test:a",Uri.fromFile(lyricFile)) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                lateinit var future:com.google.common.util.concurrent.ListenableFuture<MediaController>
                instrumentation.runOnMainSync { future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync() }
                val controller=future.get(30,TimeUnit.SECONDS)
                try {
                    instrumentation.runOnMainSync {
                        controller.shuffleModeEnabled=false
                        controller.setMediaItems(listOf("a","b","c").map { id ->
                            MediaItem.Builder().setMediaId("device-test:$id").setUri(Uri.fromFile(wave))
                                .setMediaMetadata(MediaMetadata.Builder().setTitle(if(id=="a")"Evening walk" else "Track $id").setArtist("bxane").build()).build()
                        })
                        controller.prepare();controller.play()
                    }
                    eventually("Generated PCM did not start in the session") {
                        var playing=false;instrumentation.runOnMainSync {playing=controller.isPlaying && controller.duration>0};playing
                    }
                    instrumentation.runOnMainSync {controller.pause()}
                    click("Open Now Playing");control("Playback volume")
                    capture("rynx-device-player.png")
                    val volume=.35f
                    assertTrue(control("Playback volume").performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,Bundle().apply {putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,volume)}))
                    eventually("Player volume did not reach Android's music stream") {
                        val maximum=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        kotlin.math.abs(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()/maximum-volume)<=1f/maximum
                    }
                    click("Show lyrics");click("Seek to lyric Moonlit road")
                    eventually("Lyric tap did not seek the media session") {
                        var position=0L;instrumentation.runOnMainSync {position=controller.currentPosition};position in 4_500..5_500
                    }
                    click("Show queue");click("Shuffle")
                    eventually("Shuffle action did not reach Media3") {
                        var enabled=false;instrumentation.runOnMainSync {enabled=controller.shuffleModeEnabled};enabled
                    }
                    // The original timeline indexes need not match the shuffled display order.
                    var upcoming=emptyList<Int>()
                    var selectedId=""
                    instrumentation.runOnMainSync {
                        val order=playbackOrder(controller.currentTimeline,true)
                        controller.seekToDefaultPosition(order.first())
                        selectedId=controller.currentMediaItem!!.mediaId
                        upcoming=order.drop(1)
                    }
                    val sourceIndex=upcoming.last()
                    val destinationIndex=upcoming.first()
                    var sourceTitle=""
                    instrumentation.runOnMainSync {
                        sourceTitle=controller.getMediaItemAt(sourceIndex).mediaMetadata.title.toString()
                    }
                    val reorder=control("Reorder $sourceTitle")
                    val up=reorder.actionList.first {action -> action.label?.toString()=="Move up"}
                    assertTrue(reorder.performAction(up.id))
                    eventually("Queue reorder did not update Media3's shuffle permutation") {
                        var updated=false
                        instrumentation.runOnMainSync {
                            val order=playbackOrder(controller.currentTimeline,true)
                            updated=order.indexOf(sourceIndex)<order.indexOf(destinationIndex) &&
                                controller.currentMediaItem?.mediaId==selectedId && controller.shuffleModeEnabled
                        }
                        updated
                    }
                    capture("rynx-device-queue.png")
                    click("Clear upcoming queue")
                    eventually("Clearing the queue must retain the playing song and remove only upcoming entries") {
                        var cleared=false
                        instrumentation.runOnMainSync {cleared=controller.mediaItemCount==1 && controller.currentMediaItem?.mediaId==selectedId}
                        cleared
                    }
                } finally { instrumentation.runOnMainSync { controller.stop();controller.clearMediaItems();controller.release() } }
            }
        } finally {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC,originalVolume,0)
            context.getSharedPreferences("timed-lyrics",0).edit().remove("device-test:a").apply()
            wave.delete();lyricFile.delete()
        }
    }
}
