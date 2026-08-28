# Phase 11b — The wave system · status

**State:** in progress
**Updated:** 28/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

- Task 5 / #87 — `JsonContentSource` no longer hardcodes `LEVEL_ID = "level-01"`. The constructor now
  takes a `levelId` parameter and reads `<levelId>.json` from the data directory. Per review, the id
  itself lives on `LittleSpaceshipGame` (`levelId()`, exposed the same way `seed()` is) rather than on
  `PlayScreen` — both `PlayScreen` and `ShipSelectScreen` already read session-wide state off that
  object, so this avoids coupling one screen to a sibling's constant. No directory listing was added —
  `FileHandle.list()` has no answer under TeaVM's asset packaging, and there is still exactly one level
  file, so eager single-level loading stays. See `feat/level-by-id` PR against this branch.
- **Task 1, [#84](https://github.com/LuchoC-Dev/little-spaceship/issues/84) — an entity that leaves
  the playfield leaves the simulation.** Branch `feat/entity-lifetime`. Built the two decided
  mechanisms: a `Lifetime` component (optional per archetype, `"lifetime": {"seconds": N}`, never
  removes an entity still on screen) and a safety box in `LifetimeSystem`, 128 units past every
  playfield edge, that removes an `ENEMY`-layer entity outright regardless of whether it carries a
  `Lifetime`. See `docs/planning/08-decisions-and-open-items.md`, "The 11 group, 27/08/2026", for the
  box's exact coordinates, what they clear, and the score/drop consequence this surfaced
  (`CleanupSystem` converges every destruction path uniformly, so an escaped enemy still awards its
  `ScoreValue` — a deliberate choice not to special-case an already-decided convergence point for a
  game rule #84 left open).

## In progress

Nothing else yet.

## Blocked

Waiting on 11a. Every task here is a behaviour change and 11a is the net.

## Decisions taken while implementing

- **An escaped enemy still awards its `ScoreValue` and resolves its `Drop`.** `CleanupSystem`
  converges every destruction path uniformly by existing design ("regardless of what killed its
  holder"); special-casing it for "died to the safety box or an expired lifetime" would have been
  deciding an undecided game rule (issue #84 explicitly left "whether an escaped enemy simply
  disappears, costs the player something..." open) rather than implementing a decided one. Recorded
  in `docs/planning/08-decisions-and-open-items.md`.
- **The safety box applies only to the `ENEMY` collision layer**, not to pickups or the boss's parts.
  Pickups do not move on their own (`CleanupSystem` spawns them at their source's position, `Motion`
  and `Lifetime` are never attached to one), so they cannot leave the playfield; the boss's parts stay
  on screen for the whole fight and the box never triggers on them either way. Projectiles keep the
  existing, separate, tighter `PROJECTILE_MARGIN` (16 units) — the safety box's 128-unit margin would
  leave them on screen for a very visible extra distance.
- **`Lifetime` is optional per archetype**, attached only when an archetype's `"lifetime"` spec is
  present, the same pattern `"health"`, `"spawner"` and `"weapon"` already use. No archetype in
  `assets/data/enemies.json` declares one yet — the safety box alone is what fixes #84 for existing
  content, since `assets/data/` is outside this agent's boundary (`core/` only). Whether any archetype
  should get an explicit, shorter lifetime is a content decision for `level-designer`.
- Updated the golden fingerprint in `LevelScoreReplayTest` (`score=1350→1600 entities=12→11`):
  `enemy-rush`'s "dive" trajectory carries it off the bottom of the playfield, where it used to linger
  forever, uncounted; it is now correctly swept up and scored once fully off screen. See the test's
  own updated comment for the exact accounting.

## Notes for whoever comes next

- The other three per-task worries in the plan (#85 wave-origin tracking, the wave content contract,
  `SpawnSystem` advancing on waves) are unaffected by this branch: no `core.port` interface changed,
  only a new component, a new `World` store and `ComponentFactoryRegistry` registration, and
  `LifetimeSystem`'s internals. `./gradlew build -x test` passes across every module including `web`.
- `LifetimeSystemTest` now documents the safety box's worst-case-spawn measurement directly in a test
  (`safetyBoxClearsTheWorstCaseSpawn`), so a future change to `formations.json`'s spreads or
  `enemies.json`'s radii that outgrows the 128-unit margin has to be caught by hand — the test only
  guards the number as it stood on 27/08/2026, not a live read of the JSON (out of `core`'s reach).
