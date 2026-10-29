package dev.bxane.rynx.ui

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RynxStartupSplashTest {
    @Test
    fun normalMotionPulsesAndAnimatesTheMarkThreeTimes() = runBlocking {
        val events = mutableListOf<String>()

        runRynxStartupSequence(
            reduceMotion = { false },
            onPulse = { events += "pulse:$it" },
            animatePulse = { events += "animate" },
            wait = { events += "wait:$it" },
            onFinished = { events += "finished" },
        )

        assertEquals(
            listOf(
                "pulse:1", "animate", "wait:100",
                "pulse:2", "animate", "wait:100",
                "pulse:3", "animate", "wait:90", "finished",
            ),
            events,
        )
    }

    @Test
    fun reducedMotionKeepsThreeHapticPulsesButSkipsScaleAnimation() = runBlocking {
        val events = mutableListOf<String>()

        runRynxStartupSequence(
            reduceMotion = { true },
            onPulse = { events += "pulse:$it" },
            animatePulse = { events += "animate" },
            wait = { events += "wait:$it" },
            onFinished = { events += "finished" },
        )

        assertEquals(
            listOf("pulse:1", "wait:100", "pulse:2", "wait:100", "pulse:3", "wait:90", "finished"),
            events,
        )
    }
}
