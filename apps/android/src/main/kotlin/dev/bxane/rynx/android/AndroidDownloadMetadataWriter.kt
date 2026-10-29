package dev.bxane.rynx.android

import dev.bxane.rynx.playback.download.DownloadBrandingMetadata
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.charset.StandardCharsets.UTF_8

/** Writes portable attribution next to a completed download without modifying the audio/video bytes. */
internal object AndroidDownloadMetadataWriter {
    fun write(mediaFile: File, metadata: DownloadBrandingMetadata): File {
        require(mediaFile.isFile) { "The completed media file must exist before attribution is written" }
        val directory=mediaFile.absoluteFile.parentFile ?: error("Media file has no parent directory")
        val destination=File(directory,mediaFile.name+SIDECAR_SUFFIX)
        val temporary=File(directory,".${destination.name}.${System.nanoTime()}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(metadata.toSidecarJson().toByteArray(UTF_8))
                output.fd.sync()
            }
            try {
                Files.move(temporary.toPath(),destination.toPath(),ATOMIC_MOVE,REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(),destination.toPath(),REPLACE_EXISTING)
            }
            return destination
        } finally {
            if(temporary.exists()) temporary.delete()
        }
    }

    private const val SIDECAR_SUFFIX=".rynx.json"
}
