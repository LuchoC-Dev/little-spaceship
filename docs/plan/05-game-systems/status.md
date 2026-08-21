# Phase 05 — Game systems · status

**State:** implemented, pending review
**Updated:** 21/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

- `WeaponSystem` (`SystemOrder.WEAPON`): sustained automatic fire, cooldown-gated, reading the
  player's `shotLevel` and firing the exact 1/2/3/5-projectile volley shapes from
  `docs/design/02-sprite-sizes.md`'s weapon level table (`shot-p1`/`shot-p2`, radii 1.5/2.0).
- A new `Weapon` component (cooldown timer) and `Pickup` component (what a pickup grants), plus
  `Player.score`.
- `BombSystem` (`SystemOrder.BOMB`, a new stage inserted between `WEAPON` and `SPAWN`): spends a
  bomb charge, destroys every enemy projectile and every fragile enemy on screen. Non-fragile
  enemies (tank, heavy carrier) are **not** damaged — see "Known gap" below.
- `LifetimeSystem` (`SystemOrder.LIFETIME`): expires a player or enemy projectile once it has fully
  left the playfield, by position rather than by a timer. Needed for `WeaponSystem` to not leak
  entities over a multi-minute level; no `Lifetime` timer component was built, since nothing needs
  one yet.
- `DamageSystem` now also resolves `PLAYER_PROJECTILE_VS_ENEMY`: any enemy reached by a player
  projectile is destroyed in one hit, the projectile too. The defensive-chain logic it already owned
  (invulnerability → shield → attachment → life) is untouched.
- `CleanupSystem` now spawns an actual `Pickup` entity from an entity's `Drop`, at the moment it is
  destroyed — the one place every path to destruction (ramming, weapon fire, bomb) converges, so a
  designed drop is honoured the same way regardless of what killed its holder.
- `PickupSystem` (`SystemOrder.PICKUP`): the five fixed power-up kinds (`weapon-upgrade`, `shield`,
  `extra-life`, `bomb-recharge`, `invulnerability`) plus `attachment`, each with its own consumption
  rule. A pickup already at its cap attaches a `ScoreValue` bonus to itself instead of being wasted,
  swept by the ordinary `ScoreSystem` pass — one mechanism for every point in the game, not two.
- `ScoreSystem` (`SystemOrder.SCORE`): sweeps `pendingDestruction()` for `ScoreValue`, removing it
  once awarded so an entity marked twice in one tick is never double-scored. Also exposes a static,
  pure `completionBonus(BalanceValues, Player)` for the end-of-level bonus — nothing in the core
  detects "level complete" yet (boss/victory is phase 07), so nothing calls it yet either.
- `AttachmentDefinition`/`SimpleAttachmentDefinition` and `ContentSource.attachment(String)`:
  durability is content, looked up by id, not a constant — the MVP's one attachment type uses
  `"attachment"` as both its pickup kind and its content id.
- `BalanceValues` gained six new values, all placeholders pending balancing except the two
  completion bonuses (1000/300, already in `10-mvp-initial-values.md`): `weaponFireCooldown`,
  `weaponProjectileSpeed`, `pickupRadius`, `invulnerabilityPickupDuration`,
  `lifeCompletionBonus`, `bombCompletionBonus`.
- `Simulation`'s MVP pipeline now includes `WeaponSystem`, `BombSystem`, `LifetimeSystem`,
  `PickupSystem` and `ScoreSystem`; the player is created with a `Weapon` component.
- Test suite grew from 167 to 216 (`./gradlew :core:test`), including two replay tests
  (`BombReplayTest`, `LevelScoreReplayTest`) exercising the full pipeline across a scripted,
  content-driven level.

## In progress

Nothing — the phase's task list is complete.

## Blocked

Nothing.

## Decisions taken while implementing

The plan named these as things to design, not things already decided, so they are recorded here and
also belong in `docs/planning/08-decisions-and-open-items.md`'s open-item list if not already
implied by it:

- **No `Health` component was built.** No enemy hit-point value exists anywhere in
  `docs/planning/`, so a player projectile destroys any enemy it reaches in one hit — including tank
  and heavy carrier — regardless of archetype. `Collider.fragile` already draws a similar
  "destroyed on contact" line for a body collision; this reuses that outcome for a weapon hit.
- **Known gap, following directly from the point above:** the bomb's "deals heavy damage to
  resistant enemies" (`02-mvp-functional-spec.md`) is not implemented. A bomb destroys every enemy
  projectile and every fragile enemy; a tank or heavy carrier survives it untouched, with no partial
  damage applied, because there is no `Health` value to size that damage against.
- **`BOMB` is a new `SystemOrder` stage**, inserted between `WEAPON` and `SPAWN`. The bomb destroys
  entities directly (`World.markForDestruction`) rather than through a `CollisionHit` — its range is
  the whole screen, not a shape two colliders can overlap — so it does not need to run near
  `COLLISION`.
- **`DamageSystem`'s scope widened** to also resolve `PLAYER_PROJECTILE_VS_ENEMY`, not only the
  defensive chain against the player. Both are damage resolution against a hit reported the same
  tick by `CollisionSystem`; `SystemOrder.DAMAGE`'s own stage name already reads as generic damage
  resolution, not specifically player defense, and only one system may claim a stage.
- **Six power-up/pickup kinds are fixed string constants on `PickupSystem`**
  (`KIND_WEAPON_UPGRADE` = `"weapon-upgrade"`, `KIND_SHIELD` = `"shield"`, `KIND_EXTRA_LIFE` =
  `"extra-life"`, `KIND_BOMB_RECHARGE` = `"bomb-recharge"`, `KIND_INVULNERABILITY` =
  `"invulnerability"`, `KIND_ATTACHMENT` = `"attachment"`). `Drop.pickupId` and `Pickup.kind` must
  use these exact strings; `game`'s content pipeline needs to match them.
- **Pickups do not move.** No `Motion` is attached to a spawned pickup entity; nothing in the
  planning docs asks for pickups to drift or fall.
- **The end-of-level score bonus interprets "1000 and 300 respectively" as per-unit**, not a flat
  amount: `lives * lifeCompletionBonus + bombs * bombCompletionBonus`. `10-mvp-initial-values.md`'s
  wording is ambiguous between the two readings; per-unit is the standard arcade convention and
  scales with "finishing in good shape," which is the stated intent.

## Notes for whoever comes next

**`game` no longer compiles as of this phase**, and this is expected, not a regression to chase down
inside `core`. Extending `BalanceValues` and `ContentSource` — both owned by `core` — broke the two
adapters that implement them:

- `game/.../adapter/content/JsonBalanceValues.java` needs `weaponFireCooldown()`,
  `weaponProjectileSpeed()`, `pickupRadius()`, `invulnerabilityPickupDuration()`,
  `lifeCompletionBonus()` and `bombCompletionBonus()`.
- `game/.../adapter/content/JsonContentSource.java` needs `attachment(String id)`, reading an
  `AttachmentDefinition` from content.

This is `game-presentation`'s module, not `core-domain`'s, so it was not touched here. Verified with
`./gradlew :game:compileJava`, which names exactly these two gaps.

**Acceptance criteria** (`docs/plan/05-game-systems/plan.md`):

| Criterion | Status |
|---|---|
| Every power-up covered by a test for its own consumption rule | Met — `PickupSystemTest` |
| Picking up a maxed power-up increases the score | Met — `PickupSystemTest.maxedPickupIncreasesTheScoreOnceSwept` runs `PickupSystem` then `ScoreSystem` together |
| The attachment absorbs exactly one hit, disappears, no life lost | Already covered by phase 02's `DamageSystemTest`, unchanged this phase |
| Attachment durability raised from data, no code change | Met — `PickupSystemTest.attachmentDurabilityComesFromDataNotAConstant`, `ContentDefinitionsTest` |
| The bomb clears projectiles and damages enemies in the same tick, deterministically | Partially met — clears projectiles and fragile enemies deterministically (`BombSystemTest`, `BombReplayTest`); does **not** damage resistant enemies, see "Known gap" above |
| Score matches the table in `10-mvp-initial-values.md` | Met for per-enemy and maxed-pickup values (unchanged content-driven `ScoreValue`, `maxedPickupBonus`); the completion bonus's per-unit reading is a decision, not a re-confirmation, see above |
| A full-level replay produces the same final score twice | Met — `LevelScoreReplayTest`, `BombReplayTest`; same caveat as `DamageReplayTest` (issue #12): proves determinism within this build, not against a golden fingerprint |
