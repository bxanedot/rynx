# Android screen matrix

Apple Music public screenshots and guides are the user-authorized reference. No Vyon media is available. Test renders are Rynx regression evidence; no exact comparison score or device parity is claimed.

| Screen | Working implementation | Comparison / acceptance still needed |
|---|---|---|
| Listen Now | Apple-backed chart/editorial/city/tour shelves, local recommendations, real persisted recent metadata | Source reliability, complete catalog and native validation |
| Browse / Explore | Dynamic Apple genre grid with original vector graphics and genre-chart detail | Native/reference comparison |
| Search | Local filter, Apple public song metadata and real covers first, InnerTubeX YouTube Music fallback, conservative recording matching and stream resolution; search capsule grows into a field and Android Back returns to the prior tab | Apple artwork/catalog parsing and InnerTubeX catalog tests added on this revision; run pending. Live stream/error/offline and reference comparison remain unverified |
| Library | Local document import, metadata/covers, favorites, saved remote-track metadata, SQLDelight playlist create/rename/add/remove/reorder/delete, playlist JSON import/export actions | Android picker file round trip, device persistence, sorting, folders and offline/download management |
| Compact player | Single glass capsule with shared artwork/metadata/transport; hides app navigation | Android gesture/optical checks and reference recording |
| Expanded player | Same surface grows continuously; real seek, favorite, transport, volume/output; separate navigation dock | Device/audio, predictive back and exact motion comparison |
| Queue | Real playback-order indexes, shuffle/repeat, drag and accessibility reorder, swipe remove, clear upcoming | Native session instrumentation and exact visual comparison |
| Lyrics | Real imported timed/enhanced LRC, word progress, seek, follow and scroll recovery | Native picker/recreation test and provider/translation features |
| Appearance | Glass/Solid/AMOLED, live preview, all optical/motion settings and Android storage | Actual device persistence/performance and optical acceptance |
| Settings and About Rynx | iOS-inspired grouped audio, appearance and library preferences with a separate About destination: Rynx logo, host-reported app/build/backend information, developer credit and working repository/license/issue links; dark appearance remains default | Optical comparison and accessibility on physical Android phones, especially large text and external-browser handoff |
| Radio | Explicit unavailable state and working Search action | Radio implementation |
| Audio settings / EQ | Shared 10/31-band and parametric DSP controls, presets, Pure Mode, headroom, bass/treble, width, balance, crossfeed, compressor and limiter controls connected to Media3 | Audible response, device route/rate reporting and DSP performance need device verification |
| Artist / album detail, accounts and remaining settings | Not yet implemented | Functional implementation and tests |

All existing eligible buttons, inputs, sliders, switches, segmented controls, player menus, queue panels and Appearance sheets use the centralized material contract. Untouched/unimplemented product screens are not counted complete. Large-font, landscape and three-mode headless checks supplement rather than replace Android device tests.
