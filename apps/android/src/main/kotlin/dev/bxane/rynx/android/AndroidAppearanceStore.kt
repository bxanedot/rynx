package dev.bxane.rynx.android

import android.content.Context
import androidx.core.content.edit
import dev.bxane.rynx.design.*

/** Version-tolerant local preferences. Unknown enum values retain safe defaults. */
internal class AndroidAppearanceStore(context:Context) {
    private val preferences=context.getSharedPreferences("appearance",Context.MODE_PRIVATE)
    fun load():RynxAppearance {
        val defaults=RynxAppearance()
        fun value(key:String,fallback:Float)=try {preferences.getFloat(key,fallback)} catch(_:ClassCastException){fallback}
        fun flag(key:String,fallback:Boolean)=try {preferences.getBoolean(key,fallback)} catch(_:ClassCastException){fallback}
        fun text(key:String)=try {preferences.getString(key,null)} catch(_:ClassCastException){null}
        return RynxAppearance(
            material=RynxMaterialMode.entries.firstOrNull {it.name==text("material")} ?: defaults.material,
            darkTheme=flag("dark",defaults.darkTheme),transparency=value("transparency",defaults.transparency),
            blurStrength=value("blur",defaults.blurStrength),refractionIntensity=value("refraction",defaults.refractionIntensity),
            highlightIntensity=value("highlight",defaults.highlightIntensity),tintStrength=value("tint",defaults.tintStrength),
            borderBrightness=value("border",defaults.borderBrightness),vibrancy=value("vibrancy",defaults.vibrancy),
            shadowIntensity=value("shadow",defaults.shadowIntensity),dynamicColor=flag("dynamic",true),
            motionIntensity=value("motion-intensity",1f),
            motion=RynxMotionPreference.entries.firstOrNull {it.name==text("motion")} ?: defaults.motion,
            brandVariant=RynxBrandVariant.entries.firstOrNull {it.name==text("brand-variant")} ?: defaults.brandVariant,
        ).normalized()
    }
    fun save(value:RynxAppearance) {
        val safe=value.normalized()
        preferences.edit {
            putString("material",safe.material.name);putBoolean("dark",safe.darkTheme)
            putFloat("transparency",safe.transparency);putFloat("blur",safe.blurStrength);putFloat("refraction",safe.refractionIntensity)
            putFloat("highlight",safe.highlightIntensity);putFloat("tint",safe.tintStrength);putFloat("border",safe.borderBrightness)
            putFloat("vibrancy",safe.vibrancy);putFloat("shadow",safe.shadowIntensity);putBoolean("dynamic",safe.dynamicColor)
            putFloat("motion-intensity",safe.motionIntensity);putString("motion",safe.motion.name)
            putString("brand-variant",safe.brandVariant.name)
        }
    }
}
