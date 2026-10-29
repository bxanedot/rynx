package dev.bxane.rynx.android

import android.content.Context
import androidx.core.content.edit
import dev.bxane.rynx.playback.PlaybackResumeCodec
import dev.bxane.rynx.playback.PlaybackResumeSnapshot

/** Stores only bounded, validated local-queue checkpoints; stream URLs and headers are never accepted. */
internal class AndroidPlaybackResumeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): PlaybackResumeSnapshot? {
        val encoded = runCatching { preferences.getString(KEY, null) }.getOrNull()
        val decoded = PlaybackResumeCodec.decode(encoded)
        if (preferences.contains(KEY) && decoded == null) clear()
        return decoded
    }

    fun save(snapshot: PlaybackResumeSnapshot?) {
        val encoded = snapshot?.let(PlaybackResumeCodec::encode)
        preferences.edit {
            if (encoded == null) remove(KEY) else putString(KEY, encoded)
        }
    }

    fun clear() = preferences.edit { remove(KEY) }

    private companion object {
        const val FILE = "playback-resume"
        const val KEY = "local-queue-v1"
    }
}
