@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import dev.bxane.rynx.ui.*
import java.io.File
import kotlin.test.*

class AppRenderingTest {
    private class Actions:PlayerActions {
        var imports=0;var toggles=0;var removed:String?=null
        override fun importFiles(){imports++}
        override fun toggle(){toggles++}
        override fun removeFromQueue(id:String){removed=id}
        override fun play(id:String){};override fun previous(){};override fun next(){}
        override fun seek(fraction:Float){};override fun cycleRepeat(){};override fun dismissError(){}
    }
    private fun descendants(node:SemanticsNode):List<SemanticsNode> = listOf(node)+node.children.flatMap(::descendants)
    private fun click(scene:ImageComposeScene,label:String) {
        val node=scene.semanticsOwners.flatMap{descendants(it.rootSemanticsNode)}.firstOrNull{
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true
        }?:error("No accessible control: $label")
        assertTrue(node.config[SemanticsActions.OnClick].action!!.invoke())
        scene.render().close()
    }
    private fun capture(scene:ImageComposeScene,name:String) {
        val output=File("build/reports/screenshots/$name.png").apply{parentFile.mkdirs()}
        scene.render().use{image->image.encodeToData()!!.use{output.writeBytes(it.bytes)}}
        assertTrue(output.length()>2000,"Rendered frame must contain content")
    }
    @Test fun emptyLibraryImportIsAccessibleAndInvokesPicker() {
        val actions=Actions()
        val scene=ImageComposeScene(1120,760,Density(1f)){RynxApp(PlayerUiState(),actions)}
        try {
            capture(scene,"desktop-empty")
            click(scene,"Import audio files")
            assertEquals(1,actions.imports)
        } finally{scene.close()}
    }
    @Test fun phonePlayerQueueAndTransportRenderAndInvokeActions() {
        val actions=Actions()
        val tracks=listOf(TrackItem("a","Evening walk"),TrackItem("b","Window light"),TrackItem("c","Morning tide"))
        val state=PlayerUiState(tracks,tracks,"a",true,35_000,180_000)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,actions)}
        try {
            capture(scene,"phone-library")
            click(scene,"Pause")
            assertEquals(1,actions.toggles)
            click(scene,"Open Now Playing")
            capture(scene,"phone-player")
            click(scene,"Show queue")
            capture(scene,"phone-queue")
            click(scene,"Remove Window light from queue")
            assertEquals("b",actions.removed)
        } finally{scene.close()}
    }
}
