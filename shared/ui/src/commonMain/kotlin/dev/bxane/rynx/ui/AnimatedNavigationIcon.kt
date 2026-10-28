package dev.bxane.rynx.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Rynx-owned motion values, independent of third-party application implementations. */
internal object NavigationMotionSpec {
    const val IconStiffness = 390f
    const val IconDamping = .62f
    const val LayerStiffness = 340f
    const val LayerDamping = .58f
    const val SelectionStiffness = 310f
    const val SelectionDamping = .76f
}

/**
 * Selection is keyed to the Boolean transition, not to recomposition.
 * Animatable cancels an in-flight spring on the next selection.
 */
@Composable
internal fun AnimatedNavigationIcon(
    glyph: Glyph,
    selected: Boolean,
    selectedColor: Color,
    inactiveColor: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    interaction: MutableInteractionSource? = null,
) {
    val appearance = remember { Animatable(.78f) }
    val pressed = interaction?.collectIsPressedAsState()?.value ?: false
    val tint by animateColorAsState(
        if (selected) selectedColor else inactiveColor,
        animationSpec = if (reducedMotion) snap() else spring(
            stiffness = NavigationMotionSpec.IconStiffness,
            dampingRatio = NavigationMotionSpec.IconDamping,
        ),
        label = "Navigation glyph tint",
    )
    LaunchedEffect(selected, pressed, reducedMotion) {
        if (reducedMotion) appearance.snapTo(1f)
        else {
            if (pressed) appearance.animateTo(.87f, spring(stiffness = 650f))
            else if (selected) {
                appearance.snapTo(.88f)
                appearance.animateTo(1f, spring(
                    stiffness = NavigationMotionSpec.IconStiffness,
                    dampingRatio = NavigationMotionSpec.IconDamping,
                ))
            } else appearance.animateTo(1f, spring(stiffness = NavigationMotionSpec.IconStiffness))
        }
    }
    Box(modifier.graphicsLayer { scaleX = appearance.value; scaleY = appearance.value }) {
        if (glyph == Glyph.Library) AnimatedLibraryIcon(selected, tint, reducedMotion)
        else RynxGlyph(glyph, tint, Modifier.size(22.dp))
    }
}

/** Original three-spine book glyph; each spine has its own interruptible spring. */
@Composable
internal fun AnimatedLibraryIcon(selected: Boolean, tint: Color, reducedMotion: Boolean) {
    val spines = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(selected, reducedMotion) {
        if (reducedMotion) spines.forEach { it.snapTo(0f) }
        else if (selected) {
            spines.forEachIndexed { index, spine ->
                launch {
                    delay(index * 34L)
                    spine.snapTo(-3.5f)
                    spine.animateTo(0f, spring(
                        stiffness = NavigationMotionSpec.LayerStiffness,
                        dampingRatio = NavigationMotionSpec.LayerDamping,
                    ))
                }
            }
        } else spines.forEach { launch { it.animateTo(0f, spring()) } }
    }
    Canvas(Modifier.size(22.dp)) {
        val unit = size.minDimension / 24f
        listOf(4.8f, 10.8f, 17f).forEachIndexed { index, x ->
            val rise = spines[index].value
            drawLine(tint, Offset(x * unit, (4f + rise) * unit),
                Offset(x * unit, (20f + rise) * unit), 1.9f * unit, StrokeCap.Round)
        }
        drawLine(tint, Offset(17f * unit, (4f + spines[2].value) * unit),
            Offset(21f * unit, (19f + spines[2].value) * unit), 1.9f * unit, StrokeCap.Round)
    }
}
