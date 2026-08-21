# Phase 05 — Game systems · status

**State:** implemented, pending review
**Updated:** 21/08/2026 (revised same day: `Health` added after a coordinator review caught it missing)

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

- `WeaponSystem` (`SystemOrder.WEAPON`): sustained automatic fire, cooldown-gated, reading the
  player's `shotLevel` and firing the exact 1/2/3/5-projectile volley shapes from
  `docs/design/02-sprite-sizes.md`'s weapon level table (`shot-p1`/`shot-p2`, radii 1.5/2.0).
- A new `Weapon` component (cooldown timer) and `Pickup` component (what a pickup grants), plus
  `Player.score`.
- `BombSystem` (`SystemOrder.BOMB`, a new stage inserted between `WEAPON` and `SPAWN`): spends a
  bomb charge, destroys every enemy projectile and every fragile enemy on screen outright, and
  subtracts `BalanceValues.bombDamage()` from a non-fragile ("resistant") enemy's `Health` —
  destroying it too, once that reaches zero. This is what turns "deals heavy damage to resistant
  enemies" (`02-mvp-functional-spec.md`) from an unimplemented phrase into an actual, tested rule.
- `LifetimeSystem` (`SystemOrder.LIFETIME`): expires a player or enemy projectile once it has fully
  left the playfield, by position rather than by a timer. Needed for `WeaponSystem` to not leak
  entities over a multi-minute level; no `Lifetime` timer component was built, since nothing needs
  one yet.
- A `Health` component, `12-architecture.md`'s own — `{"points": 40}` is that document's example
  for a tank — attached from content through `ComponentFactoryRegistry`'s new `"health"` factory,
  the same way `motion`/`collider`/`sprite`/`scoreValue` already are. See "Decisions taken" below
  for why this was missing from the first version of this phase and had to be added mid-review.
- `DamageSystem` now also resolves `PLAYER_PROJECTILE_VS_ENEMY`: a player projectile always
  destroys itself and subtracts `BalanceValues.weaponProjectileDamage()` from the enemy's `Health`,
  destroying the enemy once that reaches zero. An enemy with no `Health` component is treated as
  having exactly one point — shorthand for the weakest case of the same rule, not a second mechanism
  that could disagree with it, and shared with `BombSystem` through a small `HealthDamage` helper so
  both damage sources apply it identically. The defensive-chain logic `DamageSystem` already owned
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
- `BalanceValues` gained eight new values, all placeholders pending balancing except the two
  completion bonuses (1000/300, already in `10-mvp-initial-values.md`): `weaponFireCooldown`,
  `weaponProjectileSpeed`, `pickupRadius`, `invulnerabilityPickupDuration`, `lifeCompletionBonus`,
  `bombCompletionBonus`, `weaponProjectileDamage`, `bombDamage`. The last two, plus every
  per-archetype `Health` value, are recorded as open in `10-mvp-initial-values.md` the same way
  `playerSpeed`/`playerStartX/Y` already are.
- `Simulation`'s MVP pipeline now includes `WeaponSystem`, `BombSystem`, `LifetimeSystem`,
  `PickupSystem` and `ScoreSystem`; the player is created with a `Weapon` component.
- Test suite grew from 167 to 223 (`./gradlew :core:test`), including two replay tests
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

- **`Health` was missing from the first version of this phase, and that was a defect in the plans,
  not a deferred-on-purpose gap.** `12-architecture.md`'s component table lists `Health` explicitly
  ("health points, enemies and boss") and its JSON schema example gives a tank
  `"health": {"points": 40}` — the shape and even an illustrative value were decided from the start.
  It fell through a gap between phases: phase 04 read that document but modelled "does this enemy
  die outright" as `Collider.fragile` instead of building `Health`, and this phase's `plan.md` did
  not list `12-architecture.md` among its required reading, so nobody revisited the gap until a
  coordinator review caught it. Fixed two ways: `Health` is now built (see "Done" above), and
  `plan.md`'s "Before you start" now names `12-architecture.md`'s component table and schema section
  explicitly, so the next phase that needs a component's shape does not have to rediscover it either.
- **`Collider.fragile` and `Health` answer different questions, on purpose, so they cannot
  disagree.** `fragile` decides whether a ramming or the bomb kills an enemy's whole body outright —
  unchanged from phase 02, and the bomb still checks it first, before ever touching `Health`.
  `Health` decides how much sustained weapon damage an enemy can take, and is what the bomb applies
  to a *non*-fragile ("resistant") enemy instead. An enemy with no `Health` component is treated as
  having exactly one point everywhere damage is applied — weapon fire and the bomb alike — which is
  shorthand for the weakest case of the one rule, not a second rule that could disagree with the
  first. This does mean a non-fragile archetype that content forgets to give `Health` is destroyed
  in one hit despite being "resistant" on paper — unlike `"fragile"`, which fails loudly when
  omitted (`content-pipeline-design.md`), a missing `"health"` is *not* an error here: it is
  read as the deliberate one-point default. `game`'s content authoring has to know a resistant
  archetype always needs an explicit `"health"` entry; nothing in `core` enforces that pairing.
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
  `lifeCompletionBonus()`, `bombCompletionBonus()`, `weaponProjectileDamage()` and `bombDamage()`.
- `game/.../adapter/content/JsonContentSource.java` needs `attachment(String id)`, reading an
  `AttachmentDefinition` from content.

This is `game-presentation`'s module, not `core-domain`'s, so it was not touched here. Verified with
`./gradlew :game:compileJava`, which names exactly these gaps.

**`enemies.json` (owned by `game`/content design, not `core`) needs a `"health"` entry for every
archetype that should survive more than one hit** — tank and heavy carrier chief among them, per
`02-mvp-functional-spec.md`'s roster. `ComponentFactoryRegistry` accepts `"health": {"points": N}`
today; nothing in `core` enforces that a non-fragile archetype actually has one (see "Decisions
taken" above), so this is a content-authoring task, not a code one.

**Acceptance criteria** (`docs/plan/05-game-systems/plan.md`):

| Criterion | Status |
|---|---|
| Every power-up covered by a test for its own consumption rule | Met — `PickupSystemTest` |
| Picking up a maxed power-up increases the score | Met — `PickupSystemTest.maxedPickupIncreasesTheScoreOnceSwept` runs `PickupSystem` then `ScoreSystem` together |
| The attachment absorbs exactly one hit, disappears, no life lost | Already covered by phase 02's `DamageSystemTest`, unchanged this phase |
| Attachment durability raised from data, no code change | Met — `PickupSystemTest.attachmentDurabilityComesFromDataNotAConstant`, `ContentDefinitionsTest` |
| The bomb clears projectiles and damages enemies in the same tick, deterministically | Met — clears projectiles and fragile enemies outright, subtracts `bombDamage` from a resistant enemy's `Health` (`BombSystemTest.resistantEnemyWithEnoughHealthSurvives`/`resistantEnemyDestroyedOnceHealthIsExhausted`, `BombReplayTest`) |
| Score matches the table in `10-mvp-initial-values.md` | Met for per-enemy and maxed-pickup values (unchanged content-driven `ScoreValue`, `maxedPickupBonus`); the completion bonus's per-unit reading is a decision, not a re-confirmation, see above |
| A full-level replay produces the same final score twice | Met — `LevelScoreReplayTest`, `BombReplayTest`; same caveat as `DamageReplayTest` (issue #12): proves determinism within this build, not against a golden fingerprint |
