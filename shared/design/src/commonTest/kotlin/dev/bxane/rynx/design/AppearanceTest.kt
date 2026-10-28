package dev.bxane.rynx.design

import kotlin.test.*

class AppearanceTest {
    @Test fun glassIsTheDefaultAndAllMaterialsHaveExplicitEffectPolicies() {
        assertEquals(RynxMaterialMode.LiquidGlass,RynxAppearance().material)
        assertTrue(RynxAppearance().policy().backdrop)
        val solid=RynxAppearance(material=RynxMaterialMode.Solid).policy()
        assertFalse(solid.backdrop);assertFalse(solid.ambient)
        val black=RynxAppearance(material=RynxMaterialMode.Amoled).policy()
        assertEquals(0xFF000000L,black.backgroundArgb);assertEquals(0xFF000000L,black.surfaceArgb)
        assertFalse(black.backdrop);assertFalse(black.ambient);assertEquals(0f,black.shadow)
    }
    @Test fun preferencesAreFiniteAndBoundedBeforeReachingTheRenderer() {
        val saved=RynxAppearance(transparency=Float.NaN,blurStrength=Float.POSITIVE_INFINITY,
            refractionIntensity=-1f,highlightIntensity=4f,motionIntensity=Float.NaN).normalized()
        assertEquals(RynxAppearance().transparency,saved.transparency)
        assertEquals(RynxAppearance().blurStrength,saved.blurStrength)
        assertEquals(0f,saved.refractionIntensity);assertEquals(1f,saved.highlightIntensity)
        assertTrue(saved.motionIntensity.isFinite())
    }
    @Test fun reducedMotionAndRenderingPressureNeverReplaceTheSavedMaterial() {
        val glass=RynxAppearance(motion=RynxMotionPreference.FollowSystem)
        assertTrue(glass.reducedMotion(true));assertFalse(glass.reducedMotion(false))
        assertFalse(glass.copy(motion=RynxMotionPreference.Full).reducedMotion(true))
        assertTrue(glass.copy(motion=RynxMotionPreference.Reduced).reducedMotion(false))
        assertTrue(glass.copy(motionIntensity=0f).reducedMotion(false))
        assertTrue(glass.policy(renderingPressure=true).backdrop)
        assertEquals(RynxMaterialMode.LiquidGlass,glass.material)
    }
    @Test fun brandAppearanceOffersTenVectorPalettesAndTenNeutralLiquidFinishes() {
        assertEquals(RynxBrandVariant.RubyRed,RynxAppearance().brandVariant)
        assertEquals(10,RynxBrandVariant.entries.count {it.liquidTextureIndex==null})
        assertEquals((1..10).toList(),RynxBrandVariant.entries.mapNotNull {it.liquidTextureIndex})
    }
}
