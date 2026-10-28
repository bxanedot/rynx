package dev.bxane.rynx.android

import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.bxane.rynx.playback.discovery.*
import dev.bxane.rynx.ui.TrackItem
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/** The foreground playback service owns learning signals and history, including screen-off sessions. */
internal class AndroidListeningRecorder(context:Context,private val player:Player) : Player.Listener {
    private val preferences=context.getSharedPreferences("discovery",Context.MODE_PRIVATE)
    private val library=context.getSharedPreferences("library",Context.MODE_PRIVATE)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val tracker=ListeningSessionTracker()
    private var polling:Job?=null
    init {player.addListener(this);onIsPlayingChanged(player.isPlaying)}
    override fun onMediaItemTransition(mediaItem:MediaItem?,reason:Int){tracker.newSession();if(player.isPlaying)rememberRecent()}
    override fun onPositionDiscontinuity(oldPosition:Player.PositionInfo,newPosition:Player.PositionInfo,reason:Int) {
        if(newPosition.positionMs<1000 && oldPosition.positionMs>5000)tracker.newSession() else tracker.discontinuity()
    }
    override fun onIsPlayingChanged(isPlaying:Boolean) {
        polling?.cancel()
        if(!isPlaying){tracker.sample(player.currentMediaItem?.mediaId,false,player.currentPosition,SystemClock.elapsedRealtime());return}
        rememberRecent()
        polling=scope.launch {
            while(isActive) {
                val item=player.currentMediaItem
                if(tracker.sample(item?.mediaId,player.isPlaying,player.currentPosition,SystemClock.elapsedRealtime()) && item!=null) {
                    appendSignal(item)
                }
                delay(1000)
            }
        }
    }
    private fun appendSignal(item:MediaItem) {
        val extras=item.mediaMetadata.extras
        val array=runCatching {JSONArray(preferences.getString("signals","[]"))}.getOrDefault(JSONArray())
        val next=JSONArray()
        for(i in (array.length()-499).coerceAtLeast(0) until array.length())next.put(array.get(i))
        next.put(JSONObject().put("id",extras?.getString("rynx.metadata-id")?:item.mediaId)
            .put("artist",item.mediaMetadata.artist?.toString().orEmpty())
            .put("genres",JSONArray(extras?.getStringArrayList("rynx.metadata-genres").orEmpty()))
            .put("time",System.currentTimeMillis()))
        preferences.edit {putString("signals",next.toString())}
    }
    private fun rememberRecent() {
        val item=player.currentMediaItem?:return
        val old=TrackMetadataStorage.decode(library.getString("recent-tracks",null))
        if(old.firstOrNull()?.id==item.mediaId)return
        val metadata=item.mediaMetadata
        val track=TrackItem(item.mediaId,metadata.title?.toString()?:"Unknown track",metadata.artist?.toString()?:"Unknown artist",metadata.artworkUri?.toString(),metadata.extras?.getString("rynx.metadata-source"))
        val next=(listOf(track)+old.filterNot {it.id==track.id}).take(50)
        library.edit {putString("recent-tracks",TrackMetadataStorage.encode(next));putLong("history-revision",library.getLong("history-revision",0L)+1)}
    }
    fun close(){player.removeListener(this);scope.cancel()}
}
/** Network teardown may send TLS close records and must survive the Activity's cancelled scope. */
internal object AndroidResourceCloser {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    fun close(block:suspend ()->Unit){scope.launch {runCatching {block()}}}
}
