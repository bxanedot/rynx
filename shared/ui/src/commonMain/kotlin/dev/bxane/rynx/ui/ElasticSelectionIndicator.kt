package dev.bxane.rynx.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import dev.bxane.rynx.design.RynxNavigationMotionSpec
import dev.bxane.rynx.design.RynxNavigationMorphGeometry
import dev.bxane.rynx.design.rynxNavigationReleaseTarget
import dev.bxane.rynx.ui.material.rynxMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** One interruptible selection position is shared by taps, external selection and dragging. */
@Stable
internal class ElasticSelectionState(initialIndex: Int, private val count: Int) {
    private val position = Animatable(initialIndex.coerceIn(0, count.coerceAtLeast(1) - 1).toFloat())
    private var dragPosition by mutableFloatStateOf(position.value)
    private var dragStretch by mutableFloatStateOf(0f)
    private var isDragging by mutableStateOf(false)
    private var commandedIndex by mutableIntStateOf(initialIndex.coerceIn(0, count.coerceAtLeast(1) - 1))

    val value: Float get() = if (isDragging) dragPosition else position.value
    val stretch: Float get() = dragStretch
    val dragging: Boolean get() = isDragging

    fun beginDrag(scope: CoroutineScope) {
        if (isDragging) return
        dragPosition = position.value
        dragStretch = 0f
        isDragging = true
        scope.launch { position.stop() }
    }

    fun dragBy(deltaPixels: Float, cellPixels: Float, reducedMotion: Boolean) {
        if (!isDragging || !deltaPixels.isFinite() || !cellPixels.isFinite() || cellPixels <= 0f) return
        val delta = deltaPixels / cellPixels
        dragPosition = (dragPosition + delta).coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat())
        dragStretch = if (reducedMotion) 0f else (abs(delta) * 1.5f).coerceIn(0f, RynxNavigationMotionSpec.MaximumSelectionStretch)
    }

    fun finishDrag(
        scope: CoroutineScope,
        velocityPixelsPerSecond: Float,
        cellPixels: Float,
        reducedMotion: Boolean,
        onSelectIndex: (Int) -> Unit,
    ) {
        if (!isDragging) return
        val velocity = if (velocityPixelsPerSecond.isFinite() && cellPixels.isFinite() && cellPixels > 0f) {
            (velocityPixelsPerSecond / cellPixels).coerceIn(-12f, 12f)
        } else 0f
        val target = rynxNavigationReleaseTarget(dragPosition, velocity, count)
        commandedIndex = target
        isDragging = false
        onSelectIndex(target)
        scope.launch {
            if (reducedMotion) position.snapTo(target.toFloat())
            else position.animateTo(
                target.toFloat(),
                animationSpec = spring(
                    stiffness = RynxNavigationMotionSpec.SelectionStiffness,
                    dampingRatio = RynxNavigationMotionSpec.SelectionDamping,
                    visibilityThreshold = .001f,
                ),
                initialVelocity = velocity,
            )
            dragStretch = 0f
        }
    }

    suspend fun synchronize(index: Int, reducedMotion: Boolean) {
        val target = index.coerceIn(0, count.coerceAtLeast(1) - 1)
        if (isDragging || target == commandedIndex) return
        commandedIndex = target
        if (reducedMotion) {
            position.snapTo(target.toFloat())
            dragStretch = 0f
        } else {
            position.animateTo(
                target.toFloat(),
                spring(
                    stiffness = RynxNavigationMotionSpec.SelectionStiffness,
                    dampingRatio = RynxNavigationMotionSpec.SelectionDamping,
                    visibilityThreshold = .001f,
                ),
            )
            dragStretch = 0f
        }
    }
}

@Composable
internal fun rememberElasticSelectionState(selectedIndex: Int, count: Int): ElasticSelectionState =
    remember(count) { ElasticSelectionState(selectedIndex, count) }

@Composable
internal fun ElasticSelectionIndicator(
    geometry: RynxNavigationMorphGeometry,
    state: ElasticSelectionState,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(geometry.selectedIndex, reducedMotion) {
        state.synchronize(geometry.selectedIndex, reducedMotion)
    }
    val travelStretch = if (reducedMotion || state.dragging) 0f else (abs(state.value - geometry.selectedIndex) * .04f).coerceAtMost(.12f)
    val deformation = if (reducedMotion) 0f else (state.stretch + travelStretch).coerceAtMost(RynxNavigationMotionSpec.MaximumSelectionStretch)
    Box(
        Modifier.offset(x = (geometry.indicatorLeftDp - geometry.indicatorWidthDp * deformation * .5f).dp)
            .width((geometry.indicatorWidthDp * (1f + deformation)).dp)
            .fillMaxHeight()
            .then(modifier)
            .rynxMaterial(22.dp, selected = true, amount = .5f)
            .semantics { testTag = "navigation-selection-indicator" },
    )
}
