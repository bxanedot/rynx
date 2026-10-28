# Provider policy and updates

The user authorizes youtube.js, NewPipeExtractor or InnerTubeX as fallbacks. VyonEx remains primary. Android currently uses independently obtained NewPipeExtractor v0.26.5 for stream resolution after an eligible primary failure. Rynx is GPL-3.0-or-later; corresponding source, pinned dependency sources and retained notices accompany the implementation. Do not use fallback selection to evade account, age, geographic or rights restrictions.

`.github/workflows/refresh-vyonex.yml` checks stable VyonEx main hourly at minute 17 and on manual dispatch. It updates only a clean dependency checkout, requires a fast-forward stable revision, updates provenance pins, then runs boundary audit, provider/shared/interaction tests and Android packaging/lint. A passing pin is committed to the existing Rynx default branch. A failed candidate is retained as a compatibility proposal while the default branch keeps its previous pin.

This automation becomes active when pushed to the existing `bxanedot/rynx` default branch. GitHub scheduling is approximate, rather than a guaranteed 60-minute service. Historical source attribution is not rewritten by the updater. Installed applications do not download executable extractor updates at runtime. Provider updates are delivered through rebuilt applications.
