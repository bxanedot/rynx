package dev.bxane.rynx.design

import kotlin.test.*

class PlayerGeometryTest {
    @Test fun capsuleAndExpandedSurfaceShareOneContinuousGeometry() {
        val compact=playerGeometry(390f,844f,0f)
        val expanded=playerGeometry(390f,844f,1f)
        assertEquals(62f,compact.height);assertEquals(12f,compact.left);assertEquals(40f,compact.artwork.width)
        assertEquals(390f,expanded.width);assertEquals(0f,expanded.top)
        assertTrue(expanded.height < 844f-60f,"Expanded player must leave a distinct navigation dock")
        assertEquals(0f,compact.navigationReveal);assertEquals(1f,expanded.navigationReveal)
        assertFalse(compact.navigationInteractive);assertTrue(expanded.navigationInteractive)
        for(i in 1..99) {
            val p=i/100f;val before=playerGeometry(390f,844f,p-.001f);val after=playerGeometry(390f,844f,p+.001f)
            assertTrue(kotlin.math.abs(after.height-before.height)<3f)
            assertTrue(kotlin.math.abs(after.artwork.top-before.artwork.top)<3f)
            assertTrue(after.navigationReveal in 0f..1f)
        }
    }
    @Test fun navigationIsNotClickableUntilItIsSufficientlyVisible() {
        assertFalse(playerGeometry(390f,844f,.5f).navigationInteractive)
        assertTrue(playerGeometry(390f,844f,.99f).navigationInteractive)
        assertEquals(playerGeometry(390f,844f,.64f),playerGeometry(390f,844f,.64f),"Direction must not change geometry at a given progress")
        assertEquals(playerGeometry(390f,844f,0f),playerGeometry(390f,844f,-1f))
        assertEquals(playerGeometry(390f,844f,1f),playerGeometry(390f,844f,Float.NaN))
    }
}
