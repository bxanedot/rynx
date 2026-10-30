package dev.bxane.rynx.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.Dialog
import dev.bxane.rynx.design.*
import dev.bxane.rynx.ui.material.*
import dev.chrisbanes.haze.rememberHazeState

private data class Palette(val bg: Color, val surface: Color, val text: Color, val secondary: Color, val line: Color)
internal enum class AppTab(val title: String, val glyph: Glyph) {
    ListenNow("Home", Glyph.Home), Browse("Browse", Glyph.Browse), Radio("Radio", Glyph.Radio),
    Library("Library", Glyph.Library), Search("Search", Glyph.Search),
}
private val Accent = Color(0xFFFA2D48)
private val LocalPalette = staticCompositionLocalOf { Palette(Color.White,Color(0xFFF5F5F7),Color(0xFF18181A),Color(0xFF747478),Color(0xFFE5E5E8)) }

@Composable private fun Label(text: String, size: Int = 14, color: Color = LocalPalette.current.text, bold: Boolean = false, modifier: Modifier = Modifier) {
    BasicText(text, modifier, style=TextStyle(color=color,fontSize=size.sp,fontFamily=FontFamily.SansSerif,fontWeight=if(bold) FontWeight.SemiBold else FontWeight.Normal), maxLines=1, overflow=TextOverflow.Ellipsis)
}

@Composable private fun IconButton(glyph: Glyph, description: String, enabled: Boolean = true, tint: Color = LocalPalette.current.text, onClick: () -> Unit) {
    val interaction=remember {MutableInteractionSource()}
    Box(Modifier.size(44.dp).rynxPressFeedback(interaction,enabled).rynxMaterial(16.dp,interaction=interaction)
        .clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=null,onClick=onClick)
        .semantics{contentDescription=description;if(!enabled)disabled()},contentAlignment=Alignment.Center) {
        RynxGlyph(glyph,if(enabled) tint else tint.copy(alpha=.28f))
    }
}

@Composable private fun Action(text: String, action: () -> Unit, enabled: Boolean = true) {
    RynxButton(text,action,enabled=enabled,selected=true)
}

@Composable internal fun Artwork(size: Dp, modifier: Modifier = Modifier, artworkUrl:String?=null) {
    Box(modifier.size(size).clip(RoundedCornerShape(size*.09f)).background(
        Brush.linearGradient(listOf(Color(0xFF25202D),Color(0xFF653041))),
    ),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center=Offset(this.size.width/2f,this.size.height/2f)
            drawCircle(Brush.radialGradient(listOf(Accent.copy(alpha=.58f),Color.Transparent),center,this.size.minDimension*.72f),this.size.minDimension*.72f,center)
        }
        RynxBrandMark(Modifier.size(size*.52f))
        artworkUrl?.takeIf(String::isNotBlank)?.let { url ->
            AsyncImage(model=url,contentDescription=null,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        }
    }
}

/** Apple Music reference patterns; dimensions are independently selected, not measured parity. */
@Composable
fun RynxApp(state: PlayerUiState, actions: PlayerActions, onPlayerVisibleChanged:(Boolean)->Unit={}) {
    var appearance by remember {mutableStateOf(state.appearance.normalized())}
    LaunchedEffect(state.appearance) {appearance=state.appearance.normalized()}
    fun updateAppearance(value:RynxAppearance) {appearance=value.normalized();actions.updateAppearance(appearance)}
    val dark=appearance.darkTheme || appearance.material==RynxMaterialMode.Amoled
    val effectiveState=state.copy(appearance=appearance,reduceMotion=appearance.reducedMotion(state.reduceMotion))
    var appearanceOpen by rememberSaveable {mutableStateOf(false)}
    var settingsOpen by rememberSaveable {mutableStateOf(false)}
    val backdrop=rememberHazeState()
    var query by rememberSaveable { mutableStateOf("") }
    var activeTab by rememberSaveable { mutableStateOf(AppTab.ListenNow) }
    var previousTab by rememberSaveable { mutableStateOf(AppTab.ListenNow) }
    var navigationExpanded by rememberSaveable { mutableStateOf(true) }
    var requestSearchFocus by remember { mutableStateOf(false) }
    fun selectTab(tab:AppTab,focusSearch:Boolean=false) {
        if(tab==AppTab.Search) {
            if(activeTab!=AppTab.Search)previousTab=activeTab
            requestSearchFocus=focusSearch
        } else {
            previousTab=tab
            requestSearchFocus=false
        }
        activeTab=tab
    }
    fun closeSearch() { selectTab(if(previousTab==AppTab.Search)AppTab.ListenNow else previousTab) }
    val player=rememberPlayerPresentation()
    val playerVisible by remember {derivedStateOf {player.visible}}
    RynxPlatformBackHandler(enabled=activeTab==AppTab.Search && !appearanceOpen && state.discoverySelection==null,onBack=::closeSearch)
    RynxPlatformBackHandler(enabled=settingsOpen && !appearanceOpen && !state.audioSettingsOpen,onBack={settingsOpen=false})
    LaunchedEffect(playerVisible) {onPlayerVisibleChanged(playerVisible)}
    var previousTrackId by remember {mutableStateOf(state.currentId)}
    LaunchedEffect(state.currentId) {
        if(previousTrackId!=null && state.current==null)player.close(true)
        previousTrackId=state.currentId
    }
    val policy=appearance.policy(state.renderingPressure)
    val palette=if(dark) Palette(Color(policy.backgroundArgb),Color(policy.surfaceArgb),Color(0xFFF5F5F7),Color(0xFFA4A4AA),Color(0xFF353538))
        else Palette(Color(policy.backgroundArgb),Color(policy.surfaceArgb),Color(0xFF18181A),Color(0xFF747478),Color(0xFFE5E5E8))
    CompositionLocalProvider(LocalPalette provides palette,LocalRynxAppearance provides appearance,
        LocalRynxBackdrop provides backdrop,LocalRynxReducedMotion provides effectiveState.reduceMotion,
        LocalRynxRenderingPressure provides state.renderingPressure) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().rynxBackdropSource(0f).background(palette.bg))
            val windowPolicy=rynxWindowPolicy(maxWidth.value,maxHeight.value)
            val wide=windowPolicy.sideNavigation
            val searching = activeTab == AppTab.Search
            Row(Modifier.fillMaxSize().rynxBackdropSource(1f).graphicsLayer { scaleX=1f-.025f*player.progress;scaleY=scaleX }
                .semantics { if(playerVisible || settingsOpen || appearanceOpen || state.discoverySelection!=null) hideFromAccessibility() }) {
                if(wide) Column(Modifier.width(216.dp).fillMaxHeight().background(palette.surface).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Label("rynx",28,bold=true,modifier=Modifier.padding(bottom=30.dp))
                    AppTab.entries.forEach { tab -> NavItem(tab.title,tab.glyph,activeTab==tab){selectTab(tab)} }
                    Spacer(Modifier.weight(1f))
                    Label("by bxane",12,palette.secondary)
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=if(wide)32.dp else 24.dp,vertical=20.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(searching) IconButton(Glyph.Previous,"Return to ${previousTab.title}",onClick=::closeSearch)
                        Label(activeTab.title,32,bold=true,modifier=Modifier.weight(1f))
                        IconButton(Glyph.Appearance,"Settings"){settingsOpen=true}
                        if(activeTab==AppTab.Library) IconButton(Glyph.Add,"Import audio files",onClick=actions::importFiles)
                    }
                    NavigationSearchTransition(
                        expanded=searching,
                        requestFocusOnExpand=requestSearchFocus,
                        value=query,
                        onValueChange={query=it},
                        onSearch={actions.searchYouTubeMusic(query.trim())},
                        onFocusConsumed={requestSearchFocus=false},
                        reducedMotion=effectiveState.reduceMotion,
                        modifier=Modifier.padding(horizontal=24.dp),
                    )
                    if(searching && state.catalogSearchEnabled) Row(Modifier.fillMaxWidth().padding(horizontal=24.dp, vertical=12.dp),horizontalArrangement=Arrangement.End) {
                        Action(if(state.catalogSearch.loading) "Searching…" else if(state.discovery.enabled) "Search music" else "Search YouTube Music",{actions.searchYouTubeMusic(query.trim())},enabled=query.isNotBlank()&&!state.catalogSearch.loading)
                    }
                    if(state.error!=null) Row(Modifier.fillMaxWidth().background(Accent.copy(alpha=.1f)).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                        BasicText(state.error,Modifier.weight(1f),style=TextStyle(color=palette.text,fontSize=13.sp))
                        IconButton(Glyph.Close,"Dismiss error",onClick=actions::dismissError)
                    }
                    if(state.busy) Label("Opening audio…",color=palette.secondary,modifier=Modifier.padding(24.dp))
                    when(activeTab) {
                        AppTab.ListenNow -> ListenNowContent(state,actions,{selectTab(AppTab.Browse)},{selectTab(AppTab.Search,focusSearch=true)},
                            { term -> query=term;selectTab(AppTab.Search);actions.searchYouTubeMusic(term) },Modifier.weight(1f))
                        AppTab.Browse -> BrowseContent(state,actions,{term -> query=term;selectTab(AppTab.Search);actions.searchYouTubeMusic(term)},Modifier.weight(1f))
                        AppTab.Radio -> RadioContent({ _:String -> query="";selectTab(AppTab.Search,focusSearch=true) },Modifier.weight(1f))
                        AppTab.Library -> LibraryContent(state,actions,Modifier.weight(1f))
                        AppTab.Search -> SearchContent(state,actions,query,Modifier.weight(1f))
                    }
                    if(!wide) Spacer(Modifier.height(if(state.current!=null)74.dp else NavigationDockHeight.dp))
                }
            }
            Box(Modifier.fillMaxSize().semantics {if(settingsOpen || appearanceOpen || state.audioSettingsOpen || state.discoverySelection!=null)hideFromAccessibility()}) {RynxFluidPlayer(effectiveState,actions,player)}
            if(!wide) FloatingNavigation(
                hasTrack=state.current!=null,player=player,activeTab=activeTab,reduced=effectiveState.reduceMotion,
                blocked=settingsOpen || appearanceOpen || state.audioSettingsOpen || state.discoverySelection!=null,expanded=navigationExpanded,
                onExpansionChange={navigationExpanded=it},viewportWidth=maxWidth.value,viewportHeight=maxHeight.value,
                modifier=Modifier.align(Alignment.BottomCenter),
            ) {tab->
                selectTab(tab,focusSearch=tab==AppTab.Search)
                if(playerVisible)player.close(effectiveState.reduceMotion)
            }
            if(state.discoverySelection!=null)DiscoveryDetail(state,actions)
            if(settingsOpen) SettingsPage(
                state=state,
                actions=actions,
                appearance=appearance,
                onAppearanceChange=::updateAppearance,
                onOpenAppearance={appearanceOpen=true},
                onOpenAudio=actions::openAudioSettings,
                onShowAudioOutput=actions::showAudioOutput,
                onImportFiles=actions::importFiles,
                onOpenLibrary={settingsOpen=false;selectTab(AppTab.Library)},
                onImportPlaylistBackup=actions::importPlaylistBackup,
                onExportPlaylistBackup=actions::exportPlaylistBackup,
                onClose={settingsOpen=false},
            )
            if(appearanceOpen) AppearanceSheet(appearance,::updateAppearance,{appearanceOpen=false;actions.openAudioSettings()}){appearanceOpen=false}
            if(state.audioSettingsOpen) AudioDspSheet(state.audioDspSettings,state.audioSignalInfo,state.audioClipCount,
                actions::updateAudioDsp,actions::closeAudioSettings)

        }
    }
}

@Composable private fun FloatingNavigation(
    hasTrack:Boolean,player:PlayerPresentation,activeTab:AppTab,reduced:Boolean,blocked:Boolean,
    expanded:Boolean,onExpansionChange:(Boolean)->Unit,viewportWidth:Float,viewportHeight:Float,modifier:Modifier,onSelect:(AppTab)->Unit,
) {
    val geometry=playerGeometry(viewportWidth,viewportHeight,player.progress)
    val reveal=if(hasTrack)geometry.navigationReveal else 1f
    val interactive=(!hasTrack || geometry.navigationInteractive) && !blocked
    if(reveal<=0f)return
    Box(modifier.padding(horizontal=14.dp,vertical=7.dp).height(RynxNavigationMotionSpec.BarHeightDp.dp)
        .graphicsLayer {translationY=(1f-reveal)*40.dp.toPx();alpha=reveal;scaleX=.94f+.06f*reveal;scaleY=scaleX}
        .semantics {if(!interactive)hideFromAccessibility()}) {
        CompositionLocalProvider(LocalRynxMaterialLayer provides 4f) {
            MorphingNavigationBar(
                destinations=AppTab.entries.map {NavigationDestination(it.title,it.glyph)},
                selectedIndex=AppTab.entries.indexOf(activeTab),searchIndex=AppTab.entries.indexOf(AppTab.Search),
                expanded=expanded,onExpansionChange=onExpansionChange,playerReveal=if(hasTrack)reveal else 0f,
                reducedMotion=reduced,blocked=!interactive,
                onSelectIndex={index -> if(interactive)onSelect(AppTab.entries[index])},
                itemContent={index,selected,labelAlpha,morphProgress,itemModifier,enabled,onClick ->
                    BottomTabItem(AppTab.entries[index],selected,labelAlpha,morphProgress,itemModifier,enabled,onClick)
                },
            )
        }
    }
}

/**
 * Real, useful Home even when the remote catalog is unavailable. The For You shelf
 * contains ONLY the listener's local favorites and recent plays, never fake songs.
 */
@Composable private fun ListenNowContent(
    state:PlayerUiState,
    actions:PlayerActions,
    onBrowse:()->Unit,
    onSearch:()->Unit,
    onExploreQuery:(String)->Unit,
    modifier:Modifier,
) {
    val p=LocalPalette.current
    val personal=forYouTracks(state)
    LazyColumn(modifier.padding(horizontal=20.dp),contentPadding=PaddingValues(top=5.dp,bottom=28.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(bottom=14.dp)) {
                Label("YOUR MUSIC, YOUR WAY",11,Accent,bold=true)
                Spacer(Modifier.height(5.dp))
                Label("For You",29,bold=true)
                Spacer(Modifier.height(5.dp))
                Label("Picked from music you love and songs you've played.",13,p.secondary)
            }
        }
        if(personal.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp),
                    contentPadding=PaddingValues(end=6.dp)) {
                    items(personal,key={"for-you:"+it.id}) { track ->
                        Column(Modifier.width(160.dp)
                            .clickable(role=Role.Button,onClick={actions.play(track.id)})
                            .semantics { contentDescription="Play For You "+track.title }) {
                            Artwork(160.dp,artworkUrl=track.artworkUrl)
                            Spacer(Modifier.height(9.dp))
                            Label(track.title,15,bold=true)
                            Label(track.subtitle,12,p.secondary)
                        }
                    }
                }
            }
        } else {
            item {
                Column(Modifier.fillMaxWidth().rynxMaterial(21.dp,amount=.55f).padding(18.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        RynxBrandMark(Modifier.size(57.dp))
                        Column(Modifier.weight(1f)) {
                            Label("Your mix starts here",18,bold=true)
                            Spacer(Modifier.height(5.dp))
                            Label("Play or favorite a track to fill For You.",13,p.secondary)
                        }
                    }
                    Action("Find your first song",onSearch)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top=22.dp,bottom=12.dp)) {
                SectionHeading("Explore music",null)
                Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    RynxButton("Trending",{onExploreQuery("trending songs")},Modifier.weight(1f))
                    RynxButton("New music",{onExploreQuery("new music")},Modifier.weight(1f))
                    RynxButton("Chill",{onExploreQuery("chill music")},Modifier.weight(1f))
                }
            }
        }
        if(state.discovery.enabled) {
            item { DiscoveryHeader(state.discovery,actions) }
            homeDiscoverySections(state.discovery,actions)
        } else {
            item {
                Label("YouTube Music discovery isn't connected on this platform.",14,p.secondary,
                    modifier=Modifier.padding(vertical=16.dp))
                Action("Search songs",onSearch)
            }
        }
        if(state.recentTracks.isNotEmpty()) {
            item {SectionHeading("Recently Played",state.recentTracks.size.toString()+" songs")}
            items(state.recentTracks.take(6),key={"home-recent:"+it.id}) { track ->
                TrackRow(track,track.id==state.currentId,state.playing,
                    {actions.play(track.id)},track.id in state.favoriteIds,
                    {actions.toggleFavorite(track.id)})
            }
        }
        state.current?.takeIf { current -> state.tracks.none { it.id==current.id } }?.let { current ->
            item {
                SectionHeading("Now Playing",null)
                TrackRow(current,true,state.playing,{actions.toggle()},
                    current.id in state.favoriteIds,{actions.toggleFavorite(current.id)})
            }
        }
    }
}

@Composable private fun BrowseContent(state:PlayerUiState,actions:PlayerActions,onSearch:(String)->Unit,modifier:Modifier) {
    if(state.discovery.enabled) {AppleExploreContent(state.discovery,actions,modifier);return}
    val p=LocalPalette.current
    LazyColumn(modifier.padding(horizontal=20.dp),contentPadding=PaddingValues(top=8.dp,bottom=20.dp)) {
        item {
            Label("Browse music",24,bold=true,modifier=Modifier.padding(bottom=7.dp))
            Label("Search across songs, artists, albums and playlists.",14,p.secondary)
            Spacer(Modifier.height(18.dp))
            BrowseShortcut("New music",Glyph.Note){onSearch("new music")}
            BrowseShortcut("Popular songs",Glyph.Browse){onSearch("popular songs")}
            BrowseShortcut("Playlists",Glyph.Queue){onSearch("playlists")}
            BrowseShortcut("Artists",Glyph.Home){onSearch("artists")}
        }
        if(state.tracks.isNotEmpty()) {
            item { SectionHeading("Your music", "${state.tracks.size} songs") }
            items(state.tracks.take(6),key={"browse:${it.id}"}) { track ->
                TrackRow(track,track.id==state.currentId,state.playing,{actions.play(track.id)},track.id in state.favoriteIds,{actions.toggleFavorite(track.id)})
            }
        }
    }
}

@Composable private fun RadioContent(onSearch:(String)->Unit,modifier:Modifier) {
    val p=LocalPalette.current
    Column(modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        RynxGlyph(Glyph.Radio,p.secondary,Modifier.size(54.dp))
        Spacer(Modifier.height(20.dp))
        Label("Radio playback is not connected yet",20,bold=true)
        Spacer(Modifier.height(9.dp))
        Label("Find a song in Search while the radio provider is being built.",14,p.secondary)
        Spacer(Modifier.height(20.dp))
        Action("Find music",{onSearch("")})
    }
}

@Composable private fun LibraryContent(state:PlayerUiState,actions:PlayerActions,modifier:Modifier) {
    val p=LocalPalette.current
    val libraryTracks=(state.tracks+state.favoriteTracks+listOfNotNull(state.current)).distinctBy { it.id }
    val favorites=libraryTracks.filter { it.id in state.favoriteIds }
    val pinnedTracks=(favorites+state.recentTracks+state.tracks)
        .distinctBy(TrackItem::id).filter { !it.artworkUrl.isNullOrBlank() }.take(6)
    val artists=remember(state.tracks) { libraryArtists(state.tracks) }
    val albums=remember(state.tracks) { libraryAlbums(state.tracks) }
    var librarySection by rememberSaveable { mutableStateOf("home") }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlbumArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlbumTitle by rememberSaveable { mutableStateOf<String?>(null) }
    val activeArtist=artists.firstOrNull { it.name == selectedArtistName }
    val activeAlbum=albums.firstOrNull { it.artist == selectedAlbumArtist && it.title == selectedAlbumTitle }
    fun backFromCollection() {
        when {
            selectedAlbumTitle != null -> { selectedAlbumTitle=null; selectedAlbumArtist=null }
            selectedArtistName != null -> selectedArtistName=null
            else -> librarySection="home"
        }
    }
    fun openAlbum(album:LibraryAlbum) { selectedAlbumArtist=album.artist; selectedAlbumTitle=album.title }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renamePlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    var createPlaylistOpen by rememberSaveable { mutableStateOf(false) }
    var addTrack by remember { mutableStateOf<TrackItem?>(null) }
    val selectedPlaylist=state.playlists.firstOrNull { it.id==selectedPlaylistId }
    val renameTarget=state.playlists.firstOrNull { it.id==renamePlaylistId }
    RynxPlatformBackHandler(
        enabled = selectedPlaylistId != null || selectedAlbumTitle != null ||
            selectedArtistName != null || librarySection != "home",
        onBack = { if(selectedPlaylistId != null) selectedPlaylistId=null else backFromCollection() },
    )
    if(selectedPlaylistId!=null && selectedPlaylist==null) selectedPlaylistId=null
    if(renamePlaylistId!=null && renameTarget==null) renamePlaylistId=null
    LaunchedEffect(activeArtist, selectedArtistName) {
        if(selectedArtistName != null && activeArtist == null) selectedArtistName=null
    }
    LaunchedEffect(activeAlbum, selectedAlbumTitle) {
        if(selectedAlbumTitle != null && activeAlbum == null) {selectedAlbumTitle=null;selectedAlbumArtist=null}
    }

    if(activeAlbum != null) {
        LibraryAlbumScreen(activeAlbum,state,actions,modifier,::backFromCollection)
    } else if(activeArtist != null) {
        LibraryArtistScreen(activeArtist,state,actions,modifier,::backFromCollection,::openAlbum)
    } else if(librarySection == "artists") {
        LibraryCollectionIndex("Artists",artists.size,modifier,::backFromCollection) {
            items(artists,key={"artist:"+it.name}) { artist ->
                LibraryArtistRow(artist) { selectedArtistName=artist.name }
            }
        }
    } else if(librarySection == "albums") {
        LibraryCollectionIndex("Albums",albums.size,modifier,::backFromCollection) {
            items(albums,key={"album:"+it.artist+":"+it.title}) { album ->
                LibraryAlbumRow(album) { openAlbum(album) }
            }
        }
    } else if(librarySection == "songs") {
        LibraryCollectionIndex("Songs",state.tracks.size,modifier,::backFromCollection) {
            items(state.tracks,key={"collection-song:"+it.id}) { track ->
                TrackRow(track,track.id==state.currentId,state.playing,{actions.play(track.id)},
                    track.id in state.favoriteIds,{actions.toggleFavorite(track.id)},
                    onAddToPlaylist={addTrack=track})
            }
        }
    } else if(librarySection == "favorites") {
        LibraryCollectionIndex("Favorites",favorites.size,modifier,::backFromCollection) {
            items(favorites,key={"collection-favorite:"+it.id}) { track ->
                TrackRow(track,track.id==state.currentId,state.playing,{actions.play(track.id)},
                    true,{actions.toggleFavorite(track.id)},onAddToPlaylist={addTrack=track})
            }
        }
    } else if(librarySection == "playlists") {
        LibraryCollectionIndex("Playlists",state.playlists.size,modifier,::backFromCollection) {
            items(state.playlists,key={"collection-playlist:"+it.id}) { playlist ->
                LibraryPlaylistRow(playlist) { selectedPlaylistId=playlist.id; librarySection="home" }
            }
        }
    } else if(selectedPlaylist!=null) {
        Column(modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(Glyph.Previous,"Back to Library",onClick={selectedPlaylistId=null})
                Column(Modifier.weight(1f).padding(horizontal=10.dp)) {
                    Label(selectedPlaylist.name,21,bold=true)
                    Label("${selectedPlaylist.entries.size} songs",12,p.secondary)
                }
                IconButton(Glyph.Edit,"Rename playlist",onClick={renamePlaylistId=selectedPlaylist.id})
                IconButton(Glyph.Close,"Delete ${selectedPlaylist.name}",tint=Accent,onClick={
                    actions.deletePlaylist(selectedPlaylist.id);selectedPlaylistId=null
                })
            }
            if(selectedPlaylist.entries.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
                RynxGlyph(Glyph.Queue,p.secondary,Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Label("This playlist is empty",21,bold=true)
                Spacer(Modifier.height(8.dp))
                Label("Add songs from your library to start listening.",14,p.secondary)
                Spacer(Modifier.height(18.dp))
                Action("Back to Library",{selectedPlaylistId=null})
            } else LazyColumn(Modifier.weight(1f).padding(horizontal=20.dp),contentPadding=PaddingValues(vertical=8.dp)) {
                items(selectedPlaylist.entries,key={"playlist:${selectedPlaylist.id}:${it.id}"}) { entry ->
                    val track=TrackItem(entry.mediaId,entry.title,entry.artist,entry.artworkUrl)
                    TrackRow(track,track.id==state.currentId,state.playing,{actions.play(track.id)},track.id in state.favoriteIds,{actions.toggleFavorite(track.id)},
                        onRemoveFromPlaylist={actions.removePlaylistEntry(selectedPlaylist.id,entry.id)},
                        onMoveUp=if(entry.position>0)({actions.movePlaylistEntry(selectedPlaylist.id,entry.id,(entry.position-1).toInt())}) else null,
                        onMoveDown=if(entry.position<selectedPlaylist.entries.lastIndex)({actions.movePlaylistEntry(selectedPlaylist.id,entry.id,(entry.position+1).toInt())}) else null)
                }
            }
        }
    } else if(libraryTracks.isEmpty() && state.playlists.isEmpty()) Column(modifier.fillMaxSize().padding(32.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        RynxBrandMark(Modifier.size(54.dp))
        Spacer(Modifier.height(18.dp))
        Label("Your music, right here",24,bold=true)
        Spacer(Modifier.height(10.dp))
        Label("Add audio files or create a playlist to get started.",14,p.secondary)
        Spacer(Modifier.height(24.dp))
        Action("Add music",actions::importFiles)
        Spacer(Modifier.height(10.dp))
        Action("Create playlist",{createPlaylistOpen=true})
        if(state.playlistBackupAvailable) {
            Spacer(Modifier.height(10.dp))
            Action("Import playlist backup",actions::importPlaylistBackup)
        }
    } else LazyColumn(modifier.padding(horizontal=20.dp),contentPadding=PaddingValues(vertical=8.dp)) {
        if(pinnedTracks.isNotEmpty()) {
            item {
                LibraryArtworkGrid(pinnedTracks) { actions.play(it.id) }
            }
        }
        item {
            LibraryCategoryGrid(
                artistCount=artists.size,
                albumCount=albums.size,
                playlistCount=state.playlists.size,
                songCount=state.tracks.size,
                favoriteCount=favorites.size,
                onNavigate={librarySection=it},
            )
        }
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label("Playlists",20,bold=true)
                    Label("${state.playlists.size} playlists",12,p.secondary)
                }
                IconButton(Glyph.Add,"Create playlist",onClick={createPlaylistOpen=true})
            }
        }
        if(artists.isNotEmpty()) {
            item {
                LibraryCollectionHeading("Artists",artists.size) { librarySection="artists" }
            }
            items(artists.take(3),key={"library-artist:"+it.name}) { artist ->
                LibraryArtistRow(artist) { selectedArtistName=artist.name }
            }
        }
        if(albums.isNotEmpty()) {
            item {
                LibraryCollectionHeading("Albums",albums.size) { librarySection="albums" }
            }
            items(albums.take(3),key={"library-album:"+it.artist+":"+it.title}) { album ->
                LibraryAlbumRow(album) { openAlbum(album) }
            }
        }
        if(state.playlistBackupAvailable) item {
            Row(Modifier.fillMaxWidth().padding(top=10.dp,bottom=6.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                RynxButton("Import backup",actions::importPlaylistBackup,Modifier.weight(1f))
                RynxButton("Export backup",actions::exportPlaylistBackup,Modifier.weight(1f),enabled=state.playlists.isNotEmpty())
            }
        }
        items(state.playlists,key={"user-playlist:${it.id}"}) { playlist ->
            LibraryPlaylistRow(playlist) { selectedPlaylistId=playlist.id }
        }
        if(favorites.isNotEmpty()) {
            item { SectionHeading("Favorites", "${favorites.size} songs") }
            items(favorites,key={"favorite:${it.id}"}) { track ->
                TrackRow(track,track.id==state.currentId,state.playing,{actions.play(track.id)},true,{actions.toggleFavorite(track.id)},
                    onAddToPlaylist={addTrack=track})
            }
        }
        item { SectionHeading("Songs", "${state.tracks.size} songs") }
        items(state.tracks,key={"library:${it.id}"}) { track ->
            TrackRow(track,track.id==state.currentId,state.playing,{actions.play(track.id)},track.id in state.favoriteIds,{actions.toggleFavorite(track.id)},
                onAddToPlaylist={addTrack=track})
        }
    }
    if(createPlaylistOpen) PlaylistNameDialog(
        title="New Playlist",initialName="",confirmLabel="Create",
        onDismiss={createPlaylistOpen=false},
        onCreate={name->actions.createPlaylist(name);createPlaylistOpen=false},
    )
    renameTarget?.let { playlist -> PlaylistNameDialog(
        title="Rename Playlist",initialName=playlist.name,confirmLabel="Save",
        onDismiss={renamePlaylistId=null},
        onCreate={name->actions.renamePlaylist(playlist.id,name);renamePlaylistId=null},
    ) }
    addTrack?.let { track ->
        AddToPlaylistDialog(
            track=track,
            playlists=state.playlists,
            onDismiss={addTrack=null},
            onSelect={playlistId->actions.addTrackToPlaylist(playlistId,track);addTrack=null},
            onCreatePlaylist={createPlaylistOpen=true},
        )
    }
}

/** Top artwork grid mirrors the reference composition using only actual owned tracks. */
@Composable private fun LibraryArtworkGrid(tracks:List<TrackItem>,onPlay:(TrackItem)->Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom=20.dp)) {
        val tile=(maxWidth-20.dp)/3
        Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
            tracks.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    row.forEach {track ->
                        Column(Modifier.width(tile).clickable(role=Role.Button,onClick={onPlay(track)})
                            .semantics {contentDescription="Play pinned "+track.title}) {
                            Artwork(tile,artworkUrl=track.artworkUrl)
                            Spacer(Modifier.height(5.dp))
                            Label(track.title,12,bold=true)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun LibraryCategoryGrid(
    artistCount:Int,
    albumCount:Int,
    playlistCount:Int,
    songCount:Int,
    favoriteCount:Int,
    onNavigate:(String)->Unit,
) {
    // Simple inset grouped rows keep artwork the focus, as in the iOS references.
    Column(Modifier.fillMaxWidth().padding(bottom=18.dp)) {
        LibraryCategoryShortcut("Playlists",Glyph.Playlists,playlistCount) {onNavigate("playlists")}
        LibraryCategoryShortcut("Artists",Glyph.Artists,artistCount) {onNavigate("artists")}
        LibraryCategoryShortcut("Albums",Glyph.Albums,albumCount) {onNavigate("albums")}
        LibraryCategoryShortcut("Songs",Glyph.Note,songCount) {onNavigate("songs")}
        LibraryCategoryShortcut("Favorites",Glyph.HeartFill,favoriteCount) {onNavigate("favorites")}
    }
}

@Composable private fun LibraryCategoryShortcut(
    title:String,
    glyph:Glyph,
    count:Int,
    onClick:()->Unit,
) {
    val p=LocalPalette.current
    val interaction=remember {MutableInteractionSource()}
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min=58.dp)
            .rynxPressFeedback(interaction)
            .clickable(role=Role.Button,interactionSource=interaction,indication=null,onClick=onClick)
            .semantics {contentDescription="Browse "+title}
            .padding(horizontal=3.dp),verticalAlignment=Alignment.CenterVertically) {
            RynxGlyph(glyph,Accent,Modifier.size(24.dp))
            Label(title,16,modifier=Modifier.weight(1f).padding(start=15.dp))
            Label(count.toString(),13,p.secondary,modifier=Modifier.padding(end=8.dp))
            RynxGlyph(Glyph.ChevronRight,p.secondary,Modifier.size(15.dp))
        }
        Spacer(Modifier.fillMaxWidth().padding(start=42.dp).height(.6.dp).background(p.line))
    }
}

@Composable private fun LibraryPlaylistRow(
    playlist:dev.bxane.rynx.library.Playlist,
    onOpen:()->Unit,
) {
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical=3.dp)
        .rynxMaterial(12.dp,amount=.25f)
        .clickable(role=Role.Button,onClick=onOpen)
        .semantics { contentDescription="Open playlist "+playlist.name }
        .padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Artwork(48.dp,artworkUrl=playlist.entries.firstOrNull()?.artworkUrl)
        Column(Modifier.weight(1f).padding(horizontal=14.dp)) {
            Label(playlist.name,bold=true)
            Spacer(Modifier.height(3.dp))
            Label("Playlist · "+playlist.entries.size+" songs",12,p.secondary)
        }
        RynxGlyph(Glyph.ChevronRight,p.secondary)
    }
}

@Composable private fun LibraryCollectionHeading(title:String,count:Int,onSeeAll:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(top=14.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically) {
        Label(title,21,bold=true,modifier=Modifier.weight(1f))
        RynxButton("See all "+title.lowercase(),onSeeAll)
    }
}

@Composable private fun LibraryArtistRow(artist:LibraryArtist,onOpen:()->Unit) {
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp).rynxMaterial(14.dp,amount=.28f)
        .clickable(role=Role.Button,onClick=onOpen)
        .semantics { contentDescription="Open artist "+artist.name }
        .padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Artwork(58.dp,artworkUrl=artist.artworkUrl)
        Column(Modifier.padding(horizontal=13.dp).weight(1f)) {
            Label(artist.name,17,bold=true)
            Label(artist.tracks.size.toString()+" songs",13,p.secondary)
        }
        RynxGlyph(Glyph.ChevronRight,p.secondary)
    }
}

@Composable private fun LibraryAlbumRow(album:LibraryAlbum,onOpen:()->Unit) {
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp).rynxMaterial(14.dp,amount=.28f)
        .clickable(role=Role.Button,onClick=onOpen)
        .semantics { contentDescription="Open album "+album.title+" by "+album.artist }
        .padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Artwork(58.dp,artworkUrl=album.artworkUrl)
        Column(Modifier.padding(horizontal=13.dp).weight(1f)) {
            Label(album.title,17,bold=true)
            Label(album.artist+" · "+album.tracks.size+" songs",13,p.secondary)
        }
        RynxGlyph(Glyph.ChevronRight,p.secondary)
    }
}

@Composable private fun LibraryCollectionIndex(
    title:String,
    count:Int,
    modifier:Modifier,
    onBack:()->Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.()->Unit,
) {
    Column(modifier.fillMaxSize()) {
        LibraryDetailHeader(title,count.toString()+" collections",onBack)
        LazyColumn(Modifier.weight(1f).padding(horizontal=20.dp),
            contentPadding=PaddingValues(vertical=10.dp),
            content=content)
    }
}

@Composable private fun LibraryDetailHeader(title:String,subtitle:String,onBack:()->Unit) {
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
        IconButton(Glyph.Previous,"Back to Library",onClick=onBack)
        Column(Modifier.weight(1f).padding(start=12.dp)) {
            Label(title,23,bold=true)
            Label(subtitle,13,p.secondary)
        }
    }
}

@Composable private fun LibraryArtistScreen(
    artist:LibraryArtist,
    state:PlayerUiState,
    actions:PlayerActions,
    modifier:Modifier,
    onBack:()->Unit,
    onAlbum:(LibraryAlbum)->Unit,
) {
    val p=LocalPalette.current
    Column(modifier.fillMaxSize()) {
        LibraryDetailHeader(artist.name,artist.tracks.size.toString()+" songs",onBack)
        LazyColumn(Modifier.weight(1f).padding(horizontal=20.dp),
            contentPadding=PaddingValues(bottom=20.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical=10.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Artwork(116.dp,artworkUrl=artist.artworkUrl)
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Label(artist.name,23,bold=true)
                        Action("Play artist", { actions.playCollection(artist.tracks) })
                    }
                }
            }
            if(artist.albums.isNotEmpty()) {
                item { SectionHeading("Albums",artist.albums.size.toString()) }
                items(artist.albums,key={"artist-album:"+it.title}) { album ->
                    LibraryAlbumRow(album) { onAlbum(album) }
                }
            }
            item { SectionHeading("Songs",artist.tracks.size.toString()) }
            items(artist.tracks,key={"artist-song:"+it.id}) { track ->
                TrackRow(track,track.id==state.currentId,state.playing,
                    {actions.play(track.id)},track.id in state.favoriteIds,
                    {actions.toggleFavorite(track.id)})
            }
        }
    }
}

@Composable private fun LibraryAlbumScreen(
    album:LibraryAlbum,
    state:PlayerUiState,
    actions:PlayerActions,
    modifier:Modifier,
    onBack:()->Unit,
) {
    val p=LocalPalette.current
    Column(modifier.fillMaxSize()) {
        LibraryDetailHeader(album.title,album.artist,onBack)
        LazyColumn(Modifier.weight(1f).padding(horizontal=20.dp),
            contentPadding=PaddingValues(bottom=20.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical=16.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Artwork(190.dp,artworkUrl=album.artworkUrl)
                    Label(album.title,24,bold=true)
                    Label(album.artist,15,p.secondary)
                    Label(album.tracks.size.toString()+" songs",13,p.secondary)
                    Action("Play album", { actions.playCollection(album.tracks) })
                }
            }
            item { SectionHeading("Songs",album.tracks.size.toString()) }
            items(album.tracks,key={"album-song:"+it.id}) { track ->
                TrackRow(track,track.id==state.currentId,state.playing,
                    {actions.play(track.id)},track.id in state.favoriteIds,
                    {actions.toggleFavorite(track.id)})
            }
        }
    }
}

@Composable private fun PlaylistNameDialog(title:String,initialName:String,confirmLabel:String,onDismiss:()->Unit,onCreate:(String)->Unit) {
    val p=LocalPalette.current
    var name by remember(initialName) { mutableStateOf(initialName) }
    Dialog(onDismissRequest=onDismiss) {
        Column(Modifier.fillMaxWidth().rynxMaterial(28.dp,amount=.9f).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Label(title,23,bold=true)
            Label(if(initialName.isBlank()) "Give your collection a name." else "Choose a new name for this playlist.",14,p.secondary)
            BasicTextField(name,{name=it},singleLine=true,textStyle=TextStyle(color=p.text,fontSize=16.sp),modifier=Modifier.fillMaxWidth()
                .rynxMaterial(15.dp,amount=.5f).semantics{contentDescription="Playlist name"}.padding(horizontal=14.dp,vertical=15.dp),decorationBox={inner->
                    Box { if(name.isEmpty()) BasicText("Playlist name",style=TextStyle(color=p.secondary,fontSize=16.sp));inner() }
                })
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Action("Cancel",onDismiss)
                Action(confirmLabel,{onCreate(name)},enabled=name.isNotBlank())
            }
        }
    }
}

@Composable private fun AddToPlaylistDialog(
    track:TrackItem,
    playlists:List<dev.bxane.rynx.library.Playlist>,
    onDismiss:()->Unit,
    onSelect:(Long)->Unit,
    onCreatePlaylist:()->Unit,
) {
    val p=LocalPalette.current
    Dialog(onDismissRequest=onDismiss) {
        Column(Modifier.fillMaxWidth().rynxMaterial(28.dp,amount=.9f).padding(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Label("Add to Playlist",22,bold=true)
            Label(track.title,14,p.secondary)
            if(playlists.isEmpty()) Label("Create a playlist before adding songs.",14,p.secondary)
            else playlists.forEach { playlist ->
                Row(Modifier.fillMaxWidth().rynxMaterial(14.dp,amount=.45f).clickable(role=Role.Button){onSelect(playlist.id)}
                    .semantics{contentDescription="Add to ${playlist.name}"}.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Artwork(38.dp,artworkUrl=playlist.entries.firstOrNull()?.artworkUrl)
                    Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                        Label(playlist.name,bold=true)
                        Label("${playlist.entries.size} songs",12,p.secondary)
                    }
                    RynxGlyph(Glyph.Add,p.secondary)
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Action("New Playlist",onCreatePlaylist)
                Action("Done",onDismiss)
            }
        }
    }
}

@Composable private fun SearchContent(state:PlayerUiState,actions:PlayerActions,query:String,modifier:Modifier) {
    val p=LocalPalette.current
    val visible=state.tracks.filter { it.title.contains(query,ignoreCase=true) }
    val catalog=state.catalogSearch.takeIf { it.query==query.trim() } ?: CatalogSearchState()
    LazyColumn(modifier.padding(horizontal=20.dp),contentPadding=PaddingValues(top=8.dp,bottom=20.dp)) {
        if(visible.isNotEmpty()) {
            item { SectionHeading("Your Library", "${visible.size} songs") }
            items(visible,key={"search-local:${it.id}"}) { track ->
                TrackRow(track,track.id==state.currentId,state.playing,{actions.play(track.id)},track.id in state.favoriteIds,{actions.toggleFavorite(track.id)})
            }
        } else if(query.isNotBlank() && state.tracks.isNotEmpty()) {
            item { Label("No matching songs in your library",14,p.secondary,modifier=Modifier.padding(vertical=12.dp)) }
        }
        if(catalog.metadataItems.isNotEmpty()) {
            item {SectionHeading("YouTube Music", null)}
            items(catalog.metadataItems,key={"ytm-search:${it.id}"}) {track->DiscoveryTrackRow(track,actions)}
        }
        if(state.catalogSearchEnabled && catalog.metadataItems.isEmpty()) {
            item { SectionHeading("YouTube Music", null) }
            if(catalog.error!=null) item { Label(catalog.error,14,Accent,modifier=Modifier.padding(vertical=12.dp)) }
            if(catalog.loading&&catalog.items.isEmpty()) item { Label("Searching…",14,p.secondary,modifier=Modifier.padding(vertical=12.dp)) }
            items(catalog.items,key={"${it.provider}:${it.mediaId}"}) { item ->
                CatalogRow(item,state.catalogPlaybackEnabled){actions.playCatalog(item)}
            }
            if(!catalog.loading&&catalog.query.isNotBlank()&&catalog.items.isEmpty()&&catalog.error==null) item {
                Label("No YouTube Music results",14,p.secondary,modifier=Modifier.padding(vertical=12.dp))
            }
            if(catalog.continuation!=null) item {
                Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.Center) {
                    Action(if(catalog.loadingMore) "Loading…" else "Load more results",{actions.loadMoreYouTubeMusic()},enabled=!catalog.loadingMore)
                }
            }
        }
    }
}

@Composable private fun SectionHeading(title:String,detail:String?) {
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top=10.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically) {
        Label(title,20,bold=true,modifier=Modifier.weight(1f))
        detail?.let { Label(it,12,p.secondary) }
    }
}

@Composable private fun BrowseShortcut(title:String,glyph:Glyph,onClick:()->Unit) {
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp).rynxMaterial(12.dp,amount=.6f)
        .clickable(role=Role.Button,onClick=onClick).semantics{contentDescription=title;role=Role.Button}
        .padding(horizontal=14.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically) {
        RynxGlyph(glyph,Accent)
        Label(title,16,bold=true,modifier=Modifier.padding(start=12.dp).weight(1f))
        RynxGlyph(Glyph.Next,p.secondary)
    }
}

@Composable private fun CatalogRow(item: dev.bxane.rynx.playback.catalog.CatalogItem, playbackEnabled:Boolean,onPlay:()->Unit) {
    val p = LocalPalette.current
    val canPlay=playbackEnabled&&item.type==dev.bxane.rynx.playback.catalog.CatalogItemType.Track
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp).rynxMaterial(12.dp,amount=.5f)
        .clickable(enabled=canPlay,role=Role.Button,onClick=onPlay)
        .semantics { contentDescription="${item.title}, ${item.artist.orEmpty()}, ${if(canPlay) "play" else item.type.name.lowercase()}"; if(!canPlay) disabled() }
        .padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Artwork(48.dp,artworkUrl=item.artworkUrl)
        Column(Modifier.padding(start=14.dp).weight(1f)) {
            Label(item.title,bold=true)
            val detail = listOfNotNull(item.artist, item.type.name).joinToString(" · ")
            if(detail.isNotEmpty()) Label(detail,12,p.secondary)
        }
        item.durationSeconds?.let { Label(playerTime(it*1000),12,p.secondary) }
        if(canPlay) RynxGlyph(Glyph.Play,Accent,Modifier.padding(start=12.dp))
    }
}

@Composable internal fun BottomTabItem(
    tab:AppTab,selected:Boolean,labelAlpha:Float,morphProgress:Float,
    modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit,
) {
    val p=LocalPalette.current
    val reduced=LocalRynxReducedMotion.current
    val tint by animateColorAsState(if(selected) Accent else p.secondary,animationSpec=if(reduced)snap() else spring(stiffness=RynxMotionTokens.SelectionStiffness,dampingRatio=RynxMotionTokens.SelectionDamping),label="tab tint")
    val scale by animateFloatAsState(if(selected) 1f else .92f,animationSpec=if(reduced)snap() else spring(stiffness=RynxMotionTokens.SelectionStiffness,dampingRatio=RynxMotionTokens.SelectionDamping),label="tab scale")
    val interaction=remember {MutableInteractionSource()}
    Column(modifier.rynxPressFeedback(interaction,enabled).clip(RoundedCornerShape(12.dp))
        .clickable(enabled=enabled,role=Role.Tab,interactionSource=interaction,indication=null,onClick=onClick)
        .semantics{contentDescription=tab.title;this.selected=selected;role=Role.Tab}
        .graphicsLayer{scaleX=scale;scaleY=scale}.padding(vertical=4.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
        AnimatedNavigationIcon(tab.glyph,selected,Accent,p.secondary,reduced,morphProgress,Modifier.size(22.dp),interaction)
        Label(tab.title,10,tint,bold=selected,modifier=Modifier.graphicsLayer{alpha=labelAlpha})
    }
}

@Composable private fun NavItem(text:String,glyph:Glyph,selected:Boolean,onClick:()->Unit) {
    val p=LocalPalette.current
    Row(Modifier.clip(RoundedCornerShape(10.dp)).background(if(selected) Accent.copy(alpha=.1f) else Color.Transparent).clickable(role=Role.Tab,onClick=onClick).semantics{contentDescription=text;this.selected=selected}.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        RynxGlyph(glyph,if(selected) Accent else p.secondary);Label(text,color=if(selected) Accent else p.text,bold=selected)
    }
}

@Composable private fun TrackRow(track:TrackItem,selected:Boolean,playing:Boolean,onClick:()->Unit,favorite:Boolean=false,onToggleFavorite:(()->Unit)?=null,onAddToPlaylist:(()->Unit)?=null,onRemoveFromPlaylist:(()->Unit)?=null,onMoveUp:(()->Unit)?=null,onMoveDown:(()->Unit)?=null) {
    val p=LocalPalette.current
    Row(Modifier.fillMaxWidth().rynxMaterial(12.dp,selected=selected,amount=.3f).clickable(role=Role.Button,onClick=onClick).padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Artwork(44.dp,artworkUrl=track.artworkUrl)
        Column(Modifier.padding(start=14.dp).weight(1f)) { Label(track.title,bold=selected,color=if(selected) Accent else p.text);Spacer(Modifier.height(4.dp));Label(track.subtitle,12,p.secondary) }
        if(selected) RynxGlyph(if(playing) Glyph.Pause else Glyph.Play,Accent)
        onToggleFavorite?.let { toggle -> IconButton(if(favorite) Glyph.HeartFill else Glyph.Heart,if(favorite) "Remove ${track.title} from Favorites" else "Add ${track.title} to Favorites",tint=if(favorite) Accent else p.secondary,onClick=toggle) }
        onAddToPlaylist?.let { add -> IconButton(Glyph.Add,"Add ${track.title} to a playlist",tint=p.secondary,onClick=add) }
        onRemoveFromPlaylist?.let { remove -> IconButton(Glyph.Close,"Remove ${track.title} from playlist",tint=p.secondary,onClick=remove) }
        if(onMoveUp!=null || onMoveDown!=null) Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
            onMoveUp?.let { move -> SmallIconButton(Glyph.MoveUp,"Move ${track.title} earlier",onClick=move) }
            onMoveDown?.let { move -> SmallIconButton(Glyph.MoveDown,"Move ${track.title} later",onClick=move) }
        }
    }
}

@Composable private fun SmallIconButton(glyph:Glyph,description:String,enabled:Boolean=true,onClick:()->Unit) {
    val p=LocalPalette.current
    val interaction=remember {MutableInteractionSource()}
    Box(Modifier.size(32.dp).rynxPressFeedback(interaction,enabled).rynxMaterial(16.dp,interaction=interaction,amount=.35f)
        .clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=null,onClick=onClick)
        .semantics{contentDescription=description;if(!enabled)disabled()},contentAlignment=Alignment.Center) {
        RynxGlyph(glyph,if(enabled)p.secondary else p.secondary.copy(alpha=.35f),Modifier.size(18.dp))
    }
}
