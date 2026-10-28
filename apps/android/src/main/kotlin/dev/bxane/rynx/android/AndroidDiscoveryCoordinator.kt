package dev.bxane.rynx.android

import android.content.Context
import androidx.core.content.edit
import android.util.AtomicFile
import dev.bxane.rynx.apple.*
import dev.bxane.rynx.playback.discovery.*
import dev.bxane.rynx.ui.TrackItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Phone persistence/lifecycle adapter; Apple parsing and recommendations live in shared modules. */
internal class AndroidDiscoveryCoordinator(
    context:Context,private val scope:CoroutineScope,
    private val onFeed:(DiscoveryState)->Unit,
    private val onSelection:(HomeSection?,Boolean)->Unit,
    private val fallbackSearch:suspend (String)->List<MusicMetadata>,
) {
    private val preferences=context.getSharedPreferences("discovery",Context.MODE_PRIVATE)
    private val client=AppleMetadataClient(HttpClient(OkHttp),DiskAppleCache(File(context.cacheDir,"apple-metadata")))
    private val repository=AppleHomeRepository(client)
    private var job:Job?=null
    private var selectionJob:Job?=null
    private var current=DiscoveryState(enabled=true,region=preferences.getString("region",null)?:Locale.getDefault().country.lowercase().takeIf {code->Regions.all.any {it.code==code}}?:"us",regions=Regions.all)
    private var signals=readSignals()
    private val signalListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener {_,key->
        if(key=="signals") {signals=readSignals();current=rerankDiscovery(current,signals,System.currentTimeMillis());onFeed(current)}
    }
    init {preferences.registerOnSharedPreferenceChangeListener(signalListener)}
    fun load(region:String=current.region) {
        job?.cancel()
        val selected=region.takeIf {code->Regions.all.any {it.code==code}}?:"us"
        preferences.edit {putString("region",selected)}
        current=current.copy(enabled=true,loading=true,region=selected,error=null);onFeed(current)
        job=scope.launch {
            try {
                val snapshot=withContext(Dispatchers.IO){repository.load(selected,signals,emptySet(),readPreviousRanks(selected))}
                ensureActive();current=rerankDiscovery(snapshot,signals,System.currentTimeMillis());onFeed(current)
                saveDailyRanks(snapshot)
                if(snapshot.sections.all {it.tracks.isEmpty()}) {
                    val fallback=withContext(Dispatchers.IO){fallbackSearch("popular music in ${Regions.name(selected)}")}
                    ensureActive()
                    current=current.copy(sections=current.sections.map {if(it.id=="for-you")it.copy(title="Discover on YouTube Music",subtitle="Apple discovery is unavailable",tracks=fallback,unavailableReason=null) else it})
                    onFeed(current)
                }
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception) {current=current.copy(loading=false,error="Discovery could not refresh. Try again when connected.");onFeed(current)}
        }
    }
    fun section(id:String) {selectionJob?.cancel();onSelection(if(id=="regions")HomeSection("regions","Chart region","Choose a music storefront",HomeSectionKind.Daily100) else current.sections.firstOrNull {it.id==id},false)}
    fun collection(value:ChartCollection) {
        selectionJob?.cancel();onSelection(HomeSection(value.id,value.title,"Apple Music collection",HomeSectionKind.Daily100,sourceUrl=value.sourceUrl),true)
        selectionJob=scope.launch {
            try {
                val result=withContext(Dispatchers.IO){client.playlist(ApplePlaylistLink(value.id,value.title,value.sourceUrl),current.region)}
                ensureActive();onSelection(HomeSection(result.id,result.title,"Apple Music collection",HomeSectionKind.Daily100,result.tracks,result.sourceUrl,availabilityNote=result.coverageNote()),false)
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){onSelection(HomeSection(value.id,value.title,"Apple Music collection",HomeSectionKind.Daily100,sourceUrl=value.sourceUrl,unavailableReason="This collection could not load. Try again or open it in Apple Music."),false)}
        }
    }
    fun genre(value:MusicGenre) {
        selectionJob?.cancel();onSelection(HomeSection("genre:${value.id}",value.name,"iTunes genre chart",HomeSectionKind.Daily100),true)
        selectionJob=scope.launch {
            try {
                val chart=withContext(Dispatchers.IO){client.genreChart(value,current.region)}
                ensureActive();onSelection(HomeSection(chart.id,chart.title,"iTunes genre chart · Apple catalog",HomeSectionKind.Daily100,chart.tracks,chart.sourceUrl),false)
            } catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){onSelection(HomeSection("genre:${value.id}",value.name,"Apple catalog",HomeSectionKind.Daily100,unavailableReason="This genre's songs could not load. Search remains available."),false)}
        }
    }
    fun closeSelection(){selectionJob?.cancel();onSelection(null,false)}
    private fun readSignals():List<ListeningSignal> = runCatching {
        val array=JSONArray(preferences.getString("signals","[]"));(0 until array.length()).map {i->
            val item=array.getJSONObject(i);val genres=item.optJSONArray("genres")?:JSONArray()
            ListeningSignal(item.getString("id"),item.getString("artist"),(0 until genres.length()).map {genres.getString(it)},item.getLong("time"))
        }
    }.getOrDefault(emptyList())
    private fun readPreviousRanks(region:String):List<MusicMetadata> = runCatching {
        val today=System.currentTimeMillis()/86_400_000
        val value=JSONObject(preferences.getString("ranks:$region",null)?:return emptyList())
        val source=if(value.optLong("day")==today)value.optJSONArray("previous") else value.optJSONArray("current")
        if(source==null)return emptyList()
        (0 until source.length()).map {i->val item=source.getJSONObject(i);MusicMetadata(item.getString("id"),"Chart entry","",chartRank=item.getInt("rank"))}
    }.getOrDefault(emptyList())
    private fun saveDailyRanks(snapshot:DiscoveryState) {
        val tracks=snapshot.sections.firstOrNull {it.id=="region"}?.tracks.orEmpty();if(tracks.isEmpty())return
        val key="ranks:${snapshot.region}";val today=System.currentTimeMillis()/86_400_000
        val old=runCatching {JSONObject(preferences.getString(key,"{}")?:"{}")}.getOrDefault(JSONObject())
        val previous=if(old.optLong("day")==today)old.optJSONArray("previous") else old.optJSONArray("current")
        preferences.edit {putString(key,JSONObject().put("day",today).put("previous",previous?:JSONArray()).put("current",JSONArray().apply {tracks.forEach {put(JSONObject().put("id",it.id).put("rank",it.chartRank))}}).toString())}
    }
    fun close(){job?.cancel();selectionJob?.cancel();preferences.unregisterOnSharedPreferenceChangeListener(signalListener);AndroidResourceCloser.close {client.close()}}
}
private class DiskAppleCache(private val directory:File):AppleDocumentCache {
    private fun file(url:String)=File(directory,MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString(""){"%02x".format(it)})
    override fun read(url:String):CachedAppleDocument?=runCatching {val f=file(url);val value=JSONObject(f.readText());CachedAppleDocument(value.getString("body"),value.getLong("saved"),value.optLong("max-age",Long.MAX_VALUE),value.optBoolean("revalidate",false))}.getOrNull()
    @Synchronized override fun write(url:String,value:CachedAppleDocument) {
        directory.mkdirs()
        val output=AtomicFile(file(url));val stream=output.startWrite()
        try {stream.write(JSONObject().put("body",value.body).put("saved",value.savedMillis).put("max-age",value.maxAgeMillis).put("revalidate",value.mustRevalidate).toString().toByteArray());output.finishWrite(stream)}
        catch(error:Exception){output.failWrite(stream);throw error}
        directory.listFiles()?.filter {it.isFile}?.sortedByDescending {it.lastModified()}?.drop(60)?.forEach {it.delete()}
    }
}
