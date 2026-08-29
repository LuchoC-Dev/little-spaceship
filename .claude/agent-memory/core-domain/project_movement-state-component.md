---
name: movement-state-component
description: Building Trajectory (per-entity elapsed time + origin) for #161 (phase 11c task 1) — why origin was added despite invariant 6, the reflection-guard tests that must be extended for any new ComponentStore, and the deliberate "advances but nothing reads it yet" scope.
metadata:
  type: project
---

Phase 11c, task 1 (#161) added `core/domain/component/Trajectory.java`: `elapsed` (mutable float,
seconds since spawn) and `originX`/`originY` (immutable floats, the `Transform` at spawn). Advanced
by a new `MotionSystem.advanceTrajectories` step, called right after `applyPlayerInput` and before
`integrate` inside `SystemOrder.MOTION` — no new stage, per invariant 5.

**Adding a `ComponentStore` field to `World` requires two guard tests, not one, or the build breaks
for reasons that look unrelated to the change.** `WorldTest.destroyStripsEveryComponent` discovers
every `ComponentStore` field on `World` by reflection and asserts each was populated by
`populateEveryComponent` — that method must be extended by hand for a new component or the whole
test fails with a message naming the missing store. Separately, `World.destroyEntity` must call
`.remove(entity)` on the new store explicitly; nothing catches a forgotten one automatically except
that same reflective test (which checks every discovered store ends up empty after destroy). This is
the "reflection-based ComponentStore test guards" mentioned in earlier memory — this is the first time
I actually hit it end to end and had to extend `populateEveryComponent`.

**Origin (`originX`/`originY`) was a judgment call under invariant 6, not something the plan or issue
named explicitly.** The issue's acceptance criterion only says "elapsed time and whatever the shape
needs". I included origin because every shape the roadmap names by example (U-turn, diagonal, curve)
structurally needs a fixed reference point once the entity's own `Transform` has already moved away
from where it spawned — not a speculative field, but the one piece of data a "function of elapsed
time" cannot do position-relative shapes without. Recorded the reasoning explicitly in the status
fragment in case a reviewer disagrees; it's the kind of addition invariant 6 exists to catch if
wrong.

**Task 1 deliberately advances state nobody reads yet.** Nothing in `ComponentFactoryRegistry` attaches
a `Trajectory` to a spawned entity — that's tasks #163/#164, someone else's branch and a later round.
So this PR's own tests have to manually `world.trajectories().set(entity, new Trajectory(...))` to
exercise the advance at all; there is no way to demonstrate it through the normal spawn path yet.
Determinism replays pass trivially unchanged in this PR because no real entity in the game carries the
new component — that's expected here, not a sign the phase is finished.
