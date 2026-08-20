---
name: core-deferred-surface
description: What the core deliberately leaves unimplemented, and which phase owns each gap
metadata:
  type: project
---

The core grew deliberately incomplete: each phase adds only what its own systems need. Knowing what was left out **on purpose** stops it being re-decided as if it were an oversight.

**How to apply:** before adding a type to `core.port` or a component to the domain, check whether it was deferred here with a reason.

- `WorldView.player()` and `boss()` — need a player and a boss to report on. Phases 03 and 07.
- `ContentSource.enemy()`, `.trajectory()`, `.formation()` and `.timeline()` — **built in phase 04.**
  `ContentSource.pattern()` is not: see below.
- Concrete `GameEvent` implementations — the interface exists, no event does. `DamageSystem` and
  `CollisionSystem` are natural emitters per `12-architecture.md` (`PlayerHit`, `AttachmentLost`), but
  neither emits anything: nothing in `core` consumes them until `game` builds HUD/audio, and guessing
  event field shapes ahead of that consumer risked getting them wrong.
- `Health` component — architecture lists it for enemies and the boss, but no enemy hit-point values
  exist anywhere in `docs/planning/` yet. "Weak enemies die on collision" is modelled as a boolean
  (`Collider.fragile`) instead, deliberately not the eventual `Health` mechanic, which needs a weapon
  system dealing damage over multiple hits to matter.
- `Weapon` component and a `PatternDefinition`/`ContentSource.pattern()` content contract — still
  deferred after phase 04, on purpose: no `WeaponSystem` exists to consume them (phase 05), and their
  shape (shot count, spread, cooldown, projectile sprite) is exactly the kind of guess the `GameEvent`
  precedent above warns against. See `content-pipeline-design.md` for why trajectories got built in
  the same phase and patterns did not.
- `Lifetime`, `Spawner` components — still nothing needs them.
- `ScoreValue`, `Drop` components — **built in phase 04**, attached by `SpawnSystem`; nothing reads
  them yet (score/pickup systems are phase 05), same "creation side exists before consumption side"
  precedent `Shield`/`Attachment` already set in phase 02.
- `SystemOrder` stages with no system registered yet: `INPUT`, `WEAPON`, `LIFETIME`, `PICKUP`,
  `SCORE`. `SPAWN` (ordinal 3) is filled as of phase 04 — `SpawnSystem`.

See [[core-boundary-decisions]], [[defensive-chain-and-collision-design]] for the boundary shape
those additions have to respect, and [[rng-teavm-constraints]] for what stays reproducible across
runtimes. Current implementation state — what phase added what — lives in `docs/plan/*/status.md`,
not here.
