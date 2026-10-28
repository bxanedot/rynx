package dev.bxane.rynx.android

import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window
import dev.bxane.rynx.design.RenderBudget

/** Frame-duration evidence selects a cheaper Glass profile, never a different saved material. */
internal class AndroidRenderingMonitor(private val window:Window,private val onPressure:(Boolean)->Unit) {
    private var thread:HandlerThread?=null
    private val budget=RenderBudget()
    private var previous=false
    private val listener=Window.OnFrameMetricsAvailableListener {_,metrics,_->
        if(metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME)!=1L) {
            val refresh=window.decorView.display?.refreshRate ?: 60f
            val pressure=budget.frame(metrics.getMetric(FrameMetrics.TOTAL_DURATION),refresh)
            if(pressure!=previous){previous=pressure;onPressure(pressure)}
        }
    }
    fun start() {
        if(thread!=null)return
        val worker=HandlerThread("rynx-frame-monitor").apply {start()}
        thread=worker;window.addOnFrameMetricsAvailableListener(listener,Handler(worker.looper))
    }
    fun stop() {
        if(thread==null)return
        window.removeOnFrameMetricsAvailableListener(listener)
        thread?.quitSafely();thread=null
    }
}
