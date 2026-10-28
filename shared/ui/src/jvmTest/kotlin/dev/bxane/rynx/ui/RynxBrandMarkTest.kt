@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package dev.bxane.rynx.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.bxane.rynx.design.RynxBrandVariant
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RynxBrandMarkTest {
    private var time = System.nanoTime()

    private fun render(variant: RynxBrandVariant): java.awt.image.BufferedImage {
        val scene = ImageComposeScene(128, 128, Density(1f)) {
            Box(Modifier.fillMaxSize()) {
                RynxBrandMark(Modifier.size(110.dp), variant = variant)
            }
        }
        return try {
            repeat(3) { time += 1_000_000_000; scene.render(time).close() }
            scene.render(time + 1_000_000).use { frame ->
                frame.encodeToData()!!.use { encoded ->
                    ImageIO.read(ByteArrayInputStream(encoded.bytes))
                }
            }
        } finally {
            scene.close()
        }
    }

    private fun alphaMask(image: java.awt.image.BufferedImage) = buildList {
        for (y in 0 until image.height) for (x in 0 until image.width) {
            add(image.getRGB(x, y) ushr 24)
        }
    }

    @Test fun vectorPaletteChangesColorWithoutChangingTheLogoSilhouette() {
        val ruby = render(RynxBrandVariant.RubyRed)
        val blue = render(RynxBrandVariant.ElectricBlue)
        assertEquals(alphaMask(ruby), alphaMask(blue), "Palette changes must preserve the vector silhouette")
        val coloredPixels = (0 until ruby.height).sumOf { y ->
            (0 until ruby.width).count { x -> ruby.getRGB(x, y) != blue.getRGB(x, y) }
        }
        assertTrue(coloredPixels > 1_000, "Distinct palette vectors should render visibly different gradients")
        assertTrue(alphaMask(ruby).count { it > 0 } > 1_000, "The logo vector should contain visible geometry")
    }

    @Test fun liquidFinishIsClippedToTheSameVectorSilhouette() {
        val vector = render(RynxBrandVariant.RubyRed)
        val liquid = render(RynxBrandVariant.LiquidFinish01)
        val vectorMask = alphaMask(vector)
        val liquidMask = alphaMask(liquid)
        assertTrue(vectorMask.indices.all { vectorMask[it] > 0 || liquidMask[it] == 0 },
            "Texture pixels must stay inside the editable vector outline")
        assertTrue(liquidMask.count { it > 0 } > 1_000, "The selected liquid finish should be visible")
    }
}
