# eXcor engine archive

On 2026-10-29, bxane requested that Rynx stop using and archive eXcor. The Rynx submodule reference (`bxanedot/eXcor`, commit `deb51bafc219645133f3f9145a68719cade7165e`), adapter module, composite Gradle build, copied engine notices, and hourly refresh workflow were removed from the active workspace. The upstream `bxanedot/eXcor` repository was not modified or deleted; its history remains available there and in earlier Rynx commits.

The Android audio provider order is now InnerTubeX, then NewPipeExtractor. Apple song search/lookup and artwork use Rynx's independent client for Apple's public metadata APIs. Search and stream behavior have deterministic adapter tests, but live provider extraction and audible Android playback remain unverified.

The requested YouTube.js package is JavaScript (`youtubei.js` on npm). It is not an Android/Kotlin artifact and is not in the Android app's active dependency graph. Rynx does not embed a JavaScript runtime or claim YouTube.js Android support. Adding a separate JS host would require an explicit supported runtime and integration tests; the current Android provider chain remains the two native/Kotlin providers above.

This note records the current architecture; it is not an endorsement, a provider availability claim, or a release-readiness statement.
