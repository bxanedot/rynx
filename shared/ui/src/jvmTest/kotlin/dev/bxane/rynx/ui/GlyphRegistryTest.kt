package dev.bxane.rynx.ui

import com.composables.sfsymbols.SfSymbol
import kotlin.test.Test
import kotlin.test.assertNotNull

class GlyphRegistryTest {
    @Test
    fun everySemanticGlyphResolvesFromGlyphStudio() {
        Glyph.entries.forEach { glyph ->
            assertNotNull(
                SfSymbol.named(glyph.symbolName),
                "${glyph.name} must use a GlyphStudio symbol instead of the owned-vector fallback",
            )
        }
    }
}
