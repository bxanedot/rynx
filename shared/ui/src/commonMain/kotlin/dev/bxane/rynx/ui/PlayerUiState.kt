package dev.bxane.rynx.ui

import dev.bxane.rynx.playback.catalog.CatalogItem
import dev.bxane.rynx.playback.LyricLine
import dev.bxane.rynx.design.RynxAppearance
import dev.bxane.rynx.playback.discovery.*
import dev.bxane.rynx.library.Playlist
import dev.bxane.rynx.playback.audio.AudioDspSettings
import dev.bxane.rynx.playback.audio.AudioSignalInfo
import dev.bxane.rynx.playback.scrobble.ScrobbleProvider
import dev.bxane.rynx.playback.scrobble.ScrobbleSettingsState

data class TrackItem(
    val id: String,
    val title: String,
    val subtitle: String = "Local audio",
    val artworkUrl: String? = null,
    val metadataSourceUrl:String?=null,
)
data class CatalogSearchState(
    val query: String = "",
    val items: List<CatalogItem> = emptyList(),
    val continuation: String? = null,
    val metadataItems:List<MusicMetadata> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

data class PlayerUiState(
    val tracks: List<TrackItem> = emptyList(),
    val queue: List<TrackItem> = emptyList(),
    val currentId: String? = null,
    val playing: Boolean = false,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
    val busy: Boolean = false,
    val error: String? = null,
    val repeatLabel: String = "Off",
    val catalogSearch: CatalogSearchState = CatalogSearchState(),
    val catalogSearchEnabled: Boolean = true,
    val catalogPlaybackEnabled: Boolean = false,
    val currentTrack: TrackItem? = null,
    val favoriteIds: Set<String> = emptySet(),
    val favoriteTracks: List<TrackItem> = emptyList(),
    val recentTracks: List<TrackItem> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val playlistBackupAvailable: Boolean = false,
    val volume: Float = 1f,
    val volumeControlAvailable: Boolean = false,
    val audioOutputAvailable: Boolean = false,
    val audioOutputName: String = "This device",
    val audioDspSettings: AudioDspSettings = AudioDspSettings(),
    val audioSignalInfo: AudioSignalInfo = AudioSignalInfo(),
    val audioClipCount: Long = 0,
    val audioSettingsOpen: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val shuffleAvailable: Boolean = false,
    val queueEditingAvailable: Boolean = false,
    val reduceMotion: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val lyricsTrackId: String? = null,
    val lyricsImportAvailable: Boolean = false,
    /** Original timeline indexes in playback order; empty means natural order. */
    val queueOrder: List<Int> = emptyList(),
    val appearance: RynxAppearance = RynxAppearance(),
    val renderingPressure: Boolean = false,
    val discovery:DiscoveryState = DiscoveryState(),
    val discoverySelection:HomeSection?=null,
    val discoverySelectionLoading:Boolean=false,
    val scrobbling:ScrobbleSettingsState=ScrobbleSettingsState(),
) { val current: TrackItem? get() = currentTrack ?: tracks.firstOrNull { it.id == currentId } }

interface PlayerActions {
    fun importFiles()
    fun play(id: String)
    fun toggle()
    fun previous()
    fun next()
    fun seek(fraction: Float)
    fun cycleRepeat()
    fun removeFromQueue(id: String)
    fun dismissError()
    fun searchYouTubeMusic(query: String)
    fun loadMoreYouTubeMusic()
    fun playCatalog(item: dev.bxane.rynx.playback.catalog.CatalogItem) = Unit
    fun toggleFavorite(id: String) = Unit
    fun setVolume(fraction: Float) = Unit
    fun showAudioOutput() = Unit
    fun updateAudioDsp(settings: AudioDspSettings) = Unit
    fun openAudioSettings() = Unit
    fun closeAudioSettings() = Unit
    fun toggleShuffle() = Unit
    fun moveInQueue(id: String, toIndex: Int) = Unit
    fun clearUpcomingQueue() = Unit
    fun updateAppearance(appearance: RynxAppearance) = Unit
    fun importLyrics() = Unit
    fun refreshDiscovery() = Unit
    fun changeDiscoveryRegion(code:String) = Unit
    fun openDiscoverySection(id:String) = Unit
    fun openDiscoveryCollection(collection:ChartCollection) = Unit
    fun openGenre(genre:MusicGenre) = Unit
    fun closeDiscovery() = Unit
    fun playDiscovery(track:MusicMetadata) = Unit
    fun openMusicSource(url:String) = Unit
    fun createPlaylist(name:String) = Unit
    fun renamePlaylist(id:Long,name:String) = Unit
    fun deletePlaylist(id:Long) = Unit
    fun addTrackToPlaylist(playlistId:Long,track:TrackItem) = Unit
    fun removePlaylistEntry(playlistId:Long,entryId:Long) = Unit
    fun movePlaylistEntry(playlistId:Long,entryId:Long,toIndex:Int) = Unit
    fun importPlaylistBackup() = Unit
    fun exportPlaylistBackup() = Unit
    fun beginLastFmLogin() = Unit
    fun finishLastFmLogin() = Unit
    fun disconnectLastFm() = Unit
    fun setScrobblingEnabled(provider:ScrobbleProvider,enabled:Boolean) = Unit
    fun beginListenBrainzLogin() = Unit
    fun connectListenBrainz(token:String) = Unit
    fun disconnectListenBrainz() = Unit
}
