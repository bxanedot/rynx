package dev.bxane.rynx.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.layout.ContentScale
import dev.bxane.rynx.design.RynxBrandVariant
import dev.bxane.rynx.ui.generated.resources.*
import dev.bxane.rynx.ui.material.LocalRynxAppearance
import org.jetbrains.compose.resources.painterResource

/**
 * Draws the supplied Rynx lightning/ribbon mark from the vector masters. The
 * optional liquid finishes are raster fills, clipped to the same vector paths.
 */
@Composable
fun RynxBrandMark(
    modifier: Modifier = Modifier,
    variant: RynxBrandVariant = LocalRynxAppearance.current.brandVariant,
    tint: Color? = null,
) {
    val texture = when (variant.liquidTextureIndex) {
        1 -> Res.drawable.rynx_liquid_logo_01
        2 -> Res.drawable.rynx_liquid_logo_02
        3 -> Res.drawable.rynx_liquid_logo_03
        4 -> Res.drawable.rynx_liquid_logo_04
        5 -> Res.drawable.rynx_liquid_logo_05
        6 -> Res.drawable.rynx_liquid_logo_06
        7 -> Res.drawable.rynx_liquid_logo_07
        8 -> Res.drawable.rynx_liquid_logo_08
        9 -> Res.drawable.rynx_liquid_logo_09
        10 -> Res.drawable.rynx_liquid_logo_10
        else -> null
    }

    if (texture != null) {
        Image(
            painter = painterResource(texture),
            contentDescription = null,
            modifier = modifier.clip(RynxBrandShape),
            contentScale = ContentScale.FillBounds,
            colorFilter = tint?.let { ColorFilter.tint(it) },
        )
    } else {
        Canvas(modifier) {
            val top = topRibbonPath(size)
            val lower = lowerRibbonPath(size)
            val topBrush = if (tint != null) SolidColor(tint) else Brush.linearGradient(
                colors = listOf(variant.topStartArgb.asColor(), variant.topEndArgb.asColor()),
                start = Offset(size.width * 283f / 1024f, size.height * 101f / 1024f),
                end = Offset(size.width * 694f / 1024f, size.height * 609f / 1024f),
            )
            val lowerBrush = if (tint != null) SolidColor(tint) else Brush.linearGradient(
                colors = listOf(variant.bottomStartArgb.asColor(), variant.bottomEndArgb.asColor()),
                start = Offset(size.width * 315f / 1024f, size.height * 454f / 1024f),
                end = Offset(size.width * 757f / 1024f, size.height * 932f / 1024f),
            )
            drawPath(top, topBrush, style = Fill)
            drawPath(lower, lowerBrush, style = Fill)
        }
    }
}

private fun Long.asColor() = Color(toInt())

private fun topRibbonPath(size: Size) = Path().apply {
    val sx = size.width / 1024f
    val sy = size.height / 1024f
    moveTo(694f * sx, 101f * sy)
    lineTo(340f * sx, 338f * sy)
    cubicTo(301f * sx, 365f * sy, 283f * sx, 390f * sy, 283f * sx, 430f * sy)
    lineTo(283f * sx, 520f * sy)
    cubicTo(283f * sx, 571f * sy, 313f * sx, 608f * sy, 381f * sx, 609f * sy)
    lineTo(525f * sx, 608f * sy)
    lineTo(470f * sx, 561f * sy)
    cubicTo(444f * sx, 540f * sy, 436f * sx, 519f * sy, 450f * sx, 489f * sy)
    cubicTo(456f * sx, 476f * sy, 468f * sx, 460f * sy, 481f * sx, 440f * sy)
    close()
}

private fun lowerRibbonPath(size: Size) = Path().apply {
    val sx = size.width / 1024f
    val sy = size.height / 1024f
    moveTo(504f * sx, 454f * sy)
    lineTo(663f * sx, 454f * sy)
    cubicTo(717f * sx, 455f * sy, 757f * sx, 493f * sy, 757f * sx, 547f * sy)
    lineTo(757f * sx, 604f * sy)
    cubicTo(757f * sx, 656f * sy, 728f * sx, 701f * sy, 683f * sx, 729f * sy)
    lineTo(315f * sx, 932f * sy)
    lineTo(557f * sx, 609f * sy)
    cubicTo(581f * sx, 579f * sy, 592f * sx, 558f * sy, 582f * sx, 535f * sy)
    cubicTo(577f * sx, 519f * sy, 557f * sx, 503f * sy, 540f * sx, 486f * sy)
    close()
}

private object RynxBrandShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            addPath(topRibbonPath(size))
            addPath(lowerRibbonPath(size))
        }
        return Outline.Generic(path)
    }
}
