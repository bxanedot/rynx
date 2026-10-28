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

/** Real lyrics import after host recreation, using an app-owned temporary document. */
@RunWith(AndroidJUnit4::class)
class LyricsImportRecreationTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun node(label:String):AccessibilityNodeInfo? {
        fun visit(n:AccessibilityNodeInfo):AccessibilityNodeInfo? {
            if(n.contentDescription?.toString()?.let {it==label || it.startsWith("$label, ")}==true || n.text?.toString()==label)return n
            for(i in 0 until n.childCount)n.getChild(i)?.let {visit(it)?.let {return it}}
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
    }
    private fun accessibilitySnapshot():String {
        val root=instrumentation.uiAutomation.rootInActiveWindow ?: return "No active accessibility window."
        val nodes=mutableListOf<String>()
        fun visit(n:AccessibilityNodeInfo) {
            val label=n.contentDescription?.toString()
            val text=n.text?.toString()
            if(label!=null || text!=null)nodes += "${n.className}: description=$label, text=$text"
            for(i in 0 until n.childCount)n.getChild(i)?.let(::visit)
        }
        visit(root)
        return "Active package=${root.packageName}; nodes=${nodes.take(100)}"
    }
    private fun waitFor(label:String):AccessibilityNodeInfo {
        val deadline=SystemClock.elapsedRealtime()+30_000
        while(SystemClock.elapsedRealtime()<deadline){node(label)?.let{return it};SystemClock.sleep(100)}
        error("No accessible document/player control: $label. ${accessibilitySnapshot()}")
    }
    private fun click(label:String) {
        var target:AccessibilityNodeInfo?=waitFor(label)
        val details=mutableListOf<String>()
        while(target!=null) {
            details+="clickable=${target.isClickable}, enabled=${target.isEnabled}, actions=${target.actionList.map {it.id}}"
            if(target.isClickable && target.isEnabled && target.performAction(AccessibilityNodeInfo.ACTION_CLICK))return
            target=target.parent
        }
        fail("Could not activate $label: $details")
    }
    @Test fun currentTrackRemainsAvailableForLyricsImportAfterHostRecreation() {
        assumeTrue(Build.VERSION.SDK_INT>=29)
        val context=instrumentation.targetContext
        val audio=File(context.cacheDir,"lyrics-recreation.mp3")
        instrumentation.context.assets.open("tagged-silence.mp3").use {input->audio.outputStream().use(input::copyTo)}
        val resolver=context.contentResolver
        val filename="rynx-import-${SystemClock.elapsedRealtime()}.lrc"
        val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,filename);put(MediaStore.MediaColumns.MIME_TYPE,"text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/")
        })!!
        resolver.openOutputStream(uri)!!.use {it.write("[00:00]Persisted through host recreation".toByteArray())}
        try {
            ActivityScenario.launch(MainActivity::class.java).use {scenario->
                lateinit var future:com.google.common.util.concurrent.ListenableFuture<MediaController>
                instrumentation.runOnMainSync {future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()}
                val controller=future.get(30,TimeUnit.SECONDS)
                try {
                    instrumentation.runOnMainSync {
                        controller.setMediaItem(MediaItem.Builder().setMediaId("lyrics-recreation-test")
                            .setUri(Uri.fromFile(audio))
                            .setMediaMetadata(MediaMetadata.Builder().setTitle("Lyrics import test").build()).build())
                    }
                    val originalActivity=java.util.concurrent.atomic.AtomicReference<MainActivity>()
                    scenario.onActivity { originalActivity.set(it) }
                    // Recreate the host while it owns the foreground. The media session remains
                    // the authority for the current track; the real picker is launched afterward.
                    scenario.recreate()
                    scenario.onActivity {
                        assertNotSame("ActivityScenario must provide a recreated host",originalActivity.get(),it)
                    }
                    click("Open Now Playing")
                    waitFor("Lyrics import test")
                    click("Show lyrics")
                    click("Add timed lyrics")
                    click(filename)
                    waitFor("Seek to lyric Persisted through host recreation")
                } finally {instrumentation.runOnMainSync {controller.clearMediaItems();controller.release()}}
            }
        } finally {
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            audio.delete()
            resolver.delete(uri,null,null)
            context.getSharedPreferences("timed-lyrics",0).edit().remove("lyrics-recreation-test").apply()
        }
    }
}
