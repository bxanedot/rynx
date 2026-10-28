package dev.bxane.rynx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import dev.bxane.rynx.design.RynxNavigationMotionSpec
import dev.bxane.rynx.design.rynxNavigationMorphGeometry
import dev.bxane.rynx.ui.material.RynxSurface
import dev.bxane.rynx.ui.material.rynxMaterial
import kotlinx.coroutines.launch

internal data class NavigationDestination(val title: String, val glyph: Glyph)

/**
 * A single reversible dock morph. Player reveal is read directly from the player geometry;
 * user collapse/expand is animated locally and resumes after the player retracts.
 */
@Composable
internal fun MorphingNavigationBar(
    destinations: List<NavigationDestination>,
    selectedIndex: Int,
    searchIndex: Int,
    expanded: Boolean,
    onExpansionChange: (Boolean) -> Unit,
    playerReveal: Float,
    reducedMotion: Boolean,
    blocked: Boolean,
    onSelectIndex: (Int) -> Unit,
    itemContent: @Composable (Int, Boolean, Float, Float, Modifier, Boolean, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (destinations.isEmpty()) return
    val selected = selectedIndex.coerceIn(0, destinations.lastIndex)
    val player = if (playerReveal.isFinite()) playerReveal.coerceIn(0f, 1f) else 0f
    val playerActive = player >= .001f
    val userProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(
            stiffness = RynxNavigationMotionSpec.MorphStiffness,
            dampingRatio = RynxNavigationMotionSpec.MorphDamping,
            visibilityThreshold = .001f,
        ),
        label = "Navigation morph",
    )
    val progress = maxOf(userProgress, player)
    val scope = rememberCoroutineScope()
    val selection = rememberElasticSelectionState(selected, destinations.size)
    val latestOnSelect by rememberUpdatedState(onSelectIndex)
    val latestOnExpansionChange by rememberUpdatedState(onExpansionChange)
    val verticalThresholdPx = with(LocalDensity.current) { RynxNavigationMotionSpec.MorphVerticalDragDp.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
            .semantics {
                testTag = "navigation-dock"
                stateDescription = if (expanded || player >= .96f) "Navigation expanded" else "Navigation collapsed"
                if (!blocked && !playerActive) {
                    customActions = listOf(
                        CustomAccessibilityAction(if (expanded) "Collapse navigation" else "Expand navigation") {
                            latestOnExpansionChange(!expanded)
                            true
                        },
                    )
                }
            }
            .pointerInput(blocked, playerActive, verticalThresholdPx) {
                if (blocked || playerActive) return@pointerInput
                var distance = 0f
                detectVerticalDragGestures(
                    onDragStart = { distance = 0f },
                    onVerticalDrag = { change, delta ->
                        distance += delta
                        change.consume()
                    },
                    onDragEnd = {
                        if (distance <= -verticalThresholdPx) latestOnExpansionChange(false)
                        else if (distance >= verticalThresholdPx) latestOnExpansionChange(true)
                        distance = 0f
                    },
                    onDragCancel = { distance = 0f },
                )
            },
    ) {
        val availableWidth = maxWidth.value.coerceAtLeast(0f)
        val compactSearchVisible = searchIndex in destinations.indices && selected != searchIndex
        val compactWidth = RynxNavigationMotionSpec.CompactBarWidthDp
        val searchWidth = if (compactSearchVisible) RynxNavigationMotionSpec.CompactSearchWidthDp * (1f - progress) else 0f
        val gap = if (compactSearchVisible) RynxNavigationMotionSpec.CompactSearchGapDp * (1f - progress) else 0f
        val collapsedGroupWidth = compactWidth + searchWidth + gap
        val groupWidth = collapsedGroupWidth + (availableWidth - collapsedGroupWidth) * progress
        val groupLeft = (availableWidth - groupWidth) * .5f
        val barWidth = (groupWidth - searchWidth - gap).coerceAtLeast(0f)
        val searchLeft = groupLeft + barWidth + gap
        val itemHit = !blocked && (player >= .96f || player < .001f)
        val cellPixels = with(LocalDensity.current) { ((barWidth - 10f).coerceAtLeast(0f).dp / destinations.size).toPx().coerceAtLeast(1f) }
        val latestCellPixels by rememberUpdatedState(cellPixels)
        val latestReducedMotion by rememberUpdatedState(reducedMotion)
        val latestItemHit by rememberUpdatedState(itemHit)
        val dragState = rememberDraggableState { delta -> selection.dragBy(delta, latestCellPixels, latestReducedMotion) }
        val latestExpanded by rememberUpdatedState(expanded)
        val itemDragEnabled = itemHit && progress >= RynxNavigationMotionSpec.MorphHitTestThreshold && player < .001f

        LaunchedEffect(selected, reducedMotion) { selection.synchronize(selected, reducedMotion) }

        RynxSurface(
            Modifier.offset(x = groupLeft.dp).width(barWidth.dp).height(RynxNavigationMotionSpec.BarHeightDp.dp)
                .semantics { testTag = "navigation-surface" },
            RynxNavigationMotionSpec.BarCornerRadiusDp.dp,
            shadow = true,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(5.dp)) {
                val geometry = rynxNavigationMorphGeometry(
                    widthDp = maxWidth.value,
                    itemCount = destinations.size,
                    selectedIndex = selected,
                    progress = progress,
                    indicatorPosition = selection.value,
                    compactWidthDp = (compactWidth - 10f).coerceAtLeast(0f),
                )
                Box(
                    Modifier.fillMaxSize().draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        enabled = itemDragEnabled,
                        onDragStarted = { selection.beginDrag(scope) },
                        onDragStopped = { velocity ->
                            selection.finishDrag(scope, velocity, latestCellPixels, latestReducedMotion) { index ->
                                if (latestItemHit) latestOnSelect(index)
                            }
                        },
                    ),
                ) {
                    ElasticSelectionIndicator(geometry, selection, reducedMotion, Modifier.fillMaxHeight())
                    destinations.forEachIndexed { index, destination ->
                        val isSelected = index == selected
                        if (isSelected || progress > .001f) {
                            val itemVisible = isSelected || progress >= RynxNavigationMotionSpec.MorphHitTestThreshold
                            val itemEnabled = itemHit && (isSelected || geometry.otherTabsInteractive)
                            val x = geometry.itemLeftDp[index].dp
                            val width = geometry.itemWidthDp.dp
                            itemContent(
                                index,
                                isSelected,
                                geometry.labelAlpha,
                                progress,
                                Modifier.offset(x = x).width(width).fillMaxHeight()
                                    .graphicsLayer { alpha = if (isSelected) 1f else progress }
                                    .semantics { if (!itemVisible) hideFromAccessibility() },
                                itemEnabled,
                            ) {
                                if (isSelected && !playerActive) latestOnExpansionChange(!latestExpanded)
                                else {
                                    onSelectIndex(index)
                                    if (!playerActive) latestOnExpansionChange(true)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (compactSearchVisible && searchWidth > .1f) {
            val searchEnabled = !blocked && player < .001f && progress < RynxNavigationMotionSpec.MorphHitTestThreshold
            Box(
                Modifier.offset(x = searchLeft.dp).width(searchWidth.dp).height(RynxNavigationMotionSpec.CompactSearchWidthDp.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        alpha = 1f - progress
                        scaleX = .9f + .1f * (1f - progress)
                        scaleY = scaleX
                    }
                    .clip(CircleShape)
                    .rynxMaterial((RynxNavigationMotionSpec.CompactSearchWidthDp / 2f).dp, selected = true, amount = .48f)
                    .semantics {
                        if (!searchEnabled) hideFromAccessibility()
                    }
                    .clickable(enabled = searchEnabled, role = Role.Tab, onClick = { onSelectIndex(searchIndex) })
                    .semantics { contentDescription = "Search"; role = Role.Tab; testTag = "navigation-search" },
                contentAlignment = Alignment.Center,
            ) {
                RynxGlyph(destinations[searchIndex].glyph, androidx.compose.ui.graphics.Color(0xFFFA2D48))
            }
        }
    }
}
