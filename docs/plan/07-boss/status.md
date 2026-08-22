# Phase 07 — Boss · status

**State:** `core` half done — boss (six parts, keel gap closed), the carrier's periodic spawn, the
strong encounter and the victory condition. Content (`assets/data/*.json`) and rendering (health bar,
tell) are not built here; see "Notes for whoever comes next".
**Updated:** 22/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the
`plan.md` next to it says what to do and does not change to reflect progress.

## Done

- **`BossDefinition` / `SimpleBossDefinition`** (`core.port`), looked up by level id through a new
  `ContentSource.hasBoss(String)` / `.boss(String)`, the same pattern as `WaveTimeline`. Carries only
  what varies with balancing — per-part health and score, entrance speed, the fight's combat `y`,
  the cooldown between attacks, and the two patterns' projectile speeds. The parts' footprint
  (offsets, radii, sprite ids) is *not* here: it is fixed in
  `docs/design/02-sprite-sizes.md`/`06-boss-presentation.md` and hardcoded in `BossSystem` as
  constants, the same treatment `Simulation` already gives the player's collider radius.
- **`BossStatus`** (`core.port`), a `(present, hp, hpMax)` snapshot, and `WorldView.bossStatus()`.
  `present` is exactly "spawned and not yet defeated" — the signal for the health bar
  `docs/design/04-hud-layout.md` shows only during the fight, and the signal for the music change on
  entry the plan's task 4 asks for (see "Decisions taken while implementing"). `hp`/`hpMax` are the
  sum across every part, so the bar shortens the instant any part is hit or dies, not only the core.
- **`BossSystem`** (`core.domain.system`, new `SystemOrder.BOSS` stage, after `SPAWNER` and before
  `LIFETIME`). Stateful like `SpawnSystem`: tracks its own elapsed level time, the part entity ids,
  and the pattern state machine. Phases: `AWAITING` → `ENTRANCE` (descends at `entranceSpeed()` until
  the core reaches `combatY()`) → `FIGHT` (cooldown, then a fixed-order, three-beat 0.75 s tell, then
  fire, alternating spread/sweep every cycle — no randomness anywhere in the pattern) → `DEFEATED`
  (core destroyed; whatever keel, pods or arms remain are marked for destruction with it, so the boss
  never lingers as a headless husk).
- **Six parts, not five — the collider gap fix.** `visual-designer` found, drawing the parts against
  `02-sprite-sizes.md`'s map, that 25 px of the core's keel was pass-through: a shot straight into the
  boss's own aim point registered nothing. Closed exactly as proposed in `06-boss-presentation.md`:
  a sixth entity, `core-keel`, radius 13.0 at offset (0, −27); the arms move from offset y −18 to −22.
  The keel carries its own `Health`/`ScoreValue` — the core's own numbers, since it reads as part of
  the core rather than a fourth kind of part — and no `Sprite` of its own: it only extends where a hit
  against the core's existing drawn sprite registers. It behaves exactly like a pod or an arm
  otherwise — dies with the core, its own death alone ends nothing. See "Decisions taken while
  implementing" for the judgement call this involved (independent health pool vs. a shared one).
- **The tell's whole contract is the ordinary `Sprite.frame` channel.** The charging parts' frame
  steps 1, 2, 3 across the three beats and drops to 0 the instant the shot leaves — no second,
  boss-specific contract exists on `BossStatus` for "which part, how far through the tell", since
  `forEachSprite` already carries per-entity animation state for everything else in the game.
- **Damage, ram, bomb and score need zero boss-specific code.** Every part, keel included, is an
  ordinary `ENEMY`-layer, non-fragile `Collider` carrying `Health` and `ScoreValue`, so
  `DamageSystem`, `BombSystem` and `ScoreSystem` already resolve a hit, a detonation and the points
  it awards.
- **The heavy carrier now actually spawns basic enemies periodically**, per
  `02-mvp-functional-spec.md:183`. New `Spawner` component (`core.domain.component`) — named in
  `12-architecture.md`'s component table from the MVP's first draft, unbuilt until the strong
  encounter gave it a real consumer — holding which archetype to spawn, the interval, and an offset
  from the holder, all data-driven through a new `"spawner"` `ComponentFactoryRegistry` entry. New
  stateless `SpawnerSystem`, new `SystemOrder.SPAWNER` stage between `SPAWN` and `BOSS`. This is why
  the strong encounter was chosen in the first place — two carriers now produce sustained pressure,
  not just two large, stationary targets — and it is what makes the encounter's own reason for
  existing real again. See "Notes for whoever comes next" for exactly what `level-designer` should
  write to tune it.
- **A spawn inside a spawn, kept deterministic.** `SpawnerSystem` never mutates the `Spawner` store it
  iterates — a spawned child never itself carries a fresh `Spawner` — and walks that store as a plain
  index loop over its dense array, never a `HashMap`/`Set`, so which carrier's child is created first
  when several are due the same tick is exactly the carriers' own creation order. Reasoning recorded
  in full on `SystemOrder.SPAWNER`'s own javadoc, citing phase 02's finding F4 as the precedent for
  this class of same-tick ordering mistake. `SpawnerReplayTest` proves the combination — `SpawnSystem`
  placing two carriers, `SpawnerSystem` spawning their children, all in the same run — reproduces
  identically twice through the real pipeline, not only in `SpawnerSystemTest`'s isolation.
- **The strong encounter needs no new archetype.** Two `enemy-carrier` waves, each carrying a
  `"spawner"` component, is content, not code — see "Notes for whoever comes next" for exactly what
  `level-designer` should write.
- **Issue #23, the drop-slot fix.** `SpawnEvent` gained a sixth field, `dropSlot` (which formation
  slot carries the drop), with a five-argument constructor kept for source compatibility that
  defaults to slot 0. `SpawnSystem.spawnWave` now attaches `Drop` to exactly that slot's entity, and
  fails at spawn time, naming the formation, if `dropSlot` names a slot the formation does not have.
- **`WorldView.outcome()` learned about the boss.** A new `World.bossLevel` flag, set every tick by
  `BossSystem` once it resolves `hasBoss(levelId)` true, makes a boss level complete *only* by
  `World.markBossDefeated()` — never by the older "wave timeline dry, no enemy left" rule, which
  would otherwise report `COMPLETED` the instant the boss's own pre-entrance quiet gap left no enemy
  on screen, long before the fight even started. A level with no `BossSystem` registered, or one
  whose content has no boss, keeps the exact pre-phase-07 rule. `DEFEATED` is still checked first in
  both branches, so it wins a same-tick tie against a boss dying on the very tick the player's last
  life is lost.
- **`SystemOrder` gained two stages**, `SPAWNER` and `BOSS`, both between `SPAWN` and `LIFETIME` in
  that order — stated explicitly per the "fixed system order" invariant, with the reasoning in each
  stage's own javadoc.
- **Tests, all inline, none reading a file**: `BossSystemTest` (spawn timing, six parts as
  `ENEMY`/non-fragile, the health bar present only during the fight, the entrance settling at
  `combatY`, the tell stepping 1→2→3→fire→0 with real per-tick steps, core death clearing the
  remaining parts and winning with a life left, `DEFEATED` winning a same-tick tie), `BossReplayTest`
  (a full victory and a full defeat run through the real `Simulation` pipeline, each reproduced
  twice), `SpawnerSystemTest` (interval timing, offset positioning, repeated firing, creation-order
  determinism across two holders due the same tick, a destroyed holder not spawning, an unknown
  enemy id failing by name), `SpawnerReplayTest` (two carriers through the real pipeline, reproduced
  twice), `ComponentFactoryRegistryTest`'s new `"spawner"` case, plus `SpawnEvent`/`SpawnSystem` cases
  for the drop-slot fix (a two-slot formation with the drop tied to slot 1, and an out-of-range slot
  failing at spawn time), `WorldTest`'s reflection guard extended for the new `Spawner` store, and
  `SystemPipelineTest`'s canonical-order fixture updated for both new stages.
- **266 tests total, all passing.**

Acceptance criteria against `plan.md` and issue #27:

| Criterion | Status | Where |
|---|---|---|
| The boss can be defeated and can kill the player, both to the right screen | **met on the `core` side** | `BossReplayTest.victoryIsDeterministic`/`defeatIsDeterministic` prove `LevelOutcome` reaches `COMPLETED`/`DEFEATED`; which screen `game` shows for each is that module's job, not built here |
| Its health bar appears only during the fight | **met** | `BossStatus.present`, `BossSystemTest.presentOnlyDuringTheFight`; drawing the bar itself is `game-presentation`'s |
| Music changes on entry and returns correctly if the player dies | **hook provided, not implemented** | `BossStatus.present` flipping false→true is the entry edge; `game-presentation` detects it and plays/stops audio — see "Notes for whoever comes next" |
| The fight is deterministic: same seed and inputs, same outcome | **met** | `BossReplayTest`, both scenarios run twice with identical fingerprints; no `Rng` use anywhere in `BossSystem` — the pattern alternates by a fixed rule, never a roll |
| A replay covers a full victory and a full defeat | **met** | `BossReplayTest.victoryIsDeterministic`, `.defeatIsDeterministic` |
| Victory requires surviving with at least one life | **met** | `World.View.outcome()`'s `bossLevel` branch, `BossSystemTest.defeatWinsATieWithBossDefeat` |
| The strong encounter hands over the attachment, tied to one slot | **met on the `core` side** | the drop-slot fix; the actual two-carrier wave is `level-designer`'s content, not built here |
| The strong encounter is two carriers producing sustained pressure | **met** | `Spawner`/`SpawnerSystem`, `SpawnerReplayTest` |
| The boss is hittable everywhere it is drawn (the keel gap) | **met on the `core` side** | `core-keel`; the actual boss art/collider alignment is `game-presentation`'s to verify visually |

## Blocked

Nothing on the `core` side. The health bar's rendering, the tell's visual beats, the boss art, the
music itself and the actual `level-01.json`/`assets/data` boss and carrier content are all outside
`core`'s boundary and are not part of this pass.

## Decisions taken while implementing

- **No `GameEvent` for "boss entered".** No concrete `GameEvent` implementation exists anywhere in
  this codebase yet — every earlier phase deferred it for lack of a real consumer, per
  `core-deferred-surface.md`. Rather than being the first to invent one, the music-change hook is
  `BossStatus.present` itself: `game-presentation` already has to poll `WorldView` every frame for
  the HUD, and detecting `present` go `false → true` (start music) or `true → false` without
  `outcome() == COMPLETED` (the player died — stop/revert) is the same edge-detection shape it needs
  for other feedback the HUD row table names. Revisit if a second, unrelated consumer ever needs the
  same edge, at which point a real event earns its place.
- **One phase, two alternating patterns — the recommendation from the plan and the decision recorded
  in `08-decisions-and-open-items.md` — confirmed, not re-litigated.** The pattern order is
  `SPREAD, SWEEP, SPREAD, SWEEP, …`, alternated by a plain boolean flip, never by `Rng`. Determinism
  for a stateful fight like this is easiest to keep by removing randomness from the one place it
  would have mattered, not by being careful with it.
- **The boss holds position once it reaches `combatY`; it does not move during the fight.** The plan
  explicitly leaves "whether it moves or holds position" open. Holding position is the simpler,
  more readable choice the plan's own recommendation argues for, and it keeps the tell — the thing
  actually teaching the player to read the boss — as the only thing demanding attention during a
  cycle.
- **`core-keel` gets its own independent `Health`, not a hitbox that forwards damage into the core's
  own pool.** The alternative considered — attaching the *same* `Health` object reference to both the
  core and the keel entities, so a hit on either reduces one shared number — was rejected: `HealthDamage`
  marks for destruction whichever single entity was actually hit, so a shared object would leave the
  core entity itself "alive" (undestroyed) forever if the keel absorbed the killing blow, breaking
  `updateSpawned`'s `!world.isAlive(core)` check without extra, boss-specific bookkeeping to compensate.
  An independent pool is exactly the model the other five parts already use, needs no special case
  anywhere, and the cost — the keel region being incrementally tougher than a single hitbox would be,
  since a shot purely inside it does not also chip the core — is a fine trade for closing a
  pass-through gap. `reportStatus`'s `hp`/`hpMax` sum, and `handleCoreDeath`'s clean-up list, both
  simply grew by one more part.
- **A new enemy projectile is the first one this codebase has ever created.** No enemy has fired at
  all before this phase (`enemy-shooter`'s "higher rate of fire" in the functional spec was never
  built — the roster's own `ComponentFactoryRegistry` has no `"weapon"` factory yet). The boss's shot
  needed a `SpriteId`; `"boss-shot"` was invented here and needs art — flagged below.
- **Projectile angles are fixed ratios, not runtime trigonometry.** `Math.sin`/`cos` are not
  guaranteed to produce the identical float on the JVM and under TeaVM, which a replay cannot afford;
  the spread and sweep shapes are baked-in constants, the same treatment `WeaponSystem.SHOT_SPACING`
  already gets for the player's own volleys.
- **`ContentSource` gained two more methods.** `game`'s `JsonContentSource` does not implement them
  yet and will not compile until it does — the same kind of cross-module step every earlier content
  addition (`EnemyDefinition`, `AttachmentDefinition`, `WaveTimeline`) has required. Confirmed by
  running `./gradlew :game:compileJava` from this worktree, which fails naming exactly
  `boss(String)` as the missing override; adding `Spawner` did not introduce a second one, since it
  only touches `core.domain`, never `ContentSource`.
- **`SpawnerSystem` positions a child at the holder's own current position plus a fixed offset, never
  off-screen the way `SpawnSystem` places a wave's anchor.** A carrier's companion appearing at a
  station already on screen, wherever the carrier currently is, is the correct behaviour; the
  off-screen-entry trick exists for a wave's anchor specifically because a wave enters from beyond the
  playfield edge, which a carrier's own child does not need to.
- **The structure collider gap (31×39 sprite, no single radius-15 circle covers it) is recorded as
  open, not fixed.** `visual-designer` proposed two radius-11.0 colliders at offsets (0, ±10). Unlike
  the boss, no `structure-*` archetype exists anywhere yet — not in `enemies.json`, not spawned by any
  level, not referenced by any phase's content — so closing this would mean inventing the *general*
  capability for one archetype to carry more than one collider through the generic, data-driven
  content pipeline (`ComponentFactoryRegistry` currently attaches exactly one `"collider"` per
  entity), not a boss-specific fix the way `core-keel` was. That is real, unbuilt machinery with zero
  current consumer — precisely what this project avoids building ahead of a real need — so per the
  coordinator's own instruction ("if it drags, record it as open"), it is left open here rather than
  forced in. Worth a follow-up issue once a level actually places a destructible structure.

## Notes for whoever comes next

### For `level-designer`, in `level-01.json`

- **The boss.** No boss content file/schema exists yet — `game-presentation` needs to add
  `ContentSource.boss("level-01")` support first (see below) and decide where the JSON lives (a new
  `boss-l1.json`, or a `"boss"` object inside `level-01.json` — either is fine, `core` only cares
  about the shape of `BossDefinition`). Fields needed, exactly: `id`, `entersAt` (seconds since the
  level started), `coreHealth`, `podHealth`, `armHealth`, `corePoints`, `podPoints`, `armPoints`,
  `entranceSpeed`, `combatY`, `patternCooldown`, `spreadProjectileSpeed`, `sweepProjectileSpeed`. None
  of these numbers are decided by this phase — `10-mvp-initial-values.md` has no boss row yet. Nothing
  about `core-keel` needs a content entry: its health/points are the core's own numbers, read
  automatically.
- **The strong encounter is a `SpawnEvent` with `formationId` naming a two-slot formation and
  `enemyId` = `"enemy-carrier"`**, no new archetype. Give it a `dropId` of `"attachment"` and a
  `dropSlot` naming exactly one of its two slots — `0` or `1` — so the encounter hands over exactly
  one attachment, not two. This is the fix for issue #23, landed in this phase specifically so this
  encounter can rely on it.
- **`enemy-carrier` in `enemies.json` needs a `"spawner"` component added**, exactly:
  `"spawner": { "enemyId": "enemy-basic", "interval": <seconds>, "offsetX": <units>, "offsetY": <units> }`.
  None of these four values are decided by this phase — tune the interval and offset to taste; a
  negative `offsetY` (below the carrier, since `Transform.y` grows upward) reads most naturally for a
  companion trailing behind it, but nothing enforces that.
- **The boss's `entersAt` should land after the final escalation**, per
  `04-campaign-and-levels.md`'s beat 10, and the level's own four-minutes-or-more target already
  agreed in `08-decisions-and-open-items.md` under "Level 1 climax and length".

### For `game-presentation`

- **`ContentSource.boss(String)` / `.hasBoss(String)` are new abstract methods.**
  `game/adapter/content/JsonContentSource.java` does not compile until both are implemented — verified
  directly, this is not a guess.
- **The health bar reads `WorldView.bossStatus()`**: `present` for whether to draw the `BOSS` label
  and the bar frame at all (`04-hud-layout.md`'s `362,44`/`347,20` positions), `hp`/`hpMax` for
  `round(228 * hp / hpMax)` filled rows, exactly as that document already specifies.
- **The tell reads each boss part's ordinary `Sprite.frame`** through `forEachSprite`, the same
  channel as everything else. Frame `0` is idle/no tell; `1`, `2`, `3` are the tell's three beats
  (`06-boss-presentation.md`'s beats 1–3, each held for a quarter second); frame drops back to `0` the
  exact tick the shot leaves. Which two parts are charging is not a separate signal — it is simply
  whichever pods or arms currently report a nonzero frame, since the core's frame never changes.
  `core-keel` never carries a `Sprite` at all, so it never appears in `forEachSprite` — nothing to
  draw for it, by design.
- **Music change on entry**: watch `WorldView.bossStatus().present()` go `false → true` to start the
  boss track, and `true → false` to stop it — check `outcome()` at that same moment to tell victory
  (`COMPLETED`) from the player dying mid-fight (`DEFEATED`), since the spec asks the music to
  "return correctly" only for the latter.
- **`boss-shot` needs a sprite.** Invented in this phase for the boss's projectile — nothing in
  `02-sprite-sizes.md` covers it, since no enemy fired before this phase. Same rough scale as the
  player's own `shot-p1`/`shot-p2` (`PROJECTILE_RADIUS = 2.0f` in `BossSystem`).
- **`boss-core`/`boss-pod`/`boss-arm` sprites are drawn** in the art lane's worktree
  (`docs/design/06-boss-presentation.md`, `06-boss-presentation.md`'s mockups). `BossSystem` assumes
  exactly those three ids, mirrored by the renderer for the right-hand pod/arm as that document
  specifies. **The arms now sit at offset y −22, not −18** — re-check any mock or layout already built
  against the earlier number.

### A gap this phase did not close

`enemy-shooter`'s "higher rate of fire" from `02-mvp-functional-spec.md`'s minimum roster is still
unbuilt on the `core` side — no enemy `"weapon"` component factory exists yet, and nothing in this
phase needed one, since the boss's own fire goes through `BossSystem` directly rather than the generic
archetype pipeline. The carrier's periodic spawn, previously listed here as the same kind of gap, is
now built — see "Done" above. The structure collider gap is recorded as open above, in "Decisions
taken while implementing", for the reason given there.
