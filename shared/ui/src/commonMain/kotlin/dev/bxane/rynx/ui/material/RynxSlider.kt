package dev.bxane.rynx.ui.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.bxane.rynx.ui.PlayerWhite
import dev.bxane.rynx.design.RynxMotionTokens
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable internal fun RynxSlider(value:Float,label:String,enabled:Boolean,onCommit:(Float)->Unit,
    modifier:Modifier=Modifier,onPreview:(Float?)->Unit={}) {
    var width by remember { mutableFloatStateOf(1f) }
    var preview by remember { mutableStateOf<Float?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val commit by rememberUpdatedState(onCommit)
    val previewChanged by rememberUpdatedState(onPreview)
    val safeValue=if(value.isFinite()) value.coerceIn(0f,1f) else 0f
    val display=preview ?: safeValue
    val trackColor=if(LocalRynxDarkSurface.current)PlayerWhite else Color(0xFF4A4D60)
    val reduced=LocalRynxReducedMotion.current
    val thickness by animateFloatAsState(if(dragging)9f else 6f,if(reduced)snap() else spring(stiffness=RynxMotionTokens.SliderStiffness,dampingRatio=RynxMotionTokens.PressDamping))
    fun previewAt(x:Float) { preview=(x/width).coerceIn(0f,1f);previewChanged(preview) }
    fun finish() { preview?.let(commit);dragging=false }
    LaunchedEffect(safeValue) { if(!dragging && preview?.let { abs(it-safeValue)<.02f }==true) { preview=null;previewChanged(null) } }
    LaunchedEffect(preview,dragging) { if(!dragging && preview!=null) { delay(700);preview=null;previewChanged(null) } }
    Box(modifier.height(44.dp).rynxMaterial(22.dp,amount=.24f).onSizeChanged { width=it.width.toFloat().coerceAtLeast(1f) }
        .semantics {
            contentDescription=label;progressBarRangeInfo=ProgressBarRangeInfo(display,0f..1f)
            if(enabled) setProgress { requested ->
                if(requested.isFinite()) { commit(requested.coerceIn(0f,1f));true } else false
            } else disabled()
        }
        .pointerInput(enabled) { if(enabled) detectTapGestures { previewAt(it.x);finish() } }
        .pointerInput(enabled) { if(enabled) detectHorizontalDragGestures(
            onDragStart={ dragging=true;previewAt(it.x) },
            onDragEnd={finish()},onDragCancel={dragging=false;preview=null;previewChanged(null)},
        ) { change,_ -> change.consume();previewAt(change.position.x) } },contentAlignment=Alignment.CenterStart) {
        Canvas(Modifier.fillMaxWidth().height(thickness.dp)) {
            val radius=CornerRadius(size.height/2)
            drawRoundRect(trackColor.copy(alpha=if(enabled).24f else .12f),cornerRadius=radius)
            if(display>0) drawRoundRect(trackColor.copy(alpha=if(enabled).76f else .3f),size=Size(size.width*display,size.height),cornerRadius=radius)
        }
    }
}
