package dev.bxane.rynx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfRenderingMode
import com.composables.sfsymbols.SfSymbol
import com.composables.sfsymbols.SfSymbolIcon
import com.composables.sfsymbols.SfWeight

/** Apple-platform icon renderer backed by bxanedot's GlyphStudio KMP symbol library. */
@Composable
internal actual fun drawGlyphStudioSymbol(glyph: Glyph, tint: Color, modifier: Modifier): Boolean {
    val symbol = SfSymbol.named(glyph.symbolName) ?: return false
    SfSymbolIcon(
        symbol = symbol,
        modifier = modifier,
        size = 22.dp,
        weight = SfWeight.Medium,
        mode = SfRenderingMode.Monochrome,
        tint = tint,
        contentDescription = null,
    )
    return true
}
