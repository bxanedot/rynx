# Apple Music player reconstruction

User-directed execution: Android-first KMP/Compose, continue without design approval pauses. Use public Apple player/queue screenshots and behavior guides only. No prohibited application implementation is a source.

## Reference and scope

The current Apple player guide shows immersive artwork, a top grabber, left-aligned title/artist, favorite and more actions, a timeline, large transport, volume, and lyrics/output/queue footer. The queue guide documents title swiping, shuffle/repeat, drag reorder and swipe removal. Reference image `2a0095cf-3303-4409-a9a9-a420ef451563` is 490×1008; the visible app content is approximately x22–468/y76–956. Within that content, metadata starts near 53% height, timeline at 62.8%, transport center 74.5%, volume 85.8%, footer 93.5%. These are image observations, not measured Android dp or animation traces.

## Tasks

1. Write failing Compose interaction tests for volume/output, queue shuffle/repeat/reorder/removal, title swipe, collapse and large-font control access. Existing transport/favorite/search tests remain regression coverage.
2. Extract a cohesive player component and original vector controls. Implement immersive blurred artwork, observed control hierarchy, scrubbing preview/commit, large play/pause morph, volume and footer. Keep unavailable capabilities disabled with a real path to import local lyrics.
3. Implement an interruptible draggable expansion with artwork bounds interpolation, track-title gestures, reduced-motion behavior, and Android back dismissal. Preserve underlying library/navigation state.
4. Bind new commands to the Android media session: system volume, shuffle, reorder, output settings, timed-lyrics import. Queue edits act on Media3; no simulated playback.
5. Verify the whole available JVM/shared/provider/Android unit suite, Android debug and release packaging, lint, source audit and deterministic rendered frames. Record device/reference limits and avoid a parity claim without a comparison.

## Review focus

- A seek preview must not jump back to live position before release and must not commit outside 0–1.
- Queue drag/removal must preserve the active track and forward the correct original Media3 index.
- Song changes must not leave old lyrics, artwork or gesture state attached to the new track.
- System-volume changes and hardware-volume changes must both reach the UI; unavailable controls must remain disabled.
- Small screens, large fonts, unknown duration, empty queue and reduced motion must remain operable.

## Ledger

Started from the existing independent Android development slice; prior unrelated workspace changes retained. Native implementation selected by the user's continuous-work instruction. No new platform project, upstream mutation, public repo or publication is required.


## Execution record

The initial Apple-reference player slice passed 13 interaction tests after a fresh review reproduced and fixed stale seek gestures, modal click-through, and shuffled queue indexes. Native volume, Media3 shuffle, reorder and LRC import are bound to actual services. New user requirements then superseded the fullscreen layout with universal materials and coordinated navigation; continued in `2026-10-28-liquid-glass.md`. Reference fidelity and device/audio gates remain outstanding.
