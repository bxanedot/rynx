package dev.bxane.rynx.android

import android.content.ComponentName
import android.content.ContentValues
import android.os.Build
import android.net.Uri
import java.io.File
import android.os.SystemClock
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Real system-picker return after activity recreation, with an app-owned temporary document. */
@RunWith(AndroidJUnit4::class)
class LyricsImportRecreationTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun node(label:String):AccessibilityNodeInfo? {
        fun visit(n:AccessibilityNodeInfo):AccessibilityNodeInfo? {
            if(n.contentDescription?.toString()==label || n.text?.toString()==label)return n
            for(i in 0 until n.childCount)n.getChild(i)?.let {visit(it)?.let {return it}}
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
    }
    private fun waitFor(label:String):AccessibilityNodeInfo {
        val deadline=SystemClock.elapsedRealtime()+30_000
        while(SystemClock.elapsedRealtime()<deadline){node(label)?.let{return it};SystemClock.sleep(100)}
        error("No accessible document/player control: $label")
    }
    private fun click(label:String){assertTrue(waitFor(label).performAction(AccessibilityNodeInfo.ACTION_CLICK))}
    @Test fun pendingTrackIdentitySurvivesRotationWhileChoosingLyrics() {
        assumeTrue(Build.VERSION.SDK_INT>=29)
        val context=instrumentation.targetContext
        val audio=File(context.cacheDir,"lyrics-rotation.mp3")
        instrumentation.context.assets.open("tagged-silence.mp3").use {input->audio.outputStream().use(input::copyTo)}
        val resolver=context.contentResolver
        val filename="rynx-import-${SystemClock.elapsedRealtime()}.lrc"
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,filename);put(MediaStore.MediaColumns.MIME_TYPE,"text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/")
        })!!
        resolver.openOutputStream(uri)!!.use {it.write("[00:00]Persisted through rotation".toByteArray())}
        try {
            ActivityScenario.launch(MainActivity::class.java).use {scenario->
                lateinit var future:com.google.common.util.concurrent.ListenableFuture<MediaController>
                instrumentation.runOnMainSync {future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()}
                val controller=future.get(30,TimeUnit.SECONDS)
                try {
                    instrumentation.runOnMainSync {
                        controller.setMediaItem(MediaItem.Builder().setMediaId("lyrics-rotation-test")
                            .setUri(Uri.fromFile(audio))
                            .setMediaMetadata(MediaMetadata.Builder().setTitle("Lyrics import test").build()).build())
                    }
                    click("Open Now Playing");click("Show lyrics");click("Add timed lyrics")
                    scenario.recreate()
                    click(filename)
                    waitFor("Seek to lyric Persisted through rotation")
                } finally {instrumentation.runOnMainSync {controller.clearMediaItems();controller.release()}}
            }
        } finally {
            audio.delete()
            resolver.delete(uri,null,null)
            context.getSharedPreferences("timed-lyrics",0).edit().remove("lyrics-rotation-test").apply()
        }
    }
}
