# Vector asset audit

## Rynx brand mark

The owner supplied ten SVG masters in the `rynxsvg.rar` upload and directed their use. The masters contain editable path geometry and linear-gradient fills; they do not embed raster images. They are retained at `assets/brand/palette/01_silver_white.svg` through `10_champagne_gold.svg`. `RynxBrandMark` reuses the same two path silhouettes in common Compose, with the supplied per-palette gradients. The Appearance sheet exposes all ten palette variants.

The `liquidicons.rar` upload contains ten transparent 1500×1500 PNG liquid finishes with timestamp filenames. They were resized to 256×256 and converted into WebP resources at `shared/ui/src/commonMain/composeResources/drawable/rynx_liquid_logo_01.webp` through `_10.webp`; their choice labels are intentionally neutral. Each finish is clipped to the common vector silhouette, so the raster texture does not define or replace the logo geometry. These rich surface textures remain raster instead of being presented as fake vector conversions. Appearance exposes all ten finishes.

Android's adaptive launcher foreground at `apps/android/src/main/res/drawable/rynx_mark.xml` uses the same two path shapes in a native vector drawable. It uses a single white fill for launcher masking. The shared vector palette default is Ruby Red. Android device rendering and iOS/desktop launcher exports have not yet been visually verified.

## Other glyphs and assets

`shared/ui/.../RynxGlyph.kt` owns the semantic registry and resolves symbols through GlyphStudio's dynamic API from common KMP UI. Original Canvas geometry remains as a fallback for catalog misses. The Library selection bounce uses GlyphStudio's by-layer effect. Android/JVM build verification and Apple-toolchain verification are recorded separately; artwork terms, hierarchical/palette rendering, RTL mirroring and the remaining platform asset inventory are not yet verified.

Photographs, album artwork, thumbnails and the supplied liquid finish textures remain raster. Never embed raster in SVG and call it vector. Usage is limited to the owner-provided Rynx marks and finishes; no third-party artwork was added.
