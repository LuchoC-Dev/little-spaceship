# #161 — Per-entity movement state

**Owner:** `core-domain` · **Branch:** `feat/movement-state` · **Status:** done, ready for review

## What was built

- `core/domain/component/Trajectory.java` — a new plain-data component: `elapsed` (mutable, seconds
  since spawn) and `originX`/`originY` (immutable, the `Transform` the entity was placed at). Origin
  is included alongside elapsed time because a shape described relative to "where it started" (a
  U-shaped attack run, a loop) cannot be resolved from elapsed time alone once the entity's own
  `Transform` has already moved away from its spawn point. No logic lives on the component itself.
- `World.java` — a fourteenth-plus `ComponentStore<Trajectory>` field, an accessor `trajectories()`,
  and removal in `destroyEntity` alongside every other store.
- `MotionSystem.java` — a new private step, `advanceTrajectories`, called from `update()` right after
  `applyPlayerInput` and before `integrate`: it walks `world.trajectories()` and adds `step` to every
  entity's `elapsed`. Ordering matters for the future consumer, not for this task: a shape evaluator
  reading `elapsed` to decide this tick's velocity needs this tick's step already counted. Nothing
  evaluates a shape yet — that is #162/#163/#164 — so today this only accumulates the number.
- Corrected `Motion.java`'s javadoc, which claimed trajectories "are not here yet" — false the moment
  `Trajectory` exists. `TrajectoryDefinition.java` was left untouched: nothing in this task changes
  what a *content* trajectory is (still a constant vector), so its javadoc is not falsified by this
  PR. That correction belongs to whichever of #163/#86 actually adds a non-constant shape to content.
- Tests: `MotionSystemTest` gained three cases — elapsed accumulates one step at a time across two
  ticks, the origin never changes while the entity's own `Transform` does, and an entity with no
  `Trajectory` is unaffected. `WorldTest.populateEveryComponent` was extended for the new store, which
  its own reflection-based guard (`destroyStripsEveryComponent`) requires.

## Ambiguity in the plan, and the decision made

The plan's task 1 description ("a plain-data component holding that state ... and a system advancing
it") does not say whether the per-entity state should include anything beyond elapsed time. I added
`originX`/`originY` because it is the one piece of spawn-time data every shape family the roadmap
names (U-turn, diagonal, curve) structurally needs and none of them can substitute for — not a
speculative addition under invariant 6, but the minimum a "function of elapsed time" needs to also be
a function of *where from*. If a future shape needs more (a target, a phase index), that is content
for #162/#163 to add, not a reason to have withheld origin now.

## Acceptance criteria

- [x] A plain-data component holds the entity's elapsed time and whatever the shape needs — `elapsed`
  plus origin.
- [x] The advance happens inside `SystemOrder.MOTION` — `MotionSystem.advanceTrajectories`.
- [x] Elapsed time accumulates from the fixed step; no clock, no `Math.random()`, no libGDX import.
- [x] The determinism replays pass unchanged — `./gradlew :core:test` is green, 300+ tests, including
  every `*ReplayTest` and `DeterminismRulesTest`.
- [x] Every javadoc this falsifies is corrected — `Motion.java`.
- [x] `SystemOrder` is unchanged — no new stage, no reordering.

## Not done, on purpose (later tasks in this phase)

- No shape catalogue, no evaluation of `elapsed`/origin into a velocity.
- No content contract, no loader, no `assets/data/` file.
- Nothing attaches a `Trajectory` to a spawned entity yet — `ComponentFactoryRegistry` is untouched,
  so no enemy in the game carries one today. That is exactly why this PR cannot itself demonstrate
  "the same archetype with two shapes" — that is the phase's acceptance criterion, not this task's.
