package dev.bxane.rynx.android

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Build
import android.media.AudioManager
import android.media.MediaRouter2
import android.media.AudioTrack
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.provider.Settings
import android.provider.OpenableColumns
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt
import dev.bxane.rynx.library.PlaylistEntryDraft
import dev.bxane.rynx.library.InvalidPlaylistName
import dev.bxane.rynx.library.PlaylistRepository
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.bxane.rynx.playback.catalog.AudioResolutionChain
import dev.bxane.rynx.playback.catalog.CatalogItem
import dev.bxane.rynx.playback.catalog.CatalogItemType
import dev.bxane.rynx.playback.catalog.CatalogProvider
import dev.bxane.rynx.playback.catalog.MusicPlaybackCatalog
import dev.bxane.rynx.playback.catalog.ProviderAccessRefusalException
import dev.bxane.rynx.playback.toggleFavoriteId
import dev.bxane.rynx.playback.discovery.*
import dev.bxane.rynx.playback.audio.AudioDspSettings
import dev.bxane.rynx.playback.audio.AudioSignalInfo
import dev.bxane.rynx.playback.scrobble.LastFmClient
import dev.bxane.rynx.playback.scrobble.ListenBrainzClient
import dev.bxane.rynx.playback.scrobble.ScrobbleProvider
import dev.bxane.rynx.library.PlaylistArchive
import dev.bxane.rynx.ui.*
import dev.bxane.rynx.apple.AppleMetadataClient
import dev.bxane.rynx.newpipe.NewPipeFallbackAudioResolver
import dev.bxane.rynx.innertubex.InnerTubeXFallbackAudioResolver
import dev.bxane.rynx.innertubex.createInnerTubeXMusicPlaybackCatalog
import dev.bxane.rynx.innertubex.createInnerTubeXFallbackAudioResolver
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Phone host for the shared Compose UI; the service owns all audio resources. */
@UnstableApi
class MainActivity : ComponentActivity() {
    private val state = MutableStateFlow(PlayerUiState(catalogSearchEnabled=true,catalogPlaybackEnabled=true,
        volumeControlAvailable=true,audioOutputAvailable=true,shuffleAvailable=true,queueEditingAvailable=true,lyricsImportAvailable=true,
        playlistBackupAvailable=true,
        appVersionName=BuildConfig.VERSION_NAME,
        appBuildNumber=BuildConfig.VERSION_CODE.toString(),
        appPlatform="Android",
        appAudioBackend="Media3 ExoPlayer"))
    private var controller: MediaController? = null
    private var connection: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private lateinit var catalog: MusicPlaybackCatalog
    private lateinit var audioResolver: AudioResolutionChain
    private var innertubexHttpClient: HttpClient? = null
    private var innertubexResolver: InnerTubeXFallbackAudioResolver? = null
    private lateinit var playbackPreparer: MusicPlaybackPreparer
    private lateinit var catalogReady: Deferred<Boolean>
    private var searchJob: Job? = null
    private var historyRevision = -1L
    private lateinit var discovery:AndroidDiscoveryCoordinator
    private val playbackRequests by lazy {LatestPlaybackRequest(lifecycleScope){pending->
        state.update {it.copy(busy=pending || controller?.playbackState==Player.STATE_BUFFERING)}
    }}
    private val appearanceStore by lazy {AndroidAppearanceStore(applicationContext)}
    private val audioDspStore by lazy {AndroidAudioDspSettingsStore(applicationContext)}
    private val scrobbleHttpClient = lazy(LazyThreadSafetyMode.NONE) {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
        }
    }
    private val lastFmClient by lazy(LazyThreadSafetyMode.NONE) {
        LastFmClient(scrobbleHttpClient.value, BuildConfig.LASTFM_API_KEY, BuildConfig.LASTFM_SHARED_SECRET)
    }
    private val listenBrainzClient by lazy(LazyThreadSafetyMode.NONE) { ListenBrainzClient(scrobbleHttpClient.value) }
    private val scrobbleAccountStore by lazy(LazyThreadSafetyMode.NONE) {
        AndroidScrobbleAccountStore(applicationContext, lastFmClient.configured)
    }
    private val scrobbleAccountPreferences by lazy { getSharedPreferences("scrobbling-accounts", MODE_PRIVATE) }
    private val scrobbleAccountListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "lastfm.error" || key == "listenbrainz.error") lifecycleScope.launch { refreshScrobblingState() }
    }
    private var renderingMonitor:AndroidRenderingMonitor?=null
    private var outputDeviceCallback:AudioDeviceCallback?=null
    private var availableAudioRoutes:List<String> = emptyList()
    private val libraryDatabase=lazy(LazyThreadSafetyMode.NONE){AndroidLibraryDatabase(applicationContext)}
    private val playlistRepository:PlaylistRepository get()=libraryDatabase.value.playlists
    private var playlistObservation:Job?=null
    private val preferences by lazy { getSharedPreferences("library", MODE_PRIVATE) }
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val lyricsStore by lazy { AndroidTimedLyricsStore(applicationContext) }
    private var pendingLyricsTrackId:String?=null
    private var loadedLyricsTrackId:String?=null
    private var lyricsJob:Job?=null
    private val lyricsPicker=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val trackId=pendingLyricsTrackId;pendingLyricsTrackId=null
        if(uri!=null && trackId!=null) lifecycleScope.launch {
            try {
                val lines=lyricsStore.import(trackId,uri)
                state.update { if(it.currentId==trackId) it.copy(lyrics=lines,lyricsTrackId=trackId,error=null) else it }
            } catch(cancelled:CancellationException) { throw cancelled }
            catch(_:Exception) { state.update { it.copy(error="Choose a timed lyrics (.lrc) file smaller than 256K characters.") } }
        }
    }
    private val playlistImportPicker=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) lifecycleScope.launch {
            try {
                val drafts=withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use(::readPlaylistArchive)
                        ?:throw IllegalStateException("The selected file could not be opened")
                }
                playlistRepository.importPlaylists(PlaylistArchive.decode(drafts))
                state.update {it.copy(error=null)}
            } catch(cancelled:CancellationException) {throw cancelled}
            catch(_:Exception) {state.update {it.copy(error="That playlist backup could not be imported. Check that it is a valid Rynx JSON backup.")}}
        }
    }
    private val playlistExportPicker=registerForActivityResult(ActivityResultContracts.CreateDocument(PlaylistArchive.MIME_TYPE)) { uri ->
        if(uri!=null) lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val archive=PlaylistArchive.encode(playlistRepository.getPlaylists(),System.currentTimeMillis())
                    val output=contentResolver.openOutputStream(uri,"wt")?:error("The selected destination could not be opened")
                    output.bufferedWriter(Charsets.UTF_8).use {it.write(archive)}
                }
                state.update {it.copy(error=null)}
            } catch(cancelled:CancellationException) {throw cancelled}
            catch(_:Exception) {state.update {it.copy(error="The playlist backup could not be saved. Choose another location and try again.")}}
        }
    }
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val saved = preferences.getStringSet("uris", emptySet()).orEmpty().toMutableSet()
        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                saved += uri.toString()
            } catch (_: SecurityException) {
                state.update { it.copy(error="Access to this audio file could not be retained. Choose it again.") }
            }
        }
        preferences.edit { putStringSet("uris", saved) }
        restoreLibrary()
        playlistObservation=lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playlistRepository.observePlaylists().collect { playlists -> state.update { it.copy(playlists=playlists) } }
            }
        }
    }
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
        override fun onPlayerError(error: PlaybackException) {
            state.update { it.copy(error="Playback failed. Check the file and your audio output, then try again.",playing=false,busy=false) }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingLyricsTrackId=savedInstanceState?.getString("pending-lyrics-track")
        state.update { it.copy(reduceMotion=Settings.Global.getFloat(contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f) }
        enableEdgeToEdge()
        title = "rynx"
        refreshAvailableAudioRoutes()
        outputDeviceCallback=object:AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices:Array<out AudioDeviceInfo>)=refreshAvailableAudioRoutes()
            override fun onAudioDevicesRemoved(removedDevices:Array<out AudioDeviceInfo>)=refreshAvailableAudioRoutes()
        }.also { audioManager.registerAudioDeviceCallback(it,Handler(mainLooper)) }
        state.update {it.copy(appearance=appearanceStore.load())}
        state.update {it.copy(audioDspSettings=audioDspStore.load())}
        lifecycleScope.launch { refreshScrobblingState() }
        scrobbleAccountPreferences.registerOnSharedPreferenceChangeListener(scrobbleAccountListener)
        renderingMonitor=AndroidRenderingMonitor(window){pressure->state.update {it.copy(renderingPressure=pressure)}}
        val innertubexClient = HttpClient(OkHttp)
        val innertubex = createInnerTubeXFallbackAudioResolver(innertubexClient)
        catalog = createInnerTubeXMusicPlaybackCatalog(
            innertubexClient,
            AppleMetadataClient(HttpClient(OkHttp),DiskAppleCache(File(cacheDir,"apple-catalog-metadata"))),
        )
        innertubexHttpClient = innertubexClient
        innertubexResolver = innertubex
        audioResolver = AudioResolutionChain(innertubex, NewPipeFallbackAudioResolver)
        playbackPreparer = MusicPlaybackPreparer(catalog, audioResolver) { state.value.discovery.region }
        catalogReady = lifecycleScope.async(Dispatchers.IO) {
            try {
                catalog.initialize()
                true
            } catch(cancelled: CancellationException) {
                throw cancelled
            } catch(_: Exception) {
                false
            }
        }
        val favoriteTracks=TrackMetadataStorage.decode(preferences.getString("favorite-tracks",null))
        val favoriteIds=preferences.getStringSet("favorites",emptySet()).orEmpty().toSet()+favoriteTracks.map(TrackItem::id)
        val recentTracks=TrackMetadataStorage.decode(preferences.getString("recent-tracks",null))
        state.update { it.copy(favoriteIds=favoriteIds,favoriteTracks=favoriteTracks,recentTracks=recentTracks) }
        restoreLibrary()
        discovery=AndroidDiscoveryCoordinator(applicationContext,lifecycleScope,
            onFeed={feed->state.update {it.copy(discovery=feed)}},
            onSelection={section,loading->state.update {it.copy(discoverySelection=section,discoverySelectionLoading=loading)}},
            fallbackSearch={query->catalogReady.await();catalog.searchMusic(query).items.filter {it.type==CatalogItemType.Track}.map {item->
                MusicMetadata("yt:${item.mediaId}",item.title,item.artist.orEmpty(),source=MetadataSource.YouTubeMusic,artworkUrl=item.artworkUrl,durationMillis=item.durationSeconds?.times(1000),playbackId=item.mediaId)
            }})
        discovery.load()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while(true) { publish();discovery.refreshIfStale();delay(250) }
            }
        }
        val actions = object : PlayerActions {
            override fun refreshDiscovery()=discovery.load()
            override fun changeDiscoveryRegion(code:String)=discovery.load(code)
            override fun openDiscoverySection(id:String)=discovery.section(id)
            override fun openDiscoveryCollection(collection:ChartCollection)=discovery.collection(collection)
            override fun openGenre(genre:MusicGenre)=discovery.genre(genre)
            override fun closeDiscovery()=discovery.closeSelection()
            override fun playDiscovery(track:MusicMetadata)=playDiscoveryTrack(track)
            override fun openMusicSource(url:String) {
                val uri=url.toUri()
                if(uri.scheme=="https" && uri.host in setOf("music.apple.com","itunes.apple.com"))
                    runCatching {startActivity(Intent(Intent.ACTION_VIEW,uri))}
                        .onFailure {state.update {it.copy(error="No application can open this music link.")}}
            }
            override fun importFiles() = picker.launch(arrayOf("audio/*"))
            override fun play(id: String) {
                playbackRequests.cancel()
                val output = controller ?: return
                val index = (0 until output.mediaItemCount).firstOrNull { output.getMediaItemAt(it).mediaId == id }
                if(index != null) output.seekToDefaultPosition(index) else {
                    val tracks = state.value.tracks
                    val selected = tracks.indexOfFirst { it.id == id }
                    if(selected < 0) {
                        val snapshot=state.value
                        val saved=(snapshot.favoriteTracks+snapshot.recentTracks+snapshot.playlists.flatMap { playlist ->
                            playlist.entries.map { entry -> TrackItem(entry.mediaId,entry.title,entry.artist,entry.artworkUrl) }
                        }).firstOrNull { it.id==id }
                        if(saved!=null && id.startsWith("yt:")) {
                            playCatalogItem(CatalogItem(
                                provider=CatalogProvider.YouTubeMusic,
                                type=CatalogItemType.Track,
                                mediaId=id.removePrefix("yt:"),
                                title=saved.title,
                                artist=saved.subtitle,
                                artworkUrl=saved.artworkUrl,
                            ))
                        } else if(saved!=null && id.startsWith("content://")) {
                            output.setMediaItem(MediaItem.Builder().setMediaId(saved.id).setUri(saved.id).setMediaMetadata(
                                MediaMetadata.Builder().setTitle(saved.title).setArtist(saved.subtitle)
                                    .apply { saved.artworkUrl?.let { setArtworkUri(it.toUri()) } }.build(),
                            ).build())
                            output.prepare();output.play()
                        }
                        return
                    }
                    output.setMediaItems(tracks.map { track ->
                        MediaItem.Builder().setMediaId(track.id).setUri(track.id)
                            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.subtitle)
                                .apply {
                                    track.albumTitle?.let(::setAlbumTitle)
                                    track.artworkUrl?.let { setArtworkUri(it.toUri()) }
                                }.build()).build()
                    },selected,0)
                }
                output.prepare(); output.play()
            }
            override fun playCollection(tracks:List<TrackItem>) {
                val collection=tracks.distinctBy(TrackItem::id)
                if(collection.isEmpty()) return
                // Only authorized imported local audio may be enqueued directly. Remote streams
                // must go through the existing provider resolver and access-policy checks.
                val imported=state.value.tracks.map(TrackItem::id).toSet()
                if(collection.any { it.id !in imported || !it.id.startsWith("content://") }) {
                    play(collection.first().id)
                    return
                }
                playbackRequests.cancel()
                val output=controller ?: return
                val media=collection.map { track ->
                    MediaItem.Builder().setMediaId(track.id).setUri(track.id)
                        .setMediaMetadata(MediaMetadata.Builder()
                            .setTitle(track.title).setArtist(track.subtitle)
                            .apply {
                                track.albumTitle?.let(::setAlbumTitle)
                                track.artworkUrl?.let { setArtworkUri(it.toUri()) }
                            }.build())
                        .build()
                }
                output.setMediaItems(media,0,0)
                output.prepare()
                output.play()
                publish()
            }
            override fun toggle() { playbackRequests.cancel();controller?.let { if(it.isPlaying) it.pause() else { it.prepare(); it.play() } } }
            override fun previous() { playbackRequests.cancel();controller?.seekToPrevious() }
            override fun next() { playbackRequests.cancel();controller?.seekToNextMediaItem() }
            override fun seek(fraction: Float) {
                if(fraction.isFinite()) controller?.let { if(it.duration>0) it.seekTo((it.duration*fraction.coerceIn(0f,1f)).toLong()) }
            }
            override fun cycleRepeat() { controller?.let { it.repeatMode=when(it.repeatMode){Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ALL;Player.REPEAT_MODE_ALL->Player.REPEAT_MODE_ONE;else->Player.REPEAT_MODE_OFF} } }
            override fun toggleShuffle() { controller?.let { it.shuffleModeEnabled=!it.shuffleModeEnabled } }
            override fun setVolume(fraction:Float) {
                if(!fraction.isFinite())return
                val maximum=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,(maximum*fraction.coerceIn(0f,1f)).roundToInt(),0)
                publish()
            }
            override fun showAudioOutput() {
                AudioOutputRouteRequest.launch(
                    sdk=Build.VERSION.SDK_INT,
                    showNative={
                        if(Build.VERSION.SDK_INT>=34) MediaRouter2.getInstance(this@MainActivity).showSystemOutputSwitcher()
                        else false
                    },
                    showFallback={
                        val action=if(Build.VERSION.SDK_INT>=29) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS
                        startActivity(Intent(action))
                    },
                )
            }
            override fun updateAudioDsp(settings:AudioDspSettings) {
                val safe=settings.normalized()
                audioDspStore.save(safe)
                state.update {it.copy(audioDspSettings=safe)}
            }
            override fun openAudioSettings() {state.update {it.copy(audioSettingsOpen=true)}}
            override fun closeAudioSettings() {state.update {it.copy(audioSettingsOpen=false)}}
            override fun moveInQueue(id:String,toIndex:Int) {
                controller?.let { output ->
                    val destination=state.value.queue.getOrNull(toIndex)?.id ?: return
                    val result=output.sendCustomCommand(QueueSessionCommands.Move,Bundle().apply {
                        putString(QueueSessionCommands.Source,id)
                        putString(QueueSessionCommands.Destination,destination)
                    })
                    result.addListener({
                        try {
                            if(result.get().resultCode!=androidx.media3.session.SessionResult.RESULT_SUCCESS)
                                state.update { it.copy(error="The queue changed. Try moving this song again.") }
                        } catch(_:Exception) { state.update { it.copy(error="The queue could not be updated. Try again.") } }
                    },ContextCompat.getMainExecutor(this@MainActivity))
                }
            }
            override fun updateAppearance(appearance:dev.bxane.rynx.design.RynxAppearance) {
                val safe=appearance.normalized();appearanceStore.save(safe)
                state.update {it.copy(appearance=safe)}
            }
            override fun clearUpcomingQueue() {
                controller?.sendCustomCommand(QueueSessionCommands.Clear,Bundle.EMPTY)
            }
            override fun importLyrics() {
                pendingLyricsTrackId=state.value.currentId ?: return
                lyricsPicker.launch(arrayOf("text/*","application/x-lrc","application/octet-stream"))
            }
            override fun removeFromQueue(id: String) { controller?.let { output -> (0 until output.mediaItemCount).firstOrNull { output.getMediaItemAt(it).mediaId==id }?.let(output::removeMediaItem) } }
            override fun dismissError() { state.update { it.copy(error=null) } }
            override fun searchYouTubeMusic(query: String) = searchCatalog(query)
            override fun loadMoreYouTubeMusic() = loadMoreCatalog()
            override fun playCatalog(item: CatalogItem) = playCatalogItem(item)
            override fun toggleFavorite(id: String) {
                val before=state.value
                val removing=id in before.favoriteIds
                val favorites=toggleFavoriteId(before.favoriteIds,id)
                val track=before.favoriteTracks.firstOrNull { it.id==id }
                    ?: before.current?.takeIf { it.id==id }
                    ?: before.tracks.firstOrNull { it.id==id }
                    ?: before.recentTracks.firstOrNull { it.id==id }
                val favoriteTracks=if(removing) before.favoriteTracks.filterNot { it.id==id }
                    else track?.let { (before.favoriteTracks.filterNot { saved -> saved.id==id }+it).takeLast(500) }
                        ?: before.favoriteTracks
                preferences.edit {
                    putStringSet("favorites",favorites)
                    putString("favorite-tracks",TrackMetadataStorage.encode(favoriteTracks))
                }
                state.update { it.copy(favoriteIds=favorites,favoriteTracks=favoriteTracks) }
            }
            override fun createPlaylist(name:String) {
                launchLibraryMutation("Playlist could not be created.") { playlistRepository.createPlaylist(name) }
            }
            override fun renamePlaylist(id:Long,name:String) {
                launchLibraryMutation("Playlist could not be renamed.") { playlistRepository.renamePlaylist(id,name) }
            }
            override fun deletePlaylist(id:Long) {
                launchLibraryMutation("Playlist could not be deleted.") { playlistRepository.deletePlaylist(id) }
            }
            override fun addTrackToPlaylist(playlistId:Long,track:TrackItem) {
                val localUri=track.id.takeIf { it.startsWith("content://") }
                val draft=PlaylistEntryDraft(
                    mediaId=track.id,
                    title=track.title,
                    artist=track.subtitle,
                    artworkUrl=track.artworkUrl,
                    source=if(track.id.startsWith("yt:")) "youtube-music" else "local",
                    localUri=localUri,
                )
                launchLibraryMutation("Song could not be added to the playlist.") { playlistRepository.addEntry(playlistId,draft) }
            }
            override fun removePlaylistEntry(playlistId:Long,entryId:Long) {
                launchLibraryMutation("Song could not be removed from the playlist.") { playlistRepository.removeEntry(playlistId,entryId) }
            }
            override fun movePlaylistEntry(playlistId:Long,entryId:Long,toIndex:Int) {
                launchLibraryMutation("Playlist order could not be changed.") { playlistRepository.moveEntry(playlistId,entryId,toIndex) }
            }
            override fun importPlaylistBackup() = playlistImportPicker.launch(arrayOf(PlaylistArchive.MIME_TYPE))
            override fun exportPlaylistBackup() {
                val stamp=SimpleDateFormat("yyyyMMdd-HHmm",Locale.ROOT).format(Date())
                playlistExportPicker.launch("rynx-playlists-$stamp.json")
            }
            override fun beginLastFmLogin() = beginLastFmAuthorization()
            override fun finishLastFmLogin() = finishLastFmAuthorization()
            override fun disconnectLastFm() = runScrobbleAction(ScrobbleProvider.LastFm) { scrobbleAccountStore.disconnectLastFm() }
            override fun setScrobblingEnabled(provider:ScrobbleProvider,enabled:Boolean) = runScrobbleAction(provider) {
                scrobbleAccountStore.setEnabled(provider,enabled)
            }
            override fun beginListenBrainzLogin() = beginListenBrainzAuthorization()
            override fun connectListenBrainz(token:String) = runScrobbleAction(ScrobbleProvider.ListenBrainz) {
                val account=listenBrainzClient.validateToken(token)
                scrobbleAccountStore.connectListenBrainz(account,token)
            }
            override fun disconnectListenBrainz() = runScrobbleAction(ScrobbleProvider.ListenBrainz) {
                scrobbleAccountStore.disconnectListenBrainz()
            }
        }
        setContent {
            val current by state.collectAsStateWithLifecycle()
            var immersivePlayer by remember { mutableStateOf(false) }
            var startupSplashVisible by remember { mutableStateOf(savedInstanceState == null) }
            SideEffect {
                WindowCompat.getInsetsController(window,window.decorView).apply {
                    val dark=startupSplashVisible || current.appearance.darkTheme || current.appearance.material==dev.bxane.rynx.design.RynxMaterialMode.Amoled || immersivePlayer
                    isAppearanceLightStatusBars=!dark
                    isAppearanceLightNavigationBars=!dark
                }
            }
            Box(Modifier.fillMaxSize().background(
                if(startupSplashVisible) Color.Black
                else if(immersivePlayer && current.appearance.material!=dev.bxane.rynx.design.RynxMaterialMode.Amoled) Color(0xFF142430)
                else Color(current.appearance.policy().backgroundArgb),
            )) {
                Box(Modifier.fillMaxSize().safeDrawingPadding().then(
                    if(startupSplashVisible) Modifier.clearAndSetSemantics {} else Modifier,
                )) {
                    RynxApp(current,actions,onPlayerVisibleChanged={immersivePlayer=it})
                }
                if(startupSplashVisible) {
                    RynxStartupSplash(
                        reduceMotion=current.appearance.reducedMotion(current.reduceMotion),
                        onPulse={vibrateStartupPulse()},
                        onFinished={startupSplashVisible=false},
                    )
                }
            }
        }
    }

    private fun vibrateStartupPulse() {
        val vibrator=getSystemService(Vibrator::class.java) ?: return
        if(vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(38,VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onSaveInstanceState(outState:Bundle) {
        outState.putString("pending-lyrics-track",pendingLyricsTrackId)
        super.onSaveInstanceState(outState)
    }
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { refreshScrobblingState() }
        renderingMonitor?.start()
        val future = MediaController.Builder(this,SessionToken(this,ComponentName(this,PlaybackService::class.java))).buildAsync()
        connection = future
        future.addListener({
            if(connection !== future) return@addListener
            try { controller=future.get(); controller?.addListener(listener); publish() }
            catch(_: Exception) { state.update { it.copy(error="The playback service could not be connected. Reopen rynx to retry.") } }
        },ContextCompat.getMainExecutor(this))
    }
    override fun onStop() {
        renderingMonitor?.stop()
        controller?.removeListener(listener)
        controller=null
        connection?.let(MediaController::releaseFuture)
        connection=null
        super.onStop()
    }
    override fun onDestroy() {
        outputDeviceCallback?.let(audioManager::unregisterAudioDeviceCallback)
        outputDeviceCallback=null
        searchJob?.cancel()
        if(::discovery.isInitialized)discovery.close()
        playbackRequests.cancel()
        lyricsJob?.cancel()
        playlistObservation?.cancel()
        if(libraryDatabase.isInitialized())libraryDatabase.value.close()
        scrobbleAccountPreferences.unregisterOnSharedPreferenceChangeListener(scrobbleAccountListener)
        val ready=if(::catalogReady.isInitialized)catalogReady else null
        ready?.cancel()
        if(::catalog.isInitialized) {val owned=catalog;AndroidResourceCloser.close {ready?.join();owned.close()}}
        innertubexResolver?.close()
        innertubexResolver = null
        innertubexHttpClient?.close()
        innertubexHttpClient = null
        if(scrobbleHttpClient.isInitialized()) scrobbleHttpClient.value.close()
        super.onDestroy()
    }

    private suspend fun refreshScrobblingState() {
        val current=scrobbleAccountStore.load()
        state.update {it.copy(scrobbling=current)}
    }

    private fun runScrobbleAction(provider:ScrobbleProvider,action:suspend ()->Unit) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                refreshScrobblingState()
            } catch(cancelled:CancellationException) {
                throw cancelled
            } catch(failure:Exception) {
                val message=failure.message?.takeIf(String::isNotBlank) ?: "The account request failed. Try again."
                state.update {current ->
                    val scrobbling=current.scrobbling
                    val updated=when(provider) {
                        ScrobbleProvider.LastFm -> scrobbling.copy(lastFm=scrobbling.lastFm.copy(error=message))
                        ScrobbleProvider.ListenBrainz -> scrobbling.copy(listenBrainz=scrobbling.listenBrainz.copy(error=message))
                    }
                    current.copy(scrobbling=updated)
                }
            }
        }
    }

    private fun beginLastFmAuthorization() {
        lifecycleScope.launch {
            try {
                val approvalUrl=withContext(Dispatchers.IO) {
                    val token=lastFmClient.requestAuthorizationToken()
                    scrobbleAccountStore.beginLastFmApproval(token)
                    lastFmClient.authorizationUrl(token)
                }
                refreshScrobblingState()
                startActivity(Intent(Intent.ACTION_VIEW,approvalUrl.toUri()))
            } catch(cancelled:CancellationException) {
                throw cancelled
            } catch(failure:Exception) {
                val message=failure.message?.takeIf(String::isNotBlank) ?: "Last.fm authorization could not be started."
                state.update {it.copy(scrobbling=it.scrobbling.copy(lastFm=it.scrobbling.lastFm.copy(error=message)))}
            }
        }
    }

    private fun finishLastFmAuthorization() = runScrobbleAction(ScrobbleProvider.LastFm) {
        val token=scrobbleAccountStore.pendingLastFmToken()
            ?: throw IllegalStateException("Start Last.fm approval in your browser, then return here to finish login.")
        val session=lastFmClient.completeAuthorization(token)
        scrobbleAccountStore.connectLastFm(session)
    }

    private fun beginListenBrainzAuthorization() {
        scrobbleAccountStore.beginListenBrainzTokenEntry()
        lifecycleScope.launch { refreshScrobblingState() }
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW,"https://listenbrainz.org/settings/".toUri()))
        }.onFailure {
            state.update {current->current.copy(scrobbling=current.scrobbling.copy(listenBrainz=current.scrobbling.listenBrainz.copy(
                error="No browser can open ListenBrainz settings.",
            )))}
        }
    }

    private fun searchCatalog(query: String) {
        val normalized=query.trim()
        searchJob?.cancel()
        if(normalized.isEmpty()) {
            state.update { it.copy(catalogSearch=CatalogSearchState()) }
            return
        }
        state.update { it.copy(catalogSearch=CatalogSearchState(query=normalized,loading=true),error=null) }
        searchJob=lifecycleScope.launch {
            try {
                if(!catalogReady.await()) error("YouTube Music is unavailable")
                val apple=try {withContext(Dispatchers.IO){catalog.searchAppleMusic(normalized,state.value.discovery.region)}}
                    catch(cancelled:CancellationException){throw cancelled} catch(_:Exception){emptyList()}
                if(apple.isNotEmpty()) {
                    state.update {it.copy(catalogSearch=CatalogSearchState(query=normalized,metadataItems=apple))}
                    return@launch
                }
                val page=withContext(Dispatchers.IO) { catalog.searchMusic(normalized) }
                state.update { it.copy(catalogSearch=CatalogSearchState(
                    query=normalized,
                    items=page.items.distinctBy { item -> item.provider to item.mediaId },
                    continuation=page.continuation,
                )) }
            } catch(cancelled: CancellationException) {
                throw cancelled
            } catch(_: Exception) {
                state.update { it.copy(catalogSearch=CatalogSearchState(
                    query=normalized,
                    error="YouTube Music search failed. Check your connection and try again.",
                )) }
            }
        }
    }

    private fun loadMoreCatalog() {
        val before=state.value.catalogSearch
        val continuation=before.continuation ?: return
        if(before.loading||before.loadingMore) return
        searchJob?.cancel()
        state.update { it.copy(catalogSearch=it.catalogSearch.copy(loadingMore=true,error=null)) }
        searchJob=lifecycleScope.launch {
            try {
                if(!catalogReady.await()) error("YouTube Music is unavailable")
                val page=withContext(Dispatchers.IO) { catalog.searchMore(continuation) }
                state.update { current ->
                    val active=current.catalogSearch
                    if(active.query!=before.query) current else current.copy(catalogSearch=active.copy(
                        items=(active.items+page.items).distinctBy { item -> item.provider to item.mediaId },
                        continuation=page.continuation,
                        loadingMore=false,
                    ))
                }
            } catch(cancelled: CancellationException) {
                throw cancelled
            } catch(_: Exception) {
                state.update { it.copy(catalogSearch=it.catalogSearch.copy(
                    loadingMore=false,
                    error="More YouTube Music results could not be loaded. Check your connection and try again.",
                )) }
            }
        }
    }

    private fun playDiscoveryTrack(track:MusicMetadata) = startMusicRequest {
        catalogReady.await()
        playbackPreparer.prepareDiscovery(track)
    }
    private fun playCatalogItem(item:CatalogItem) {
        if(item.type!=CatalogItemType.Track)return
        startMusicRequest {
            catalogReady.await()
            playbackPreparer.prepareCatalog(item)
        }
    }
    private fun startMusicRequest(load:suspend ()->PreparedMusic) {
        state.update {it.copy(error=null)}
        playbackRequests.start(load=load,commit={prepared->
            val stream=prepared.stream;val item=prepared.item;val apple=prepared.metadata
            val output=controller
            if(stream==null)state.update {it.copy(error="This track could not be played right now.")}
            else if(output==null)state.update {it.copy(error="The player is reconnecting. Try this track again shortly.")}
            else {
                StreamRequestHeaderStore.register(stream.url,stream.headers)
                val metadata=MediaMetadata.Builder().setTitle(apple?.title?:item.title).setArtist(apple?.artist?:item.artist?:"YouTube Music")
                    .setExtras(Bundle().apply {apple?.sourceUrl?.let {putString("rynx.metadata-source",it)};apple?.let {putString("rynx.metadata-id",it.id);putStringArrayList("rynx.metadata-genres",ArrayList(it.genreIds))}})
                    .apply {(apple?.artworkUrl?:item.artworkUrl)?.let {setArtworkUri(it.toUri())}}.build()
                val id="yt:${item.mediaId}"
                val media=MediaItem.Builder().setMediaId(id).setUri(stream.url).setMimeType(stream.mimeType).setMediaMetadata(metadata).build()
                output.setMediaItem(media);output.prepare();output.play();discovery.closeSelection();publish()
            }
        },onFailure={failure->state.update {it.copy(error=when(failure) {
            is NoMatchingRecordingException -> "No matching recording was found on YouTube Music. Try a search result or open this song in Apple Music."
            is ProviderAccessRefusalException -> "The provider restricts playback of this track."
            else -> "Rynx couldn't prepare a playable stream. Check your connection and try again."
        })}})
    }
    private fun restoreLibrary() {
        lifecycleScope.launch(Dispatchers.IO) {
        val metadata=LocalAudioMetadata(applicationContext)
        val tracks=preferences.getStringSet("uris",emptySet()).orEmpty().sorted().mapNotNull { raw ->
            val uri=raw.toUri()
            try {
                val title=contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { cursor ->
                    if(cursor.moveToFirst()) cursor.getString(0) else null
                } ?: return@mapNotNull null
                metadata.read(uri,title)
            } catch(_: Exception) { null }
        }
        state.update { it.copy(tracks=tracks) }
        }
    }
    private fun publish() {
        val output=controller ?: return
        val previous=state.value
        val currentItem=output.currentMediaItem
        val currentTrack=currentItem?.let { trackFor(it,previous.tracks) }
        val revision=preferences.getLong("history-revision",0L)
        val recentTracks=if(historyRevision!=revision) {
            historyRevision=revision;TrackMetadataStorage.decode(preferences.getString("recent-tracks",null))
        } else previous.recentTracks
        state.update { current -> current.copy(
            currentId=currentItem?.mediaId,
            currentTrack=currentTrack,
            recentTracks=recentTracks,
            queue=(0 until output.mediaItemCount).map { index ->
                val item=output.getMediaItemAt(index)
                trackFor(item,current.tracks)
            },
            playing=output.isPlaying,
            busy=output.playbackState==Player.STATE_BUFFERING || playbackRequests.isPending,
            positionMillis=output.currentPosition.coerceAtLeast(0),
            durationMillis=output.duration.coerceAtLeast(0),
            audioSignalInfo=readAudioSignalInfo(output),
            audioClipCount=AudioEngineDiagnostics.clippingCount,
            repeatLabel=when(output.repeatMode){Player.REPEAT_MODE_ALL->"All";Player.REPEAT_MODE_ONE->"One";else->"Off"},
            shuffleEnabled=output.shuffleModeEnabled,
            queueOrder=playbackOrder(output.currentTimeline,output.shuffleModeEnabled),
            queueEditingAvailable=output.availableSessionCommands.contains(QueueSessionCommands.Move),
            volume=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()/audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1),
            reduceMotion=Settings.Global.getFloat(contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f,
        ) }
        val trackId=currentItem?.mediaId
        if(trackId!=loadedLyricsTrackId) {
            loadedLyricsTrackId=trackId;lyricsJob?.cancel()
            state.update { it.copy(lyrics=emptyList(),lyricsTrackId=trackId) }
            if(trackId!=null) lyricsJob=lifecycleScope.launch {
                val lines=lyricsStore.load(trackId)
                state.update { if(it.currentId==trackId) it.copy(lyrics=lines,lyricsTrackId=trackId) else it }
            }
        }
    }

    private fun readAudioSignalInfo(output:MediaController):AudioSignalInfo {
        val selected=output.currentTracks.groups.firstOrNull { it.type==C.TRACK_TYPE_AUDIO && it.isSelected }
        val format=selected?.let { group ->
            (0 until group.length).firstOrNull(group::isTrackSelected)?.let(group::getTrackFormat)
        }
        val encoding=format?.pcmEncoding
        val bitDepth=when(encoding) {
            C.ENCODING_PCM_8BIT->8
            C.ENCODING_PCM_16BIT,C.ENCODING_PCM_16BIT_BIG_ENDIAN->16
            C.ENCODING_PCM_24BIT,C.ENCODING_PCM_24BIT_BIG_ENDIAN->24
            C.ENCODING_PCM_32BIT,C.ENCODING_PCM_32BIT_BIG_ENDIAN->32
            C.ENCODING_PCM_FLOAT,C.ENCODING_PCM_FLOAT_BIG_ENDIAN->32
            else->null
        }
        val nativeRate=AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).takeIf {it>0}
        val dsp=AudioEngineDiagnostics.pcmFormat
        val dspDepth=when(dsp?.encoding) {
            C.ENCODING_PCM_8BIT->8
            C.ENCODING_PCM_16BIT,C.ENCODING_PCM_16BIT_BIG_ENDIAN->16
            C.ENCODING_PCM_24BIT,C.ENCODING_PCM_24BIT_BIG_ENDIAN->24
            C.ENCODING_PCM_32BIT,C.ENCODING_PCM_32BIT_BIG_ENDIAN,C.ENCODING_PCM_FLOAT,C.ENCODING_PCM_FLOAT_BIG_ENDIAN->32
            else->null
        }
        return AudioSignalInfo(
            sourceSampleRateHz=format?.sampleRate?.takeIf {it>0},
            sourceBitDepth=bitDepth,
            sourceEncoding=format?.sampleMimeType,
            primaryOutputNativeRateHz=nativeRate,
            availableOutputRoutes=availableAudioRoutes,
            dspInputSampleRateHz=dsp?.sampleRateHz,
            dspInputBitDepth=dspDepth,
            dspInputChannelCount=dsp?.channelCount,
            dspProcessingActive=dsp?.processingEnabled,
        )
    }
    private fun refreshAvailableAudioRoutes() {
        availableAudioRoutes=audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .mapNotNull {it.productName?.toString()?.takeIf(String::isNotBlank)}.distinct()
    }
    private fun trackFor(item: MediaItem, tracks: List<TrackItem>): TrackItem =
        tracks.firstOrNull { it.id==item.mediaId } ?: TrackItem(
            id=item.mediaId,
            title=item.mediaMetadata.title?.toString()?.takeIf(String::isNotBlank) ?: "Unknown track",
            subtitle=item.mediaMetadata.artist?.toString()?.takeIf(String::isNotBlank) ?: "Unknown artist",
            artworkUrl=item.mediaMetadata.artworkUri?.toString(),
            metadataSourceUrl=item.mediaMetadata.extras?.getString("rynx.metadata-source"),
            albumTitle=item.mediaMetadata.albumTitle?.toString(),
        )

    private fun launchLibraryMutation(fallback:String,operation:suspend ()->Any?) {
        lifecycleScope.launch {
            try {
                operation()
            } catch(cancelled:CancellationException) {
                throw cancelled
            } catch(failure:Exception) {
                val message=(failure as? InvalidPlaylistName)?.message ?: fallback
                state.update { it.copy(error=message) }
            }
        }
    }
}

private fun readPlaylistArchive(input:InputStream):String {
    val output=ByteArrayOutputStream()
    val buffer=ByteArray(8_192)
    var total=0
    while(true) {
        val count=input.read(buffer)
        if(count<0)break
        if(count==0)continue
        total+=count
        if(total>PlaylistArchive.MAX_ARCHIVE_BYTES)
            throw dev.bxane.rynx.library.InvalidPlaylistArchive("Playlist archive exceeds size limit")
        output.write(buffer,0,count)
    }
    return output.toByteArray().toString(Charsets.UTF_8)
}
