package dev.bxane.rynx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import dev.bxane.rynx.ui.material.*
import dev.bxane.rynx.design.RynxMotionTokens
import dev.bxane.rynx.playback.normalizedPlaybackOrder

@Composable internal fun PlayerQueue(state:PlayerUiState,actions:PlayerActions,modifier:Modifier=Modifier,interactive:Boolean=true,headerHeight:androidx.compose.ui.unit.Dp=64.dp) {
    val order=normalizedPlaybackOrder(state.queue.size,state.queueOrder)
    val currentIndex=state.queue.indexOfFirst { it.id==state.currentId }
    val currentOrderIndex=order.indexOf(currentIndex)
    val upcoming=order.drop(currentOrderIndex+1).map { IndexedValue(it,state.queue[it]) }
    Column(modifier) {
        Spacer(Modifier.height(headerHeight))
        Row(Modifier.fillMaxWidth().padding(top=14.dp,bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            QueueMode(Glyph.Shuffle,"Shuffle",state.shuffleEnabled,interactive && state.shuffleAvailable,Modifier.weight(1f),actions::toggleShuffle)
            QueueMode(Glyph.Repeat,"Change repeat mode, currently ${state.repeatLabel}",state.repeatLabel!="Off",interactive,Modifier.weight(1f),actions::cycleRepeat)
            QueueMode(Glyph.Infinity,"Autoplay unavailable",false,false,Modifier.weight(1f)) {}
            QueueMode(Glyph.AutoMix,"Automix unavailable",false,false,Modifier.weight(1f)) {}
        }
        Row(Modifier.fillMaxWidth().padding(bottom=12.dp),verticalAlignment=Alignment.CenterVertically) {
            PlayerText("Queue",17,bold=true,modifier=Modifier.weight(1f))
            Box(Modifier.heightIn(min=44.dp).clickable(enabled=interactive && state.queueEditingAvailable && upcoming.isNotEmpty(),role=Role.Button,onClick=actions::clearUpcomingQueue)
                .semantics { contentDescription="Clear upcoming queue";if(!interactive || !state.queueEditingAvailable || upcoming.isEmpty()) disabled() }
                .padding(start=12.dp),contentAlignment=Alignment.Center) {
                PlayerText("Clear",16,if(upcoming.isNotEmpty()).6f else .3f)
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp),contentPadding=PaddingValues(bottom=20.dp),userScrollEnabled=interactive) {
            if(upcoming.isEmpty()) item { PlayerText("Nothing queued",16,.6f,modifier=Modifier.padding(vertical=18.dp)) }
            items(upcoming,key={it.index}) { indexed ->
                val orderIndex=order.indexOf(indexed.index)
                PlayerQueueRow(indexed.value,orderIndex,currentOrderIndex+1,order.lastIndex,actions,
                    interactive && state.queueEditingAvailable,state.reduceMotion,interactive,onMove={ destination ->
                        actions.moveInQueue(indexed.value.id,order[destination])
                    })
            }
        }
    }
}

@Composable private fun QueueMode(glyph:Glyph,label:String,selected:Boolean,enabled:Boolean,modifier:Modifier,onClick:()->Unit) {
    Box(modifier.height(38.dp).rynxMaterial(22.dp,selected=selected)
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick).semantics {
            contentDescription=label;this.selected=selected;if(!enabled) disabled()
        },contentAlignment=Alignment.Center) {
        RynxGlyph(glyph,PlayerWhite.copy(alpha=if(enabled)1f else .28f),Modifier.size(23.dp))
    }
}

@Composable private fun PlayerQueueRow(track:TrackItem,index:Int,minimumIndex:Int,lastIndex:Int,actions:PlayerActions,editable:Boolean,reduceMotion:Boolean,interactive:Boolean,onMove:(Int)->Unit) {
    val density=LocalDensity.current
    val haptics=LocalHapticFeedback.current
    val revealWidth=with(density) { 76.dp.toPx() }
    var swipe by remember(track.id) { mutableFloatStateOf(0f) }
    var swiping by remember(track.id) { mutableStateOf(false) }
    var reorderY by remember(track.id) { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(with(density) { 62.dp.toPx() }) }
    val animatedSwipe by animateFloatAsState(swipe,if(reduceMotion)snap() else spring(stiffness=RynxMotionTokens.QueueStiffness,dampingRatio=RynxMotionTokens.QueueDamping))
    val actualSwipe=if(swiping || reduceMotion)swipe else animatedSwipe
    Box(Modifier.fillMaxWidth().height(62.dp).onSizeChanged { rowHeight=it.height.toFloat() }
        .zIndex(if(reorderY!=0f)1f else 0f).graphicsLayer { translationY=reorderY }.clip(RoundedCornerShape(8.dp))) {
        if(swipe<0f) Box(Modifier.align(Alignment.CenterEnd).width(76.dp).fillMaxHeight().background(Color(0xFFE84E5B))
            .clickable(enabled=editable,role=Role.Button) { actions.removeFromQueue(track.id) }
            .semantics { contentDescription="Remove ${track.title} from queue" },contentAlignment=Alignment.Center) {
            RynxGlyph(Glyph.Close,PlayerWhite,Modifier.size(22.dp))
        }
        Row(Modifier.fillMaxSize().offset { IntOffset(actualSwipe.roundToInt(),0) }
            .rynxMaterial(8.dp,amount=.35f)
            .pointerInput(track.id,editable) { if(editable) detectHorizontalDragGestures(
                onDragStart={swiping=true},onDragEnd={swiping=false;swipe=if(swipe < -revealWidth*.4f)-revealWidth else 0f},
                onDragCancel={swiping=false;swipe=0f},
            ) { change,delta -> change.consume();swipe=(swipe+delta).coerceIn(-revealWidth,0f) } },
            verticalAlignment=Alignment.CenterVertically) {
            Row(Modifier.weight(1f).fillMaxHeight().clickable(enabled=interactive,role=Role.Button) { actions.play(track.id) }
                .semantics {
                    contentDescription="Play ${track.title} from queue"
                    if(editable) customActions=listOf(CustomAccessibilityAction("Remove from queue") { actions.removeFromQueue(track.id);true })
                },verticalAlignment=Alignment.CenterVertically) {
                Artwork(46.dp,artworkUrl=track.artworkUrl)
                Column(Modifier.padding(start=12.dp).weight(1f)) { PlayerText(track.title,17);PlayerText(track.subtitle,13,.55f) }
            }
            Box(Modifier.size(44.dp).semantics {
                contentDescription="Reorder ${track.title}"
                if(editable) customActions=buildList {
                    if(index>minimumIndex) add(CustomAccessibilityAction("Move up") { onMove(index-1);true })
                    if(index<lastIndex) add(CustomAccessibilityAction("Move down") { onMove(index+1);true })
                } else disabled()
            }.pointerInput(track.id,index,editable) { if(editable) detectDragGesturesAfterLongPress(
                onDragStart={haptics.performHapticFeedback(HapticFeedbackType.LongPress)},
                onDragCancel={reorderY=0f},
                onDragEnd={
                    val destination=(index+(reorderY/rowHeight).roundToInt()).coerceIn(minimumIndex,lastIndex)
                    reorderY=0f
                    if(destination!=index) onMove(destination)
                },
            ) { change,amount -> change.consume();reorderY+=amount.y } },contentAlignment=Alignment.Center) {
                RynxGlyph(Glyph.Reorder,PlayerWhite.copy(alpha=.35f),Modifier.size(22.dp))
            }
        }
    }
}
