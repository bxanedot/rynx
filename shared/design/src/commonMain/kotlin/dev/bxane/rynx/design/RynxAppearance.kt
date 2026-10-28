package dev.bxane.rynx.design

enum class RynxMaterialMode(val label: String) { LiquidGlass("Liquid Glass"), Solid("Solid"), Amoled("True AMOLED") }
enum class RynxMotionPreference { FollowSystem, Full, Reduced }

data class RynxAppearance(
    val material: RynxMaterialMode = RynxMaterialMode.LiquidGlass,
    val darkTheme: Boolean = false,
    val transparency: Float = .64f,
    val blurStrength: Float = .62f,
    val refractionIntensity: Float = .38f,
    val highlightIntensity: Float = .42f,
    val tintStrength: Float = .32f,
    val borderBrightness: Float = .24f,
    val vibrancy: Float = .5f,
    val shadowIntensity: Float = .35f,
    val dynamicColor: Boolean = true,
    val motionIntensity: Float = 1f,
    val motion: RynxMotionPreference = RynxMotionPreference.FollowSystem,
    val brandVariant: RynxBrandVariant = RynxBrandVariant.RubyRed,
) {
    fun normalized(): RynxAppearance {
        fun Float.unit(fallback: Float) = if (isFinite()) coerceIn(0f, 1f) else fallback
        return copy(transparency=transparency.unit(.64f),blurStrength=blurStrength.unit(.62f),
            refractionIntensity=refractionIntensity.unit(.38f),highlightIntensity=highlightIntensity.unit(.42f),
            tintStrength=tintStrength.unit(.32f),borderBrightness=borderBrightness.unit(.24f),vibrancy=vibrancy.unit(.5f),
            shadowIntensity=shadowIntensity.unit(.35f),motionIntensity=motionIntensity.unit(1f))
    }
    fun reducedMotion(system: Boolean): Boolean = motionIntensity <= 0f || when(motion) {
        RynxMotionPreference.FollowSystem -> system
        RynxMotionPreference.Full -> false
        RynxMotionPreference.Reduced -> true
    }
    fun policy(renderingPressure: Boolean = false): RynxMaterialPolicy {
        val safe=normalized()
        val black=material==RynxMaterialMode.Amoled
        val dark=darkTheme || black
        return RynxMaterialPolicy(
            backgroundArgb=if(black)0xFF000000L else if(dark)0xFF141417L else 0xFFF9F9FCL,
            surfaceArgb=if(black)0xFF000000L else if(dark)0xFF26262CL else 0xFFF0F0F5L,
            backdrop=material==RynxMaterialMode.LiquidGlass,
            ambient=material==RynxMaterialMode.LiquidGlass && dynamicColor,
            shadow=if(black)0f else safe.shadowIntensity,
            lowCost=renderingPressure,
        )
    }
}

data class RynxMaterialPolicy(val backgroundArgb:Long,val surfaceArgb:Long,val backdrop:Boolean,val ambient:Boolean,val shadow:Float,val lowCost:Boolean)

/** Authored motion values. No measured Apple or Vyon trajectory is claimed. */
object RynxMotionTokens {
    const val PressStiffness = 700f
    const val PressDamping = .86f
    const val SheetStiffness = 420f
    const val SheetDamping = .91f
    const val SelectionStiffness = 520f
    const val SelectionDamping = .86f
    const val PressScale = .965f
    const val ColorMillis = 240
    const val ArtworkMillis = 420
    const val LyricsWordMillis = 180
    const val SliderStiffness = 900f
    const val QueueStiffness = 600f
    const val QueueDamping = .90f
}
