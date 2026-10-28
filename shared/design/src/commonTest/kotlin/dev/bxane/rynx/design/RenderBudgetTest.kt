package dev.bxane.rynx.design

import kotlin.test.*

class RenderBudgetTest {
    @Test fun pressureAndRecoveryRequireSustainedEvidence() {
        val budget=RenderBudget(windowSize=4)
        repeat(4){budget.frame(30_000_000,60f)};assertFalse(budget.underPressure)
        repeat(4){budget.frame(30_000_000,60f)};assertTrue(budget.underPressure)
        repeat(8){budget.frame(5_000_000,60f)};assertTrue(budget.underPressure)
        repeat(4){budget.frame(5_000_000,60f)};assertFalse(budget.underPressure)
        assertEquals(20,budget.frames);assertEquals(8,budget.missedDeadlines)
    }
    @Test fun highRefreshDevicesUseTheirOwnFrameBudgetAndInvalidSamplesAreIgnored() {
        val budget=RenderBudget(windowSize=2)
        repeat(4){budget.frame(12_000_000,120f)};assertTrue(budget.underPressure)
        budget.frame(1,Float.NaN);budget.frame(0,60f)
        assertEquals(4,budget.frames)
    }
}
