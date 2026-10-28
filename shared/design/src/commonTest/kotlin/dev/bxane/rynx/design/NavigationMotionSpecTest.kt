package dev.bxane.rynx.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationMotionSpecTest {
    @Test fun compactCapsuleKeepsTheSelectedGlyphCenteredAndOtherSlotsTogether() {
        val geometry = rynxNavigationMorphGeometry(340f, 5, selectedIndex = 3, progress = 0f)
        assertEquals(58f, geometry.surfaceWidthDp)
        assertEquals(141f, geometry.surfaceLeftDp)
        assertEquals(58f, geometry.itemWidthDp)
        assertTrue(geometry.itemLeftDp.all { it == geometry.surfaceLeftDp })
        assertTrue(geometry.selectedItemAccessible)
        assertFalse(geometry.otherTabsInteractive)
        assertEquals(0f, geometry.labelAlpha)
        assertEquals(1f, geometry.compactSearchAlpha)
    }

    @Test fun expandedBarRestoresSeparateTabPositionsAndLabels() {
        val geometry = rynxNavigationMorphGeometry(340f, 5, selectedIndex = 2, progress = 1f)
        assertEquals(340f, geometry.surfaceWidthDp)
        assertEquals(0f, geometry.surfaceLeftDp)
        assertEquals(68f, geometry.itemWidthDp)
        assertEquals(listOf(0f, 68f, 136f, 204f, 272f), geometry.itemLeftDp)
        assertEquals(1f, geometry.labelAlpha)
        assertEquals(0f, geometry.compactSearchAlpha)
        assertTrue(geometry.otherTabsInteractive)
    }

    @Test fun morphAndSelectionTargetsStayContinuousAndVelocityAware() {
        var previous = rynxNavigationMorphGeometry(390f, 5, 4, 0f)
        for (step in 1..100) {
            val next = rynxNavigationMorphGeometry(390f, 5, 4, step / 100f)
            assertTrue(next.surfaceWidthDp >= previous.surfaceWidthDp)
            assertTrue(next.itemWidthDp >= previous.itemWidthDp)
            assertTrue(next.labelAlpha in 0f..1f)
            assertTrue(next.compactSearchAlpha in 0f..1f)
            assertTrue(kotlin.math.abs(next.surfaceLeftDp - previous.surfaceLeftDp) < 4f)
            previous = next
        }
        assertEquals(3, rynxNavigationReleaseTarget(position = 2.15f, velocityItemsPerSecond = 2.2f, count = 5))
        assertEquals(0, rynxNavigationReleaseTarget(position = .4f, velocityItemsPerSecond = -8f, count = 5))
        assertEquals(4, rynxNavigationReleaseTarget(position = 3.8f, velocityItemsPerSecond = 9f, count = 5))
    }

    @Test fun malformedGeometryAndVelocityAreBounded() {
        val compact = rynxNavigationMorphGeometry(Float.NaN, 0, Int.MAX_VALUE, Float.NaN)
        assertEquals(0f, compact.surfaceWidthDp)
        assertEquals(0f, compact.surfaceLeftDp)
        assertTrue(compact.selectedIndex == 0)
        assertEquals(0, rynxNavigationReleaseTarget(Float.NaN, Float.POSITIVE_INFINITY, 2))
    }
}
