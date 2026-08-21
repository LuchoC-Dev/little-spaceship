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
- `Health` component — **built in phase 05**, and this entry was wrong once already: an earlier
  version of it said "still does not exist" and treated the gap as an undecided future mechanic.
  It was not undecided — `12-architecture.md`'s component table names it and its JSON schema example
  gives a tank `"health": {"points": 40}` — it had simply never been built, because phase 04 modelled
  fragility as `Collider.fragile` instead and this phase's `plan.md` did not list
  `12-architecture.md` among its required reading. A coordinator review caught the miss mid-phase.
  See [[core-deferred-surface]]'s own lesson below and `game-systems-design.md` for the
  `fragile`/`Health` relationship phase 05 settled on.
- `Weapon` component — **built in phase 05**, holds only the fire cooldown timer; `Player.shotLevel`
  still carries the persistent upgrade level, per phase 02's "persistent state lives on `Player`"
  precedent.
- A `PatternDefinition`/`ContentSource.pattern()` content contract — **still deferred**, even though
  `WeaponSystem` now exists. The four weapon levels' shot counts and shapes are fixed data read
  straight from `docs/design/02-sprite-sizes.md`'s table and hardcoded as constants inside
  `WeaponSystem`, the same way `Simulation` hardcodes the player's sprite id and collider radius from
  a synchronisation-point document. Nothing in the MVP asks for a second weapon pattern, so there is
  still no real second case to generalise a content contract against.
- `Lifetime`, `Spawner` components — `Lifetime` stays deferred even though `LifetimeSystem` now
  exists (phase 05): it expires projectiles by playfield position, not by a timer, so it never needed
  the component. `Spawner` still has no consumer.
- `ScoreValue`, `Drop` components — built in phase 04, **now read** (phase 05): `ScoreValue` by
  `ScoreSystem`, `Drop` by `CleanupSystem`, which turns it into an actual `Pickup` entity read in
  turn by `PickupSystem`.
- `SystemOrder` stages with no system registered yet: only `INPUT` — every other stage is filled as
  of phase 05. `BOMB` is a new stage phase 05 inserted between `WEAPON` and `SPAWN`.

See [[core-boundary-decisions]], [[defensive-chain-and-collision-design]] for the boundary shape
those additions have to respect, [[game-systems-design]] for the damage/scoring/pickup shape phase
05 settled on, and [[rng-teavm-constraints]] for what stays reproducible across runtimes. Current
implementation state — what phase added what — lives in `docs/plan/*/status.md`, not here.
