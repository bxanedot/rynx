# Clean-room navigation motion

Implement the user-specified navigation behavior in Rynx's own shared Compose UI. Keep the player's existing presentation controller authoritative for compact/expanded visibility; let a separate, reversible navigation morph follow player reveal and user collapse state. Reuse the shared Haze material policy and original semantic vector glyphs.

## Ledger

- User supplied the behavior specification. No Vyon, Convx, BitChord, or Vivi implementation source is used.
- Existing PR branch `feature/original-navigation-motion` contains the user's initial icon, layered Library glyph, draggable indicator, and responsive window policy commits. Continue on that branch.
- Navigation geometry and motion values are Rynx authored tokens; they are not claimed to be measured from an unavailable Vyon recording.
- Initial implementation adds a continuous bar/capsule morph, a single velocity-aware selection state, progressive labels, compact Search action, history-preserving search field expansion, and player reveal coordination.
- Verification: shared design/UI and app-harness suites passed (78 cases across the recorded run); Android debug/test APK builds and lint passed. The Android Back/query instrumentation test compiles but awaits execution on a hosted Android device. The headless suite covers selection, drag settling, reversible vertical collapse/reveal, accessibility actions, player handoff and Search return history.
- Remaining parity limitation: no Vyon or Apple reference recordings were supplied for frame-by-frame trajectory comparison.
