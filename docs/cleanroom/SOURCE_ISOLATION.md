# Source isolation
Established 2026-10-27 in /workspace/rynx, a fresh Git repository with no remote.
No Vyon, BitChord, Convx or Vivi Music repository was cloned, searched, read or decompiled.
Initial workspace check searched filenames for reference media and AGENTS.md; no application source content was read.
Only explicitly approved VyonEx and GlyphStudio repositories were cloned as pinned submodules. The user later authorized NewPipeExtractor as an Android fallback; only its public API/Javadocs, release metadata and license were consulted and it is integrated as a pinned binary dependency.
The boundary is enforced by agent instructions, explicit repository allowlisting and provenance review. This is procedural isolation, not an OS security boundary: the process retains unrestricted filesystem/network access.
Do not mount prohibited repositories in future implementation sessions. Do not accept implementation specifications derived from them.

`python3 tools/audit_workspace.py` verifies the two approved dependency URLs/revisions, unchanged tracked checkouts and implementation symlinks. It does not establish global network/filesystem confinement.

The user later explicitly selected the existing `bxanedot/rynx` repository. Its empty-repository metadata was inspected before connecting it as origin. The clean-room source and tests can be published there; no new public repository is created and no original application/dependency repository is overwritten.
