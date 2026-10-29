# Rynx implementation sequence
The user's master prompt is the scope and execution order. User follow-up replaces Vyon visuals with Apple Music references; measured fidelity is still not claimed.

1. Establish clean-room instructions, provenance, pinned approved dependencies, requested inventory and reference capture protocol.
2. Obtain reference evidence; expand requested inventory into a complete observed inventory with hidden settings and platform variants. Measure screen/motion specifications before major UI work.
3. Meanwhile build shared queue foundation: immutable identity-based entries, repeat/end/skip semantics, deterministic editing, empty/error state invariants. Validate in commonTest on JVM. These semantics are independently chosen foundations, pending observable parity validation.
4. Prepare compatible upstream toolchains, audit licenses, implement typed Excor adapter from actual public API with cancellation/error/refusal tests.
5. Once reference evidence exists, add Compose shells and measured design system, native playback and a complete local/Excor playback vertical slice. Verify real audio and background lifecycle, then visual and motion recordings.
6. Implement independent Spotify domain and supported native integrations from observed feature contracts and official APIs.
7. Continue queue/lyrics/audio/download/library vertical slices, advanced capabilities, synchronized rooms and sandboxed providers with meaningful tests.
8. Complete TV/Wear/desktop target behavior, produce installable artifacts, compare deterministic screenshots and frame trajectories, test DSP/audio/device interruptions and measure performance.

No future step is complete merely because its folder or contract exists. No public remote or upstream mutation is authorized.

Executed initial scope: source isolation, requested inventory, reference research, shared queue foundation, adaptive Compose UI, desktop local decode/output code, persistence, render/callback tests and a Linux development JAR. Original phases 4–12 and much of phase 3 remain incomplete.

## Android media and library continuation

The current Android-first continuation adds an independently authored shared PCM DSP runtime, routes it through the existing Media3 audio sink, stores DSP settings locally, and exposes a real settings/player control surface. It also adds shared SQLDelight playlist rules/persistence and an Android sidecar schema for download attribution. The sidecar writer is not yet connected to a download-completion pipeline because Rynx still has no completed downloader; it must not be described as attribution on every download yet.

Next gates: run Android instrumentation on the current branch, verify audible playback and DSP behavior on a device, connect attribution when an authorized download pipeline exists, and keep Spotify and service/artwork eligibility blocked until their actual prerequisites and tests are available.
