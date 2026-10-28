# Shared interface materials

Latest user requirement: Liquid Glass is the default. Solid and True AMOLED use the same interactive tree, shapes, player geometry and navigation. Compact playback hides app navigation; expanding playback reveals it from the same progress controller. This deliberately changes the previous fullscreen Apple still layout.

## Implementation

- `shared/design`: validated appearance preferences, explicit three-mode rendering policies, shared spring values, player geometry and frame-budget hysteresis.
- `RynxMaterial`: one Haze source registry per window. Background, app content, player artwork and overlays have explicit source levels; a surface only samples lower sources. Nested surfaces reuse the parent treatment without another full-strength optical pass.
- Haze `2.0.1` supplies the source capture and optical renderer under Apache-2.0. Rynx selects styling, interaction semantics, inputs and layout independently. Its experimental Android 37.2 native-backdrop flag is not enabled; source-backed rendering and the library's platform fallback are used.
- Liquid Glass exposes transparency, blur, refraction, highlights, tint, border, vibrancy, shadows and dynamic color. Solid bypasses backdrop effects and uses opaque tonal surfaces. AMOLED uses `#000000` backgrounds/backplates, no ambient artwork blur, refraction or shadows; actual cover art remains media.
- Shared controls retain accessible actions, press feedback, focus/hover response and reduced-motion behavior in every mode. Appearance changes apply live and are persisted by the Android host.
- Android frame-duration monitoring chooses Haze's Performance profile after sustained missed budgets. It does not change the stored material. Three healthy observation windows recover Balanced quality. These are implementation policies, not a device benchmark.

## Player and navigation

The phone player is one capsule-to-sheet surface, one shared artwork node and one transport set. A continuous 0–1 progress value drives its rectangle, artwork, labels, control positions, progressive control visibility and the navigation dock. Navigation is absent at 0, progressively visible after 0.42, and interactive only after 96% reveal. Its expanded player region leaves an 82dp navigation dock. Drag input is direct; release springs preserve velocity and support reversal. Secondary controls reveal at .78–.98; hidden/fading panels cannot receive input. Landscape phones retain the bottom dock with artwork at left and controls at right. Values are authored approximations pending permitted motion recordings.

## Acceptance limits

Headless interaction, geometry and screenshot tests cover the implemented tree. They do not establish Android GPU cost, physical-device optical fidelity, high-refresh performance, motion equivalence or 95% screenshot parity. Actual device and low-end-device acceptance remain required. The material system applies to existing implemented screens; unimplemented Spotify, downloads and other feature screens do not become complete through this work.
