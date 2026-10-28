package dev.bxane.rynx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bxane.rynx.playback.LyricLine
import dev.bxane.rynx.playback.activeLyricIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.bxane.rynx.ui.material.*
import dev.bxane.rynx.design.RynxMotionTokens

@Composable internal fun PlayerLyrics(state:PlayerUiState,actions:PlayerActions,modifier:Modifier=Modifier,interactive:Boolean=true,headerHeight:androidx.compose.ui.unit.Dp=64.dp) {
    val lines=state.lyrics.takeIf { state.lyricsTrackId==state.currentId }.orEmpty()
    val active=activeLyricIndex(lines,state.positionMillis)
    val list=rememberLazyListState()
    var follow by remember(state.currentId) { mutableStateOf(true) }
    var height by remember { mutableIntStateOf(0) }
    LaunchedEffect(list,state.currentId) {
        var resume:Job?=null
        list.interactionSource.interactions.collect { event ->
            when(event) {
                is DragInteraction.Start -> { resume?.cancel();follow=false }
                is DragInteraction.Stop,is DragInteraction.Cancel -> { resume=launch { delay(5000);follow=true } }
            }
        }
    }
    LaunchedEffect(active,follow,height) {
        if(follow && active>=0) {
            if(state.reduceMotion) list.scrollToItem(active,-height/4) else list.animateScrollToItem(active,-height/4)
        }
    }
    Column(modifier) {
        Spacer(Modifier.height(headerHeight))
        if(lines.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
            PlayerText("Lyrics for this song",20,bold=true)
            Spacer(Modifier.height(8.dp));PlayerText("Add a timed lyrics (.lrc) file",14,.6f)
            if(state.lyricsImportAvailable) Box(Modifier.padding(top=18.dp).rynxMaterial(22.dp)
                .clickable(enabled=interactive,role=Role.Button,onClick=actions::importLyrics).semantics { contentDescription="Add timed lyrics" }.padding(horizontal=22.dp,vertical=13.dp)) {
                PlayerText("Add timed lyrics",15,bold=true)
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top=20.dp).onSizeChanged { height=it.height },state=list,
                contentPadding=PaddingValues(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(20.dp),userScrollEnabled=interactive) {
                itemsIndexed(lines) { index,line ->
                    val current=index==active
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(enabled=interactive && state.durationMillis>0,role=Role.Button) {
                        actions.seek((line.startMillis.toDouble()/state.durationMillis).toFloat().coerceIn(0f,1f));follow=true
                    }.semantics { contentDescription="Seek to lyric ${line.text}" }) {
                        LyricText(line,lines.getOrNull(index+1)?.startMillis ?: state.durationMillis,state.positionMillis,current,state.reduceMotion)
                    }
                }
            }
            if(!follow) Box(Modifier.align(Alignment.End).clickable(enabled=interactive,role=Role.Button) { follow=true }.semantics { contentDescription="Resume following lyrics" }.padding(8.dp)) {
                PlayerText("Resume lyrics",13,.8f)
            }
        }
    }
}

@Composable private fun LyricText(line:LyricLine,nextStart:Long,position:Long,active:Boolean,reduceMotion:Boolean) {
    val style=TextStyle(fontSize=30.sp,lineHeight=39.sp,fontWeight=FontWeight.Bold,color=PlayerWhite.copy(alpha=if(active)1f else .32f))
    if(line.words.isEmpty() || !active) BasicText(line.text,style=style)
    else FlowRow {
        line.words.forEachIndexed { index,word ->
            val end=line.words.getOrNull(index+1)?.startMillis ?: nextStart
            val target=((position-word.startMillis).toFloat()/(end-word.startMillis).coerceAtLeast(1)).coerceIn(0f,1f)
            val animated by animateFloatAsState(target,tween(if(reduceMotion)0 else RynxMotionTokens.LyricsWordMillis))
            val fraction=if(reduceMotion)target else animated
            BasicText(word.text,style=style.copy(brush=Brush.horizontalGradient(
                0f to PlayerWhite,fraction to PlayerWhite,fraction to PlayerWhite.copy(alpha=.32f),1f to PlayerWhite.copy(alpha=.32f),
            )))
        }
    }
}
