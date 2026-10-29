package dev.bxane.rynx.android

import android.content.Context
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
        write(snapshot, waitForDisk = false)
    }

    /** Flushes a test or handoff checkpoint before a service is started immediately afterward. */
    fun saveDurably(snapshot: PlaybackResumeSnapshot?): Boolean = write(snapshot, waitForDisk = true)

    private fun write(snapshot: PlaybackResumeSnapshot?, waitForDisk: Boolean): Boolean {
        val encoded = snapshot?.let(PlaybackResumeCodec::encode)
        val editor = preferences.edit()
        if (encoded == null) editor.remove(KEY) else editor.putString(KEY, encoded)
        return if (waitForDisk) editor.commit() else { editor.apply(); true }
    }

    fun clear() { preferences.edit().remove(KEY).apply() }

    private companion object {
        const val FILE = "playback-resume"
        const val KEY = "local-queue-v1"
    }
}
