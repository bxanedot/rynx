# Universal materials and coordinated player

User-directed continuation of the Apple-reference Android implementation. Latest specification explicitly overrides the previous fullscreen-player/navigation relationship: compact player hides app navigation; expansion reveals it continuously; the expanded player leaves a separate navigation dock. No Vyon implementation or source-derived reference may be used.

1. Add a shared, validated appearance model, material/shape/motion tokens, and continuous player geometry/navigation mapping. Test invalid persisted values, three material policies, motion preferences, geometry endpoints, intermediate continuity and reversed progress.
2. Integrate the independently licensed Haze 2.0.1 renderer through documented typed APIs. Centralize source capture and material layering; nested controls reuse their parent material without another full-strength blur. Solid and AMOLED bypass all backdrop effects. Provide original press/focus/hover feedback in every mode.
3. Add Appearance > Interface Material, live controls and Android persistence. Connect all existing app/button/input/menu/navigation/player surfaces to the shared contract. Use automatic rendering-cost reduction independently of the saved material mode.
4. Replace separate compact/expanded player surfaces with one geometry-driven surface and shared artwork/title/transport. Tie navigation reveal and hit testing to the same expansion progress. Support drag from the compact player, reversal, release velocity, back handling, system insets and large fonts.
5. Run interaction/motion/render tests, Android packaging/lint and device tests where the emulator is usable. Review the independent implementation, retain test evidence and document measured-reference limits. No production or pixel-parity claim without actual acceptance.

## Ledger

Ruling: Reserve a separate bottom navigation dock for the expanded player — latest explicit user requirement supersedes the previous Apple fullscreen still — normalized player control proportions apply within the remaining player region.

Renderer evidence: public Haze 2.0.1 documentation at https://chrisbanes.github.io/haze/latest/effects/glass/ and /core-concepts/; Apache-2.0 implementation. No source or app template from any prohibited music application is involved. Use source-backed capture; experimental Android 37.2 native-backdrop path stays disabled until actual-device evaluation.


Task 1: shared appearance, policy, geometry, density and render-budget tests implemented. Invalid values normalize before reaching shader configuration. AMOLED policies are explicit; rendering pressure changes quality, never the saved material.
Task 2: Haze integration and source layering implemented. The live-source renderer test passed for changing red/blue content; Solid ignored that change and AMOLED sampled as pure black.
Task 3: settings, live preview, shared buttons/slider/switch/segments/search input, menus and Android preference storage implemented. Android persistence instrumentation compiles; runtime validation remains pending.
Task 4: one player surface/artwork/transport and continuous navigation handoff implemented. The new appearance/navigation interaction tests pass. Native/device, cancellation and restored-animation checks are being completed.
Ruling: use Solid for the long gesture regression suite and separately test Glass rendering/modes — the headless software shader and unaccelerated emulator competed for host resources — no rendering mode or animation is disabled in the application to improve a screenshot score. A full Glass player interaction run also passed before this harness optimization.
Ruling: velocity thresholds use progress per second rather than physical pixels — otherwise identical gestures behave differently with density — this changes the prior authored threshold and remains unmeasured against reference recordings.
Observed RED: both new density-invariance and nearest-snap tests failed with the previous pixel threshold/.78 snap point. Fix uses normalized velocity and the nearest endpoint unless flung.


Final review: fresh reviewer read only Rynx. Important findings: pending catalog ownership, fading-panel input, and lyric-picker recreation identity. LatestPlaybackRequest and fading-panel regressions were observed failing before fixes and pass afterward. Picker identity is saved/restored in Bundle; actual DocumentsUI recreation test compiles but has not run, so its native verification is still pending. Full local suite: 82/82, Android debug/release/test packaging and lint pass; two nonfatal lint warnings.

Final: minor (deferred): physical-device focus/optical/large-font overlap and process-death preference durability need native acceptance; cached-store recreation does not establish durable disk completion.
Ruling: publish to the existing empty bxanedot/rynx repository — the user explicitly selected that repo — no new repository or original upstream is changed; initial CI may reveal device-specific failures and is not a production release.

New user scope (after this verification): Apple-first metadata with YouTube Music fallback, Apple-derived Explore genres, Rynx recommendations and expanded Home discovery. Continued as a separate functional slice; existing material/phone work is retained.
