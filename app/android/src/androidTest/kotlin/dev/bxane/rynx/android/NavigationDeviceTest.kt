package dev.bxane.rynx.android

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
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

    private fun findVisibleText(label: String): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.text?.toString() == label) return node
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                visit(child)?.let { return it }
            }
            return null
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
    }

    private fun findEditableField(): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isEditable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }) return node
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
        throw AssertionError("$message. ${accessibilitySnapshot()}")
    }

    private fun appearsWithin(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            SystemClock.sleep(50)
        }
        return condition()
    }

    private fun accessibilitySnapshot(): String {
        val root=instrumentation.uiAutomation.rootInActiveWindow ?: return "No active accessibility window."
        val nodes=mutableListOf<String>()
        fun visit(node:AccessibilityNodeInfo) {
            val label=node.contentDescription?.toString()
            val text=node.text?.toString()
            val actions=node.actionList.map { it.id }
            val bounds=android.graphics.Rect().also(node::getBoundsInScreen)
            if(label!=null || text!=null || node.isEditable || AccessibilityNodeInfo.ACTION_SET_TEXT in actions) {
                nodes += "class=${node.className}, description=$label, text=$text, selected=${node.isSelected}, enabled=${node.isEnabled}, clickable=${node.isClickable}, bounds=$bounds, editable=${node.isEditable}, actions=$actions"
            }
            for(index in 0 until node.childCount)node.getChild(index)?.let(::visit)
        }
        visit(root)
        return "Active package=${root.packageName}; accessibility nodes=${nodes.take(100)}"
    }

    private fun startMainActivity(): String {
        val packageName = instrumentation.targetContext.packageName
        return shell("am start -W -n $packageName/${MainActivity::class.java.name}")
    }

    private fun keepMainActivityInForeground() {
        val packageName = instrumentation.targetContext.packageName
        if (instrumentation.uiAutomation.rootInActiveWindow?.packageName == packageName) return
        val launch = startMainActivity()
        if (!launch.contains("Status: ok")) throw AssertionError("Android did not foreground Rynx: $launch")
        instrumentation.waitForIdleSync()
    }

    private fun click(label: String) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            keepMainActivityInForeground()
            var target = find(label)
            while (target != null) {
                if (target.isEnabled && target.isClickable && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
                target = target.parent
            }
            SystemClock.sleep(100)
        }
        throw AssertionError("Could not activate navigation action: $label. ${accessibilitySnapshot()}")
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }

    @Test fun searchKeepsItsQueryAndBackReturnsToThePreviousTab() {
        val packageName = instrumentation.targetContext.packageName
        // Launch from Android itself so the real app task is foregrounded. Do not force-stop:
        // instrumentation runs in the target process on some API levels.
        val launch = startMainActivity()
        assertTrue("Android did not launch Rynx: $launch", launch.contains("Status: ok"))
        instrumentation.waitForIdleSync()
        eventually("Rynx did not become the foreground accessibility window") {
            instrumentation.uiAutomation.rootInActiveWindow?.packageName == packageName
        }

        click("Library")
        eventually("Library screen did not open") { findVisibleText("Your music, right here") != null }
        click("Search")
        eventually("Search screen did not open") { findVisibleText("YouTube Music") != null }
        eventually("Search input did not finish its expansion") {
            findEditableField()?.let { node -> node.isEnabled && node.actionList.any { action -> action.id == AccessibilityNodeInfo.ACTION_SET_TEXT } } == true
        }
        val query = "navigation survives back"
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query) }
        assertTrue("Search must accept accessibility text input", findEditableField()!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        if (!appearsWithin(2_000) { findVisibleText("Your music, right here") != null }) {
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        }
        eventually("Android back must return to the tab that opened Search") { findVisibleText("Your music, right here") != null }

        click("Search")
        var restoredQuery:String? = null
        eventually("Search query was lost while returning from the prior tab") {
            restoredQuery = findEditableField()?.text?.toString()
            restoredQuery == query
        }
        assertEquals(query, restoredQuery)
    }
}
