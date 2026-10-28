@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.ui

import androidx.compose.ui.ImageComposeScene
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.PointerEventType
import dev.bxane.rynx.playback.LyricLine
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import dev.bxane.rynx.design.*
import kotlinx.coroutines.*
import kotlin.test.*

class PlayerMorphTest {
    private val actions=object:PlayerActions {
        override fun importFiles(){};override fun play(id:String){};override fun toggle(){};override fun previous(){};override fun next(){}
        override fun seek(fraction:Float){};override fun cycleRepeat(){};override fun removeFromQueue(id:String){};override fun dismissError(){}
        override fun searchYouTubeMusic(query:String){};override fun loadMoreYouTubeMusic(){}
    }
    private var time=System.nanoTime()
    private fun frame(scene:ImageComposeScene){repeat(3){time+=20_000_000;scene.render(time).close()}}
    private fun nodes(node:SemanticsNode):List<SemanticsNode> = listOf(node)+node.children.flatMap(::nodes)
    private fun tag(scene:ImageComposeScene,name:String):SemanticsNode {
        frame(scene)
        return scene.semanticsOwners.flatMap {nodes(it.unmergedRootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==name}
    }
    private fun state():PlayerUiState {
        val track=TrackItem("a","Evening walk","bxane")
        return PlayerUiState(tracks=listOf(track),queue=listOf(track),currentId="a",durationMillis=180_000,reduceMotion=true,
            appearance=RynxAppearance(material=RynxMaterialMode.Solid))
    }
    @Test fun titleDoesNotJumpWhenTheGrowingSurfacePassesSevenHundredDp() {
        val scope=CoroutineScope(Dispatchers.Unconfined);val player=PlayerPresentation(scope)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxFluidPlayer(state(),actions,player)}
        try {
            frame(scene);player.dragBy(-.90f*762f,762f)
            val before=tag(scene,"player-metadata").boundsInRoot.top
            player.dragBy(-.03f*762f,762f)
            val after=tag(scene,"player-metadata").boundsInRoot.top
            assertTrue(kotlin.math.abs(after-before)<35f,"One continuous drag must not teleport metadata: $before -> $after")
        } finally {scene.close();scope.cancel()}
    }
    @Test fun queueArtworkReturnsToTheCapsuleBeforeThePaneIsReset() {
        val scope=CoroutineScope(Dispatchers.Unconfined);val player=PlayerPresentation(scope)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxFluidPlayer(state(),actions,player)}
        try {
            frame(scene);player.open(true,PlayerPane.Queue);frame(scene)
            player.beginDrag();player.dragBy(.95f*762f,762f)
            val art=tag(scene,"player-artwork").boundsInRoot
            val surface=tag(scene,"player-surface").boundsInRoot
            assertTrue(art.width<50f,"Queue artwork must continuously shrink into the mini-player: $art")
            assertTrue(art.top-surface.top<20f,"Artwork must move toward the compact inset before collapse completes")
        } finally {scene.close();scope.cancel()}
    }
    @Test fun fadingQueueAndLyricsDoNotExposeActionsDuringRetraction() {
        for(pane in listOf(PlayerPane.Queue,PlayerPane.Lyrics)) {
            lateinit var player:PlayerPresentation
            val track=TrackItem("b","Window light")
            val state=state().copy(queue=state().queue+track,lyrics=listOf(LyricLine(0,"Evening begins")),lyricsTrackId="a",lyricsImportAvailable=true,queueEditingAvailable=true,reduceMotion=false)
            val scene=ImageComposeScene(390,844,Density(1f)){
                val scope=rememberCoroutineScope()
                val presentation=remember {PlayerPresentation(scope)}
                SideEffect {player=presentation}
                RynxFluidPlayer(state,actions,presentation)
            }
            try {
                frame(scene);player.open(true,pane);repeat(12){frame(scene)}
                player.close(false);frame(scene)
                assertTrue(player.progress in .68f.. .96f,"The test must inspect a visible, partly retracted pane: ${player.progress}")
                fun accessible(node:SemanticsNode):List<SemanticsNode> = if(node.config.contains(SemanticsProperties.HideFromAccessibility))emptyList() else listOf(node)+node.children.flatMap(::accessible)
                val labels=scene.semanticsOwners.flatMap {accessible(it.rootSemanticsNode)}.flatMap {it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()}
                assertFalse(labels.any {it.startsWith("Play Window light") || it.startsWith("Seek to lyric")},"Fading $pane controls must leave the accessibility tree: $labels")
            } finally {scene.close()}
        }
    }
    @Test fun primaryTransportAndMetadataNeverOverlapDuringExpansion() {
        val scope=CoroutineScope(Dispatchers.Unconfined);val player=PlayerPresentation(scope)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxFluidPlayer(state(),actions,player)}
        try {
            frame(scene)
            for(p in listOf(0f,.025f,.05f,.075f,.1f,.3f,.5f,.7f,.9f,1f)) {
                player.dragBy((player.progress-p)*762f,762f)
                val metadata=tag(scene,"player-metadata").boundsInRoot
                val play=scene.semanticsOwners.flatMap {nodes(it.unmergedRootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Play")==true}.boundsInRoot
                assertFalse(metadata.overlaps(play),"Metadata and transport must remain separate at progress $p: $metadata / $play")
            }
        } finally {scene.close();scope.cancel()}
    }

}
