package dev.bxane.rynx.desktop

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.nio.file.Files
import kotlin.test.*

class DesktopPlayerTest {
    @Test fun restoresLibraryAndPublishesUnreadableMediaFailure() = runBlocking {
        val dir=Files.createTempDirectory("rynx-session-").toFile()
        val media=dir.resolve("unavailable.wav").apply{writeText("invalid audio")}
        val store=LocalLibraryStore(dir.resolve("library.txt"))
        store.save(listOf(media))
        val player=DesktopPlayer(store)
        try {
            val restored=withTimeout(5000){player.state.first{it.tracks.size==1}}
            assertEquals("unavailable",restored.tracks.single().title)
            player.play(restored.tracks.single().id)
            val failed=withTimeout(5000){player.state.first{it.error!=null}}
            assertFalse(failed.playing)
            assertFalse(failed.busy)
            assertTrue(failed.error!!.contains("format is not supported"))
            player.dismissError()
            assertNull(player.state.value.error)
        } finally { player.close();dir.deleteRecursively() }
    }

    @Test fun importAfterCorruptRestoreDoesNotOverwriteOriginalIndex() = runBlocking {
        val dir=Files.createTempDirectory("rynx-recovery-").toFile()
        val index=dir.resolve("library.txt").apply{writeText("recoverable but invalid index")}
        val media=dir.resolve("new.wav").apply{writeText("new media")}
        val player=DesktopPlayer(LocalLibraryStore(index))
        try {
            withTimeout(5000){player.state.first{it.error!=null}}
            player.import(listOf(media))
            withTimeout(5000){player.state.first{it.tracks.size==1}}
            player.cycleRepeat()
            withTimeout(5000){player.state.first{it.repeatLabel=="All"}}
            assertEquals("recoverable but invalid index",index.readText())
        } finally { player.close();dir.deleteRecursively() }
    }

    @Test fun autoAdvanceFailurePublishesRecoverableStateAndKeepsMonitoring() = runBlocking {
        val dir=Files.createTempDirectory("rynx-advance-").toFile()
        val first=dir.resolve("first.wav")
        javax.sound.sampled.AudioInputStream(java.io.ByteArrayInputStream(ByteArray(1600)),javax.sound.sampled.AudioFormat(8000f,16,1,true,false),800).use {
            javax.sound.sampled.AudioSystem.write(it,javax.sound.sampled.AudioFileFormat.Type.WAVE,first)
        }
        val invalid=dir.resolve("invalid.wav").apply{writeText("invalid audio")}
        val store=LocalLibraryStore(dir.resolve("library.txt"))
        store.save(listOf(first,invalid))
        val ended=java.util.concurrent.atomic.AtomicBoolean(false)
        // A deterministic output driver lets us test the controller's end event without claiming audible playback.
        val fake=java.lang.reflect.Proxy.newProxyInstance(javaClass.classLoader,arrayOf(javax.sound.sampled.Clip::class.java)){_,method,_->
            when(method.name){
                "getFramePosition"->if(ended.get())800 else 0
                "getFrameLength"->800
                "getMicrosecondPosition"->if(ended.get())100000L else 0L
                "getMicrosecondLength"->100000L
                else->null
            }
        } as javax.sound.sampled.Clip
        val player=DesktopPlayer(store,{fake})
        try {
            val restored=withTimeout(5000){player.state.first{it.tracks.size==2}}
            val firstId=restored.tracks.first().id
            repeat(2) {
                ended.set(false)
                player.play(firstId)
                withTimeout(5000){player.state.first{it.playing && it.currentId==firstId}}
                ended.set(true)
                val failed=withTimeout(5000){player.state.first{it.error!=null}}
                assertFalse(failed.playing)
                assertFalse(failed.busy)
                player.dismissError()
            }
        } finally {player.close();dir.deleteRecursively()}
    }
}
