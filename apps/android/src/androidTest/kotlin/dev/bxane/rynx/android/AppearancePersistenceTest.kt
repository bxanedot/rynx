package dev.bxane.rynx.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.bxane.rynx.design.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearancePersistenceTest {
    @Test fun everyMaterialAndOpticalPreferenceSurvivesAStoreRecreation() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val original=AndroidAppearanceStore(context).load()
        try {
            for(mode in RynxMaterialMode.entries) for(variant in listOf(RynxBrandVariant.ChampagneGold,RynxBrandVariant.LiquidFinish07)) {
                val saved=RynxAppearance(material=mode,darkTheme=true,transparency=.41f,blurStrength=.72f,
                    refractionIntensity=.32f,highlightIntensity=.63f,tintStrength=.11f,borderBrightness=.49f,
                    vibrancy=.22f,shadowIntensity=.26f,dynamicColor=false,motionIntensity=.52f,motion=RynxMotionPreference.Reduced,
                    brandVariant=variant)
                AndroidAppearanceStore(context).save(saved)
                assertEquals(saved,AndroidAppearanceStore(context).load())
            }
            context.getSharedPreferences("appearance",0).edit().putString("material","future-material")
                .putString("brand-variant","future-brand").putFloat("blur",Float.NaN).putString("tint","invalid-value").apply()
            val safe=AndroidAppearanceStore(context).load()
            assertEquals(RynxMaterialMode.LiquidGlass,safe.material)
            assertEquals(RynxBrandVariant.RubyRed,safe.brandVariant)
            assertEquals(RynxAppearance().blurStrength,safe.blurStrength)
            assertEquals(RynxAppearance().tintStrength,safe.tintStrength)
        } finally {AndroidAppearanceStore(context).save(original)}
    }
}
