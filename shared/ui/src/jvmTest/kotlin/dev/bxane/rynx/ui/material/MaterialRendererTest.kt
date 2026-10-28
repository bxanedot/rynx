@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.ui.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.bxane.rynx.design.*
import dev.chrisbanes.haze.rememberHazeState
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.*

class MaterialRendererTest {
    private var clock=System.nanoTime()
    private fun sample(scene:ImageComposeScene):java.awt.Color {
        repeat(3){clock+=1_000_000_000;scene.render(clock).close()}
        return scene.render(clock+1_000_000).use { image -> image.encodeToData()!!.use {
            val bitmap=ImageIO.read(ByteArrayInputStream(it.bytes));java.awt.Color(bitmap.getRGB(90,90))
        }}
    }
    @Test fun glassSamplesChangingContentWhileOpaqueModesIgnoreIt() {
        for(mode in RynxMaterialMode.entries) {
            val color=mutableStateOf(Color(0xFFCA3249))
            val scene=ImageComposeScene(180,180,Density(1f)) {
                val haze=rememberHazeState()
                CompositionLocalProvider(LocalRynxAppearance provides RynxAppearance(material=mode,darkTheme=true),
                    LocalRynxBackdrop provides haze,LocalRynxDarkSurface provides true) {
                    Box(Modifier.fillMaxSize()) {
                        Canvas(Modifier.fillMaxSize().rynxBackdropSource(0f)){drawRect(color.value)}
                        RynxSurface(Modifier.padding(25.dp).size(130.dp),24.dp) {}
                    }
                }
            }
            try {
                val red=sample(scene)
                color.value=Color(0xFF2856D5)
                val blue=sample(scene)
                if(mode==RynxMaterialMode.LiquidGlass) {
                    assertTrue(red.red>red.blue,"Glass must sample the initial source: $red")
                    assertTrue(blue.blue>blue.red,"Glass must sample live source changes: $blue")
                } else {
                    assertEquals(red,blue,"Opaque materials must not sample the backdrop")
                    if(mode==RynxMaterialMode.Amoled)assertEquals(java.awt.Color.BLACK,blue)
                }
            } finally {scene.close()}
        }
    }
}
