package dev.bxane.rynx.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.bxane.rynx.design.*
import dev.bxane.rynx.ui.material.*

internal enum class PlayerPane { Artwork, Queue, Lyrics }

@Stable internal class PlayerPresentation(private val scope:CoroutineScope,initialProgress:Float=0f,initialPane:PlayerPane=PlayerPane.Artwork,initialExpanded:Boolean=false) {
    var progress by mutableFloatStateOf(initialProgress);private set
    var pane by mutableStateOf(initialPane)
    internal var targetExpanded by mutableStateOf(initialExpanded);private set
    private var lastVelocity=0f
    var motionIntensity=1f
    private var animation:Job?=null
    val visible:Boolean get()=targetExpanded || progress>0f
    fun open(reduceMotion:Boolean,pane:PlayerPane=PlayerPane.Artwork) { this.pane=pane;animateTo(1f,lastVelocity,reduceMotion) }
    fun close(reduceMotion:Boolean) { animateTo(0f,lastVelocity,reduceMotion) }
    fun resumeAfterRestore(reduced:Boolean) {
        if(animation==null && ((targetExpanded && progress<1f) || (!targetExpanded && progress>0f)))
            animateTo(if(targetExpanded)1f else 0f,0f,reduced)
    }
    fun applyReducedMotion(reduced:Boolean) {if(reduced && animation?.isActive==true)animateTo(if(targetExpanded)1f else 0f,0f,true)}
    fun beginDrag() { animation?.cancel();lastVelocity=0f }
    fun dragBy(delta:Float,height:Float) { progress=(progress-delta/height.coerceAtLeast(1f)).coerceIn(0f,1f) }
    fun finishDrag(velocity:Float,height:Float,reduceMotion:Boolean) {
        val fractionalVelocity=velocity/height.coerceAtLeast(1f)
        val target=when {fractionalVelocity>1.1f -> 0f;fractionalVelocity < -1.1f -> 1f;progress<.5f -> 0f;else -> 1f}
        animateTo(target,-fractionalVelocity,reduceMotion)
    }
    private fun animateTo(target:Float,velocity:Float,reduceMotion:Boolean) {
        animation?.cancel();targetExpanded=target==1f
        if(reduceMotion) { progress=target;lastVelocity=0f;if(target==0f)pane=PlayerPane.Artwork;return }
        animation=scope.launch {
            val intensity=motionIntensity.coerceIn(.2f,1f)
            Animatable(progress).animateTo(target,animationSpec=spring(
                stiffness=RynxMotionTokens.SheetStiffness/(intensity*intensity),
                dampingRatio=RynxMotionTokens.SheetDamping+(1f-intensity)*(1f-RynxMotionTokens.SheetDamping),
            ),initialVelocity=velocity) {
                progress=value.coerceIn(0f,1f);lastVelocity=this.velocity
            }
            progress=target;lastVelocity=0f;if(target==0f)pane=PlayerPane.Artwork
        }
    }
}

@Composable internal fun rememberPlayerPresentation():PlayerPresentation {
    val scope=rememberCoroutineScope()
    return rememberSaveable(saver=listSaver(
        save={listOf(it.progress,it.pane.name,it.targetExpanded)},
        restore={PlayerPresentation(scope,it[0] as Float,PlayerPane.valueOf(it[1] as String),it[2] as Boolean)},
    )) { PlayerPresentation(scope) }
}

/** One surface, one artwork node and one transport set across every expansion fraction. */
@Composable internal fun RynxFluidPlayer(state:PlayerUiState,actions:PlayerActions,presentation:PlayerPresentation) {
    val track=state.current ?: return
    SideEffect {presentation.motionIntensity=state.appearance.motionIntensity}
    LaunchedEffect(presentation) {presentation.resumeAfterRestore(state.reduceMotion)}
    LaunchedEffect(state.reduceMotion) {presentation.applyReducedMotion(state.reduceMotion)}
    val p=presentation.progress
    val interactive=p>=.96f
    val secondary=progressBetween(p,.78f,.98f)
    val keyboard=LocalSoftwareKeyboardController.current
    val focus=LocalFocusManager.current
    if(presentation.visible) PlayerBackHandler(true) {
        if(presentation.pane!=PlayerPane.Artwork)presentation.pane=PlayerPane.Artwork else presentation.close(state.reduceMotion)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val geometry=playerGeometry(maxWidth.value,maxHeight.value,p)
        val width=geometry.width.dp;val height=geometry.height.dp
        val density=LocalDensity.current
        val expandedWidth=maxWidth.value
        val expandedHeight=maxHeight-NavigationDockHeight.dp
        val expandedCompact=expandedHeight<700.dp || density.fontScale>1.3f
        val dragHeight=with(density){expandedHeight.toPx()}
        val dragModifier=Modifier.draggable(
            state=rememberDraggableState {presentation.dragBy(it,dragHeight)},orientation=Orientation.Vertical,
            enabled=presentation.pane==PlayerPane.Artwork || p<.96f,
            onDragStarted={presentation.beginDrag()},onDragStopped={presentation.finishDrag(it,dragHeight,state.reduceMotion)},
        )
        if(p>.001f) Box(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {while(true)awaitPointerEvent().changes.forEach {it.consume()}}
        })
        val openPlayer={focus.clearFocus();keyboard?.hide();presentation.open(state.reduceMotion)}
        CompositionLocalProvider(LocalRynxMaterialLayer provides 2f,LocalRynxDarkSurface provides true) {
            Box(Modifier.offset(x=geometry.left.dp,y=geometry.top.dp).size(width,height)
                .rynxMaterial(geometry.radius.dp,amount=1f-p,shadow=p<.2f).then(dragModifier).semantics {
                    testTag="player-surface"
                    if(p<.001f) {
                        contentDescription="Open Now Playing";role=Role.Button
                        onClick("Expand Now Playing"){openPlayer();true}
                    } else paneTitle="Now Playing"
                }
                .clip(RoundedCornerShape(geometry.radius.dp))
                .then(if(p<.001f)Modifier.clickable(role=Role.Button,onClick=openPlayer) else Modifier)) {
                val landscape=geometry.landscape
                val margin=if(landscape)24.dp else (width*.081f).coerceIn(20.dp,40.dp)
                val compact=expandedCompact
                fun mix(a:Float,b:Float)=a+(b-a)*p
                val paneHeaderHeight=maxOf(64f,31f*density.fontScale*1.35f)
                val headerTitleY=50f+(paneHeaderHeight-31f*density.fontScale*1.2f)/2
                val leftRegion=if(landscape)expandedWidth*.43f else width.value
                val controlTargetLeft=if(landscape)leftRegion+24f else margin.value
                val controlTargetWidth=if(landscape)expandedWidth-controlTargetLeft-24f else width.value-margin.value*2
                val controlLeft=if(landscape)mix(margin.value,controlTargetLeft) else margin.value
                val controlWidth=if(landscape)mix(width.value-margin.value*2,controlTargetWidth) else controlTargetWidth
                val landscapeTop=((expandedHeight.value-260f)/2).coerceAtLeast(0f)
                val titleY=if(landscape)minOf(height.value*.70f,(height.value-42f*density.fontScale*1.2f-16f).coerceAtLeast(20f)).dp
                    else height*(if(compact).35f else .53f)
                val timelineY=if(landscape)mix(31f,landscapeTop+22f).dp else height*(if(compact && density.fontScale>1.8f).58f else if(compact).60f else .628f)
                val volumeY=if(landscape)mix(31f,landscapeTop+170f).dp else height*(if(compact).855f else .858f)
                val footerY=if(landscape)mix(31f,landscapeTop+235f).dp else height*(if(compact).94f else .935f)
                val overflow=if(landscape)(260f-expandedHeight.value).coerceAtLeast(0f) else 0f
                val controlsScroll=rememberScrollState(with(density){overflow.dp.toPx()}.toInt())
                val paneTarget=if(presentation.pane==PlayerPane.Artwork)0f else 1f
                val paneMorph by animateFloatAsState(paneTarget,if(state.reduceMotion)snap() else spring(
                    stiffness=RynxMotionTokens.SelectionStiffness,dampingRatio=RynxMotionTokens.SelectionDamping))
                val q=if(state.reduceMotion)paneTarget else paneMorph
                var lastPane by remember {mutableStateOf(PlayerPane.Queue)}
                SideEffect {if(presentation.pane!=PlayerPane.Artwork)lastPane=presentation.pane}
                val thumbnail=PlayerArtworkGeometry(mix(10f,margin.value),mix(11f,50f+(paneHeaderHeight-64f)/2),mix(40f,64f),mix(40f,64f))
                fun paneMix(a:Float,b:Float)=a+(b-a)*q
                val artwork=PlayerArtworkGeometry(
                    paneMix(geometry.artwork.left,thumbnail.left),paneMix(geometry.artwork.top,thumbnail.top),
                    paneMix(geometry.artwork.width,thumbnail.width),paneMix(geometry.artwork.height,thumbnail.height),
                )
                Box(Modifier.fillMaxSize().rynxBackdropSource(2f)) {
                    Box(Modifier.fillMaxSize().graphicsLayer {alpha=p}) {PlayerBackdrop(track.artworkUrl,state.appearance,state.reduceMotion,state.renderingPressure)}
                    Box(Modifier.offset(x=artwork.left.dp,y=artwork.top.dp).size(artwork.width.dp,artwork.height.dp).semantics {testTag="player-artwork"}
                        .clip(RoundedCornerShape((8f*(1f-p)).dp))
                        .graphicsLayer {compositingStrategy=CompositingStrategy.Offscreen}.drawWithCache {
                            val mask=Brush.verticalGradient(.55f to Color.White,1f to Color.White.copy(alpha=1f-p))
                            onDrawWithContent {drawContent();drawRect(mask,blendMode=BlendMode.DstIn)}
                        }) {HeroArtwork(track.artworkUrl)}
                }
                CompositionLocalProvider(LocalRynxMaterialLayer provides 3f) {
                    val metadataX=mix(61f,margin.value+76f*q)
                    val metadataY=mix(20f,paneMix(titleY.value,headerTitleY))
                    Column(Modifier.offset(x=metadataX.dp,y=metadataY.dp)
                        .width(mix((width.value-167f).coerceAtLeast(60f),(leftRegion-margin.value*2-96f-76f*q).coerceAtLeast(60f)).dp)
                        .trackSwipe(track.id,actions).semantics {contentDescription="Track information";testTag="player-metadata"}) {
                        BasicText(track.title,style=TextStyle(color=PlayerWhite,fontSize=mix(15f,paneMix(22f,17f)).sp,fontWeight=FontWeight.SemiBold),maxLines=1,overflow=TextOverflow.Ellipsis)
                        if(secondary>0f)BasicText(track.subtitle,Modifier.graphicsLayer {alpha=secondary},
                            style=TextStyle(color=PlayerWhite.copy(alpha=.63f),fontSize=mix(13f,paneMix(20f,14f)).sp),maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    if(secondary>0f)Box(Modifier.offset(x=(if(landscape)mix(width.value-margin.value-88f,leftRegion-margin.value-88f) else width.value-margin.value-88f).dp,y=paneMix(titleY.value+2f,50f+(paneHeaderHeight-44f)/2).dp)
                        .graphicsLayer {alpha=secondary}.semantics {if(!interactive)hideFromAccessibility()}) {PlayerTrackActions(state,actions,interactive)}
                    if(q>0f && secondary>0f) {
                        val paneInteractive=interactive && q>=.96f && presentation.pane!=PlayerPane.Artwork
                        val panel=Modifier.offset(x=margin,y=50.dp).width((leftRegion-margin.value*2).dp).height((if(landscape)expandedHeight-50.dp else timelineY-72.dp).coerceAtLeast(120.dp))
                            .graphicsLayer {alpha=secondary*q}.semantics {if(!paneInteractive)hideFromAccessibility()}
                        if((if(presentation.pane==PlayerPane.Artwork)lastPane else presentation.pane)==PlayerPane.Queue)PlayerQueue(state,actions,panel,paneInteractive,paneHeaderHeight.dp) else PlayerLyrics(state,actions,panel,paneInteractive,paneHeaderHeight.dp)
                    }
                    if(secondary>0f) {
                        val hidden=Modifier.graphicsLayer {alpha=secondary}.semantics {if(!interactive)hideFromAccessibility()}
                        Box(Modifier.align(Alignment.TopCenter).size(76.dp,34.dp).then(hidden)
                            .draggable(state=rememberDraggableState {presentation.dragBy(it,dragHeight)},orientation=Orientation.Vertical,
                                onDragStarted={presentation.beginDrag()},onDragStopped={presentation.finishDrag(it,dragHeight,state.reduceMotion)})
                            .clickable(enabled=interactive,role=Role.Button){presentation.close(state.reduceMotion)}
                            .semantics {contentDescription="Close Now Playing"},contentAlignment=Alignment.Center) {
                            Box(Modifier.size(59.dp,4.dp).background(PlayerWhite.copy(alpha=.33f),RoundedCornerShape(2.dp)))
                        }
                        if(state.error!=null)PlayerText(state.error,12,.85f,modifier=Modifier.padding(horizontal=margin).offset(y=36.dp).then(hidden))
                    }
                        Box(Modifier.fillMaxSize().then(if(landscape && overflow>0f)Modifier.verticalScroll(controlsScroll,enabled=interactive) else Modifier)) {
                            Box(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top,unbounded=true)
                                .height(if(landscape && overflow>0f)260.dp else height)
                                .graphicsLayer {if(landscape && overflow>0f)translationY=controlsScroll.value*(1f-p)}) {
                    if(secondary>0f) {
                        val hidden=Modifier.graphicsLayer {alpha=secondary}.semantics {if(!interactive)hideFromAccessibility()}
                        PlayerTimeline(state,actions,Modifier.offset(x=controlLeft.dp,y=timelineY-22.dp).width(controlWidth.dp).then(hidden),interactive)
                        PlayerVolume(state,actions,Modifier.offset(x=controlLeft.dp,y=volumeY-22.dp).width(controlWidth.dp).then(hidden),interactive)
                        Row(Modifier.offset(x=controlLeft.dp,y=footerY-22.dp).width(controlWidth.dp).then(hidden),horizontalArrangement=Arrangement.SpaceAround) {
                            PlayerButton(Glyph.Lyrics,if(presentation.pane==PlayerPane.Lyrics)"Hide lyrics" else "Show lyrics",{
                                presentation.pane=if(presentation.pane==PlayerPane.Lyrics)PlayerPane.Artwork else PlayerPane.Lyrics
                            },enabled=interactive && (state.lyricsImportAvailable || state.lyrics.isNotEmpty()),selected=presentation.pane==PlayerPane.Lyrics,iconSize=23.dp)
                            PlayerButton(Glyph.Output,"Audio output",actions::showAudioOutput,enabled=interactive && state.audioOutputAvailable,iconSize=25.dp)
                            PlayerButton(Glyph.Queue,if(presentation.pane==PlayerPane.Queue)"Hide queue" else "Show queue",{
                                presentation.pane=if(presentation.pane==PlayerPane.Queue)PlayerPane.Artwork else PlayerPane.Queue
                            },enabled=interactive,selected=presentation.pane==PlayerPane.Queue,iconSize=25.dp)
                        }
                    }
                    val centerY=mix(31f,if(landscape)landscapeTop+104f else expandedHeight.value*(if(compact).74f else .745f))
                    val pauseSize=mix(44f,68f)
                    val pauseX=mix(width.value-76f,if(landscape)controlTargetLeft+controlTargetWidth*.5f else width.value*.5f)
                    PlayerButton(if(state.playing)Glyph.Pause else Glyph.Play,if(state.playing)"Pause" else "Play",actions::toggle,
                        Modifier.offset(x=(pauseX-pauseSize/2).dp,y=(centerY-pauseSize/2).dp),
                        iconSize=mix(24f,48f).dp,touchSize=pauseSize.dp,reduceMotion=state.reduceMotion)
                    val nextSize=mix(44f,64f)
                    val nextX=mix(width.value-28f,if(landscape)controlTargetLeft+controlTargetWidth*.80f else width.value*.80f)
                    PlayerButton(Glyph.Next,"Next track",actions::next,
                        Modifier.offset(x=(nextX-nextSize/2).dp,y=(centerY-nextSize/2).dp),
                        iconSize=mix(24f,40f).dp,touchSize=nextSize.dp,reduceMotion=state.reduceMotion)
                    if(secondary>0f)PlayerButton(Glyph.Previous,"Previous track",actions::previous,
                        Modifier.offset(x=((if(landscape)mix(width.value*.20f,controlTargetLeft+controlTargetWidth*.20f) else width.value*.20f)-32f).dp,y=(centerY-32f).dp).graphicsLayer {alpha=secondary}
                            .semantics {if(!interactive)hideFromAccessibility()},enabled=interactive,iconSize=40.dp,touchSize=64.dp,reduceMotion=state.reduceMotion)
                            }
                        }
                }
            }
        }
    }
}

@Composable private fun PlayerBackdrop(artworkUrl:String?,appearance:RynxAppearance,reduced:Boolean,pressure:Boolean) {
    Crossfade(artworkUrl,animationSpec=tween(if(reduced)0 else RynxMotionTokens.ArtworkMillis)) { url ->
        BoxWithConstraints(Modifier.fillMaxSize().background(if(appearance.material==RynxMaterialMode.Amoled)Color.Black else Color(0xFF142430))) {
            val backgroundWidth=maxWidth;val backgroundHeight=maxHeight
            val policy=appearance.policy(pressure)
            if(policy.ambient && !url.isNullOrBlank()) {
                AsyncImage(url,null,Modifier.fillMaxSize().blur(64.dp,BlurredEdgeTreatment.Unbounded).graphicsLayer {scaleX=1.2f;scaleY=1.2f},contentScale=ContentScale.Crop)
            }
            val hasArtwork=!url.isNullOrBlank() && policy.ambient
            if(appearance.material!=RynxMaterialMode.Amoled) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Black.copy(alpha=.13f),.40f to Color.Black.copy(alpha=.10f),
                .64f to (if(hasArtwork)Color.Black.copy(alpha=.32f) else Color(0xFF152530).copy(alpha=.65f)),
                1f to (if(hasArtwork)Color.Black.copy(alpha=.66f) else Color(0xFF0E1B24).copy(alpha=.94f)),
            )))
        }
    }
}

@Composable private fun HeroArtwork(url:String?) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF272E42),Color(0xFF713246)))),contentAlignment=Alignment.Center) {
        RynxBrandMark(Modifier.size(minOf(maxWidth,maxHeight)*.52f),tint=Color(0xFFFF6A81).copy(alpha=.5f))
        if(!url.isNullOrBlank()) AsyncImage(url,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
    }
}

@Composable private fun PlayerTrackActions(state:PlayerUiState,actions:PlayerActions,enabled:Boolean=true) {
    val track=state.current
    var more by remember(track?.id) {mutableStateOf(false)}
    val favorite=track?.id in state.favoriteIds
    Row {
        PlayerButton(if(favorite)Glyph.StarFill else Glyph.Star,if(favorite)"Remove from Favorites" else "Add to Favorites",
            {track?.let {actions.toggleFavorite(it.id)}},enabled=enabled && track!=null,iconSize=24.dp)
        Box {
            PlayerButton(Glyph.More,"More track options",{more=true},enabled=enabled,iconSize=23.dp)
            if(more)Popup(alignment=Alignment.TopEnd,offset=IntOffset(0,40),onDismissRequest={more=false},properties=PopupProperties(focusable=true)) {
                RynxSurface(Modifier.width(232.dp),18.dp,shadow=true) {
                    Column(Modifier.padding(8.dp)) {
                        PlayerMenuItem(if(favorite)"Remove from Favorites" else "Add to Favorites"){track?.let {actions.toggleFavorite(it.id)};more=false}
                        if(state.shuffleAvailable)PlayerMenuItem(if(state.shuffleEnabled)"Turn shuffle off" else "Turn shuffle on"){actions.toggleShuffle();more=false}
                        PlayerMenuItem("Repeat: ${state.repeatLabel}"){actions.cycleRepeat();more=false}
                        track?.metadataSourceUrl?.let {url->PlayerMenuItem("Open in Apple Music"){actions.openMusicSource(url);more=false}}
                    }
                }
            }
        }
    }
}

@Composable private fun PlayerMenuItem(label:String,onClick:()->Unit) {
    val interaction=remember {androidx.compose.foundation.interaction.MutableInteractionSource()}
    Box(Modifier.fillMaxWidth().heightIn(min=48.dp).rynxPressFeedback(interaction)
        .rynxMaterial(12.dp,interaction=interaction,amount=.3f)
        .clickable(role=Role.Button,interactionSource=interaction,indication=null,onClick=onClick)
        .semantics { contentDescription=label }.padding(12.dp)) { PlayerText(label) }
}

@Composable private fun Modifier.trackSwipe(trackId:String?,actions:PlayerActions):Modifier {
    val threshold=with(LocalDensity.current){48.dp.toPx()}
    return pointerInput(trackId) {
        var distance=0f
        detectHorizontalDragGestures(onDragStart={distance=0f},onDragEnd={
            if(distance < -threshold)actions.next() else if(distance>threshold)actions.previous()
        }) { change,delta -> change.consume();distance+=delta }
    }
}

@Composable internal expect fun PlayerBackHandler(enabled:Boolean,onBack:()->Unit)
