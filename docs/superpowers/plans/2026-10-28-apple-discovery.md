# Apple-first discovery and local recommendations

Continue the functioning Android/material checkpoint; no prohibited source references. User requests Apple-first metadata, YouTube Music fallback, real Apple-derived Explore genres and Home discovery (global/region/neighbors Daily 100, best new songs, new this week, recent plays, trending, city charts and tour content). No fake chart geography, recommendations, artwork rights or unavailable feeds.

1. Verify official Apple public metadata/feed/genre and authorised MusicKit contracts; retain actual endpoint evidence. Keep protected feeds explicitly unavailable without required access. Never borrow website tokens or use iTunes previews as full playback.
2. Test and implement shared immutable metadata/discovery models, conservative cross-provider matching and original recommendation ranking with artist/genre affinity, popularity, freshness, diversity and replay penalties. Recommendations are Rynx decisions, not claims to reproduce Apple's private algorithm.
3. Implement typed KMP Apple provider, bounded network requests, actual genre/chart parsing, cached/offline results, region/neighbor handling and optional authorised MusicKit access. Apple failures fall back to Excor metadata; playback remains explicit Excor/NewPipe.
4. Add Home sections and Apple-derived genre tiles using original vector decorations and central material controls. A chart drilldown shows its actual ranked results. Connect song selection to verified cross-provider identity and real playback. Persist region and listening history.
5. Verify provider fixtures, ranking/matching/error/cancellation behavior, actual public endpoints, shared UI actions, Android build/lint and native CI. Keep requested but unavailable editorial/city/tour capabilities incomplete with exact reasons.

## Ledger

Research delegated under the parallel-agents skill to an independent read-only official-source researcher while the parent ships the already-tested checkpoint and develops shared contracts. No production files shared between workers. Apple promotional content terms must be respected; original tile graphics and factual metadata can avoid embedding unapproved Apple artwork.


Final review: independent read-only review found five Important issues: alternate versions accepted by matching, hidden modal playback feedback, fallback shelf erased by learning, UI-dependent/replay/seek-biased listening, and empty artist enrichment counted as a chart success. Four behavioral regressions were observed RED in shared/provider/UI tests. New service-owned listening tracker tests initially failed to compile because the required tracker did not exist; its runtime native background test is pending CI. Fix pass expands version guards, enriches before matching, adds modal progress/errors, preserves fallback-only candidates, moves signals/history to the service, and makes empty/partial enrichment explicit. No second review requested.

Native CI evidence: after fixing runner KVM permissions, LaunchTest reproduced NetworkOnMainThreadException during OkHttp TLS pool teardown in Activity.onDestroy. Network resource teardown is moved to a process-owned IO cleanup scope, after cancelling/joining initialization. Launch/recreation is the existing native regression; rerun pending.

Ruling: country Daily100 is a labelled Rynx daily snapshot of Apple storefront most-played, while Global is the actual Apple Global playlist — those sources have measurably different ranking contracts — users see the source distinction and no synthetic global chart. Cost if wrong: users may expect exact country daily-playlist ranking.
Ruling: New on tour displays genuine Apple tour set lists and links to concert discovery, with no fabricated show dates — no verified public concert-date API is available — the date/venue experience remains incomplete.
Ruling: New this week filters actual release dates over New Music Daily — no verified universal upcoming-release feed exists — discovery does not cover every release.
Final: minor (deferred): Android favorite/skip training and artist-image display are not wired; the shared ranker/API alone does not complete them. Artwork/service usage audit remains a release gate separate from URL availability.


Final fix validation: default full suite 106 cases / 104 passed / 2 live opt-in skips / zero failures; Android debug/test packaging, lint, unit and source audit pass. Alternate recording, missing modal feedback, fallback erasure and empty enrichment have observed behavioral RED→GREEN cases. Service-owned tracker covers seek/pause/repeat and background-independent sampling; actual native BackgroundListeningTest remains pending. The harness's nonmonotonic timestamps exposed a Compose spatial-index error; its clock now advances monotonically and all 43 Compose cases pass.


Reference refinement: inspected current public Apple Browse by Genre at a 390×844 viewport. Source uses square artwork/captions, unlike the initial rectangular overlays. The geometry regression was observed RED and now passes; Browse categories parse real curator IDs separately from canonical genre IDs. Original vector artwork remains a recorded substitution. Daily-refresh/backoff shared test was observed missing, implemented, and passes.

Native runtime continuation: API 36 test run reproduced empty player commands. Media3's deprecated onConnect super default is a sentinel, not the real available commands; converting it caused every player action to be ignored. Moved to documented onConnectAsync and own-package/UID grants. Native regression rerun pending in a separate fast phone workflow, while release/shared verification continues independently. No tests are removed or marked passed.

Native runtime result: Media3 command fix passes generated-PCM playback and background-listening checks on API 36. Two remaining failures occur when activating the compact player through accessibility. Expansion is now an explicit action on its named root semantic node; native selectors handle merged descriptions and actionable system-picker ancestors. All 43 Compose app cases, seven shared UI cases and Android debug/test packaging/lint pass after this change; native rerun pending.
