package dev.bxane.rynx.android

import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BackgroundListeningTest {
    @Test fun foregroundServiceLearnsActualListeningWhileActivityIsStopped() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val context=instrumentation.targetContext
        val preferences=context.getSharedPreferences("discovery",0)
        val previous=preferences.getString("signals",null)
        val wave=File(context.cacheDir,"rynx-background-listening.wav")
        val data=ByteArray(16_000*45)
        val header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray());putInt(36+data.size);put("WAVEfmt ".toByteArray());putInt(16)
            putShort(1);putShort(1);putInt(8000);putInt(16000);putShort(2);putShort(16)
            put("data".toByteArray());putInt(data.size)
        }.array()
        wave.outputStream().use {it.write(header);it.write(data)}
        val learned=CountDownLatch(1)
        val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener {p,key->
            if(key=="signals" && p.getString(key,"").orEmpty().contains("background-listening-test"))learned.countDown()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        try {
            ActivityScenario.launch(MainActivity::class.java).use {scenario->
                lateinit var future:com.google.common.util.concurrent.ListenableFuture<MediaController>
                instrumentation.runOnMainSync {future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()}
                val controller=future.get(30,TimeUnit.SECONDS)
                try {
                    val started=CountDownLatch(1)
                    instrumentation.runOnMainSync {
                        controller.addListener(object:Player.Listener {override fun onIsPlayingChanged(isPlaying:Boolean){if(isPlaying)started.countDown()}})
                        controller.setMediaItem(MediaItem.Builder().setMediaId("background-listening-test").setUri(Uri.fromFile(wave))
                            .setMediaMetadata(MediaMetadata.Builder().setTitle("Original background PCM").setArtist("bxane").build()).build())
                        controller.prepare();controller.play()
                    }
                    assertTrue("Generated PCM must play",started.await(30,TimeUnit.SECONDS))
                    scenario.moveToState(Lifecycle.State.CREATED)
                    assertTrue("Service must record 30 seconds of listening without Activity publication",learned.await(45,TimeUnit.SECONDS))
                } finally {instrumentation.runOnMainSync {controller.stop();controller.clearMediaItems();controller.release()}}
            }
        } finally {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            preferences.edit().apply {if(previous==null)remove("signals") else putString("signals",previous)}.apply()
            wave.delete()
        }
    }
}
