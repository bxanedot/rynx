package dev.bxane.rynx.android

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import dev.bxane.rynx.playback.LyricLine
import dev.bxane.rynx.playback.parseTimedLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** User-selected LRC files are copied into private app storage, not retained as external URI grants. */
internal class AndroidTimedLyricsStore(private val context:Context) {
    private val preferences=context.getSharedPreferences("timed-lyrics",Context.MODE_PRIVATE)
    suspend fun load(trackId:String):List<LyricLine> = withContext(Dispatchers.Default) {
        preferences.getString(trackId,null)?.let(::parseTimedLyrics).orEmpty()
    }
    suspend fun import(trackId:String,uri:Uri):List<LyricLine> {
        val text=withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val buffer=CharArray(262_145);var length=0
                while(length<buffer.size) {
                    val read=reader.read(buffer,length,buffer.size-length)
                    if(read<0)break
                    length+=read
                }
                require(length<=262_144) { "Lyrics file is too large" }
                String(buffer,0,length)
            } ?: error("Lyrics file could not be opened")
        }
        val lines=withContext(Dispatchers.Default) { parseTimedLyrics(text) }
        require(lines.isNotEmpty()) { "No timed lyric lines found" }
        preferences.edit { putString(trackId,text) }
        return lines
    }
}
