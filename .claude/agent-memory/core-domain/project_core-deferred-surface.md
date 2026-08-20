---
name: core-deferred-surface
description: What the core deliberately leaves unimplemented, and which phase owns each gap
metadata:
  type: project
---

The core grew deliberately incomplete: each phase adds only what its own systems need. Knowing what was left out **on purpose** stops it being re-decided as if it were an oversight.

**How to apply:** before adding a type to `core.port` or a component to the domain, check whether it was deferred here with a reason.

- `WorldView.player()` and `boss()` — need a player and a boss to report on. Phases 03 and 07.
- `ContentSource.enemy()` and `timeline()` — content pipeline, phase 04.
- Concrete `GameEvent` implementations — the interface exists, no event does, still. Phase 02 added
  `DamageSystem`/`CollisionSystem`, both natural emitters per `12-architecture.md`
  (`PlayerHit`, `AttachmentLost`), but neither emits anything yet: nothing in `core` consumes them
  until `game` builds HUD/audio (phase 06), and guessing event field shapes ahead of that consumer
  risked getting them wrong. Revisit when phase 05 or 06 needs the feedback.
- `Health` component — architecture lists it for enemies and the boss, but no enemy hit-point values
  exist anywhere in `docs/planning/` yet. Phase 02 modelled "weak enemies die on collision" as a
  boolean (`Collider.fragile`) instead, deliberately not the eventual `Health` mechanic, which needs
  `WeaponSystem` (phase 05) to matter.
- Components beyond `Transform`, `Motion`, `Collider`, `Sprite`, `Player`, `Invulnerable`, `Shield`,
  `Attachment` (the last four added in phase 02).
- `Weapon`, `Lifetime`, `ScoreValue`, `Drop`, `Spawner` components — still nothing needs them.
- `SystemOrder` stages with no system registered yet: `INPUT`, `WEAPON`, `SPAWN`, `LIFETIME`,
  `PICKUP`, `SCORE`. Phase 02 registered `MOTION`, `COLLISION`, `DAMAGE`, `CLEANUP` in
  `Simulation.mvpPipeline()`, which is no longer empty.
- `CollisionSystem` detects all four confirmed layer pairs, but only two are consumed
  (`ENEMY_VS_PLAYER`, `ENEMY_PROJECTILE_VS_PLAYER`, both by `DamageSystem`). The other two
  (`PLAYER_PROJECTILE_VS_ENEMY`, `PICKUP_VS_PLAYER`) are detected and tested, waiting for
  `WeaponSystem`/`PickupSystem` in phase 05 to read `World.collisionHits()` too.
- Player top speed and the slow-movement multiplier have no value in
  `docs/planning/10-mvp-initial-values.md`. `BalanceValues.playerSpeed()`/`.playerSlowFactor()` exist
  with placeholder numbers (140 units/s, ×0.45) pending a real one from balancing.

See [[core-boundary-decisions]], [[defensive-chain-and-collision-design]] for the boundary shape
those additions have to respect, and [[rng-teavm-constraints]] for what stays reproducible across
runtimes.
