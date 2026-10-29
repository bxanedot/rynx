package dev.bxane.rynx.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Shared startup mark animation; platform hosts provide the matching pulse haptic. */
@Composable
fun RynxStartupSplash(
    reduceMotion: Boolean = false,
    onPulse: (pulseNumber: Int) -> Unit,
    onFinished: () -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val currentReduceMotion = rememberUpdatedState(reduceMotion)
    val currentOnPulse = rememberUpdatedState(onPulse)
    val currentOnFinished = rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        runRynxStartupSequence(
            reduceMotion = { currentReduceMotion.value },
            onPulse = { currentOnPulse.value(it) },
            animatePulse = {
                scale.animateTo(
                    targetValue = RynxSplashMotion.PeakScale,
                    animationSpec = tween(
                        durationMillis = RynxSplashMotion.RiseMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = RynxSplashMotion.DampingRatio,
                        stiffness = RynxSplashMotion.SpringStiffness,
                        visibilityThreshold = 0.001f,
                    ),
                )
            },
            wait = { delay(it) },
            onFinished = { currentOnFinished.value() },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
            .clearAndSetSemantics { contentDescription = "Rynx" },
        contentAlignment = Alignment.Center,
    ) {
        RynxBrandMark(
            modifier = Modifier
                .size(RynxSplashMotion.MarkSizeDp.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            tint = Color.White,
        )
    }
}

/** Runs the product-owned pulse and dismissal sequence with injectable timing for tests. */
internal suspend fun runRynxStartupSequence(
    reduceMotion: () -> Boolean,
    onPulse: (pulseNumber: Int) -> Unit,
    animatePulse: suspend () -> Unit,
    wait: suspend (durationMillis: Long) -> Unit,
    onFinished: () -> Unit,
) {
    repeat(RynxSplashMotion.PulseCount) { index ->
        onPulse(index + 1)
        if (!reduceMotion()) animatePulse()
        if (index < RynxSplashMotion.PulseCount - 1) wait(RynxSplashMotion.GapMillis)
    }
    wait(RynxSplashMotion.ExitHoldMillis)
    onFinished()
}

/** Product-owned startup choreography, shared by every Compose Multiplatform host. */
internal object RynxSplashMotion {
    const val PulseCount = 3
    const val MarkSizeDp = 88
    const val PeakScale = 1.16f
    const val RiseMillis = 90
    const val GapMillis = 100L
    const val ExitHoldMillis = 90L
    const val DampingRatio = 0.68f
    const val SpringStiffness = 720f
}
