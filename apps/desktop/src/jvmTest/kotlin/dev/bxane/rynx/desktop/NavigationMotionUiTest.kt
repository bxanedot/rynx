@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import dev.bxane.rynx.design.RynxAppearance
import dev.bxane.rynx.design.RynxMaterialMode
import dev.bxane.rynx.ui.*
import kotlin.test.*

class NavigationMotionUiTest {
    private class Actions : PlayerActions {
        override fun importFiles() {}
        override fun play(id: String) {}
        override fun toggle() {}
        override fun previous() {}
        override fun next() {}
        override fun seek(fraction: Float) {}
        override fun cycleRepeat() {}
        override fun removeFromQueue(id: String) {}
        override fun dismissError() {}
        override fun searchYouTubeMusic(query: String) {}
        override fun loadMoreYouTubeMusic() {}
    }

    private var clock = System.nanoTime()
    private fun frame(scene: ImageComposeScene) {
        repeat(3) { clock += 20_000_000; scene.render(clock).close() }
    }

    private fun nodes(node: SemanticsNode): List<SemanticsNode> =
        if (node.config.contains(SemanticsProperties.HideFromAccessibility)) emptyList()
        else listOf(node) + node.children.flatMap(::nodes)

    private fun find(scene: ImageComposeScene, description: String): SemanticsNode {
        frame(scene)
        return scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }.firstOrNull {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { text -> text.contains(description) } == true
        } ?: error("Missing navigation control: $description")
    }

    private fun findTag(scene: ImageComposeScene, tag: String): SemanticsNode {
        frame(scene)
        return scene.semanticsOwners.flatMap { it.unmergedRootSemanticsNode.children.flatMap(::allNodes) }
            .firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
            ?: scene.semanticsOwners.flatMap { allNodes(it.unmergedRootSemanticsNode) }
                .firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
            ?: error("Missing semantics test tag: $tag")
    }

    private fun allNodes(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::allNodes)

    private fun click(scene: ImageComposeScene, description: String) {
        val node = find(scene, description)
        assertTrue(node.config[SemanticsActions.OnClick].action!!.invoke(), "Could not activate $description")
        frame(scene)
    }

    private fun swipeVertically(scene: ImageComposeScene, distance: Float) {
        val start = findTag(scene, "navigation-dock").boundsInRoot.center
        val end = start + Offset(0f, distance)
        scene.sendPointerEvent(PointerEventType.Press, start, timeMillis = clock / 1_000_000)
        clock += 40_000_000
        scene.sendPointerEvent(PointerEventType.Move, start + (end - start) * .55f, timeMillis = clock / 1_000_000)
        clock += 40_000_000
        scene.sendPointerEvent(PointerEventType.Move, end, timeMillis = clock / 1_000_000)
        scene.sendPointerEvent(PointerEventType.Release, end, timeMillis = clock / 1_000_000 + 16)
        frame(scene)
    }

    private fun app(state: PlayerUiState = PlayerUiState(reduceMotion = true, appearance = RynxAppearance(material = RynxMaterialMode.Solid))): ImageComposeScene = ImageComposeScene(390, 844, Density(1f)) {
        RynxApp(
            state,
            Actions(),
        )
    }

    @Test fun repeatedSelectionRetractsTheDockAndSearchReturnsToThePreviousTab() {
        val scene = app()
        try {
            click(scene, "Library")
            click(scene, "Library")
            assertEquals("Navigation collapsed", findTag(scene, "navigation-dock").config[SemanticsProperties.StateDescription])
            assertNotNull(find(scene, "Search"))

            click(scene, "Search")
            assertNotNull(find(scene, "Search your music"))
            click(scene, "Return to Library")
            val library = find(scene, "Library")
            assertEquals(true, library.config.getOrNull(SemanticsProperties.Selected))
        } finally {
            scene.close()
        }
    }

    @Test fun horizontalDragSettlesTheElasticIndicatorOnTheDestination() {
        val scene = app()
        try {
            val start = find(scene, "Listen Now").boundsInRoot.center
            val end = find(scene, "Library").boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Press, start, timeMillis = clock / 1_000_000)
            repeat(12) { step ->
                clock += 24_000_000
                val fraction = (step + 1) / 12f
                scene.sendPointerEvent(PointerEventType.Move, start + (end - start) * fraction, timeMillis = clock / 1_000_000)
                scene.render(clock).close()
            }
            clock += 80_000_000
            scene.sendPointerEvent(PointerEventType.Move, end, timeMillis = clock / 1_000_000)
            scene.sendPointerEvent(PointerEventType.Release, end, timeMillis = clock / 1_000_000 + 16)
            frame(scene)
            assertEquals(true, find(scene, "Library").config.getOrNull(SemanticsProperties.Selected))
        } finally {
            scene.close()
        }
    }

    @Test fun rapidSelectionChangesRetargetTheSameIndicatorInsideTheBar() {
        val scene = app(PlayerUiState(reduceMotion = false, appearance = RynxAppearance(material = RynxMaterialMode.Solid)))
        try {
            click(scene, "Browse")
            val inFlight = findTag(scene, "navigation-selection-indicator").boundsInRoot
            click(scene, "Library")
            frame(scene)
            val settledTarget = findTag(scene, "navigation-selection-indicator").boundsInRoot
            val bar = findTag(scene, "navigation-surface").boundsInRoot
            assertEquals(true, find(scene, "Library").config.getOrNull(SemanticsProperties.Selected))
            assertTrue(inFlight.width > 0f && settledTarget.width > 0f)
            assertTrue(settledTarget.left >= bar.left - 1f && settledTarget.right <= bar.right + 1f)
        } finally {
            scene.close()
        }
    }

    @Test fun verticalSwipeRetractsAndRestoresTheDock() {
        val scene = app()
        try {
            swipeVertically(scene, -52f)
            assertEquals("Navigation collapsed", findTag(scene, "navigation-dock").config[SemanticsProperties.StateDescription])
            swipeVertically(scene, 52f)
            assertEquals("Navigation expanded", findTag(scene, "navigation-dock").config[SemanticsProperties.StateDescription])
        } finally {
            scene.close()
        }
    }

    @Test fun playerRevealOverridesButThenRestoresTheUsersCollapsedNavigationChoice() {
        val track = TrackItem("a", "Evening walk", "bxane")
        val appState = mutableStateOf(
            PlayerUiState(reduceMotion = true, appearance = RynxAppearance(material = RynxMaterialMode.Solid)),
        )
        val scene = ImageComposeScene(390, 844, Density(1f)) {
            RynxApp(appState.value, Actions())
        }
        try {
            val dock = findTag(scene, "navigation-dock")
            assertTrue(dock.config[SemanticsActions.CustomActions].first { it.label == "Collapse navigation" }.action.invoke())
            appState.value = PlayerUiState(
                tracks = listOf(track), queue = listOf(track), currentId = track.id, playing = true,
                durationMillis = 180_000, reduceMotion = true,
                appearance = RynxAppearance(material = RynxMaterialMode.Solid),
            )
            frame(scene)
            click(scene, "Open Now Playing")
            assertNotNull(find(scene, "Library"))
            assertEquals("Navigation expanded", findTag(scene, "navigation-dock").config[SemanticsProperties.StateDescription])
            click(scene, "Library")
            assertFailsWith<IllegalStateException> { findTag(scene, "navigation-dock") }
            appState.value = PlayerUiState(reduceMotion = true, appearance = RynxAppearance(material = RynxMaterialMode.Solid))
            frame(scene)
            assertEquals("Navigation collapsed", findTag(scene, "navigation-dock").config[SemanticsProperties.StateDescription])
        } finally {
            scene.close()
        }
    }

    @Test fun accessibilityCanExpandTheDockWithoutMovingPlayerState() {
        val scene = app()
        try {
            val dock = findTag(scene, "navigation-dock")
            val action = dock.config[SemanticsActions.CustomActions].first { it.label == "Collapse navigation" }
            assertTrue(action.action.invoke())
            assertEquals("Navigation collapsed", findTag(scene, "navigation-dock").config[SemanticsProperties.StateDescription])
            val expand = findTag(scene, "navigation-dock").config[SemanticsActions.CustomActions].first { it.label == "Expand navigation" }
            assertTrue(expand.action.invoke())
            assertEquals("Navigation expanded", findTag(scene, "navigation-dock").config[SemanticsProperties.StateDescription])
        } finally {
            scene.close()
        }
    }
}
