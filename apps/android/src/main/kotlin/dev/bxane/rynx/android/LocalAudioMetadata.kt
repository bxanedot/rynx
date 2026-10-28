package dev.bxane.rynx.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AtomicFile
import androidx.annotation.WorkerThread
import dev.bxane.rynx.ui.TrackItem
import java.io.File
import java.security.MessageDigest

/** Reads only user-authorized audio; the bounded cover cache never stores the audio itself. */
internal class LocalAudioMetadata(private val context: Context) {
    @WorkerThread
    fun read(uri: Uri, displayName: String): TrackItem {
        var title = displayName
        var artist = "Local audio"
        var cover: String? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf(String::isNotBlank)?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf(String::isNotBlank)?.let { artist = it }
            cover = retriever.embeddedPicture?.let { cacheCover(uri, it) }
        } catch (_: Exception) {
            // SAF names remain useful when a codec, tag, or revoked document cannot be read.
        } finally { retriever.release() }
        return TrackItem(uri.toString(), title, artist, cover)
    }

    private fun cacheCover(uri: Uri, bytes: ByteArray): String? {
        if (bytes.size > 16 * 1024 * 1024) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > 1200) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        try {
            val directory = File(context.cacheDir, "cover-art").apply { mkdirs() }
            val key = MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray()).joinToString("") { "%02x".format(it) }
            val file = File(directory, "$key.jpg")
            val atomic = AtomicFile(file)
            val output = atomic.startWrite()
            try {
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output))
                atomic.finishWrite(output)
            } catch (failure: Exception) { atomic.failWrite(output); throw failure }
            return Uri.fromFile(file).toString()
        } finally { bitmap.recycle() }
    }
}
