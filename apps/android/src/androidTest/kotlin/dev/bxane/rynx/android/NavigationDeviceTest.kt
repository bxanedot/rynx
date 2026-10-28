package dev.bxane.rynx.android

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Native checks for the Compose search handoff and Android back-stack behavior. */
@RunWith(AndroidJUnit4::class)
class NavigationDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun find(label: String): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.contentDescription?.toString()?.let { it == label || it.startsWith("$label, ") } == true) return node
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                visit(child)?.let { return it }
            }
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
    }

    private fun eventually(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError(message)
    }

    private fun click(label: String) {
        eventually("Missing navigation action: $label") { find(label) != null }
        var target = find(label)
        while (target != null) {
            if (target.isEnabled && target.isClickable && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
            target = target.parent
        }
        throw AssertionError("Could not activate navigation action: $label")
    }

    @Test fun searchKeepsItsQueryAndBackReturnsToThePreviousTab() {
        ActivityScenario.launch(MainActivity::class.java).use {
            click("Library")
            click("Search")
            eventually("Search input did not finish its expansion") {
                find("Search your music")?.let { node -> node.isEnabled && node.actionList.any { action -> action.id == AccessibilityNodeInfo.ACTION_SET_TEXT } } == true
            }
            val query = "navigation survives back"
            val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query) }
            assertTrue("Search must accept accessibility text input", find("Search your music")!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))

            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            SystemClock.sleep(350)
            if (find("Library")?.isSelected != true) instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            eventually("Android back must return to the tab that opened Search") { find("Library")?.isSelected == true }

            click("Search")
            eventually("Search query was lost while returning from the prior tab") {
                find("Search your music")?.text?.toString() == query
            }
            assertEquals(query, find("Search your music")?.text?.toString())
        }
    }
}
