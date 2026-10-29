# Provider policy and updates

Following bxane's 2026-10-29 request to archive eXcor, Android uses InnerTubeX v0.11.2 as the primary YouTube Music search/audio provider and NewPipeExtractor v0.26.5 as the Android fallback. The shared InnerTubeX adapter uses the published KMP API and accepts only direct HTTPS audio supported by the existing Media3 path. Explicit account, age, region, availability, or provider refusals stop resolution instead of trying another engine.

Apple search, typed song lookup, charts, and cover artwork use Apple's public iTunes/RSS metadata endpoints through Rynx-owned KMP code. Apple audio previews are not used for playback. Apple image URLs are upgraded to a large image size only when the public URL uses Apple's standard dimension marker. Artwork/API availability does not establish artwork usage rights; see `docs/cleanroom/APPLE_CATALOG_EVIDENCE.md`.

YouTube.js is the JavaScript package `youtubei.js`, not a Kotlin/Android library. It is not bundled, and there is no Android JavaScript runtime or adapter in Rynx. Do not represent it as an active Android fallback. A future supported JS target needs its own host implementation, version/license pin, and runtime tests.

eXcor's submodule, Rynx adapter, dependency notices, composite build, hourly refresh script, and scheduled workflow were removed from the active project. The upstream eXcor repository was not modified. The archived reference and prior implementation history are recorded in `docs/archive/EXCOR.md`.

Rynx is GPL-3.0-or-later and retains InnerTubeX, QuackJS, NewPipeExtractor, and other active notices. Provider pins are updated deliberately through a reviewed change; installed apps do not fetch or execute new extractor code at runtime. Provider service availability, live extraction, and audible playback must be verified separately.
