@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import dev.bxane.rynx.design.*
import dev.bxane.rynx.playback.discovery.*
import dev.bxane.rynx.ui.*
import kotlin.test.*

class DiscoveryUiTest {
    private class Actions:PlayerActions {
        var genre:MusicGenre?=null;var selected:MusicMetadata?=null;var section:String?=null;var openedUrl:String?=null
        override fun openDiscoverySection(id:String){section=id}
        override fun openGenre(genre:MusicGenre){this.genre=genre}
        override fun playDiscovery(track:MusicMetadata){selected=track}
        override fun openMusicSource(url:String){openedUrl=url}
        override fun importFiles(){};override fun play(id:String){};override fun toggle(){};override fun previous(){};override fun next(){}
        override fun seek(fraction:Float){};override fun cycleRepeat(){};override fun removeFromQueue(id:String){};override fun dismissError(){}
        override fun searchYouTubeMusic(query:String){};override fun loadMoreYouTubeMusic(){}
    }
    private var frameTime=System.nanoTime()
    private fun find(scene:ImageComposeScene,label:String):SemanticsNode {
        repeat(3){frameTime+=1_000_000_000L;scene.render(frameTime).close()}
        fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
        return scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}.firstOrNull {it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true}?:error("Missing discovery control $label")
    }
    private fun click(scene:ImageComposeScene,label:String){assertTrue(find(scene,label).config[SemanticsActions.OnClick].action!!.invoke())}
    @Test fun exploreUsesAppleGenresAndInvokesRealGenreAction() {
        val actions=Actions();val pop=MusicGenre("14","Pop")
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(PlayerUiState(reduceMotion=true,appearance=RynxAppearance(material=RynxMaterialMode.Solid),discovery=DiscoveryState(enabled=true,genres=listOf(pop))),actions)}
        try {
            click(scene,"Browse")
            val tile=find(scene,"Explore Pop").boundsInRoot
            assertTrue(tile.height>=tile.width,"Apple reference uses square artwork with captions below")
            click(scene,"Explore Pop");assertEquals(pop,actions.genre)
        } finally {scene.close()}
    }
    @Test fun youtubeMusicMetadataResultHasWorkingSourceLinkAndPlaybackAction() {
        val actions=Actions();val source="https://music.youtube.com/watch?v=moonlight"
        val song=MusicMetadata("yt:moonlight","Moonlight","bxane",sourceUrl=source,playbackId="moonlight")
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(PlayerUiState(reduceMotion=true,appearance=RynxAppearance(material=RynxMaterialMode.Solid),discovery=DiscoveryState(enabled=true),catalogSearch=CatalogSearchState(metadataItems=listOf(song))),actions)}
        try {
            click(scene,"Search")
            click(scene,"Play discovery Moonlight")
            assertEquals(song,actions.selected)
            click(scene,"Open Moonlight")
            assertEquals(source,actions.openedUrl)
        } finally {scene.close()}
    }
    @Test fun homeOffersARealChartDrilldownAndRendersGenreTiles() {
        val actions=Actions();val song=MusicMetadata("apple:1","Moonlight","bxane",chartRank=1)
        val section=HomeSection("global","Daily 100 · Global","Apple Music",HomeSectionKind.Daily100,listOf(song))
        val feed=DiscoveryState(enabled=true,sections=listOf(section),genres=listOf(MusicGenre("14","Pop"),MusicGenre("21","Rock")))
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(PlayerUiState(reduceMotion=true,appearance=RynxAppearance(material=RynxMaterialMode.Solid),discovery=feed),actions)}
        try {
            click(scene,"Open Daily 100 · Global");assertEquals("global",actions.section)
            val file=java.io.File("build/reports/screenshots/phone-discovery-home.png").apply {parentFile.mkdirs()}
            scene.render(frameTime+1_000_000).use {it.encodeToData()!!.use {data->file.writeBytes(data.bytes)}}
            click(scene,"Browse")
            val genres=java.io.File("build/reports/screenshots/phone-explore-genres.png")
            scene.render(frameTime+1_000_000).use {it.encodeToData()!!.use {data->genres.writeBytes(data.bytes)}}
        } finally {scene.close()}
    }

    @Test fun activeDiscoveryDetailExposesPreparationAndPlaybackFailure() {
        val section=HomeSection("global","Chart","Apple Music",HomeSectionKind.Daily100)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(PlayerUiState(busy=true,error="No matching recording",discoverySelection=section,appearance=RynxAppearance(material=RynxMaterialMode.Solid)),Actions())}
        try {assertNotNull(find(scene,"Preparing music"));assertNotNull(find(scene,"No matching recording"))} finally {scene.close()}
    }

}
