# Phase 11f — The four web defects · status

**State:** in progress — opened on 01/09/2026 on `phase/11f-web-defects`, branched from `dev` once 11e merged in [#214](https://github.com/LuchoC-Dev/little-spaceship/pull/214)
**Updated:** 01/09/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch. Read those for what each one did; this is what the phase amounts to.

## Done

Nothing yet. The phase opened on 01/09/2026.

**It is the last phase of the 11 group.** 11a, 11b, 11c, 11d and 11e are on `dev`; this one was planned to run in parallel with all of them from day one and did not, so it inherits none of their work and blocks none of it.

## In progress

Five tasks, and **every one of them already had an issue** — these are defects found by playing the deployed build on 25/08/2026, not tasks invented by the plan:

| Task | Issue | Lane |
|---|---|---|
| 1 | [#40](https://github.com/LuchoC-Dev/little-spaceship/issues/40) — QUIT does nothing on the web target | `game-presentation`, **decision before code** |
| 2 | [#41](https://github.com/LuchoC-Dev/little-spaceship/issues/41) — losing pointer lock breaks mouse control until the page is refocused | `game-presentation` |
| 3 | [#42](https://github.com/LuchoC-Dev/little-spaceship/issues/42) — no in-game options; volume cannot be changed while playing | `game-presentation` |
| 4 | [#43](https://github.com/LuchoC-Dev/little-spaceship/issues/43) — the shield and the attachment are invisible | `game-presentation`, **wiring before drawing** |
| 5 | [#19](https://github.com/LuchoC-Dev/little-spaceship/issues/19) — `game` has no test suite | decide, or execute what 11a decided |

## Blocked

Nothing is blocked. This phase depends on no other phase and touches no file another phase is changing: `game/`, `desktop/` and `web/`, with nothing in `core/` and nothing in `assets/data/`.

**But one acceptance criterion cannot be satisfied by any agent, and this is the second phase in a row where that is structural rather than a scheduling problem.**

Every closure must cite **the deployed build**, not a local desktop run. `CLAUDE.md` records why: *"Headless Chrome cannot validate the web runtime. It fails under SwiftShader even when a real browser works, so CI can only verify that the build compiles."* CI proves the build compiles; **a human proves it runs**. [#41](https://github.com/LuchoC-Dev/little-spaceship/issues/41) is the sharpest case — it is verified by losing pointer lock in a real browser and regaining control, and there is no other way to verify it.

Phase 11e ran in two halves for the same reason and the play session reversed a change made the day before on the repository's own arithmetic, and found two defects that a clean generated document, a green `pre-pr-check` and a `reviewer` audit had all passed. **This phase should expect the same shape**: agents build, and the verdict comes from a browser somebody is looking at.

## Decisions taken while implementing

Record here anything decided that the plan did not specify, and why. If it changes a game rule, it also belongs in `docs/planning/08-decisions-and-open-items.md`.

**Task 1 is a decision before it is code**, and the plan says so. `MenuScreen.java:30` wires QUIT to `Gdx.app::exit`, which closes the window on desktop and can do nothing in a browser — JavaScript may not close a tab it did not open. Hide it on web, give it a different meaning there, or accept it; `docs/planning/02-mvp-functional-spec.md` asks for Play/Options/Quit and was written for a desktop game. **It is the first dead control a stranger meets, so "accept it" needs a reason if it wins.**

## Notes for whoever comes next

**Two things this phase must not do, both of them named by the plan and both of them mistakes this repository has already made.**

**Do not fix [#43](https://github.com/LuchoC-Dev/little-spaceship/issues/43) by drawing.** `module-satellite`, `ship-bank`, `ship-tilt`, `ship-hit`, the thrust and muzzle effects and five `icon-*` glyphs are **already in `assets/atlas/sprites.png` and referenced by nothing** — art waiting for a system. Check what exists before asking `visual-designer` for anything new. The inverse failure happened four times in two days: art a phase called delivered existed only under `docs/design/` with nothing in `assets/` and no code loading it. This is that failure from the other side.

**Do not assume a fix instead of verifying it**, which is precisely how [#41](https://github.com/LuchoC-Dev/little-spaceship/issues/41) shipped. Phase 09's task 4 was *"verify pointer capture"*; it was never actually verified, and this is the defect that would have caught. Everything else in that phase was checked against reality and this one criterion was assumed.

**Two adjacent backlog items were given issues at this phase's opening rather than being fixed in passing**, as the plan instructs — they are real and they are not in scope:

- [#218](https://github.com/LuchoC-Dev/little-spaceship/issues/218) — the shooting sound glitches under sustained fire. Hypothesis only: `Sound` instance exhaustion. **Not diagnosed.**
- [#219](https://github.com/LuchoC-Dev/little-spaceship/issues/219) — the download is 2.5 MB and 1.3 MB of it is two uncompressed music WAVs. Measured on 25/08, not estimated. OGG is the single largest load-time win available and libGDX plays it on both targets.

**Out of scope and stated so it is not drifted into:** anything in `core/`, new art, and Safari — Chrome and Firefox are verified, Edge was dropped by the project owner, and Safari stays unverified unless the owner says otherwise.

**`assets/startup-logo.png` must still exist when this phase ends.** Without it the app crashes when preloading finishes, with an error that never mentions the logo.
