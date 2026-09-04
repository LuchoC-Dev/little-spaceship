# Phase 11i — A path is a list of segments, and a shape can be mirrored · status

**State:** **open.** Branch `phase/11i-path-vocabulary`, created from `dev` on 03/09/2026. Four tasks; nothing merged yet.
**Updated:** 03/09/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch.

## Why this phase exists

The project owner drew eleven enemy paths on 03/09/2026 and **the movement vocabulary cannot express any of them**. They need three things it does not have: an ordered list of segments, waits, and bounded loops — plus mirroring, so that a shape and its mirror are not two hand-written entries.

This is the first half of what the owner means by giving level 1 more depth. The second half, the choreography — a formation following one path in single file rather than moving in rigid parallel — is 11j.

## The two decisions taken before the phase opened

**Loops and waits are bounded.** The owner reached this themselves, and it is what makes this phase small rather than architectural.

`docs/plan/11c-movement-shapes/shape-catalogue.md` refuses `enterAndHold` with a specific hazard: an entity at rest inside the playfield never leaves, so `LifetimeSystem` never removes it and a `cleared` wave behind it cannot end. **A bounded path always ends by leaving**, so the catalogue's rule 3 survives unchanged, `LifetimeSystem` needs no change, and `Cleared` stays usable instead of being poisoned. An indefinite wait becomes a large number; a permanent loop becomes a large count.

**The representation stays relative.** A shape is reusable; the authored values are what put a path where the sketch puts it.

## What the code already gives, found before the phase opened

**The contract is already a pure function of elapsed time** — `core/port/TrajectoryDefinition.java`'s `verticalVelocityAt(float)`, described in its own javadoc as *"a pure function of `elapsedSeconds` and this shape's own parameters — reads nothing else"* — and `core/domain/component/Trajectory.java` holds exactly `trajectoryId` and `elapsed`.

**With bounded segments of known duration, a path is still a pure function of `elapsed`.** Walk the list accumulating durations. **No new per-entity state is needed** — which is exactly the cost `shape-catalogue.md` refused waypoints over: *"each costs per-entity path state well beyond the elapsed-time clock"*. **Bounding the loops did not override that refusal, it dissolved it.**

The one real change is that `vx()` is fixed for an entity's whole life — *"Horizontal velocity never varies with time in either kind"* — and a path that turns needs the horizontal component to be a function of elapsed time too.

## Tasks

| Task | Issue | Owner |
|---|---|---|
| 1 | the path contract and its evaluation, mirroring included | `core-domain` |
| 2 | the loader | `game-presentation` |
| 3 | entries enough to prove it, and a test scenario each | `level-designer` |
| 4 | [#252](https://github.com/LuchoC-Dev/little-spaceship/issues/252) — a dropped pickup falls | `core-domain` |

**The sketches are examples, not a delivery list.** The owner decides how many trajectories exist after seeing the system work.

## What is open

Everything. The phase has just opened.
