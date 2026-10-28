package dev.bxane.rynx.design

import kotlin.math.roundToInt

/** Authored Rynx navigation motion. Values are tunable design tokens, not measured reference data. */
object RynxNavigationMotionSpec {
    const val IconStiffness = 390f
    const val IconDamping = .62f
    const val LibraryLayerStiffness = 340f
    const val LibraryLayerDamping = .58f
    const val SelectionStiffness = 310f
    const val SelectionDamping = .76f
    const val MorphStiffness = 430f
    const val MorphDamping = .88f
    const val SearchStiffness = 470f
    const val SearchDamping = .9f
    const val CompactBarWidthDp = 58f
    const val CompactSearchWidthDp = 48f
    const val CompactSearchGapDp = 8f
    const val BarHeightDp = 64f
    const val BarCornerRadiusDp = 28f
    const val DragProjectionSeconds = .18f
    const val MaximumSelectionStretch = .2f
    const val SelectionFlingVelocityItemsPerSecond = 2.1f
    const val MorphHitTestThreshold = .84f
    const val MorphVerticalDragDp = 28f
    const val SearchFocusThreshold = .82f
    const val SearchInputEnableThreshold = .18f
}

data class RynxNavigationMorphGeometry(
    val selectedIndex: Int,
    val surfaceLeftDp: Float,
    val surfaceWidthDp: Float,
    val itemWidthDp: Float,
    val itemLeftDp: List<Float>,
    val indicatorLeftDp: Float,
    val indicatorWidthDp: Float,
    val labelAlpha: Float,
    val compactSearchAlpha: Float,
    val selectedItemAccessible: Boolean,
    val otherTabsInteractive: Boolean,
)

/**
 * One reversible layout map for the dock, icon slots, selection capsule and labels.
 * The compact slots converge on the selected icon; the surface keeps continuous geometry.
 */
fun rynxNavigationMorphGeometry(
    widthDp: Float,
    itemCount: Int,
    selectedIndex: Int,
    progress: Float,
    indicatorPosition: Float = selectedIndex.toFloat(),
    compactWidthDp: Float = RynxNavigationMotionSpec.CompactBarWidthDp,
): RynxNavigationMorphGeometry {
    val width = if (widthDp.isFinite()) widthDp.coerceAtLeast(0f) else 0f
    val count = itemCount.coerceAtLeast(1)
    val selected = selectedIndex.coerceIn(0, count - 1)
    val p = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
    val compact = if (compactWidthDp.isFinite()) compactWidthDp.coerceIn(0f, width) else 0f
    val expandedCell = width / count
    val surfaceWidth = mix(compact, width, p)
    val surfaceLeft = (width - surfaceWidth) * .5f
    val itemWidth = mix(compact, expandedCell, p)
    val compactItemLeft = (width - compact) * .5f
    val positions = List(count) { index -> mix(compactItemLeft, expandedCell * index, p) }
    val stableIndicatorPosition = if (indicatorPosition.isFinite()) indicatorPosition.coerceIn(0f, (count - 1).toFloat()) else selected.toFloat()
    val expandedIndicatorLeft = expandedCell * stableIndicatorPosition
    return RynxNavigationMorphGeometry(
        selectedIndex = selected,
        surfaceLeftDp = surfaceLeft,
        surfaceWidthDp = surfaceWidth,
        itemWidthDp = itemWidth,
        itemLeftDp = positions,
        indicatorLeftDp = mix(surfaceLeft, expandedIndicatorLeft, p),
        indicatorWidthDp = mix(compact, expandedCell, p),
        labelAlpha = p,
        compactSearchAlpha = 1f - p,
        selectedItemAccessible = true,
        otherTabsInteractive = p >= RynxNavigationMotionSpec.MorphHitTestThreshold,
    )
}

/** Release uses recent drag velocity so a quick swipe may settle on the next tab. */
fun rynxNavigationReleaseTarget(position: Float, velocityItemsPerSecond: Float, count: Int): Int {
    val safeCount = count.coerceAtLeast(1)
    val current = if (position.isFinite()) position.coerceIn(0f, (safeCount - 1).toFloat()) else 0f
    val velocity = if (velocityItemsPerSecond.isFinite()) velocityItemsPerSecond.coerceIn(-12f, 12f) else 0f
    val projection = if (kotlin.math.abs(velocity) >= RynxNavigationMotionSpec.SelectionFlingVelocityItemsPerSecond) {
        velocity * RynxNavigationMotionSpec.DragProjectionSeconds
    } else 0f
    return (current + projection).roundToInt().coerceIn(0, safeCount - 1)
}

private fun mix(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
