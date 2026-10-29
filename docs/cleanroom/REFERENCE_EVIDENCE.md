# Reference evidence
| ID | Source | Permitted use | Observation |
|---|---|---|---|
| REQ-001 | User master implementation prompt, 2026-10-27 | Requirements only | Describes requested scope; does not establish current reference behavior |
| APPLE-007 | Apple Music Google Play listing, inspected 2026-10-28 | Public Android UI reference only | Promotional screenshots expose Home/Browse/Radio/Library/Search, recommendation and recent shelves, player controls and Radio. No source media copied into Rynx. |
| APPLE-008 | MakeUseOf, “How to Set Your Apple Music Equalizer,” screenshot of iPhone Music settings, https://www.makeuseof.com/how-to-set-apple-music-equalizer/ (inspected 2026-10-29) | Public UI reference for grouped settings presentation only | Dark black canvas, inset charcoal groups, uppercase section captions, compact text rows, trailing values, disclosure arrows and native green switches. Screenshot is not retained or copied as an app asset. |
| DEP-001 | Approved Excor README and library build configuration | Public API integration and dependency requirements | See EXCOR_INTEGRATION.md |
| DEP-002 | Approved GlyphStudio README, sfsymbols build and version catalog | Library integration requirements | Owner confirms implementation Apache-2.0; SF Symbols artwork terms are separate |

No Vyon screenshot, recording, running device or build is available. Apple Music public guides and Play Store promotional screenshots are authorized as design references, but no dynamic Apple recording, accessibility hierarchy or measured dp/color/motion tokens have been captured.
For each future capture record: evidence ID, build version, platform/OS, viewport in px/dp, density, theme/accent, font scaling, locale, source authorization, deterministic content, timestamp, SHA-256 and reproduction steps.
Record original frame timestamps for motion; do not infer spring coefficients from a single end-state screenshot.
Keep private reference media under reference-private/ (ignored); commit only permitted evidence and hashes.

User follow-up authorizes Apple Music across platforms as the practical visual reference and describes Vyon as matching Apple Music. See ../parity/APPLE_MUSIC_REFERENCE.md for public guides and listing evidence. Public Apple images are not application assets; avoid retaining store artwork in Rynx resources.

2026-10-28 Apple catalog continuation: official public metadata/genre/chart endpoints, visible editorial playlist anchors and public schema.org JSON-LD were checked independently. Public Apple Browse by Genre serves as a category reference for original geometric tiles. See APPLE_CATALOG_EVIDENCE.md. No Apple application JavaScript/private state or web bearer token was used as an implementation source.
