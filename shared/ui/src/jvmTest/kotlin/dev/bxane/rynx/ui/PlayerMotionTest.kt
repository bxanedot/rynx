package dev.bxane.rynx.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.test.*

class PlayerMotionTest {
    @Test fun releaseVelocityHasTheSameMeaningAtEveryScreenDensity() {
        val scope=CoroutineScope(Dispatchers.Unconfined)
        try {
            val ordinary=PlayerPresentation(scope,initialProgress=.2f)
            val dense=PlayerPresentation(scope,initialProgress=.2f)
            ordinary.finishDrag(-800f,700f,true)
            dense.finishDrag(-2400f,2100f,true)
            assertEquals(1f,ordinary.progress,"An upward fling should open from a partial expansion")
            assertEquals(ordinary.progress,dense.progress,"Density must not change release decisions")
        } finally {scope.cancel()}
    }
    @Test fun reducedMotionSettlesToTheNearestSurfaceWithoutMovingTheQueue() {
        val scope=CoroutineScope(Dispatchers.Unconfined)
        try {
            val player=PlayerPresentation(scope,initialProgress=.65f,initialPane=PlayerPane.Queue)
            player.finishDrag(0f,700f,true)
            assertEquals(1f,player.progress)
            assertEquals(PlayerPane.Queue,player.pane)
        } finally {scope.cancel()}
    }
}
