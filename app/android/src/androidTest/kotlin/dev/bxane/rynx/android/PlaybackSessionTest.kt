package dev.bxane.rynx.android

import android.content.ComponentName
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlaybackSessionTest {
    @Test fun generatedPcmPlaysThroughServiceAndPausesFromController() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = java.io.File(context.cacheDir,"session-test.wav")
        val samples=ByteArray(16000*4)
        val header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray());putInt(36+samples.size);put("WAVEfmt ".toByteArray());putInt(16)
            putShort(1);putShort(1);putInt(8000);putInt(16000);putShort(2);putShort(16)
            put("data".toByteArray());putInt(samples.size)
        }.array()
        file.outputStream().use { it.write(header);it.write(samples) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                lateinit var future: com.google.common.util.concurrent.ListenableFuture<MediaController>
                instrumentation.runOnMainSync {
                    future=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()
                }
                val controller=future.get(15,TimeUnit.SECONDS)
                try {
                    instrumentation.runOnMainSync {
                        assertTrue("Own controller must be able to set media",controller.isCommandAvailable(androidx.media3.common.Player.COMMAND_SET_MEDIA_ITEM))
                        assertTrue("Own controller must be able to play",controller.isCommandAvailable(androidx.media3.common.Player.COMMAND_PLAY_PAUSE))
                        controller.setMediaItem(
                            MediaItem.Builder()
                                .setMediaId("session-test")
                                .setUri(Uri.fromFile(file))
                                .setMediaMetadata(MediaMetadata.Builder().setTitle("Generated session audio").build())
                                .build(),
                        )
                        controller.prepare();controller.play()
                    }
                    val deadline=SystemClock.elapsedRealtime()+10000
                    var progressed=false
                    while(!progressed&&SystemClock.elapsedRealtime()<deadline) {
                        instrumentation.runOnMainSync { progressed=controller.isPlaying&&controller.currentPosition>100 }
                        SystemClock.sleep(50)
                    }
                    assertTrue("Decoded PCM must advance through the real session: ${playbackDiagnostics(controller)}",progressed)
                    var pausedAt = 0L
                    instrumentation.runOnMainSync {
                        assertEquals("Generated session audio", controller.mediaMetadata.title.toString())
                        controller.pause()
                        pausedAt = controller.currentPosition
                        assertTrue("Controller must acknowledge pause before resuming",!controller.playWhenReady)
                    }
                    instrumentation.runOnMainSync { controller.play() }
                    val resumed=SystemClock.elapsedRealtime()+5000
                    var resumedPlayback=false
                    while(!resumedPlayback&&SystemClock.elapsedRealtime()<resumed) {
                        instrumentation.runOnMainSync { resumedPlayback=controller.isPlaying&&controller.currentPosition>pausedAt }
                        SystemClock.sleep(50)
                    }
                    assertTrue("MediaController must resume the same generated item: ${playbackDiagnostics(controller)}",resumedPlayback)
                    instrumentation.runOnMainSync { controller.stop();controller.clearMediaItems() }
                } finally { instrumentation.runOnMainSync { controller.release() } }
            }
        } finally { file.delete() }
    }
}
