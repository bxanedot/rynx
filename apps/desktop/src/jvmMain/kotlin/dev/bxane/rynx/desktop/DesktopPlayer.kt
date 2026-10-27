package dev.bxane.rynx.desktop

import dev.bxane.rynx.playback.*
import dev.bxane.rynx.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/** Serialized native audio ownership. Decoder/file work never runs on the Compose thread. */
internal class DesktopPlayer(
    private val library: LocalLibraryStore = LocalLibraryStore(File(System.getProperty("user.home"), ".rynx/library-v1.txt")),
    private val createClip: () -> Clip = AudioSystem::getClip,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(PlayerUiState())
    val state = mutableState.asStateFlow()
    private var files = linkedMapOf<String, File>()
    private var queue = PlaybackQueue()
    private var clip: Clip? = null
    private var active = false
    private var libraryRestored = false

    init {
        command { addFiles(library.load());libraryRestored=true }
        scope.launch {
            while(isActive) {
                delay(100)
                mutex.withLock { recover {
                    val output = clip
                    if(output != null) {
                        if(active && output.framePosition >= output.frameLength) {
                            active = false
                            queue = queue.advance(AdvanceReason.Ended)
                            openCurrent()
                        } else mutableState.update { it.copy(positionMillis=output.microsecondPosition/1000,playing=active) }
                    }
                } }
            }
        }
    }

    private fun command(block: suspend () -> Unit) {
        scope.launch { mutex.withLock { recover(block) } }
    }

    /** Used by user commands and automatic advancement so either failure remains recoverable. */
    private suspend fun recover(block: suspend () -> Unit) {
        try { block() } catch(cancelled: CancellationException) { throw cancelled }
        catch(error: Exception) {
            active=false
            runCatching { clip?.close() }
            clip=null
            mutableState.update { it.copy(busy=false,playing=false,error=message(error)) }
        }
    }

    fun import(filesToAdd: List<File>) = command {
        addFiles(filesToAdd)
        if(libraryRestored) library.save(files.values.toList())
        else mutableState.update { it.copy(error="The saved library could not be restored. Its index is preserved; new imports are available for this session only.") }
    }

    private fun addFiles(filesToAdd: List<File>) {
        filesToAdd.forEach { file ->
            val canonical = file.canonicalFile
            if(canonical.isFile) files[canonical.toURI().toASCIIString()] = canonical
        }
        mutableState.update { it.copy(tracks=files.map{ (id,file) -> TrackItem(id,file.nameWithoutExtension) }) }
    }

    fun play(id: String) = command {
        if(!files.containsKey(id)) return@command
        if(queue.entries.none { it.mediaId == id }) queue=PlaybackQueue(files.keys.map{ QueueEntry(it,it) },repeat=queue.repeat)
        queue=queue.select(id)
        openCurrent()
    }
    fun toggle() = command {
        val output=clip
        if(output==null) {
            if(queue.current==null && queue.entries.isNotEmpty()) queue=queue.select(queue.entries.first().id)
            openCurrent()
        } else {
            if(active) output.stop() else {
                if(output.framePosition>=output.frameLength) output.framePosition=0
                output.start()
            }
            active=!active
            mutableState.update{it.copy(playing=active)}
        }
    }
    fun next() = command { queue=queue.advance(AdvanceReason.UserSkip);openCurrent() }
    fun previous() = command {
        val output=clip
        if(output!=null && output.microsecondPosition>3_000_000) output.microsecondPosition=0
        else { queue=queue.previous();openCurrent() }
    }
    fun seek(fraction:Float) = command {
        if(!fraction.isFinite()) return@command
        clip?.let { it.microsecondPosition=(it.microsecondLength*fraction.coerceIn(0f,1f)).toLong() }
    }
    fun cycleRepeat() = command {
        queue=queue.withRepeat(when(queue.repeat){RepeatMode.Off->RepeatMode.All;RepeatMode.All->RepeatMode.One;RepeatMode.One->RepeatMode.Off})
        publishQueue()
    }
    fun remove(id:String) = command {
        val before=queue.current?.id
        queue=queue.remove(id)
        if(before!=queue.current?.id) openCurrent() else publishQueue()
    }
    fun dismissError() { mutableState.update{it.copy(error=null)} }

    private suspend fun openCurrent() {
        clip?.close();clip=null;active=false
        publishQueue()
        mutableState.update{it.copy(playing=false,positionMillis=0,durationMillis=0)}
        val current=queue.current?:return
        val file=files[current.mediaId]?:return
        mutableState.update{it.copy(busy=true,error=null)}
        val pcm=PcmDecoder.decode(file)
        currentCoroutineContext().ensureActive()
        val output=createClip()
        try {
            output.open(pcm.format,pcm.bytes,0,pcm.bytes.size)
            currentCoroutineContext().ensureActive()
            output.start()
            clip=output;active=true
            mutableState.update{it.copy(busy=false,playing=true,durationMillis=pcm.durationMillis)}
        } catch(error:Exception) {output.close();throw error}
    }

    private fun publishQueue() {
        mutableState.update { state -> state.copy(
            currentId=queue.current?.mediaId,
            queue=queue.entries.mapNotNull{entry->state.tracks.firstOrNull{it.id==entry.mediaId}},
            repeatLabel=queue.repeat.name,
        ) }
    }
    override fun close() {
        scope.cancel()
        // Complete cleanup after any in-flight decoder has released the mutex.
        runBlocking(Dispatchers.IO) { mutex.withLock { clip?.close();clip=null;active=false } }
    }
    private fun message(error:Exception):String = when(error) {
        is javax.sound.sampled.LineUnavailableException -> "No audio output is available. Check your sound device and try again."
        is javax.sound.sampled.UnsupportedAudioFileException -> "This audio format is not supported. Choose a WAV, AIFF or AU file."
        is IllegalArgumentException -> error.message?:"The file or audio device is not supported."
        is java.io.IOException -> "The audio file could not be read. Check that it is still available."
        else -> "Playback failed. Choose another file or try again."
    }
}
