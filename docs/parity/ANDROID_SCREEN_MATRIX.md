# Android screen matrix

Apple Music public screenshots and guides are the user-authorized reference. No Vyon media is available. Test renders are Rynx regression evidence; no exact comparison score or device parity is claimed.

| Screen | Working implementation | Comparison / acceptance still needed |
|---|---|---|
| Listen Now | Apple-backed chart/editorial/city/tour shelves, local recommendations, real persisted recent metadata | Source reliability, complete catalog and native validation |
| Browse / Explore | Dynamic Apple genre grid with original vector graphics and genre-chart detail | Native/reference comparison |
| Search | Local filter, Apple public song metadata and real covers first, InnerTubeX YouTube Music fallback, conservative recording matching and stream resolution; search capsule grows into a field and Android Back returns to the prior tab | Apple artwork/catalog parsing and InnerTubeX catalog tests added on this revision; run pending. Live stream/error/offline and reference comparison remain unverified |
| Library / Artists / Albums | iOS-inspired GlyphStudio-icon category grid for Playlists, Artists, Albums, Songs and Favorites with real accessible navigation; tagged local artist and album indexes, artwork-led detail pages, local collection queue playback, metadata persistence, favorites, SQLDelight playlists and backups | Reference screenshot pixel/motion comparison, physical-phone library import and long-list performance, richer multi-disc/sort metadata, folders and offline/download management |
| Compact player | Single glass capsule with shared artwork/metadata/transport; hides app navigation | Android gesture/optical checks and reference recording |
| Expanded player | Same surface grows continuously; real seek, favorite, transport, volume/output; separate navigation dock | Device/audio, predictive back and exact motion comparison |
| Queue | Real playback-order indexes, shuffle/repeat, drag and accessibility reorder, swipe remove, clear upcoming | Native session instrumentation and exact visual comparison |
| Lyrics | Real imported timed/enhanced LRC, word progress, seek, follow and scroll recovery | Native picker/recreation test and provider/translation features |
| Appearance | Glass/Solid/AMOLED, live preview, all optical/motion settings and Android storage | Actual device persistence/performance and optical acceptance |
| Settings and About Rynx | iOS-inspired grouped audio, appearance and library preferences with a separate About destination: Rynx logo, host-reported app/build/backend information, developer credit and working repository/license/issue links; dark appearance remains default | Optical comparison and accessibility on physical Android phones, especially large text and external-browser handoff |
| Radio | Explicit unavailable state and working Search action | Radio implementation |
| Audio settings / EQ | Shared 10/31-band and parametric DSP controls, presets, Pure Mode, headroom, bass/treble, width, balance, crossfeed, compressor and limiter controls connected to Media3 | Audible response, device route/rate reporting and DSP performance need device verification |
| Artist / album detail, accounts and remaining settings | Local artist/album detail implemented with real metadata, artwork, track listing and Play Collection actions. Remote catalog artist/album detail and user accounts are not implemented | End-to-end native collection playback test, remote detail integration and product feature completion |

All existing eligible buttons, inputs, sliders, switches, segmented controls, player menus, queue panels and Appearance sheets use the centralized material contract. Untouched/unimplemented product screens are not counted complete. Large-font, landscape and three-mode headless checks supplement rather than replace Android device tests.


## Screenshot-directed UI implementation (2026-10-30)

The owner provided five screenshots as a **visual direction** for Rynx and explicitly requested the owner's own [GlyphStudio](https://github.com/bxanedot/GlyphStudio) symbols. Rynx already has a pinned `dependencies/GlyphStudio` Git submodule (`7db5de2`) and uses `SfSymbolIcon` via the shared `RynxGlyph` registry. The new category icons map directly to GlyphStudio `person.crop.circle`, `square.stack` and `music.note.list`; `GlyphRegistryTest` verifies the mapping exists rather than silently substituting unrelated icons. This change **does not** change the GlyphStudio source, fork its implementation or replace the user's icon library.

The new screenshot-directed Library navigation adds rounded shortcut cards for Playlists, Artists, Albums, Songs and Favorites; artwork-led artist and album detail pages; working Back and play-collection actions; and a retained dark/light/AMOLED material policy. Data comes from imported local media tags; unknown albums are not fabricated. Existing queue, playlist and MediaSession integrations remain in place.

This is an implementation pass, **not a pixel-perfect parity claim**. User-provided screenshot assets are not checked into the project, and per-screen optical verification on physical Android devices remains open. Apple screenshot artwork, logos and proprietary resources are not copied into the app. GlyphStudio's implementation and Apple's symbol artwork have separate licensing conditions; see `docs/cleanroom/LICENSE_AUDIT.md`.
