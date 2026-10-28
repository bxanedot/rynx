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
        var imports=0;var toggles=0;var removed:String?=null;var played:String?=null;var favorite:String?=null
        override fun importFiles(){imports++}
        override fun toggle(){toggles++}
        override fun removeFromQueue(id:String){removed=id}
        override fun play(id:String){played=id};override fun previous(){};override fun next(){}
        override fun seek(fraction:Float){};override fun cycleRepeat(){};override fun dismissError(){}
        override fun searchYouTubeMusic(query:String){};override fun loadMoreYouTubeMusic(){}
        override fun toggleFavorite(id:String){favorite=id}
    }
    private fun descendants(node:SemanticsNode):List<SemanticsNode> = listOf(node)+node.children.flatMap(::descendants)
    private fun click(scene:ImageComposeScene,label:String) {
        scene.render().close()
        val node=scene.semanticsOwners.flatMap{descendants(it.rootSemanticsNode)}.firstOrNull{
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true
        }?:error("No accessible control: $label; visible descriptions=${scene.semanticsOwners.flatMap{descendants(it.rootSemanticsNode)}.mapNotNull{node->node.config.getOrNull(SemanticsProperties.ContentDescription)}.flatten()}")
        assertTrue(node.config[SemanticsActions.OnClick].action!!.invoke())
        scene.render().close()
    }
    private fun assertText(scene:ImageComposeScene,label:String) {
        scene.render().close()
        assertTrue(scene.semanticsOwners.flatMap{descendants(it.rootSemanticsNode)}.any { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text==label }==true
        },"Expected visible text: $label")
    }
    private fun assertSelectedTab(scene:ImageComposeScene,label:String) {
        scene.render().close()
        val node=scene.semanticsOwners.flatMap{descendants(it.rootSemanticsNode)}.firstOrNull{
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true
        }?:error("No navigation tab: $label")
        assertTrue(node.config.getOrNull(SemanticsProperties.Selected)==true,"Expected selected tab: $label")
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
            click(scene,"Add files")
            assertEquals(1,actions.imports)
        } finally{scene.close()}
    }
    @Test fun phonePlayerQueueAndTransportRenderAndInvokeActions() {
        val actions=Actions()
        val tracks=listOf(TrackItem("a","Evening walk"),TrackItem("b","Window light"),TrackItem("c","Morning tide"))
        val state=PlayerUiState(tracks,tracks,"a",true,35_000,180_000,reduceMotion=true,queueEditingAvailable=true)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,actions)}
        try {
            capture(scene,"phone-library")
            click(scene,"Pause")
            assertEquals(1,actions.toggles)
            click(scene,"Open Now Playing")
            capture(scene,"phone-player")
            click(scene,"Show queue")
            capture(scene,"phone-queue")
        } finally{scene.close()}
    }
    @Test fun phoneNavigationOffersListenNowBrowseRadioLibraryAndSearch() {
        val actions=Actions()
        val state=PlayerUiState(tracks=listOf(TrackItem("a","Evening walk")))
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,actions)}
        try {
            assertSelectedTab(scene,"Listen Now")
            click(scene,"Browse")
            assertSelectedTab(scene,"Browse")
            click(scene,"Radio")
            assertSelectedTab(scene,"Radio")
            click(scene,"Library")
            assertSelectedTab(scene,"Library")
            click(scene,"Search")
            assertSelectedTab(scene,"Search")
        } finally{scene.close()}
    }
    @Test fun homeRecentTrackCardIsAccessibleAndStartsTheSelectedTrack() {
        val actions=Actions()
        val state=PlayerUiState(recentTracks=listOf(TrackItem("a","Evening walk","Artist", "https://example.test/art.jpg")))
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,actions)}
        try {
            assertText(scene,"Recently Played")
            capture(scene,"phone-home-shelf")
            click(scene,"Play Evening walk")
            assertEquals("a",actions.played)
        } finally {scene.close()}
    }
    @Test fun expandedPlayerCanToggleFavoriteForTheCurrentTrack() {
        val actions=Actions()
        val track=TrackItem("a","Evening walk","Artist")
        val state=PlayerUiState(tracks=listOf(track),queue=listOf(track),currentId="a",playing=true,durationMillis=180_000,currentTrack=track,reduceMotion=true)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,actions)}
        try {
            click(scene,"Open Now Playing")
            click(scene,"Add to Favorites")
            assertEquals("a",actions.favorite)
        } finally {scene.close()}
    }
    @Test fun librarySeparatesFavoriteTracksFromTheFullSongList() {
        val track=TrackItem("a","Evening walk","Artist")
        val state=PlayerUiState(tracks=listOf(track),favoriteIds=setOf("a"))
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,Actions())}
        try {
            click(scene,"Library")
            assertText(scene,"Favorites")
            assertText(scene,"Evening walk")
        } finally {scene.close()}
    }
    @Test fun libraryShowsSavedStreamingFavoriteWhenItIsNotInTheLocalSongList() {
        val track=TrackItem("yt:track-1","Song on repeat","Artist","https://example.test/art.jpg")
        val state=PlayerUiState(favoriteIds=setOf(track.id),favoriteTracks=listOf(track))
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,Actions())}
        try {
            click(scene,"Library")
            assertText(scene,"Favorites")
            assertText(scene,"Song on repeat")
        } finally {scene.close()}
    }
}
