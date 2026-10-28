# Android UI tokens

Source of truth: `shared/design/RynxAppearance.kt`, `NavigationMotionSpec.kt`, `PlayerGeometry.kt` and the shared material/player components. No Vyon captures exist. The user authorizes public Apple Music imagery as the direct reference; no exact Android or animation parity is claimed.

## Measured reference proportions

The current public Apple player still is 490×1008 with app content approximately x22–468 / y76–956. Relative to that content: metadata .53H, timeline .628H, transport .745H, volume .858H, footer .935H, horizontal control margin .081W. These are image observations, not dp. See `APPLE_MUSIC_REFERENCE.md` for source and hash.

## Authored material tokens

| Token | Default / policy |
|---|---|
| Material | Liquid Glass |
| Transparency / blur | .64 / .62 |
| Refraction / highlight | .38 / .42 |
| Tint / border | .32 / .24 |
| Vibrancy / shadow | .50 / .35 |
| Dynamic tint / motion intensity | enabled / 1.0 |
| Light background / surface | #F9F9FC / #F0F0F5 |
| Dark background / surface | #141417 / #26262C |
| AMOLED background / surface | #000000 / #000000 |
| Compact player | 12dp horizontal inset, 62dp height, 27dp radius, 40dp artwork |
| Expanded player | Full available width; 28dp radius; separate 82dp navigation dock |
| Navigation reveal | smoothstep from .42 to 1 of player progress; hit testing at reveal ≥ .96 |

## Authored navigation motion

`RynxNavigationMotionSpec` owns original defaults: icon spring 390/.62, Library layer spring 340/.58, selection spring 310/.76, bar morph 430/.88 and search expansion 470/.90. The compact dock is 58×64dp; its separate Search target is 48dp. Labels track the same morph progress, and Search expands from a 52dp capsule. Horizontal settling projects velocity by .18s with a 2.1-tabs-per-second fling threshold; a 28dp vertical swipe retracts or restores the dock. Search input becomes active after .18 expansion and autofocus begins after .82. These are implementation defaults, not measured Apple or Vyon motion values.

Appearance floats normalize finite values to [0,1] before shader use. Solid has opaque tonal surfaces and no backdrop effects. AMOLED disables ambient glow, backdrop blur, refraction and shadows. Actual cover art remains media. All modes share geometry and semantic actions.

Haze 2.0.1 uses one source registry with explicit source levels. Nested controls reuse the parent treatment rather than stacking full blur passes. The live Appearance preview has a source above its parent sheet and below its own preview glass, preventing the sheet from sampling its child. Rendering pressure selects a lower-cost profile without changing the saved material.

The portrait player applies observed control proportions within the available player area. Short screens/large fonts use an accessible scroll layout. Phone landscape uses artwork/metadata at left and controls at right, preserving the bottom dock. These are authored Android adaptations; their reference comparisons remain pending. Font files and Apple artwork are not embedded.
