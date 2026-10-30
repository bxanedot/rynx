package dev.bxane.rynx.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.bxane.rynx.design.RynxNavigationMotionSpec
import com.composables.sfsymbols.SfBounceDirection
import com.composables.sfsymbols.SfEffectScope
import com.composables.sfsymbols.SfSymbolEffect

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
    morphProgress: Float = 1f,
    modifier: Modifier = Modifier,
    interaction: MutableInteractionSource? = null,
) {
    val appearance = remember { Animatable(.78f) }
    val pressed = interaction?.collectIsPressedAsState()?.value ?: false
    val tint by animateColorAsState(
        if (selected) selectedColor else inactiveColor,
        animationSpec = if (reducedMotion) snap() else spring(
            stiffness = RynxNavigationMotionSpec.IconStiffness,
            dampingRatio = RynxNavigationMotionSpec.IconDamping,
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
                    stiffness = RynxNavigationMotionSpec.IconStiffness,
                    dampingRatio = RynxNavigationMotionSpec.IconDamping,
                ))
            } else appearance.animateTo(1f, spring(stiffness = RynxNavigationMotionSpec.IconStiffness))
        }
    }
    val morphScale = .94f + .06f * morphProgress.coerceIn(0f, 1f)
    Box(modifier.graphicsLayer { scaleX = appearance.value * morphScale; scaleY = appearance.value * morphScale }) {
        val glyphEffect = remember(glyph, selected, reducedMotion) {
            if (selected && !reducedMotion) {
                SfSymbolEffect.Bounce(direction = SfBounceDirection.Up, scope = SfEffectScope.ByLayer)
            } else null
        }
        RynxGlyph(
            glyph = glyph,
            tint = tint,
            modifier = Modifier.size(22.dp),
            effect = glyphEffect,
            trigger = selected,
            active = selected,
        )
    }
}
