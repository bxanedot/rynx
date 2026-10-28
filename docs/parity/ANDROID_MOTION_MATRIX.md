# Android motion matrix

All values below are independently authored settings, not measured Vyon or Apple trajectories. Public Apple stills and interaction guides establish layout/behavior only. Android predictive back and frame-by-frame reference comparison remain unverified.

| ID | Trigger / transition | Current implementation | Interruption and accessibility | Evidence |
|---|---|---|---|---|
| MOT-001 | Press / release | Scale 1 → .965; spring stiffness 700, damping .86; shared material highlight | New input continues from current value; reduced motion snaps | Headless shared control and interaction tests |
| MOT-002 | Navigation selection | Capsule position/width spring 520 / .86, coordinated tint and icon scale | Reversible; reduced motion snaps; radio-group semantics | MaterialUiTest |
| MOT-003 | Compact → expanded player | One surface/artwork/metadata/transport; direct drag progress 0–1; release spring 420 / .91 | Velocity normalized to progress/second; nearest snap unless flung; rapid reversal retains progress; reduced motion snaps | PlayerMotionTest, PlayerMorphTest, ApplePlayerUiTest |
| MOT-004 | Player → navigation handoff | Same player progress; smoothstep reveal .42–1; translate, scale, opacity; interactive at reveal ≥ .96 | Hidden navigation is absent from hit testing and accessibility; reversal uses same clock | MaterialUiTest navigation and gesture cases |
| MOT-005 | Player secondary controls | Progressive reveal .78–.98; constant expanded geometry prevents jumps as height changes | Controls enabled only at the open endpoint; modal backdrop blocks the library | PlayerMorphTest and modal interaction cases |
| MOT-006 | Artwork → lyrics / queue | One artwork/metadata geometry driven by pane progress; collapse returns continuously to compact bounds | Grabber owns collapse; content owns scroll; fading panels cannot receive input | Morph endpoint, intermediate sweep and retraction tests |
| MOT-007 | Play / pause | Original vector geometry interpolates between triangle and bars | Same control and semantics throughout; reduced motion snaps | Transport interaction cases |
| MOT-008 | Timeline / volume | Direct drag preview; release commits normalized value; held seek waits briefly for host acknowledgement | Cancellation/track change abandons stale seek; unknown duration disables seeking | Timeline/scrub and track-change tests |
| MOT-009 | Lyrics | Timed line emphasis, word progress, automatic following; user scroll pauses following and offers recovery | Current-line seek uses real host action; reduced motion avoids animated follow | LRC parser and lyric interaction cases |
| MOT-010 | Appearance / sheets | Shared spring geometry and modal input ownership; tint uses 240ms; artwork uses 420ms | Full / reduced / follow-system preference; live appearance updates | Mode, modal and render tests |

Gesture release threshold is ±1.1 expansion units per second. Springs are interruptible and carry their current velocity. AMOLED retains the same interaction/motion tree while omitting optical effects. These headless checks do not establish device frame pacing, optical equivalence or reference animation fidelity.
