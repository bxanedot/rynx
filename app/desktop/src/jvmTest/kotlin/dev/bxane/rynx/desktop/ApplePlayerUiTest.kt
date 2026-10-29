@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.desktop

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import dev.bxane.rynx.ui.*
import dev.bxane.rynx.playback.LyricLine
import java.io.File
import dev.bxane.rynx.design.RynxAppearance
import dev.bxane.rynx.design.RynxMaterialMode
import kotlin.test.*

class ApplePlayerUiTest {
    private class Actions : PlayerActions {
        var requestedVolume=-1f; var outputRequests=0; var shuffles=0; var repeats=0; var nexts=0
        var moved:Pair<String,Int>?=null; var removed:String?=null; var cleared=0
        var seeked=-1f;var lyricImports=0
        override fun importFiles() {} ; override fun play(id:String) {} ; override fun toggle() {}
        override fun previous() {} ; override fun next() { nexts++ }
        override fun seek(fraction:Float) { seeked=fraction } ; override fun dismissError() {}
        override fun cycleRepeat() { repeats++ }
        override fun removeFromQueue(id:String) { removed=id }
        override fun searchYouTubeMusic(query:String) {} ; override fun loadMoreYouTubeMusic() {}
        override fun setVolume(fraction:Float) { requestedVolume=fraction }
        override fun showAudioOutput() { outputRequests++ }
        override fun toggleShuffle() { shuffles++ }
        override fun moveInQueue(id:String,toIndex:Int) { moved=id to toIndex }
        override fun importLyrics() { lyricImports++ }
        override fun clearUpcomingQueue() { cleared++ }
    }
    private var clock=System.nanoTime()
    private fun frame(scene:ImageComposeScene,advance:Long=16_666_667) { clock+=advance;scene.render(clock).close() }
    private fun settle(scene:ImageComposeScene) { frame(scene);frame(scene,1_000_000_000);frame(scene,1_000_000_000) }
    private fun nodes(node:SemanticsNode):List<SemanticsNode> = if(node.config.contains(SemanticsProperties.HideFromAccessibility)) emptyList() else listOf(node)+node.children.flatMap(::nodes)
    private fun find(scene:ImageComposeScene,label:String):SemanticsNode {
        settle(scene)
        return scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }.firstOrNull {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true
        } ?: error("Missing player control: $label")
    }
    private fun click(scene:ImageComposeScene,label:String) { assertTrue(find(scene,label).config[SemanticsActions.OnClick].action!!.invoke());settle(scene) }
    private fun drag(scene:ImageComposeScene,from:Offset,to:Offset) {
        scene.sendPointerEvent(PointerEventType.Press,from,timeMillis=clock/1_000_000)
        repeat(10) { index ->
            clock+=20_000_000
            scene.sendPointerEvent(PointerEventType.Move,from+(to-from)*((index+1)/10f),timeMillis=clock/1_000_000)
            frame(scene)
        }
        scene.sendPointerEvent(PointerEventType.Release,to,timeMillis=clock/1_000_000+20)
        settle(scene)
    }
    private val tracks=listOf(TrackItem("a","Evening walk","bxane"),TrackItem("b","Window light"),TrackItem("c","Morning tide"))
    private fun state()=PlayerUiState(tracks=tracks,queue=tracks,currentId="a",playing=true,
        appearance=RynxAppearance(material=RynxMaterialMode.Solid),positionMillis=35_000,durationMillis=180_000,volume=.25f,volumeControlAvailable=true,
        audioOutputAvailable=true,shuffleAvailable=true,queueEditingAvailable=true,reduceMotion=true)

    @Test fun playerVolumeAndOutputControlsReachTheHost() {
        val actions=Actions();val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state(),actions)}
        try {
            click(scene,"Open Now Playing")
            val volume=find(scene,"Playback volume")
            assertTrue(volume.config[SemanticsActions.SetProgress].action!!(.7f))
            assertEquals(.7f,actions.requestedVolume)
            assertEquals(.25f,volume.config[SemanticsProperties.ProgressBarRangeInfo].current)
            click(scene,"Audio output")
            assertEquals(1,actions.outputRequests)
            val screenshot=File("build/reports/screenshots/phone-apple-player.png").apply { parentFile.mkdirs() }
            scene.render(clock+1_000_000).use { image -> image.encodeToData()!!.use { screenshot.writeBytes(it.bytes) } }
        } finally {scene.close()}
    }
    @Test fun queueControlsEditTheRealQueueWithoutReplacingTransport() {
        val actions=Actions();val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state(),actions)}
        try {
            click(scene,"Open Now Playing");click(scene,"Show queue")
            click(scene,"Shuffle");click(scene,"Change repeat mode, currently Off")
            assertEquals(1,actions.shuffles);assertEquals(1,actions.repeats)
            val reorder=find(scene,"Reorder Morning tide")
            val up=reorder.config[SemanticsActions.CustomActions].first { it.label=="Move up" }
            assertTrue(up.action());assertEquals("c" to 1,actions.moved)
            click(scene,"Next track");assertEquals(1,actions.nexts)
        } finally {scene.close()}
    }
    @Test fun swipingAQueueRowRevealsItsRemoveAction() {
        val actions=Actions();val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state(),actions)}
        try {
            click(scene,"Open Now Playing");click(scene,"Show queue")
            val row=find(scene,"Play Window light from queue").boundsInRoot
            drag(scene,row.center+Offset(60f,0f),row.center-Offset(100f,0f))
            click(scene,"Remove Window light from queue")
            assertEquals("b",actions.removed)
        } finally {scene.close()}
    }
    @Test fun titleSwipeSkipsAndDismissPreservesTheCurrentSong() {
        val actions=Actions();val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state(),actions)}
        try {
            click(scene,"Open Now Playing")
            val title=find(scene,"Track information").boundsInRoot
            drag(scene,title.center+Offset(65f,0f),title.center-Offset(65f,0f))
            assertEquals(1,actions.nexts)
            click(scene,"Close Now Playing")
            assertNotNull(find(scene,"Open Now Playing"))
        } finally {scene.close()}
    }
    @Test fun largeTextStillLeavesTheMainPlayerControlsOnScreen() {
        val scene=ImageComposeScene(360,640,Density(1f,1.6f)){RynxApp(state(),Actions())}
        try {
            click(scene,"Open Now Playing")
            for(label in listOf("Pause","Playback position","Playback volume","Show queue")) {
                val bounds=find(scene,label).boundsInRoot
                assertTrue(bounds.top>=0 && bounds.bottom<=640f,"$label must remain on screen: $bounds")
            }
        } finally {scene.close()}
    }
    @Test fun timedLyricsCanSeekTheSongAndEmptyLyricsHaveARealImportAction() {
        val actions=Actions()
        val state=state().copy(lyrics=listOf(LyricLine(0,"Evening begins"),LyricLine(60_000,"Moonlit road")),lyricsTrackId="a",lyricsImportAvailable=true)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,actions)}
        try {
            click(scene,"Open Now Playing");click(scene,"Show lyrics")
            click(scene,"Seek to lyric Moonlit road")
            assertEquals(1f/3f,actions.seeked)
        } finally {scene.close()}
        val empty=ImageComposeScene(390,844,Density(1f)){RynxApp(state.copy(lyrics=emptyList()),actions)}
        try {
            click(empty,"Open Now Playing");click(empty,"Show lyrics");click(empty,"Add timed lyrics")
            assertEquals(1,actions.lyricImports)
        } finally {empty.close()}
    }
    @Test fun mainControlsUseTheObservedPlayerPositions() {
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state(),Actions())}
        try {
            click(scene,"Open Now Playing")
            assertTrue(find(scene,"Pause").boundsInRoot.center.y in 565f..571f)
            assertTrue(find(scene,"Playback position").boundsInRoot.center.y in 475f..482f)
            assertTrue(find(scene,"Playback volume").boundsInRoot.center.y in 650f..658f)
        } finally {scene.close()}
    }
    @Test fun scrubPreviewCommitsOnlyOnReleaseAndUnknownDurationIsNotSeekable() {
        val actions=Actions();val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state(),actions)}
        try {
            click(scene,"Open Now Playing")
            val bounds=find(scene,"Playback position").boundsInRoot
            val start=Offset(bounds.left+bounds.width*.2f,bounds.center.y)
            val end=Offset(bounds.left+bounds.width*.8f,bounds.center.y)
            scene.sendPointerEvent(PointerEventType.Press,start,timeMillis=clock/1_000_000)
            repeat(6) { index ->
                clock+=30_000_000
                scene.sendPointerEvent(PointerEventType.Move,start+(end-start)*((index+1)/6f),timeMillis=clock/1_000_000)
                frame(scene)
            }
            assertEquals(-1f,actions.seeked,"Dragging should preview rather than spam Media3 seeks")
            scene.sendPointerEvent(PointerEventType.Release,end,timeMillis=clock/1_000_000+30)
            settle(scene)
            assertTrue(actions.seeked in .78f.. .82f)
        } finally {scene.close()}
        val unknown=ImageComposeScene(390,844,Density(1f)){RynxApp(state().copy(durationMillis=0),Actions())}
        try {
            click(unknown,"Open Now Playing")
            assertFalse(find(unknown,"Playback position").config.contains(SemanticsActions.SetProgress))
        } finally {unknown.close()}
    }
    @Test fun draggingTheArtworkDownCollapsesThePlayer() {
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state().copy(reduceMotion=false),Actions())}
        try {
            click(scene,"Open Now Playing")
            drag(scene,Offset(195f,170f),Offset(195f,540f))
            assertNotNull(find(scene,"Open Now Playing"))
        } finally {scene.close()}
    }
    @Test fun reversingTheDragBeforeReleaseKeepsThePlayerExpanded() {
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state().copy(reduceMotion=false),Actions())}
        try {
            click(scene,"Open Now Playing")
            val start=Offset(195f,170f)
            scene.sendPointerEvent(PointerEventType.Press,start,timeMillis=clock/1_000_000)
            for(y in listOf(210f,280f,340f,390f,330f,280f,230f,190f)) {
                clock+=30_000_000
                scene.sendPointerEvent(PointerEventType.Move,Offset(195f,y),timeMillis=clock/1_000_000);frame(scene)
            }
            scene.sendPointerEvent(PointerEventType.Release,Offset(195f,190f),timeMillis=clock/1_000_000+20)
            settle(scene)
            assertNotNull(find(scene,"Playback volume"))
            assertTrue(find(scene,"Pause").boundsInRoot.center.y in 565f..571f)
        } finally {scene.close()}
    }
    @Test fun aTrackChangeCancelsAnInFlightSeek() {
        val actions=Actions();val current=mutableStateOf(state())
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(current.value,actions)}
        try {
            click(scene,"Open Now Playing")
            val bounds=find(scene,"Playback position").boundsInRoot
            val start=Offset(bounds.left+bounds.width*.2f,bounds.center.y)
            val end=Offset(bounds.left+bounds.width*.8f,bounds.center.y)
            scene.sendPointerEvent(PointerEventType.Press,start,timeMillis=clock/1_000_000)
            repeat(6) { index ->
                clock+=30_000_000
                scene.sendPointerEvent(PointerEventType.Move,start+(end-start)*((index+1)/6f),timeMillis=clock/1_000_000);frame(scene)
            }
            current.value=current.value.copy(currentId="b",positionMillis=9_000,durationMillis=210_000)
            settle(scene)
            scene.sendPointerEvent(PointerEventType.Release,end,timeMillis=clock/1_000_000+30)
            settle(scene)
            assertEquals(-1f,actions.seeked,"A gesture that belonged to track A must never seek track B")
            assertEquals(9_000f/210_000f,find(scene,"Playback position").config[SemanticsProperties.ProgressBarRangeInfo].current,.001f)
        } finally {scene.close()}
    }
    @Test fun theExpandedPlayerBlocksClicksToTheLibraryBehindIt() {
        val actions=Actions();val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state(),actions)}
        try {
            val underlying=find(scene,"Find music").boundsInRoot.center
            click(scene,"Open Now Playing")
            scene.sendPointerEvent(PointerEventType.Press,underlying,timeMillis=clock/1_000_000)
            scene.sendPointerEvent(PointerEventType.Release,underlying,timeMillis=clock/1_000_000+30)
            settle(scene);click(scene,"Close Now Playing")
            assertNotNull(find(scene,"Find music"),"A player-background tap must not switch the underlying tab")
        } finally {scene.close()}
    }

    @Test fun shuffledQueueUsesPlaybackOrderAndEditsTheOriginalTimelineEntry() {
        val actions=Actions()
        val state=state().copy(currentId="c",shuffleEnabled=true,queueOrder=listOf(2,0,1))
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,actions)}
        try {
            click(scene,"Open Now Playing");click(scene,"Show queue")
            val a=find(scene,"Play Evening walk from queue")
            val b=find(scene,"Play Window light from queue")
            assertTrue(a.boundsInRoot.top < b.boundsInRoot.top,"Upcoming tracks must follow shuffle order")
            val reorder=find(scene,"Reorder Window light")
            val up=reorder.config[SemanticsActions.CustomActions].first {it.label=="Move up"}
            assertTrue(up.action())
            assertEquals("b" to 0,actions.moved,"Queue edits must carry the destination's original timeline index")
        } finally {scene.close()}
    }

    @Test fun clearingTheQueueLeavesThePlayingSongUnderHostControl() {
        val actions=Actions();val current=mutableStateOf(state())
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(current.value,actions)}
        try {
            click(scene,"Open Now Playing");click(scene,"Show queue");click(scene,"Clear upcoming queue")
            assertEquals(1,actions.cleared)
            assertNotNull(find(scene,"Play Window light from queue"),"UI must wait for the actual queue update")
            current.value=current.value.copy(queue=listOf(tracks.first()))
            settle(scene)
            assertTrue(find(scene,"Clear upcoming queue").config.contains(SemanticsProperties.Disabled))
            assertNotNull(find(scene,"Pause"))
        } finally {scene.close()}
    }

}
