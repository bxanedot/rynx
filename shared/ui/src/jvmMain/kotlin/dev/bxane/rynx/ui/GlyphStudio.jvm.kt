package dev.bxane.rynx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Non-Apple desktop builds use original Rynx vectors rather than Apple's SF Symbols artwork. */
@Composable
internal actual fun drawGlyphStudioSymbol(glyph: Glyph, tint: Color, modifier: Modifier): Boolean = false
