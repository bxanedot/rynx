package dev.bxane.rynx.design

/** Hysteresis prevents a single slow startup frame from toggling material quality. */
class RenderBudget(private val windowSize:Int=48) {
    init {require(windowSize>0)}
    var underPressure=false;private set
    var frames:Long=0;private set
    var missedDeadlines:Long=0;private set
    private var inWindow=0
    private var slow=0
    private var pressuredWindows=0
    private var recoveredWindows=0
    fun frame(durationNanos:Long,refreshRate:Float):Boolean {
        if(durationNanos<=0 || !refreshRate.isFinite() || refreshRate<=0)return underPressure
        val missed=durationNanos>1_000_000_000.0/refreshRate*1.20
        frames++;if(missed){slow++;missedDeadlines++};inWindow++
        if(inWindow>=windowSize) {
            val fraction=slow.toFloat()/inWindow
            pressuredWindows=if(fraction>.25f)pressuredWindows+1 else 0
            recoveredWindows=if(fraction<.08f)recoveredWindows+1 else 0
            if(pressuredWindows>=2)underPressure=true
            if(recoveredWindows>=3)underPressure=false
            slow=0;inWindow=0
        }
        return underPressure
    }
}
