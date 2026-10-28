# Vector asset audit
All requested: application icon, brand mark, navigation, controls, decoration, geometric backgrounds, empty states, player accents, TV/Wear/desktop icons. Non-Android targets remain unverified; no reference masters were supplied.
For each asset record evidence, owner/license, original vector master, viewport, fills/strokes/gradients/alpha, optical alignment, generated outputs and visual comparison.
Photographs/artwork/thumbnails remain raster. Never embed raster inside SVG to claim conversion.

Implemented original Compose Canvas masters in `shared/ui/.../RynxGlyph.kt`: Play, Pause, Next, Previous, Search, Add, Library, Queue, Close, Note, Sun, Moon, Repeat and the Rynx monogram. Controls route through RynxGlyph; these are original equivalents, not copied SF artwork. Geometry parity, animated symbol effects, hierarchical/palette modes, RTL mirroring and broader asset inventory remain unverified/unimplemented.

Brand masters: `assets/brand/rynx-mark.svg` and `assets/brand/rynx-placeholder-artwork.svg`. Android uses `apps/android/src/main/res/drawable/rynx_mark.xml` for the adaptive launcher foreground; Compose draws the Rynx placeholder as an original gradient and vector mark. These are independently authored provisional branding, not copied Vyon/Apple artwork. Device icon rendering and artwork placeholder comparisons remain unverified.
