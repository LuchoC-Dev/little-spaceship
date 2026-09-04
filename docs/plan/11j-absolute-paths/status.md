# Phase 11j — Paths written where they happen, and a speed that does not resize them · status

**State:** **open.** Branch `phase/11j-absolute-paths`, created from `dev` on 04/09/2026. Three tasks; nothing merged yet.
**Updated:** 04/09/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch.

## Why this phase exists

**Its agenda came out of the project owner playing phase 11i**, not out of a plan — the second time in this group that a play session wrote the next phase.

Having watched the five path scenarios and approved the vocabulary, they worked out the consequence of the format themselves: a segment is velocity × duration, so raising speed **scales the shape** — an L becomes a bigger L with the same angles. Useful as a source of variations, and it makes "enter at the top centre and leave to the right" expensive to author, because the values have to be computed backwards before they can be written.

Their proposal was to keep the system relative underneath and put an absolute form on top. **That turns out to cost nothing in `core`**: a destination and a speed is a reparameterization of a velocity and a duration, resolved at load exactly as `mirrorOf` and the `wait` shorthand already are.

## The three decisions taken before the phase opened

- **An absolute authoring syntax**, resolved in the loader, with **no new `core` API**. The cost accepted knowingly: an absolutely-authored path can only happen in one place, because a wave's `atX` stops meaning anything for it.
- **"Faster" means the same shape at the same size, traversed sooner** — velocities up and durations down together. The other meaning, the shape growing, stays an authoring consequence and does **not** become a knob: nothing written asks for one.
- **Level 1 gets new trajectories, and `level-designer` decides how many.** The owner delegated the number explicitly, with the direction that paths should be how movement is authored from here *"al menos que sean movimientos simples"*.

## What that direction does not mean

**The three kinds do not substitute for one another.** `constant` is the simple case the owner exempted. `arc` produces a **smooth curve**, and a `path` — piecewise-constant velocity — cannot: it turns in angles. `strike-run`, `veer-left` and `veer-right` are the only curved motion in the game, and a `path` imitating one produces a polygon.

So: **`path` for what turns, waits or repeats; `constant` for what is straight; `arc` for what curves.** And `arc` is the closest thing this project has to the curves deferred out of this phase.

**Nothing is deleted here.** Every entry level 1 uses stays until 11k replaces it.

## Tasks

| Task | What | Owner |
|---|---|---|
| 1 | the absolute authoring syntax | `game-presentation` |
| 2 | the speed multiplier | to be decided: load-time or spawn-event |
| 3 | the trajectories level 1 will be rebuilt from, with a scenario each | `level-designer` |

## What is open

Everything. The phase has just opened.
