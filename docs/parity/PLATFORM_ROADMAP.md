# Platform roadmap

P0 Android phones: active. P1 tablets, P2 desktop, P3 TV, P4 Wear and the planned iOS/iPadOS port are gated on completed Android phone acceptance. Existing desktop work is paused, not deleted. No non-Android platform is being expanded.

Milestone A: Android app, common KMP UI/domain targets, lifecycle host, local file access and Media3 service introduced. All Rynx application code is Kotlin; shared UI/domain lives in `commonMain`, while Android OS integration stays in Kotlin `androidMain`/the Android host. iOS targets are not configured or built yet. Build/device results are recorded separately in ANDROID_VISUAL_RESULTS.md and ANDROID_AUDIO_TESTS.md. Database, DI beyond explicit construction, provider integration and navigation remain incomplete.

Milestones B–I remain incomplete: Apple Music-style screens/motion, live Excor playback, library/offline, Spotify, audio/lyrics, advanced scope, visual comparison and release readiness. Android Excor search/stream wiring and NewPipe fallback compile, but no live source test or device validation has passed. NewPipe's GPL release obligations are an open distribution blocker. No completion gate has passed. Compile API 37 and target API 36 are used by Rynx; AGP successfully installed API 37 for the approved Excor included build.
