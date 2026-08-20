# Phase 04 — Content pipeline · status

**State:** core side done, review round 1 fixed
**Updated:** 20/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

The `core` half of the phase: contracts, registry, `SpawnSystem`, the timeline, and the player-in-world fix. The JSON loader in `game` is not part of this pass — see "Notes for whoever comes next".

- **Content contracts in `core.port`.** `ComponentSpec` (a generic name + typed key/value bag) is
  what makes "an enemy is a list of components, not a class" real: `EnemyDefinition.components()`
  returns a `List<ComponentSpec>`, and nothing in `core` needs to know what a `"collider"` or a
  `"motion"` component is. Every accessor is required (no default variant survived review — see
  "Review round 1"), failing with a message that distinguishes a missing key from one present with
  the wrong type, both naming the component and the field. Also added: `TrajectoryDefinition`,
  `FormationDefinition` + `FormationSlot`, `SpawnEvent`, `WaveTimeline`. Each has a straightforward
  record implementation (`Simple*`) that both core tests and, once it exists, the `game` loader can
  use directly instead of writing their own. `MapComponentSpec` is the same idea for `ComponentSpec`
  — backed by a plain `Map<String, Object>`, which is what a `JsonValue` object maps onto without
  reflection.
- **`ContentSource` grew `enemy()`, `trajectory()`, `formation()` and `timeline()`.** All four are
  meant to fail by naming the id they could not resolve, never by returning null.
- **`BalanceValues` grew `playerStartX()`/`.playerStartY()`.** Placeholders, same open status as
  `playerSpeed()`/`.playerSlowFactor()` from phase 02 — see `08-decisions-and-open-items.md` and
  `10-mvp-initial-values.md`.
- **Component factory registry**, `core.domain.content.ComponentFactoryRegistry` +
  `ComponentFactory`. `name -> factory`, looked up by exact key, never iterated — registration order
  has no observable effect on a replay. `withDefaults()` registers `motion`, `collider`, `sprite`,
  `scoreValue`; a fifth component type is one more `.register(...)` call, no change to `SpawnSystem`
  or to the loader.
- **Two new components**: `ScoreValue` (points, attached from the archetype's `"scoreValue"` spec)
  and `Drop` (a pickup content id, attached directly by `SpawnSystem` from the *event*, never from
  the archetype — per `03-game-systems.md`, a designed drop marks one wave instance, not every enemy
  of that archetype). `World` grew their stores and frees them in `destroyEntity`.
- **`SpawnSystem`**, `SystemOrder.SPAWN` (ordinal 3, before `COLLISION` at 5). Walks a level's
  `WaveTimeline` with a single advancing cursor — the timeline is validated sorted by
  `SimpleWaveTimeline`'s constructor, which is what makes that cursor correct instead of skipping or
  reordering waves. For each due `SpawnEvent`, resolves the `EnemyDefinition` and `FormationDefinition`
  and creates one entity per formation slot, positioned at `atX * PLAYFIELD_WIDTH + slot.offsetX` on
  the x axis; the y axis is `PLAYFIELD_HEIGHT + radius + (slot.offsetY() − lowestOffsetY)`, measured
  against the whole formation's lowest `offsetY`, not each slot's own — see "Review round 1" (B1) for
  why the simpler per-slot version was wrong. `PLAYFIELD_HEIGHT = 270f` is a new constant here, the
  vertical counterpart to `MotionSystem.PLAYFIELD_WIDTH`, sourced from `docs/design/04-hud-layout.md`'s
  208x270 playfield figure.
- **The extra task: the player now exists from tick zero.** `Simulation`'s shared constructor calls a
  private `spawnPlayer` after building `World`, using `BalanceValues.playerStartX/Y()` for position
  and two new private constants (`ship-basic`, radius 3.0, both from
  `docs/design/02-sprite-sizes.md` — art facts, not balance values, same reasoning as
  `MotionSystem.PLAYFIELD_WIDTH`). The public 3-argument constructor still works unchanged and still
  registers no `SpawnSystem` (no level id to build a timeline from); a new 4-argument overload takes
  a level id and wires `SpawnSystem` into the pipeline too.
- **Tests**, all inline, none reading a file: `MapComponentSpecTest`, `ContentDefinitionsTest`
  (validation of every `Simple*` type — empty ids, no components, no slots, negative timestamps,
  an anchor outside `[0,1]`, an out-of-order timeline naming the offending index),
  `ComponentFactoryRegistryTest` (all four default factories, an unknown component name failing by
  name, registering a new one), `SpawnSystemTest` (due/not-due waves, the cursor never re-firing,
  one tick firing several waves, a multi-slot formation, a designed drop attaching only to that
  wave's entities, the same trajectory id reused across two archetypes, an unknown enemy id and a
  malformed component both failing with the archetype id named), and
  `LevelContentIntegrationTest`, which builds all six level 1 archetypes with the exact sprite ids
  and collider radii from `02-sprite-sizes.md` and the exact score values from
  `10-mvp-initial-values.md`, runs them through a real `Simulation` for ten seconds, and checks every
  archetype's sprite id appears in the world alongside the player's.
- **167 tests total, all passing** after review round 1's fixes (below).

Acceptance criteria against `plan.md`:

| Criterion | Status | Where |
|---|---|---|
| Changing an enemy's stats requires editing JSON only | met on the `core` side | `ComponentSpec`/`EnemyDefinition` carry no code-side stat; `game`'s loader is what makes this true end to end — not built here |
| The same trajectory attaches to two different archetypes from data alone | met | `SpawnSystemTest.sameTrajectoryReusedAcrossArchetypes`, `LevelContentIntegrationTest.tankOnRushTrajectoryIsADataChange` |
| Adding a component type is one registration, no loader change | met | `ComponentFactoryRegistryTest.registeringANewFactoryWorks` |
| Core tests build definitions inline, never read a file | met | every test in this phase |
| Malformed content fails naming the file and the offending id | met for the id half | every `core` failure names an id (enemy, component field, trajectory, timeline index) — see "Notes for whoever comes next" for what the loader still owes |
| All content ids are in English | earned by the loader, not here | every id in this branch lives in a test fixture; no content file exists yet to actually be in English or not — same treatment phase 02 gave an equivalent claim, corrected in review |

Task 5 of the plan ("trajectories and firing patterns") is **half-built**: trajectories are done,
firing patterns are deliberately deferred to phase 05, along with `Weapon` — see "Decisions taken
while implementing" for why. Not an oversight, but also not something the table above should be
allowed to paper over by omission.

## In progress

Awaiting a second pass from `reviewer` on the round 1 fixes below.

## Blocked

Nothing. The JSON loader in `game` is out of scope for `core-domain` by design (see the phase's
own instructions) and is the next piece of work, not a blocker on this one.

## Review round 1

`reviewer` accepted the phase on pull request #16, with two real bugs and three honesty findings to
fix before merge. The reviewer verified every citation in the branch — sprite radii and ids, the HUD
270 figure, the score table, the `SystemOrder` ordinals — checks out; that had not been true of
earlier phases. What changed:

| # | Finding | Fix |
|---|---|---|
| B1 | `SpawnSystem`'s javadoc guaranteed every enemy enters "fully off-screen regardless of its size", but the y position only accounted for the entity's own collider radius, not the formation's vertical extent. A formation like `diagonal` (offsets 0, −15, −30) put two of its three entities inside the visible playfield the instant they spawned. | `spawnWave` now computes the formation's lowest `offsetY` once and measures the spawn height against *that* slot, so every other slot lands further above it: `y = PLAYFIELD_HEIGHT + radius + (slot.offsetY() − lowestOffsetY)`. Pinned by `SpawnSystemTest.diagonalFormationEntersFullyOffScreen`, which asserts every entity's bottom edge (`y − radius`) clears `PLAYFIELD_HEIGHT` — no test exercised a non-zero `offsetY` before this. |
| B2 | `"fragile"` defaulted to `false` in `ComponentFactoryRegistry`, but four of the six level 1 archetypes are fragile — omitting the key in content would silently produce a weak enemy that survives ramming the player. Worse: `MapComponentSpec`'s optional accessors returned the default on a *wrongly typed* value too, so `"fragile": "true"` as a JSON string also silently read as `false` — exactly the bug the malformed-content acceptance criterion exists to catch. | Removed every optional accessor from `ComponentSpec`; `flag`, `number` and `text` are all required now, failing with a message that distinguishes "missing" from "wrong type", both naming the component and the field. `attachCollider` calls `spec.flag("fragile")` with no default, so omitting it fails loudly instead of guessing. Pinned by `MapComponentSpecTest.wrongTypeFlagFails`, `ComponentFactoryRegistryTest.colliderFragileIsRequired`. |
| H1 | "All content ids are in English" was marked met on the strength of test fixtures, in a branch with no content file — the same defect phase 02 shipped and had to correct in review. | Acceptance table now marks it "earned by the loader, not here", matching how the other loader-dependent criteria are already phrased. |
| H2 | Task 5 ("trajectories and firing patterns") reads as done in the task list with no mention that only half of it was built. | Called out explicitly under the acceptance table: trajectories done, firing patterns deferred to phase 05, with the reason repeated from "Decisions taken while implementing". |
| H3 | Two `ComponentSpec` accessors (`number(key, default)`, `text(key, default)`) had no consumer anywhere in `core` — the same "no consumer, don't guess" test that correctly killed `PatternDefinition`, not applied to this interface. | Removed both; only `flag`, now required, survived review, and it has a real consumer (`attachCollider`). |

167 tests total (up from 164 — two dead-accessor tests removed, five new ones added for the required
`flag` accessor, the wrong-type failure path, and the diagonal-formation regression), all passing.

## Decisions taken while implementing

- **`ComponentSpec` is a generic name + typed bag, not one interface per component.** The
  architecture doc's own JSON example (`"collider": {"radius": 7, "layer": "enemy"}`) is already
  shaped this way; a typed interface per component would have meant a matching Java type for every
  JSON object, which is exactly the machinery a `name -> factory` registry is supposed to replace.
- **Firing patterns are not in this pass.** The plan's task 5 names them alongside trajectories, but
  no `WeaponSystem` or `Weapon` component exists yet (phase 05's job per `docs/plan/05-game-systems`),
  and declaring a `PatternDefinition` now would mean guessing its shape — shot count, spread, cooldown,
  projectile sprite — with no consumer to check it against. `core-deferred-surface.md` already
  flagged exactly this risk for `GameEvent` in phase 02; the same reasoning applies here. Trajectories
  did get built because `MotionSystem` already exists and already consumes a `Motion` component, so
  their shape (a constant velocity) is not a guess.
- **`Drop` is attached by `SpawnSystem` directly, never through `ComponentFactoryRegistry`.** A
  designed drop is a property of the `SpawnEvent`, not of the `EnemyDefinition`'s component list —
  the plan's task 8 says so explicitly. Routing it through the same generic registry as archetype
  components would have blurred that distinction back in.
- **Which archetypes are "fragile" was a judgement call.** Recorded as open in
  `08-decisions-and-open-items.md`: the functional spec names "basic, light and fast" as weak and
  only excludes tank and heavy carrier, leaving the shooter archetype's classification implicit.
  Read broadly here — everything except tank and carrier is weak.
- **`Simulation` gained a second public constructor instead of changing the existing one's arity.**
  The 3-argument constructor still works and still spawns the player, just without `SpawnSystem`
  registered (no level id to build a timeline from). This kept every existing test compiling with a
  one-line fix (`DamageReplayTest`) instead of a rewrite, while still giving `game` a 4-argument
  constructor that wires the whole MVP pipeline including spawning.
- **`SpawnSystem` holds mutable instance state (`levelTime`, `cursor`)**, the first system in this
  codebase to do so — every other system so far is a pure static-method holder. `GameSystem`'s own
  javadoc allows "state of their own beyond the strictly necessary"; tracking a level's elapsed time
  and timeline cursor on `World` instead would have meant inventing a general "level time" concept
  that nothing else needs yet. A fresh `Simulation` always builds a fresh `SpawnSystem`, so this stays
  as reproducible as everything else the composition root creates once per run.

## Notes for whoever comes next

- **What `game`'s JSON loader still has to build**, against the contracts declared here:
  - Read `enemies.json`, `trajectories.json`, `formations.json`, `level-01.json` and `balance.json`
    with `JsonReader`/`JsonValue`, never `Json`.
  - For every JSON object naming a component, build a `MapComponentSpec` (or an equivalent
    `ComponentSpec`) and hand the whole list to `new SimpleEnemyDefinition(id, components)`.
  - Build `SimpleTrajectoryDefinition`, `SimpleFormationDefinition` (+ `FormationSlot`) and
    `SimpleWaveTimeline` (+ `SpawnEvent`) directly from parsed JSON — no core-side conversion needed.
  - Wrap whatever `IllegalArgumentException` those constructors or `ContentSource`'s lookups throw
    with the file name, since `core` only ever knows the id, never the file. That combination is
    what the acceptance criterion "names the file and the offending id" actually needs — `core`
    supplies the id half unconditionally, `game` has to supply the file half.
  - Implement `ContentSource` itself: four maps (or similar) keyed by id, built once at load time,
    handed to `Simulation`'s constructor. `balance()` needs a real `BalanceValues`, including the
    still-placeholder `playerSpeed`, `playerSlowFactor`, `playerStartX`, `playerStartY`.
  - **Every `"collider"` component in `enemies.json` must include `"fragile"` explicitly** — it is a
    required field as of review round 1 (B2), not a default. `enemies.json` needs `true` for basic,
    light, shooter and rush; `false` for tank and carrier.
  - The composition root now needs a level id string (e.g. `"level-01"`) to pass to `Simulation`'s
    4-argument constructor.
- **`SystemOrder.SPAWN` is ordinal 3, `COLLISION` is 5.** A spawned enemy is visible to `CollisionSystem`
  the same tick it appears, since `SPAWN` runs before it. Confirmed by re-checking the ordinals
  directly in `SystemOrder`, per this phase's own warning about a previous status file getting this
  wrong.
- **`ComponentFactoryRegistry` factories always attach the enemy collision layer.** The content
  pipeline only spawns enemies in this phase — no pickups, no structures — so `"collider"`'s factory
  hardcodes `CollisionLayer.ENEMY` rather than reading a `"layer"` field nothing would ever set to
  anything else yet. Whoever spawns pickups or structures through this same registry needs to either
  add that field back or give pickups/structures their own spawning path.
- **The six level 1 archetypes are not hardcoded anywhere in `core`.** They only exist as literal
  values inside `LevelContentIntegrationTest`, built to prove the generic machinery covers them —
  copy those exact ids, radii and score values into `enemies.json`, they are sourced straight from
  `docs/design/02-sprite-sizes.md` and `docs/planning/10-mvp-initial-values.md`.
