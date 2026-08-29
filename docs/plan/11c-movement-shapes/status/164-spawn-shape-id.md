# 164 — a spawn chooses its movement shape, the archetype supplies the default

**Half written by `core-domain`.** `game-presentation` appends its own half below, for
`JsonContentSource.parseSpawnEvent`, once this half has landed.

## What was built

**The binding.** `SpawnEvent` (`core/src/main/java/dev/luchoc/littlespaceship/core/port/SpawnEvent.java`)
gains a seventh component, `trajectoryId` — the archetype's own default when `null`/empty, an
override otherwise, exactly the decision the project owner wrote on 27/08/2026. `hasTrajectoryOverride()`
mirrors `hasDrop()`'s pattern. The existing five- and six-argument constructors are kept, delegating
to the new seven-argument canonical one with `trajectoryId = null` — every existing call site across
`core`'s tests and `game`'s content loader keeps compiling unchanged, the same record-constructor
trick used for `dropSlot` in issue #23 (recorded in this agent's memory from phase 07).

**The evaluation.** `Trajectory` (`core/.../domain/component/Trajectory.java`) now carries
`trajectoryId` alongside `elapsed`, so `MotionSystem` knows which shape to re-evaluate — a plain
content-id string, the same pattern `Spawner.enemyId` already uses, not a cached
`TrajectoryDefinition` object. `ComponentFactoryRegistry.attachMotion` now attaches a `Trajectory`
for every entity it gives a `Motion` to (all kinds, `constant` included — see "one design decision"
below), through a new shared `ComponentFactoryRegistry.attachTrajectory(World, int, String)` that
both `attachMotion` (the archetype default) and `SpawnSystem.spawnWave` (a spawn event's override)
call.

`MotionSystem.advanceTrajectories` now does two things per entity per tick, in the same pass, in
this order: increments `Trajectory.elapsed` by the fixed step, then resolves
`world.content().trajectory(trajectoryId)` and writes `verticalVelocityAt(elapsed)` into that
entity's own `Motion.vy`, before `integrate()` runs — so this tick's integration already uses this
tick's velocity. This is the closed form `TrajectoryDefinition#verticalVelocityAt` computes directly
from `elapsed`, never an accumulation of per-tick deltas, per the catalogue's explicit instruction
and #163's own implementation.

`SpawnSystem.spawnWave` calls `ComponentFactoryRegistry.attachTrajectory` right after
`attachComponents`, only when `event.hasTrajectoryOverride()` is true — replacing the archetype's
just-attached default `Motion`/`Trajectory` with the override's.

## One design decision worth flagging: `Trajectory` is attached for every kind, not only `arc`

`Trajectory.java`'s own pre-#164 javadoc read "an entity with no Trajectory simply keeps whatever
constant Motion its trajectory gave it at spawn" — implying only entities on a non-constant shape
would get one. I attached it uniformly instead: `MotionSystem` re-evaluates every entity's
`Trajectory` every tick regardless of kind, and a `constant` shape's `verticalVelocityAt` ignores
elapsed time and returns the same value every tick, so the result is bit-identical to the one-time
snapshot that shipped before this component was wired in — verified by the new
`constantShapeStaysConstantAcrossTicks` test (`MotionSystemTest`). Uniform code with no per-kind
branching, at the cost of a component + a hash-map lookup per enemy per tick that a `constant` shape
does not strictly need. The MVP moves a few hundred entities at most; this was not worth a second
code path. `Trajectory.java`'s own javadoc was rewritten to describe this rather than the older,
narrower plan.

## The exact JSON key `game-presentation` should add, and where

**`"trajectory"`**, beside `"spawn"`, `"formationId"`/`"formation"` and `"dropId"` in a wave's spawn
entry — exactly the key the shape catalogue's own "For #164" section already named. Optional; absent
or empty means no override, the archetype's own `"motion"` spec decides as it always has.

```json
{ "at": 200.0, "enemyId": "enemy-rush", "formationId": "single", "atX": 0.5, "trajectory": "strike-run" }
```

Maps to `SpawnEvent`'s seventh component, `trajectoryId`. `JsonContentSource.parseSpawnEvent`
(`game/adapter/content/JsonContentSource.java:310`, not touched by this branch, per my module
boundary) should read it with `entry.getString("trajectory", null)` or equivalent and pass it through
to the seven-argument `SpawnEvent` constructor. No validation is expected there beyond what
`SpawnEvent`'s own compact constructor already skips (an unrecognised id fails once `SpawnSystem`
resolves it against `ContentSource`, the same as an unrecognised `enemyId` or `formationId` today).

## How I demonstrated one archetype, two shapes

`SpawnSystemTest.oneArchetypeTwoShapesFromOneSpawnEvent` (new): one `enemy-rush` archetype in test
content, `"motion": {"trajectory": "dive"}`. Two spawn events of the same wave, same archetype id:
the first with no override (spawns on `dive`, `vy = -80`), the second overriding `trajectoryId` to
`"strike-run"` (an `ArcTrajectoryDefinition`, `vy = -110` at elapsed zero). After both ticks, one of
the two live `Motion`s reads `-80` and the other `-110` — the same content id, `enemy-rush`, entered
on two different shapes, exactly the phase's own goal sentence.

`MotionSystemTest.arcShapeIsFollowedAndCurves` (new): a lone entity on `strike-run` run for 300
ticks (5 simulated seconds). Every tick's own vertical delta is asserted strictly greater than the
previous tick's (`ay > 0` decelerating the descent, then reversing it) — proof the velocity is a
genuinely different number every tick, not a value read once and integrated linearly. The entity's
`y` is asserted to start climbing again after bottoming out (`sawSignChange`), and `Motion.vy` at
`t = 5s` is asserted against the closed form `vy + ay·t = -110 + 27·5 = 25`, matching #163's own
worked numbers rather than a value this branch invented.

## Determinism replays

`./gradlew :core:test -q` — clean, no output, exit 0. This runs every existing replay
(`BombReplayTest`, `BossReplayTest`, `LevelContentIntegrationTest`, `LevelScoreReplayTest`,
`SpawnerReplayTest`) unchanged and green, and `DeterminismRulesTest` in the same run.
`./gradlew :core:test --tests "*DeterminismRulesTest*" --tests "*ReplayTest*" -q` was also run
in isolation to confirm those specific suites pass on their own — clean, exit 0.

No replay's own behaviour changed: none of the existing fixtures use an `arc` trajectory or a
`SpawnEvent` override, so `Motion.vy`'s per-tick re-evaluation is a no-op on every entity those tests
spawn (all `constant`), which is exactly what `constantShapeStaysConstantAcrossTicks` isolates and
checks directly.

## Acceptance criteria — my half

- One archetype, two shapes, demonstrated in a test: done, `oneArchetypeTwoShapesFromOneSpawnEvent`.
- A shape that is not a constant vector exists and is followed: done, `arcShapeIsFollowedAndCurves`
  proves the velocity changes tick by tick and the path actually curves (climbs back up), not only
  that a field was set.
- Determinism replays pass: confirmed, `./gradlew :core:test -q` clean.
- `DeterminismRulesTest` stays green: confirmed, same run.
- `SystemOrder` unchanged: confirmed — `git diff` touches no file under `core/domain/system/` except
  `MotionSystem.java`'s and `SpawnSystem.java`'s own method bodies and javadoc; `SystemOrder.java`
  itself is untouched.
- Every javadoc this phase falsifies is corrected: `Trajectory.java`'s class javadoc rewritten (the
  "no Trajectory means constant" sentence no longer describes this branch's behaviour);
  `MotionSystem`'s class javadoc and `advanceTrajectories`'s own javadoc updated to describe the
  evaluation, not just the accumulation; `ComponentFactoryRegistry.attachMotion`'s javadoc updated;
  `SpawnSystem`'s class javadoc gained a paragraph on the override. `TrajectoryDefinition.java` and
  `Motion.java` were already correct going into this branch — checked, nothing to change in either.

## Files touched

- `core/src/main/java/dev/luchoc/littlespaceship/core/port/SpawnEvent.java`
- `core/src/main/java/dev/luchoc/littlespaceship/core/domain/component/Trajectory.java`
- `core/src/main/java/dev/luchoc/littlespaceship/core/domain/content/ComponentFactoryRegistry.java`
- `core/src/main/java/dev/luchoc/littlespaceship/core/domain/system/MotionSystem.java`
- `core/src/main/java/dev/luchoc/littlespaceship/core/domain/system/SpawnSystem.java`
- `core/src/test/java/dev/luchoc/littlespaceship/core/domain/system/MotionSystemTest.java`
- `core/src/test/java/dev/luchoc/littlespaceship/core/domain/system/SpawnSystemTest.java`
- `core/src/test/java/dev/luchoc/littlespaceship/core/domain/WorldTest.java` (fixed `new Trajectory()`
  call sites broken by the constructor now requiring an id)
- `core/src/test/java/dev/luchoc/littlespaceship/core/domain/content/ComponentFactoryRegistryTest.java`
  (asserted the new `Trajectory` attachment alongside the existing `Motion` one)

## `pre-pr-check`

Run against `phase/11c-movement-shapes` after the code and test commits, before this status fragment
was added (the "records nothing in the phase status" line below is exactly this fragment being
written next):

```
pre-pr-check: branch 'feat/spawn-shape-id' against 'phase/11c-movement-shapes'

pass branch name: feat/spawn-shape-id
pass 2 commit(s) on top of phase/11c-movement-shapes
pass commit subjects
pass no Co-Authored-By trailers
FAIL the working tree is not clean — commit or drop these before the pull request
       ?? docs/plan/11c-movement-shapes/status/164-spawn-shape-id.md
pass no build output in the diff
pass markdown links resolve
pass scripts carry the executable bit
FAIL this branch does work and records nothing in the phase status
       add one file at docs/plan/<phase>/status/<issue>-<slug>.md describing what
       this task did, what it decided, and what it leaves open
pass no workflow file in the diff

code changed — running ./gradlew build
warning: Supported source version 'RELEASE_11' from annotation processor 'org.gradle.api.internal.tasks.compile.processing.TimeTrackingProcessor' less than -source '17'
1 warning
pass ./gradlew build

pre-pr-check: FAIL — 2 check(s) failed. Fix them; do not open the pull request.
```

Both failures are exactly this file being written and committed next; `./gradlew build` itself
already passed. **Do not open the pull request** — the coordinator opens it once
`game-presentation`'s loader half lands in the same branch.

---
