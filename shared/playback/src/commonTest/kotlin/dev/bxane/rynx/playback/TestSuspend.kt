package dev.bxane.rynx.playback

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/** Runs the synchronous fake-provider suspend functions used by common contract tests. */
internal fun <T> runSuspend(block: suspend () -> T): T {
    var completed: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) {
            completed = result
        }
    })
    return checkNotNull(completed) { "Test suspend block did not complete synchronously" }.getOrThrow()
}
