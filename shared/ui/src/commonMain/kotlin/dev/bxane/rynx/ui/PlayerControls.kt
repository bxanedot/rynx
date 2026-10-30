package dev.bxane.rynx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import dev.bxane.rynx.design.*
import dev.bxane.rynx.ui.material.*

internal val PlayerWhite=Color(0xFFF8FAFB)

@Composable internal fun PlayerText(text:String,size:Int=15,alpha:Float=1f,bold:Boolean=false,modifier:Modifier=Modifier) {
    BasicText(text,modifier,style=TextStyle(color=PlayerWhite.copy(alpha=alpha),fontSize=size.sp,
        fontFamily=FontFamily.SansSerif,fontWeight=if(bold) FontWeight.SemiBold else FontWeight.Normal),
        maxLines=1,overflow=TextOverflow.Ellipsis)
}

@Composable internal fun PlayerButton(glyph:Glyph,label:String,onClick:()->Unit,modifier:Modifier=Modifier,
    enabled:Boolean=true,selected:Boolean=false,iconSize:Dp=24.dp,touchSize:Dp=44.dp,background:Boolean=false,reduceMotion:Boolean=false) {
    val interaction=remember { MutableInteractionSource() }
    Box(modifier.size(touchSize).rynxPressFeedback(interaction,enabled).rynxMaterial(touchSize/2,selected,interaction,amount=if(background)1f else .60f)
        .clickable(enabled=enabled,role=Role.Button,interactionSource=interaction,indication=null,onClick=onClick)
        .semantics { contentDescription=label;this.selected=selected;if(!enabled) disabled() },contentAlignment=Alignment.Center) {
        val tint=PlayerWhite.copy(alpha=if(enabled)1f else .3f)
        val glyphEffect = remember(selected, reduceMotion) {
            if (selected && !reduceMotion) {
                com.composables.sfsymbols.SfSymbolEffect.Bounce(
                    direction = com.composables.sfsymbols.SfBounceDirection.Up,
                    scope = com.composables.sfsymbols.SfEffectScope.ByLayer
                )
            } else null
        }
        if(glyph==Glyph.Play || glyph==Glyph.Pause) PlayPauseSymbol(glyph==Glyph.Pause,tint,Modifier.size(iconSize),reduceMotion)
        else RynxGlyph(glyph,tint,Modifier.size(iconSize),effect=glyphEffect,trigger=selected,active=selected)
    }
}

/** Two independently authored polygons preserve their identity while play becomes pause. */
@Composable private fun PlayPauseSymbol(playing:Boolean,tint:Color,modifier:Modifier,reduceMotion:Boolean) {
    val fraction by animateFloatAsState(if(playing)1f else 0f,animationSpec=if(reduceMotion)snap() else spring(stiffness=RynxMotionTokens.PressStiffness,dampingRatio=RynxMotionTokens.PressDamping))
    val p=if(reduceMotion) { if(playing)1f else 0f } else fraction
    Canvas(modifier) {
        fun polygon(from:List<Offset>,to:List<Offset>)=Path().apply {
            from.indices.forEach { i ->
                val point=from[i]+(to[i]-from[i])*p
                val x=point.x*size.width/24;val y=point.y*size.height/24
                if(i==0) moveTo(x,y) else lineTo(x,y)
            };close()
        }
        drawPath(polygon(listOf(Offset(5f,2f),Offset(12f,6.4f),Offset(12f,17.6f),Offset(5f,22f)),
            listOf(Offset(4.5f,2f),Offset(10f,2f),Offset(10f,22f),Offset(4.5f,22f))),tint)
        drawPath(polygon(listOf(Offset(12f,6.4f),Offset(21f,12f),Offset(21f,12f),Offset(12f,17.6f)),
            listOf(Offset(14f,2f),Offset(19.5f,2f),Offset(19.5f,22f),Offset(14f,22f))),tint)
    }
}

@Composable internal fun PlayerTimeline(state:PlayerUiState,actions:PlayerActions,modifier:Modifier=Modifier,interactive:Boolean=true) {
    var preview by remember(state.currentId) { mutableStateOf<Float?>(null) }
    val enabled=interactive && state.durationMillis>0
    val value=if(enabled) (state.positionMillis.toDouble()/state.durationMillis).toFloat().coerceIn(0f,1f) else 0f
    val time=preview?.let { (it*state.durationMillis).toLong() } ?: state.positionMillis
    Column(modifier) {
        // Disposing the old pointer handler cancels its gesture before a new track can receive it.
        key(state.currentId) {
            RynxSlider(value,"Playback position",enabled,actions::seek,Modifier.fillMaxWidth(),onPreview={preview=it})
        }
        Row(Modifier.fillMaxWidth().offset(y=(-5).dp)) {
            PlayerText(if(enabled) playerTime(time) else "--:--",12,.48f)
            Spacer(Modifier.weight(1f))
            PlayerText(if(enabled) "-${playerTime((state.durationMillis-time).coerceAtLeast(0))}" else "--:--",12,.48f)
        }
    }
}

@Composable internal fun PlayerTransport(state:PlayerUiState,actions:PlayerActions,modifier:Modifier=Modifier) {
    Row(modifier.fillMaxWidth().height(68.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
        PlayerButton(Glyph.Previous,"Previous track",actions::previous,enabled=state.current!=null,iconSize=40.dp,touchSize=64.dp,reduceMotion=state.reduceMotion)
        PlayerButton(if(state.playing)Glyph.Pause else Glyph.Play,if(state.playing)"Pause" else "Play",actions::toggle,
            enabled=state.current!=null,iconSize=48.dp,touchSize=68.dp,reduceMotion=state.reduceMotion)
        PlayerButton(Glyph.Next,"Next track",actions::next,enabled=state.current!=null,iconSize=40.dp,touchSize=64.dp,reduceMotion=state.reduceMotion)
    }
}

@Composable internal fun PlayerVolume(state:PlayerUiState,actions:PlayerActions,modifier:Modifier=Modifier,interactive:Boolean=true) {
    Row(modifier.fillMaxWidth().height(44.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        RynxGlyph(Glyph.SpeakerQuiet,PlayerWhite.copy(alpha=.5f),Modifier.size(15.dp))
        RynxSlider(state.volume,"Playback volume",interactive && state.volumeControlAvailable,actions::setVolume,Modifier.weight(1f))
        RynxGlyph(Glyph.SpeakerLoud,PlayerWhite.copy(alpha=.5f),Modifier.size(23.dp))
    }
}

internal fun playerTime(millis:Long):String {
    val seconds=millis.coerceAtLeast(0)/1000
    return "${seconds/60}:${(seconds%60).toString().padStart(2,'0')}"
}
