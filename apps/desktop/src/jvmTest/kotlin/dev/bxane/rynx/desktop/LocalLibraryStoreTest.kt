package dev.bxane.rynx.desktop

import java.nio.file.Files
import kotlin.test.*

class LocalLibraryStoreTest {
    @Test fun roundTripsUnicodePathsDeduplicatesAndSkipsMissingFiles() {
        val dir=Files.createTempDirectory("rynx-library-").toFile()
        try {
            val song=dir.resolve("A song — 夜.wav").apply{writeBytes(byteArrayOf(1))}
            val missing=dir.resolve("missing.wav")
            val store=LocalLibraryStore(dir.resolve("state/library.txt"))
            store.save(listOf(song,song,missing))
            assertEquals(listOf(song.canonicalFile),store.load())
            store.save(emptyList())
            assertEquals(emptyList(),store.load())
        } finally{dir.deleteRecursively()}
    }
    @Test fun corruptLibraryIsReportedAndRetainedForRecovery() {
        val dir=Files.createTempDirectory("rynx-library-").toFile()
        try {
            val index=dir.resolve("library.txt").apply{writeText("unexpected format")}
            assertFailsWith<java.io.IOException>{LocalLibraryStore(index).load()}
            assertEquals("unexpected format",index.readText())
        } finally{dir.deleteRecursively()}
    }
}
