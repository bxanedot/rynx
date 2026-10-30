package dev.bxane.rynx.android

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/** Adds the Rynx processor to the existing Media3 audio sink; the player and session remain unchanged. */
@UnstableApi
internal class RynxAudioRenderersFactory(
    context: Context,
    private val rynxProcessor: AudioProcessor,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        // Media3's float-output branch skips the custom AudioProcessor chain for high-resolution PCM.
        // Keep Rynx's real EQ/protection/dither attached to the PCM chain. This does not imply
        // 24/32-bit output: Media3 may down-convert to 16-bit on this path.
        .setEnableFloatOutput(false)
        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        .setAudioProcessors(arrayOf(rynxProcessor))
        .build()
}
