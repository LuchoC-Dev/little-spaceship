# Phase 02 — Core mechanics · status

**State:** in review
**Updated:** 20/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

Everything the plan's task list asks for, on branch `feat/core-mechanics`.

- **`MotionSystem`.** Integrates every entity's velocity, drives the player's `Motion` from `InputFrame`'s already-summed vector by clamping its *magnitude* to a configured top speed — not per axis, which is what makes a diagonal input no faster than a single axis. Slow movement reduces the same cap; it is not a separate code path. The player is clamped to the 208 px playfield width; enemies are left untouched and leave freely.
- **`CollisionSystem`.** Naive pair comparison for the four confirmed layer pairs: player projectile × enemy, enemy projectile × player, enemy × player, pickup × player. Results land in `World.collisionHits()`, an internal per-tick buffer, not a `GameEvent` — it never crosses towards presentation and is fully resolved within the same tick.
- **`DamageSystem`.** The single place the defensive chain lives: invulnerability → shield → attachment → life, in that order, each layer consumed at most once per hit. Invulnerability absorbs a hit completely, with no side effect at all — no layer consumed, no enemy destroyed, no projectile removed. This is also the only system that grants invulnerability, after *any* absorbed damage and not only after a life is lost, with a shorter duration for shield/attachment than for a life. Granting it immediately is what stops several hits from chaining within one tick: a second hit processed in the same call sees the invulnerability the first one just granted.
- **Collision damage.** A weak enemy (`Collider.fragile == true`) is destroyed by crashing into the player; a tank or heavy carrier is not. An enemy projectile is always consumed on contact.
- **Respawn.** Implicit rather than a destroy-and-recreate: the player entity is never destroyed on losing a life, so it is already exactly where it died. Losing a life only decrements `Player.lives`; bombs, shot level, the shield and the attachment are untouched unless the hit itself consumed them.
- **`CleanupSystem`.** The only system that calls `World.destroyEntity`. Every other system marks an entity via `World.markForDestruction`, resolved here at the end of the tick.
- **New domain types.** `Player`, `Invulnerable`, `Shield`, `Attachment` components; `Collider` gains a `fragile` flag; `CollisionHit`/`CollisionPair` in a new `domain.collision` package. `World` grows the four new component stores, `playerEntity()`, `collisionHits()` and `pendingDestruction()`/`markForDestruction()`.
- **`Simulation.mvpPipeline()`** now registers Motion, Collision, Damage and Cleanup — the first real systems this project ships, replacing the empty pipeline from phase 01.
- **Tests.** 129 total (91 inherited, 38 new), all passing, none needing libGDX: `MotionSystemTest`, `CollisionSystemTest`, `DamageSystemTest`, `CleanupSystemTest`, and `DamageReplayTest`, which runs a scripted damage sequence through the real MVP pipeline twice and compares the final state.

Every acceptance criterion in `plan.md` is covered:

| Criterion | Where |
|---|---|
| Full defensive chain, order and one-consumption-per-layer | `DamageSystemTest` |
| Shield/attachment invulnerability shorter than respawn's | `DamageSystemTest.shieldDamageGrantsTheShorterInvulnerability`, `.lifeLossGrantsTheLongerInvulnerability` |
| Losing a life keeps persistent power-ups | `DamageSystemTest.losingALifeDoesNotClearPersistentPowerUps` |
| Keyboard/mouse opposite directions cancel | `MotionSystemTest.oppositeDirectionsCancel` |
| Diagonal not faster than axis | `MotionSystemTest.diagonalIsNotFasterThanAxis` |
| Weak enemies die, heavy ones survive | `DamageSystemTest.weakEnemyDiesOnCollision`, `.heavyEnemySurvivesCollision` |
| Replay reproduces the same final state twice | `DamageReplayTest` |

## In progress

Nothing. Ready for `reviewer`.

## Blocked

Nothing.

## Decisions taken while implementing

Record here anything decided that the plan did not specify, and why. If it changes a game rule, it also belongs in `docs/planning/08-decisions-and-open-items.md`.

- **`10-mvp-initial-values.md` does not fix a player top speed or a slow-movement multiplier.** It fixes the *policy* — additive devices, clamped result, slow as a multiplier — but no concrete numbers. Added `BalanceValues.playerSpeed()` and `.playerSlowFactor()` with placeholder values (140 units/s, ×0.45), documented on the interface as pending a real number from balancing. This is a gap in `10-mvp-initial-values.md` worth closing there, not a game rule this phase invented.
- **"Weak enemies die in the crash" is modelled as a boolean, not as hit points.** `Collider` gained a `fragile` flag rather than the architecture's eventual `Health` component. Enemy hit points are not decided anywhere yet (no numbers exist for them), and a full `Health` mechanic would need `WeaponSystem` to matter, which is phase 05's job. The flag captures exactly the confirmed rule — weak archetypes die on collision, tanks and heavy carriers do not — without inventing balance data ahead of the phase that owns it.
- **`CollisionSystem` detects all four confirmed layer pairs, but `DamageSystem` only consumes two of them this phase** (enemy × player, enemy projectile × player). Player projectile × enemy and pickup × player are detected and tested the same way, but nothing consumes them yet: `WeaponSystem` and `PickupSystem` arrive in phase 05. This mirrors the precedent already set by `SystemOrder` — a stage with no system registered is simply skipped — at the granularity of a collision pair instead of a pipeline stage.
- **No `GameEvent` is emitted by `DamageSystem` or `CollisionSystem` this phase**, even though `12-architecture.md` lists `PlayerHit` and `AttachmentLost` as eventually-decided events. Nothing in `core` consumes them yet — HUD and audio are `game`-module work, not built. Adding event shapes without a concrete consumer risked guessing fields that would need to change once phase 06 actually reads them. Flagged here rather than silently skipped, since the architecture document does list them as expected.
- **The player is clamped to the playfield only on the x axis**, matching the plan's literal wording ("the playfield is 208 px wide and clamps the player"). No vertical bound exists anywhere in the planning docs, so none was invented.
- **Respawn does not destroy and recreate the player entity.** "Reappears near where it died" is satisfied trivially by never moving it: the position at the moment of the fatal hit *is* the respawn position. This avoids inventing a separate spawn-point tracking mechanism that nothing in the plan asks for.
- **`Shield` is a marker component with no fields**, since the confirmed rule is that any hit it absorbs removes it entirely — unlike `Attachment`, which carries `durability` as the architecture already decides.

## Notes for whoever comes next

- **Collision detection lives in `World.collisionHits()`, not a `GameEvent`.** It is cleared and refilled by `CollisionSystem` at the start of every tick and consumed by `DamageSystem` right after, within the same tick. If phase 05 adds `WeaponSystem`/`PickupSystem`, they read the same buffer and filter for `PLAYER_PROJECTILE_VS_ENEMY`/`PICKUP_VS_PLAYER` — no change to `CollisionSystem` needed.
- **`World.markForDestruction(int)` is how any system retires an entity.** Only `CleanupSystem` calls `World.destroyEntity` — mirroring the same hazard `ComponentStore`'s own javadoc already warns about (removing from a dense array mid-iteration reorders it and skips an entry).
- **`core.testsupport`** (test-only, `src/test/java`) holds `TestBalance` and `TestContent`, a configurable `BalanceValues`/`ContentSource` pair for any test that needs a `World` without a real content pipeline. Architecture tests only scan `src/main/java`, so this package is invisible to them by construction.
- **Numbers still missing from `10-mvp-initial-values.md`:** player top speed and the slow-movement multiplier. Worth adding there once balancing picks real values; `BalanceValues.playerSpeed()`/`.playerSlowFactor()` already exist to receive them.
