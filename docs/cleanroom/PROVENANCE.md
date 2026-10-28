# Provenance
Rynx documentation, tests and application-domain code were independently authored in this session from the user requirements and public Kotlin APIs.
No prohibited implementation sources were consulted.
Gradle wrapper scripts and JAR were retained unchanged from approved VyonEx commit 239a8d056c90b2101bf04de27a639d591acc69a3 (Gradle build tooling, Apache-2.0). Wrapper properties select that dependency's Gradle 9.8.2 distribution, with its official distribution SHA-256 added.
Dependencies are Git submodules, with upstream notices retained. Rynx does not copy the VyonEx sample application's playback or queue implementation.
The UI uses independently authored Compose components and original geometric glyphs after the user authorized Apple Music references. Public Apple UI guide screenshots were inspected as evidence only; no Apple artwork or font files are embedded. Player/material motion is independently authored; its exact reference trajectory remains unmeasured. The VyonEx adapter uses the approved engine's public API declarations only; VyonEx internals and its sample UI were not used as application implementation material.

Gradle LICENSE/NOTICE retained from the official Gradle 9.8.2 distribution (the wrapper-pinned, checksum-verified tooling distribution). Independent code reviewer consulted only Rynx files and respected source isolation. No public application release or upstream modification occurred.

Android-first slice: independently authored activity, document-picker persistence, Media3 service/controller host, generated-PCM instrumentation check, and original geometric launcher vector. No Vyon, Convx, BitChord or Vivi Music implementation material was used. Existing Apple-reference shared UI is provisional carryover and not evidence of Vyon parity.

Provider continuation: the Rynx-owned VyonEx adapter was moved into the JVM/Android shared KMP source group using the approved VyonEx public API. Android search, stream resolution and per-request header delivery were authored independently. The user authorized one fallback; Android uses NewPipeExtractor v0.26.5 through its documented API only, with its upstream GPL-3.0-or-later license retained. No extractor internals were translated into Rynx. NewPipe live behavior and device playback remain unverified. Rynx uses GPL-3.0-or-later with retained notices and pinned source references.


Universal-material continuation: public Haze 2.0.1 documentation and permitted APIs supplied source-backed backdrop capture. Rynx independently authors appearance policies, controls, geometry, gestures, navigation handoff, render-budget decisions and tests. Original silent MP3/PCM and geometric embedded covers provide test fixtures. Public Apple player/queue stills inform observed proportions; reference images are not embedded in the app. An independent final reviewer read Rynx implementation only and did not consult prohibited sources.

The user explicitly requested the existing bxanedot/rynx repository. Metadata inspection found it empty and independent (not a fork) before this implementation was prepared for its initial source push. No original app or approved dependency repository is modified. The source has development status; publication does not establish a production release.
