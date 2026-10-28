package dev.bxane.rynx.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.bxane.rynx.ui.material.rynxMaterial
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.min

/**
 * The indicator remains one surface while its position and width deform.
 * Pointer movement updates the same Animatable used by selection changes.
 */
@Composable
internal fun ElasticSelectionIndicator(
    selectedIndex: Int,
    count: Int,
    reducedMotion: Boolean,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    val position = remember { Animatable(selectedIndex.toFloat()) }
    val stretch = remember { Animatable(0f) }
    var destination by remember { mutableIntStateOf(selectedIndex) }
    val scope = rememberCoroutineScope()
    val latestSelection by rememberUpdatedState(onSelectIndex)
    LaunchedEffect(selectedIndex, reducedMotion) {
        destination = selectedIndex.coerceIn(0, count - 1)
        val target = destination.toFloat()
        if (reducedMotion) {
            position.snapTo(target)
            stretch.snapTo(0f)
        } else {
            position.animateTo(target, spring(
                stiffness = NavigationMotionSpec.SelectionStiffness,
                dampingRatio = NavigationMotionSpec.SelectionDamping,
            ))
            stretch.animateTo(0f, spring())
        }
    }
    BoxWithConstraints(modifier) {
        val cell = maxWidth / count
        val cellPixels = with(LocalDensity.current) { cell.toPx().coerceAtLeast(1f) }
        val gesture = Modifier.pointerInput(cellPixels, count, reducedMotion) {
            var dragging = false
            detectHorizontalDragGestures(
                onDragStart = { dragging = true; scope.launch { position.stop() } },
                onHorizontalDrag = { change, dx ->
                    if (dragging) {
                        change.consume()
                        scope.launch {
                            position.snapTo((position.value + dx / cellPixels).coerceIn(0f, (count - 1).toFloat()))
                            if (!reducedMotion) stretch.snapTo((abs(dx) / cellPixels).coerceIn(0f, .22f))
                        }
                    }
                },
                onDragCancel = {
                    dragging = false
                    scope.launch { stretch.animateTo(0f, spring()) }
                },
                onDragEnd = {
                    dragging = false
                    val target = position.value.roundToInt().coerceIn(0, count - 1)
                    destination = target
                    latestSelection(target)
                    scope.launch {
                        position.animateTo(target.toFloat(), if (reducedMotion) snap() else spring(
                            stiffness = NavigationMotionSpec.SelectionStiffness,
                            dampingRatio = NavigationMotionSpec.SelectionDamping,
                        ))
                        stretch.animateTo(0f, spring())
                    }
                },
            )
        }
        val travelStretch = if (reducedMotion) 0f else min(.19f, abs(destination - position.value) * .055f)
        val deformation = (stretch.value + travelStretch).coerceIn(0f, .25f)
        Box(Modifier.offset(x = cell * position.value - cell * deformation * .5f)
            .width(cell * (1f + deformation)).fillMaxHeight()
            .then(gesture)
            .rynxMaterial(22.dp, selected = true, amount = .5f))
    }
}
