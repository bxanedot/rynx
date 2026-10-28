package dev.bxane.rynx.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RynxWindowPolicyTest {
    @Test fun compactPhoneUsesBottomNavigation() {
        val p=rynxWindowPolicy(390f,844f)
        assertEquals(RynxWindowClass.Compact,p.windowClass)
        assertFalse(p.sideNavigation)
        assertTrue(p.minimumTouchTarget>=48f)
    }
    @Test fun expandedDesktopGetsSideNavigation() {
        val p=rynxWindowPolicy(1120f,760f)
        assertEquals(RynxWindowClass.Expanded,p.windowClass)
        assertTrue(p.sideNavigation)
    }
    @Test fun shortLandscapeKeepsDock() {
        assertFalse(rynxWindowPolicy(960f,540f).sideNavigation)
    }
    @Test fun invalidSizeIsSafe() {
        val p=rynxWindowPolicy(Float.NaN,Float.POSITIVE_INFINITY)
        assertEquals(RynxWindowClass.Compact,p.windowClass)
        assertFalse(p.sideNavigation)
    }
}
