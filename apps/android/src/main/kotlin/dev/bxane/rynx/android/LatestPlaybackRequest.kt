package dev.bxane.rynx.android

import kotlinx.coroutines.*

/** Owns one pending catalog load; explicit transport intents supersede it. */
internal class LatestPlaybackRequest(private val scope:CoroutineScope,private val onPending:(Boolean)->Unit={}) {
    private var job:Job?=null
    private var generation=0L
    val isPending:Boolean get()=job?.isActive==true
    fun cancel() {generation++;job?.cancel();job=null;onPending(false)}
    fun <T> start(load:suspend ()->T,commit:(T)->Unit,onFailure:(Exception)->Unit) {
        cancel();val request=generation;onPending(true)
        job=scope.launch(start=CoroutineStart.LAZY) {
            try {
                val result=load()
                currentCoroutineContext().ensureActive()
                if(request==generation)commit(result)
            }
            catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Exception){if(request==generation && currentCoroutineContext().isActive)onFailure(failure)}
            finally {if(request==generation){job=null;onPending(false)}}
        }.also {it.start()}
    }
}
