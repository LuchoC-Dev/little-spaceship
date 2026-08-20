---
name: defensive-chain-and-collision-design
description: How CollisionSystem and DamageSystem communicate internally, and where the defensive chain's boundary cases were decided
metadata:
  type: project
---

Built in phase 02 (`docs/plan/02-core-mechanics/`). Two design choices here are load-bearing for any
later phase that touches collision, weapons, pickups or the player's defensive state.

**Internal collision results are not `GameEvent`s.** `CollisionSystem` writes `CollisionHit` records
into `World.collisionHits()`, a plain list cleared and refilled every tick — not the
`GameEventQueue`, which drains only once per tick to an external sink. Conflating the two would let
a mid-tick internal signal sit in the same queue as end-of-tick presentation events; they have
different lifetimes on purpose. `DamageSystem` reads `collisionHits()` in the very next stage, same
tick.

**Not every future consumer of that buffer can just read it as-is — check `SystemOrder` first.**
`PickupSystem` (`SystemOrder.PICKUP`, ordinal 7) runs after `COLLISION` (5), so it would see the
current tick's `PICKUP_VS_PLAYER` hits, same as `DamageSystem` does. `WeaponSystem`
(`SystemOrder.WEAPON`, ordinal 2) runs *before* `COLLISION`, so reading `PLAYER_PROJECTILE_VS_ENEMY`
from it would resolve the *previous* tick's hits — one tick late, silently, since nothing about that
is a compile error or an obviously failing test. An earlier version of this note claimed both systems
could read the buffer unchanged; that was wrong for `WeaponSystem` and was caught in review, not by a
test. Whoever adds `WeaponSystem` has to either resolve `PLAYER_PROJECTILE_VS_ENEMY` from a stage
after `COLLISION` (mirroring what `DamageSystem` does today) or give `SystemOrder` a new stage for it
— check the *ordinal*, not just "does a consuming system exist yet", before assuming a buffer written
earlier in the pipeline is readable from a stage that runs before it is refilled.

**Invulnerability, when active, absorbs a hit with zero side effects.** Not just "no life lost" —
also no enemy destroyed, no projectile consumed. This was a genuine design call, not stated
explicitly anywhere in `docs/planning/`: the literal reading of "the defensive chain: invulnerability
→ shield → attachment → life" as four *layers* implies the first one that catches a hit absorbs it
completely, same as a shield does. If this reading turns out wrong once there is real gameplay
feedback (e.g. a ramming enemy should maybe still die even while the player is invulnerable), it is
a one-line change in `DamageSystem.resolvePlayerHit`, but it changes the observed acceptance
criterion "weak enemies die on collision" under that specific combination.

**Removing a component while iterating its `ComponentStore` reorders the dense array**, exactly like
destroying an entity does (`ComponentStore`'s own javadoc warns about entity destruction, but the
hazard is really about the store, not the entity). `DamageSystem.decayInvulnerability` collects
expired entities into a side list first and removes them after the loop — the same pattern
`CleanupSystem` uses for entity destruction, just one level down.

See [[core-boundary-decisions]] and [[core-deferred-surface]] for what else was deferred and why.
