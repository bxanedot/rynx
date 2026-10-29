package dev.bxane.rynx.android

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LatestPlaybackRequestTest {
    @Test fun selectingAnotherSongPreventsALateNonCooperativeResultFromReplacingIt()=runBlocking {
        val scope=CoroutineScope(coroutineContext+SupervisorJob())
        val result=CompletableDeferred<String>();val started=CompletableDeferred<Unit>()
        val songs=mutableListOf<String>()
        val requests=LatestPlaybackRequest(scope)
        try {
            requests.start(load={started.complete(Unit);withContext(NonCancellable){result.await()}},commit={songs+=it},onFailure={throw AssertionError(it)})
            started.await();requests.cancel();songs+="local selection";result.complete("older catalog selection")
            scope.coroutineContext[Job]!!.children.toList().joinAll()
            assertEquals(listOf("local selection"),songs)
        } finally {scope.cancel()}
    }
    @Test fun aCancelledLoadCannotClearTheNewerLoadingState()=runBlocking {
        val scope=CoroutineScope(coroutineContext+SupervisorJob())
        val old=CompletableDeferred<String>();val latest=CompletableDeferred<String>();val started=CompletableDeferred<Unit>()
        val pending=mutableListOf<Boolean>();val songs=mutableListOf<String>()
        val requests=LatestPlaybackRequest(scope,{pending+=it})
        try {
            requests.start(load={started.complete(Unit);withContext(NonCancellable){old.await()}},commit={songs+=it},onFailure={throw AssertionError(it)})
            started.await()
            requests.start(load={latest.await()},commit={songs+=it},onFailure={throw AssertionError(it)})
            yield();old.complete("old");yield();yield()
            assertTrue("Old completion must not clear the latest load",requests.isPending)
            assertEquals(true,pending.last())
            latest.complete("latest");scope.coroutineContext[Job]!!.children.toList().joinAll()
            assertEquals(listOf("latest"),songs);assertFalse(pending.last())
        } finally {scope.cancel()}
    }
}
