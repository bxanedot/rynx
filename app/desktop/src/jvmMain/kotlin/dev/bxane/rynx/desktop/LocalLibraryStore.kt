package dev.bxane.rynx.desktop

import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.*

/** Stores file references only; never copies, modifies or removes the user's media. */
internal class LocalLibraryStore(private val index: File) {
    fun load():List<File> {
        if(!index.exists()) return emptyList()
        if(index.length()>2*1024*1024) throw IOException("Library index exceeds size limit")
        val lines=index.readLines()
        if(lines.firstOrNull()!="rynx-library-v1") throw IOException("Unknown library index format")
        return try {
            lines.drop(1).filter{it.isNotBlank()}.map{line->
                val uri=URI(line)
                if(uri.scheme!="file") throw IOException("Unsupported library entry")
                File(uri).canonicalFile
            }.distinct().filter{it.isFile}
        } catch(error:IllegalArgumentException) {throw IOException("Invalid library entry",error)}
        catch(error:java.net.URISyntaxException) {throw IOException("Invalid library entry",error)}
    }
    fun save(files:List<File>) {
        val text=(listOf("rynx-library-v1")+files.map{it.canonicalFile.toURI().toASCIIString()}.distinct()).joinToString("\n",postfix="\n")
        if(text.toByteArray(Charsets.UTF_8).size>2*1024*1024) throw IOException("Library index exceeds size limit")
        val path=index.toPath().toAbsolutePath()
        Files.createDirectories(path.parent)
        val temporary=Files.createTempFile(path.parent,"library-",".tmp")
        try {
            Files.writeString(temporary,text)
            try { Files.move(temporary,path,ATOMIC_MOVE,REPLACE_EXISTING) }
            catch(_:AtomicMoveNotSupportedException) { Files.move(temporary,path,REPLACE_EXISTING) }
        } finally {Files.deleteIfExists(temporary)}
    }
}
