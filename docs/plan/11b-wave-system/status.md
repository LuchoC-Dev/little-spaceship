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
  box's exact coordinates and what they clear. **The project owner then decided, on 28/08/2026, the
  game rule the first version of this branch had correctly refused to decide by implementation: an
  enemy that escapes gives the player nothing** — no score, no drop, no `EnemyDestroyed` (which would
  otherwise have played an explosion sound off screen through `AudioDirector`, the simulation's
  `GameEventSink`). `LifetimeSystem` now strips an escaping entity's `ScoreValue`, `Drop` and
  `Collider` before marking it for destruction, so `ScoreSystem` and `CleanupSystem` need no change at
  all — each already does nothing when the component it reads is absent.
- **Task 3, [#111](https://github.com/LuchoC-Dev/little-spaceship/issues/111) — the wave content
  contract.** Branch `feat/wave-contract`. Added `WaveDefinition` (id, spawns, endCondition — three
  things, nothing else) and `WaveEndCondition` (`FixedDuration` or `Cleared`, sealed to those two) as
  the ninth content kind, plus a `ContentSource.wave(id)` lookup — a **default** method, so `game`'s
  `JsonContentSource` keeps compiling untouched until #113 overrides it; #112's issue now carries
  flipping it to abstract once that loader lands. **Correction after the first review pass:** the
  offset was first placed on `WaveDefinition` itself, which the project owner caught — a wave that
  carries its own placement can only ever sit in one spot, defeating the reuse the whole type exists
  for. The offset now lives on a new `WavePlacement` record (`waveId`, `offsetSeconds`), the type a
  level's ordered sequence becomes; the same `WaveDefinition` can back as many `WavePlacement`s as a
  level (or two levels) need, each with its own offset. `SpawnSystem` was not touched, per the task's
  scope. `WaveTimeline` was left with its existing shape and behaviour (`SpawnSystem` still walks it
  as a flat, level-scoped `SpawnEvent` list) because changing its shape would have broken
  `SpawnSystem`'s compile, which this task may not touch — its javadoc now says so explicitly instead
  of silently disagreeing with the new contract, and names #112 as the task that migrates
  `SpawnSystem` onto `WaveDefinition` and `WavePlacement` and retires or repoints it. `SpawnEvent`'s
  own doc no longer assumes one reference frame, since it is now reused wave-relative (inside
  `WaveDefinition.spawns()`) as well as level-relative (inside the legacy `WaveTimeline`). See
  `feat/wave-contract` PR against this branch.

- **Task 2, [#85](https://github.com/LuchoC-Dev/little-spaceship/issues/85) — an entity records which
  wave spawned it.** Branch `feat/entity-wave-id`. New component `WaveOrigin` (`core/domain/component/`),
  a `World.waveOrigins()` store, attached by `SpawnSystem.spawnWave` — one wave instance is still one
  call to that method until the wave content contract exists, so its id is the timeline cursor
  position. `SpawnerSystem.spawnChild` copies the holder's `WaveOrigin` onto every child it creates,
  when the holder has one — the carrier-children rule the project owner decided on 28/08/2026, written
  into `docs/planning/08-decisions-and-open-items.md`. No other entity-creation site (`CleanupSystem`'s
  dropped pickup, `EnemyWeaponSystem`'s and `WeaponSystem`'s projectiles, `BossSystem`'s parts and
  projectiles) inherits a `WaveOrigin` — the decided rule names carriers and their `Spawner`-spawned
  children specifically. Nothing reads the component yet; the `cleared` end condition is #112.

- **Task 7, [#113](https://github.com/LuchoC-Dev/little-spaceship/issues/113) — `JsonContentSource`
  reads `waves.json` and a level's wave references.** Branch `feat/wave-loader`. `waves.json` is
  optional (a data directory without it loads with an empty wave registry, since `level-01.json`
  still carries `"events"` and no shipped content uses the new format yet) and, when present, is
  parsed into `WaveDefinition`s keyed by id, backing `ContentSource.wave(id)` directly. Its `"end"`
  block discriminates on a `"type"` string (`"fixedDuration"` needs `"seconds"`, `"cleared"` needs
  nothing else); any other value is rejected rather than defaulted.
  A level file's top-level block gained a third allowed key, `"waves"`, an ordered list of
  `{"wave", "offset"}` placements — mutually exclusive with the legacy `"events"` list, and one of
  the two is required. Since `SpawnSystem` has not migrated onto `WaveDefinition`/`WavePlacement`
  yet (#112), a level's placements are **flattened at load time** into the same absolute-time
  `SpawnEvent` list `"events"` would have produced by hand: each placement starts `offsetSeconds`
  after the previous one ends, a wave's own wave-relative `at` values are shifted by that start, and
  the merged result is sorted by absolute `at` (necessary because a negative offset intentionally
  overlaps two placements, which can interleave their events). **Only `FixedDuration` waves can be
  flattened this way** — a `Cleared` wave's end is a runtime fact this loader has no access to, so a
  placement naming one fails loudly, naming the level and the wave id, rather than being flattened
  wrong. `level-01.json` was not touched (out of scope, #114) and still loads exactly as before.
  Verified with a scratch program compiled against the real `core.jar`/`game` classes and a small
  fixture `waves.json` (two waves, one `FixedDuration` referenced twice in a level to demonstrate
  reuse, one `Cleared`) — see the task's PR description for the full list of error paths exercised.

## In progress

Nothing else yet.

## Blocked

Waiting on 11a. Every task here is a behaviour change and 11a is the net.

## Decisions taken while implementing

- **First cut: an escaped enemy still awarded its `ScoreValue` and resolved its `Drop`**, deliberately
  not special-cased — issue #84 explicitly left "does escaping cost or gain the player anything" open,
  and special-casing it would have been deciding an undecided game rule by implementation. **The
  project owner then decided it, on 28/08/2026: an escaped enemy gives nothing.** Implemented by
  having `LifetimeSystem` — the only place that knows an entity is escaping rather than being
  defeated — strip that entity's `ScoreValue`, `Drop` and `Collider` before calling
  `World.markForDestruction`. `ScoreSystem` and `CleanupSystem` needed no change: both already do
  nothing when the component they read is absent, and `CleanupSystem`'s own "converges every
  destruction path uniformly, regardless of what killed its holder" stays exactly true — no
  source-aware branch was added to it. Two passes over `world.colliders()` inside `expireEnemies`
  (collect escaping entities first, strip and mark them only after the loop finishes) rather than one,
  because stripping an entity's own `Collider` while a loop is still walking that same store reorders
  its dense array and skips an element — `ComponentStore`'s own documented hazard. Recorded in
  `docs/planning/08-decisions-and-open-items.md`.
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
- The golden fingerprint in `LevelScoreReplayTest` went through two values before settling: the
  original bug (`entities=12`, the escaped `enemy-rush` never removed) to the first-cut fix
  (`score=1600 entities=11`, escaping scored) to the final rule (`score=1350 entities=11`, escaping
  scores nothing — the entity count is fixed, the score is exactly what it was before #84 since
  nothing was ever credited for an escape). Only the final value is committed; see the test's own
  comment for the accounting.

## Notes for whoever comes next

- The other three per-task worries in the plan (#85 wave-origin tracking, the wave content contract,
  `SpawnSystem` advancing on waves) are unaffected by this branch: no `core.port` interface changed,
  only a new component, a new `World` store and `ComponentFactoryRegistry` registration, and
  `LifetimeSystem`'s internals. `./gradlew build -x test` passes across every module including `web`.
- `LifetimeSystemTest` now documents the safety box's worst-case-spawn measurement directly in a test
  (`safetyBoxClearsTheWorstCaseSpawn`), so a future change to `formations.json`'s spreads or
  `enemies.json`'s radii that outgrows the 128-unit margin has to be caught by hand — the test only
  guards the number as it stood on 27/08/2026, not a live read of the JSON (out of `core`'s reach).
