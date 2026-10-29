package dev.bxane.rynx.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** SF Symbols artwork is reserved for Apple targets; Android draws Rynx's original vectors. */
@Composable
internal actual fun drawGlyphStudioSymbol(glyph: Glyph, tint: Color, modifier: Modifier): Boolean = false
