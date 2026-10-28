package dev.bxane.rynx.playback.discovery

/** Counts actual advancing playback in a session, independently of UI lifecycle or seek position. */
class ListeningSessionTracker(private val thresholdMillis:Long=30_000) {
    private var id:String?=null
    private var elapsed:Long?=null
    private var position:Long?=null
    private var wasPlaying=false
    private var listened=0L
    private var emitted=false
    fun discontinuity(){elapsed=null;position=null;wasPlaying=false}
    fun newSession(){id=null;listened=0;emitted=false;discontinuity()}
    fun sample(trackId:String?,playing:Boolean,positionMillis:Long,elapsedMillis:Long):Boolean {
        if(trackId==null){newSession();return false}
        if(id!=trackId){newSession();id=trackId}
        val time=elapsed?.let {elapsedMillis-it}?:0
        val advance=position?.let {positionMillis-it}?:0
        if(playing && wasPlaying && time>0 && advance>0 && advance<=time*4+1000)listened+=minOf(time,advance)
        elapsed=elapsedMillis;position=positionMillis;wasPlaying=playing
        if(!emitted && listened>=thresholdMillis){emitted=true;return true}
        return false
    }
}
