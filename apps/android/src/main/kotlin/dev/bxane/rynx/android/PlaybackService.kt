package dev.bxane.rynx.android

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.core.net.toUri
import android.util.Log
import dev.bxane.rynx.playback.PlaybackResumeCodec
import dev.bxane.rynx.playback.PlaybackResumeSnapshot
import dev.bxane.rynx.playback.PlaybackResumeTrack
import dev.bxane.rynx.playback.RepeatMode

/** Owns audio independently of the activity and supplies Media3's media notification. */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var listening:AndroidListeningRecorder?=null
    private var audioSettingsStore:AndroidAudioDspSettingsStore?=null
    private var audioSettingsObservation:AutoCloseable?=null
    private var audioProcessor:RynxMedia3AudioProcessor?=null
    private var player:ExoPlayer?=null
    private var resumeStore:AndroidPlaybackResumeStore?=null
    private var failedMediaItem:Pair<Int,String>?=null
    private val checkpointHandler=Handler(Looper.getMainLooper())
    private val checkpointTask=object:Runnable {
        override fun run() {
            persistCheckpoint()
            if(player!=null) checkpointHandler.postDelayed(this,2_000)
        }
    }
    override fun onCreate() {
        super.onCreate()
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(this, "rynx"))
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val base = DefaultDataSource.Factory(this, http)
        val requestAware = ResolvingDataSource.Factory(
            base,
            ProviderHeaderResolver(StreamRequestHeaderStore::headersFor),
        )
        val settingsStore=AndroidAudioDspSettingsStore(applicationContext)
        val processor=RynxMedia3AudioProcessor(settingsStore.load())
        audioSettingsStore=settingsStore
        audioProcessor=processor
        audioSettingsObservation=settingsStore.observe(processor::updateSettings)
        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(RynxAudioRenderersFactory(this,processor))
            .setMediaSourceFactory(DefaultMediaSourceFactory(requestAware))
            .build().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
                setHandleAudioBecomingNoisy(true)
            }
        val store=AndroidPlaybackResumeStore(applicationContext)
        this.player=player
        resumeStore=store
        val savedSnapshot=store.load()
        Log.d(RESUME_LOG_TAG,"Loaded local resume queue items=${savedSnapshot?.tracks?.size ?: 0} pid=${android.os.Process.myPid()}")
        restoreCheckpoint(player,store,savedSnapshot)
        player.addListener(object:Player.Listener {
            override fun onEvents(player:Player,events:Player.Events) {
                if(events.contains(Player.EVENT_PLAYER_ERROR)) {
                    failedMediaItem=player.currentMediaItemIndex to player.currentMediaItem?.mediaId.orEmpty()
                    store.clear()
                    return
                }
                val active=player.currentMediaItemIndex to player.currentMediaItem?.mediaId.orEmpty()
                if(failedMediaItem!=null && active!=failedMediaItem) failedMediaItem=null
                persistCheckpoint()
            }
            override fun onPlayerError(error:androidx.media3.common.PlaybackException) {
                failedMediaItem=player.currentMediaItemIndex to player.currentMediaItem?.mediaId.orEmpty()
                store.clear()
            }
        })
        checkpointHandler.postDelayed(checkpointTask,2_000)
        listening=AndroidListeningRecorder(applicationContext,player)
        val activity = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(activity)
            .setCallback(QueueSessionCallback(packageName)).build()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() {
        checkpointHandler.removeCallbacks(checkpointTask)
        persistCheckpoint()
        listening?.close();listening=null
        audioSettingsObservation?.close();audioSettingsObservation=null
        audioProcessor=null;audioSettingsStore=null
        session?.let { it.player.release(); it.release() }
        session = null
        player=null;resumeStore=null
        StreamRequestHeaderStore.clear()
        super.onDestroy()
    }

    private fun restoreCheckpoint(player:ExoPlayer,store:AndroidPlaybackResumeStore,snapshot:PlaybackResumeSnapshot?) {
        if(snapshot==null) return
        runCatching {
            val items=snapshot.tracks.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.mediaId)
                    .setUri(track.mediaId.toUri())
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).build())
                    .build()
            }
            player.setMediaItems(items,snapshot.currentIndex,snapshot.positionMillis)
            player.repeatMode=when(snapshot.repeatMode) {
                RepeatMode.Off -> Player.REPEAT_MODE_OFF
                RepeatMode.All -> Player.REPEAT_MODE_ALL
                RepeatMode.One -> Player.REPEAT_MODE_ONE
            }
            if(snapshot.shuffleEnabled) {
                player.setShuffleOrder(DefaultShuffleOrder(snapshot.playbackOrder.toIntArray(),0L))
            }
            player.shuffleModeEnabled=snapshot.shuffleEnabled
            player.playWhenReady=false
            player.prepare()
        }.onSuccess {
            Log.d(RESUME_LOG_TAG,"Restored local resume queue items=${player.mediaItemCount}")
        }.onFailure {
            Log.e(RESUME_LOG_TAG,"Local resume restore failed (${it.javaClass.simpleName})")
            store.clear()
        }
    }

    private fun persistCheckpoint() {
        val output=player ?: return
        val store=resumeStore ?: return
        val count=output.mediaItemCount
        if(count==0 || output.currentMediaItemIndex !in 0 until count) {
            store.save(null)
            return
        }
        val active=output.currentMediaItemIndex to output.currentMediaItem?.mediaId.orEmpty()
        if(active==failedMediaItem) {
            store.clear()
            return
        }
        if(failedMediaItem!=null) failedMediaItem=null
        val tracks=(0 until count).map { index ->
            val item=output.getMediaItemAt(index)
            PlaybackResumeTrack(
                mediaId=item.mediaId,
                title=item.mediaMetadata.title?.toString().orEmpty(),
                artist=item.mediaMetadata.artist?.toString().orEmpty(),
            )
        }
        val order=if(output.shuffleModeEnabled) {
            if(output.currentTimeline.windowCount==count) playbackOrder(output.currentTimeline,true)
            else tracks.indices.toList()
        } else emptyList()
        val snapshot=PlaybackResumeSnapshot(
            tracks=tracks,
            currentIndex=output.currentMediaItemIndex,
            positionMillis=output.currentPosition.coerceAtLeast(0),
            repeatMode=when(output.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.All
                Player.REPEAT_MODE_ONE -> RepeatMode.One
                else -> RepeatMode.Off
            },
            shuffleEnabled=output.shuffleModeEnabled,
            playbackOrder=order,
        )
        if(PlaybackResumeCodec.isPersistableQueue(tracks)) store.save(snapshot) else store.save(null)
    }

    private companion object {
        const val RESUME_LOG_TAG="RynxResume"
    }
}
