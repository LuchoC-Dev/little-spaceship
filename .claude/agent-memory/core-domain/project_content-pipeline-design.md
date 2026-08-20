---
name: content-pipeline-design
description: How the phase 04 content contracts are shaped — generic ComponentSpec, the factory registry, and why patterns/weapon were left out
metadata:
  type: project
---

Built in phase 04 (`docs/plan/04-content-pipeline/`). The shape decided here is load-bearing for
whoever writes `game`'s JSON loader and for phase 05's `WeaponSystem`/`PickupSystem`.

**`ComponentSpec` is one generic interface (name + typed key/value bag), not one Java type per
component.** This was the one point in the whole phase where a typed-interface-per-component design
looked tempting and would have been wrong: it would have meant a matching Java type for every JSON
object in `enemies.json`, which defeats the entire point of a `name -> factory` registry. The
generic shape is also literally what `12-architecture.md`'s own JSON example already implies
(`"collider": {"radius": 7, "layer": "enemy"}`), so it was not really a free choice.

**Firing patterns were deliberately not built, even though the plan's task list names them next to
trajectories.** No `Weapon` component and no `WeaponSystem` exist yet (phase 05's job). Declaring a
`PatternDefinition` now would mean guessing shot count / spread / cooldown / projectile sprite with
no consumer to check the guess against — the exact mistake phase 02's memory already flagged for
`GameEvent` (`core-deferred-surface.md`). Trajectories *were* built in the same task because
`MotionSystem` already exists and already consumes a `Motion` component, so a trajectory's shape (a
constant velocity, nothing curved) was not a guess — it is exactly what the existing consumer reads.
**If a future phase is tempted to declare a content contract for something with no consuming system
yet, check for a real, already-built consumer first, the same way this phase did for trajectories vs.
patterns.**

**`Drop` (the component) is attached directly by `SpawnSystem` from the `SpawnEvent`, never through
`ComponentFactoryRegistry`.** A designed drop marks one instance of a wave, not the archetype — the
plan says so explicitly (task 8). Routing it through the same generic per-archetype-component registry
as `motion`/`collider`/`sprite`/`scoreValue` would have quietly turned it back into archetype data,
which is the exact bug the plan calls out avoiding.

**A `ComponentFactoryRegistry` is looked up by exact key, never iterated.** Registration order in
`withDefaults()` therefore has zero effect on a replay — worth remembering before "reordering
registration calls" gets flagged as a determinism risk it is not.

See [[core-deferred-surface]] for the running list of what is deferred and why, and
[[defensive-chain-and-collision-design]] for the `SystemOrder` ordinal-checking discipline this
phase's `SpawnSystem` (ordinal 3, before `COLLISION` at 5) had to respect too.
