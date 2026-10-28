package dev.bxane.rynx.android

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.Util
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Owns audio independently of the activity and supplies Media3's media notification. */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var listening:AndroidListeningRecorder?=null
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
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(requestAware))
            .build().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
                setHandleAudioBecomingNoisy(true)
            }
        listening=AndroidListeningRecorder(applicationContext,player)
        val activity = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(activity)
            .setCallback(QueueSessionCallback(packageName)).build()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() {
        listening?.close();listening=null
        session?.let { it.player.release(); it.release() }
        session = null
        StreamRequestHeaderStore.clear()
        super.onDestroy()
    }
}
