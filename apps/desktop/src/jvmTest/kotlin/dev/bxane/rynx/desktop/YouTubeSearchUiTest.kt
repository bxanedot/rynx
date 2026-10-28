@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import dev.bxane.rynx.playback.catalog.*
import dev.bxane.rynx.ui.*
import kotlin.test.*

class YouTubeSearchUiTest {
    private class Actions : PlayerActions {
        val searches=mutableListOf<String>()
        var pageRequests=0
        override fun searchYouTubeMusic(query:String){searches+=query}
        override fun loadMoreYouTubeMusic(){pageRequests++}
        override fun importFiles(){};override fun play(id:String){};override fun toggle(){}
        override fun previous(){};override fun next(){};override fun seek(fraction:Float){}
        override fun cycleRepeat(){};override fun removeFromQueue(id:String){};override fun dismissError(){}
    }
    private fun nodes(scene:ImageComposeScene)=scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
    private fun descendants(node:SemanticsNode):List<SemanticsNode> = listOf(node)+node.children.flatMap(::descendants)
    private fun click(scene:ImageComposeScene,label:String) {
        scene.render().close()
        val node=nodes(scene).firstOrNull{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true}
            ?:error("No accessible control: $label")
        assertTrue(node.config[SemanticsActions.OnClick].action!!.invoke())
        scene.render().close()
    }
    @Test fun submitsTrimmedQueryAndRequestsMoreResults() {
        val actions=Actions()
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(PlayerUiState(catalogSearch=CatalogSearchState(query="jazz",continuation="next")),actions)}
        try {
            click(scene,"Search")
            val field=nodes(scene).first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Search your music")==true}
            field.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("  jazz  "))
            scene.render().close()
            click(scene,"Search YouTube Music")
            assertEquals(listOf("jazz"),actions.searches)
            click(scene,"Load more results")
            assertEquals(1,actions.pageRequests)
        } finally {scene.close()}
    }
    @Test fun showsMappedProviderItemsWithoutOfferingFakePlaybackControls() {
        val actions=Actions()
        val result=CatalogItem(CatalogProvider.YouTubeMusic,CatalogItemType.Track,"abc123","Blue Hour","Artist",210)
        val scene=ImageComposeScene(390,844,Density(1f)){
            RynxApp(PlayerUiState(catalogSearch=CatalogSearchState(query="jazz",items=listOf(result))),actions)
        }
        try {
            click(scene,"Search")
            val field=nodes(scene).first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Search your music")==true}
            field.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("jazz"))
            scene.render().close()
            click(scene,"Search YouTube Music")
            scene.render().close()
            val labels=nodes(scene).mapNotNull{it.config.getOrNull(SemanticsProperties.Text)?.joinToString("")}
            assertTrue(labels.any{it.contains("Blue Hour")},"Visible text: $labels")
            assertTrue(labels.any{it.contains("Artist")})
            assertFalse(nodes(scene).any{it.config.getOrNull(SemanticsProperties.ContentDescription)?.any{label->label.contains("Play Blue Hour")}==true})
        } finally {scene.close()}
    }
}
