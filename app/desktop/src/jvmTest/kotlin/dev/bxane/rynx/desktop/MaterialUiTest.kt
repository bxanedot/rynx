@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import dev.bxane.rynx.design.*
import java.io.File
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import dev.bxane.rynx.ui.*
import kotlin.test.*

class MaterialUiTest {
    private class Actions:PlayerActions {
        var appearance:RynxAppearance?=null
        var playedCollection:List<String> = emptyList()
        override fun playCollection(tracks:List<TrackItem>) { playedCollection=tracks.map(TrackItem::id) }
        override fun updateAppearance(appearance:RynxAppearance){this.appearance=appearance}
        override fun importFiles(){};override fun play(id:String){};override fun toggle(){}
        override fun previous(){};override fun next(){};override fun seek(fraction:Float){}
        override fun cycleRepeat(){};override fun removeFromQueue(id:String){};override fun dismissError(){}
        override fun searchYouTubeMusic(query:String){};override fun loadMoreYouTubeMusic(){}
    }
    private var clock=System.nanoTime()
    private fun settle(scene:ImageComposeScene){repeat(3){clock+=1_000_000_000;scene.render(clock).close()}}
    private fun nodes(node:SemanticsNode):List<SemanticsNode> = if(node.config.contains(SemanticsProperties.HideFromAccessibility))emptyList() else listOf(node)+node.children.flatMap(::nodes)
    private fun find(scene:ImageComposeScene,label:String):SemanticsNode {
        settle(scene)
        return scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}.firstOrNull {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true
        } ?: error("Missing material control: $label")
    }
    @Test fun appearanceExposesThreeMaterialsAndMotionControls() {
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(PlayerUiState(),Actions())}
        try {
            click(scene,"Settings")
            assertNotNull(find(scene,"Sound & EQ"))
            assertTrue(find(scene,"Interface & Motion").config[SemanticsActions.OnClick].action!!.invoke())
            settle(scene)
            for(label in listOf("Liquid Glass","Solid","True AMOLED","Follow system motion")) assertNotNull(find(scene,label))
        } finally {scene.close()}
    }
    private fun click(scene:ImageComposeScene,label:String) {assertTrue(find(scene,label).config[SemanticsActions.OnClick].action!!.invoke());settle(scene)}
    private fun capture(scene:ImageComposeScene,name:String) {
        val file=File("build/reports/screenshots/$name.png").apply {parentFile.mkdirs()}
        scene.render(clock+1_000_000).use {it.encodeToData()!!.use {data->file.writeBytes(data.bytes)}}
    }
    @Test fun changingMaterialsUpdatesTheLiveScreenAndReachesPersistence() {
        val actions=Actions();val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(PlayerUiState(),actions)}
        try {
            click(scene,"Settings")
            click(scene,"Interface & Motion")
            for(mode in RynxMaterialMode.entries) {
                click(scene,mode.label)
                assertEquals(mode,actions.appearance!!.material)
                capture(scene,"phone-material-${mode.name.lowercase()}")
            }
            click(scene,"Done")
            assertEquals(RynxMaterialMode.Amoled,actions.appearance!!.material)
            click(scene,"Close settings")
            capture(scene,"phone-amoled-home")
        } finally {scene.close()}
    }
    @Test fun settingsPageDefaultsToLightAndProvidesDarkAppearanceToggle() {
        val actions=Actions();val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(PlayerUiState(),actions)}
        try {
            assertFalse(RynxAppearance().darkTheme)
            click(scene,"Settings")
            capture(scene,"settings-page-dark")
            for(label in listOf("Sound & EQ","Audio Output","Dark Appearance","Interface & Motion","Import Music","Manage Library","About Rynx")) {
                assertNotNull(find(scene,label))
            }
            click(scene,"Dark Appearance")
            assertEquals(true,actions.appearance!!.darkTheme)
        } finally {scene.close()}
    }
    @Test fun aboutRynxHasVersionProjectLicensesAndNavigatesBackToSettings() {
        val state = PlayerUiState(
            appVersionName = "0.1.0-dev",
            appBuildNumber = "12",
            appPlatform = "Android",
            appAudioBackend = "Media3 ExoPlayer",
        )
        val scene = ImageComposeScene(390, 844, Density(1f)) { RynxApp(state, Actions()) }
        try {
            click(scene, "Settings")
            click(scene, "About Rynx")
            for (label in listOf("Version", "Build", "Platform", "Audio engine", "Source code",
                    "Report a problem", "Open-source license", "Notices and acknowledgements",
                    "Third-party licenses", "Local playback", "Bit-perfect DAC output")) {
                assertNotNull(find(scene, label))
            }
            capture(scene, "phone-about-rynx")
            click(scene, "Back to Settings")
            assertNotNull(find(scene, "Dark Appearance"))
        } finally { scene.close() }
    }

    @Test fun libraryShowsGlyphStudioCategoryShortcutsAndNavigatesIntoRealSections() {
        val tracks=listOf(
            TrackItem("content://library/first","First Song","Nights",albumTitle="Night Drive"),
            TrackItem("content://library/second","Second Song","Nights",albumTitle="Night Drive"),
        )
        val state=PlayerUiState(tracks=tracks,favoriteIds=setOf(tracks[0].id))
        val scene=ImageComposeScene(390,844,Density(1f)) { RynxApp(state,Actions()) }
        try {
            click(scene,"Library")
            for(category in listOf("Playlists","Artists","Albums","Songs","Favorites"))
                assertNotNull(find(scene,"Browse "+category))
            capture(scene,"phone-library-glyphstudio-categories")
            click(scene,"Browse Albums")
            assertNotNull(find(scene,"Open album Night Drive by Nights"))
            click(scene,"Back to Library")
            click(scene,"Browse Songs")
            assertNotNull(find(scene,"Back to Library"))
            click(scene,"Back to Library")
            click(scene,"Browse Favorites")
            assertNotNull(find(scene,"Back to Library"))
        } finally { scene.close() }
    }

    @Test fun importedAlbumsOpenArtistAndAlbumDetailsWithRealQueueAction() {
        val actions=Actions()
        val tracks=listOf(
            TrackItem("content://library/a","Opening","Nights",albumTitle="Night Drive"),
            TrackItem("content://library/b","Ending","Nights",albumTitle="Night Drive"),
            TrackItem("content://library/c","Daylight","Mornings",albumTitle="Sunrise"),
        )
        val scene=ImageComposeScene(390,844,Density(1f)) { RynxApp(PlayerUiState(tracks=tracks),actions) }
        try {
            click(scene,"Library")
            click(scene,"Open artist Nights")
            assertNotNull(find(scene,"Play artist"))
            click(scene,"Open album Night Drive by Nights")
            assertNotNull(find(scene,"Play album"))
            capture(scene,"phone-library-album-detail")
            click(scene,"Play album")
            assertEquals(listOf("content://library/a","content://library/b"),actions.playedCollection)
            click(scene,"Back to Library")
            assertNotNull(find(scene,"Play artist"))
            click(scene,"Back to Library")
            assertNotNull(find(scene,"Open artist Nights"))
        } finally {scene.close()}
    }

    @Test fun compactPlayerHidesNavigationAndAnExpandedPlayerRestoresItWithoutOverlap() {
        val track=TrackItem("a","Evening walk","bxane")
        val state=PlayerUiState(tracks=listOf(track),queue=listOf(track),currentId="a",appearance=RynxAppearance(material=RynxMaterialMode.Solid),reduceMotion=true)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,Actions())}
        try {
            settle(scene)
            assertFailsWith<IllegalStateException>{find(scene,"Library")}
            click(scene,"Open Now Playing")
            val navigation=find(scene,"Library").boundsInRoot
            val footer=find(scene,"Show queue").boundsInRoot
            assertTrue(navigation.top>footer.bottom,"Expanded player and navigation must not overlap")
            click(scene,"Library")
            assertNotNull(find(scene,"Import audio files"))
            assertFailsWith<IllegalStateException>{find(scene,"Library")}
            assertNotNull(find(scene,"Open Now Playing"))
        } finally {scene.close()}
    }
    @Test fun draggingTheCompactSurfaceUpOpensTheSamePlayer() {
        val track=TrackItem("a","Evening walk","bxane")
        val state=PlayerUiState(tracks=listOf(track),currentId="a",appearance=RynxAppearance(material=RynxMaterialMode.Solid),durationMillis=180_000,volumeControlAvailable=true)
        val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,Actions())}
        try {
            val compact=find(scene,"Open Now Playing").boundsInRoot
            val from=compact.center;val to=Offset(from.x,160f)
            scene.sendPointerEvent(PointerEventType.Press,from,timeMillis=clock/1_000_000)
            repeat(12){i->clock+=30_000_000;scene.sendPointerEvent(PointerEventType.Move,from+(to-from)*((i+1)/12f),timeMillis=clock/1_000_000);scene.render(clock).close()}
            scene.sendPointerEvent(PointerEventType.Release,to,timeMillis=clock/1_000_000+30)
            settle(scene)
            assertNotNull(find(scene,"Playback volume"));assertNotNull(find(scene,"Library"))
        } finally {scene.close()}
    }

    @Test fun landscapePlayerRetainsNavigationAndAccessibleControls() {
        val track=TrackItem("a","Evening walk","bxane")
        val state=PlayerUiState(tracks=listOf(track),currentId="a",playing=true,durationMillis=180_000,reduceMotion=true,
            volumeControlAvailable=true,appearance=RynxAppearance(material=RynxMaterialMode.Solid))
        val scene=ImageComposeScene(844,390,Density(1f)){RynxApp(state,Actions())}
        try {
            click(scene,"Open Now Playing")
            val navigation=find(scene,"Library").boundsInRoot
            val footer=find(scene,"Show queue").boundsInRoot
            assertTrue(navigation.top>footer.bottom,"Landscape must retain a separate navigation dock")
            for(label in listOf("Pause","Playback position","Playback volume","Show queue")) {
                val box=find(scene,label).boundsInRoot
                assertTrue(box.top>=0f && box.bottom<=390f-82f,"Landscape control clipped: $label $box")
            }
        } finally {scene.close()}
    }
    @Test fun everyMaterialUsesTheSamePlayerAndNavigationGeometry() {
        val track=TrackItem("a","Evening walk","bxane")
        var expected:List<androidx.compose.ui.geometry.Rect>?=null
        for(mode in RynxMaterialMode.entries) {
            val state=PlayerUiState(tracks=listOf(track),currentId="a",playing=true,durationMillis=180_000,reduceMotion=true,
                volumeControlAvailable=true,appearance=RynxAppearance(material=mode))
            val scene=ImageComposeScene(390,844,Density(1f)){RynxApp(state,Actions())}
            try {
                click(scene,"Open Now Playing")
                val bounds=listOf("Pause","Playback position","Playback volume","Show queue","Library").map {find(scene,it).boundsInRoot}
                if(expected==null)expected=bounds else assertEquals(expected,bounds,"Changing material must preserve geometry")
                capture(scene,"phone-player-${mode.name.lowercase()}")
            } finally {scene.close()}
        }
    }

}
