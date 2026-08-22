# Phase 07 — Boss · status

**State:** `core` half done — boss, strong encounter and victory condition. Content
(`assets/data/*.json`) and rendering (health bar, tell) are not built here; see "Notes for whoever
comes next".
**Updated:** 22/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the
`plan.md` next to it says what to do and does not change to reflect progress.

## Done

- **`BossDefinition` / `SimpleBossDefinition`** (`core.port`), looked up by level id through a new
  `ContentSource.hasBoss(String)` / `.boss(String)`, the same pattern as `WaveTimeline`. Carries only
  what varies with balancing — per-part health and score, entrance speed, the fight's combat `y`,
  the cooldown between attacks, and the two patterns' projectile speeds. The five parts' footprint
  (offsets, radii, sprite ids) is *not* here: it is fixed in
  `docs/design/02-sprite-sizes.md`/`06-boss-presentation.md` and hardcoded in `BossSystem` as
  constants, the same treatment `Simulation` already gives the player's collider radius.
- **`BossStatus`** (`core.port`), a `(present, hp, hpMax)` snapshot, and `WorldView.bossStatus()`.
  `present` is exactly "spawned and not yet defeated" — the signal for the health bar
  `docs/design/04-hud-layout.md` shows only during the fight, and the signal for the music change on
  entry the plan's task 4 asks for (see "Decisions taken while implementing"). `hp`/`hpMax` are the
  sum across all five parts, so the bar shortens the instant any part is hit or dies, not only the
  core.
- **`BossSystem`** (`core.domain.system`, new `SystemOrder.BOSS` stage, after `SPAWN` and before
  `LIFETIME`). Stateful like `SpawnSystem`: tracks its own elapsed level time, the five part entity
  ids, and the pattern state machine. Phases: `AWAITING` → `ENTRANCE` (descends at
  `entranceSpeed()` until the core reaches `combatY()`) → `FIGHT` (cooldown, then a fixed-order,
  three-beat 0.75 s tell, then fire, alternating spread/sweep every cycle — no randomness anywhere in
  the pattern) → `DEFEATED` (core destroyed; whatever pods/arms remain are marked for destruction
  with it, so the boss never lingers as a headless husk).
- **The tell's whole contract is the ordinary `Sprite.frame` channel.** The charging parts' frame
  steps 1, 2, 3 across the three beats and drops to 0 the instant the shot leaves — no second,
  boss-specific contract exists on `BossStatus` for "which part, how far through the tell", since
  `forEachSprite` already carries per-entity animation state for everything else in the game.
- **Damage, ram, bomb and score need zero boss-specific code.** Every part is an ordinary
  `ENEMY`-layer, non-fragile `Collider` carrying `Health` and `ScoreValue`, so `DamageSystem`,
  `BombSystem` and `ScoreSystem` already resolve a hit, a detonation and the points it awards.
- **The strong encounter needs no new archetype.** Two `enemy-carrier` waves is content, not code —
  see "Notes for whoever comes next" for exactly what `level-designer` should write.
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
- **`SystemOrder` gained a `BOSS` stage**, after `SPAWN` and before `LIFETIME` — stated explicitly per
  the "fixed system order" invariant, with the reasoning in the enum's own javadoc.
- **Tests, all inline, none reading a file**: `BossSystemTest` (spawn timing, five parts as
  `ENEMY`/non-fragile, the health bar present only during the fight, the entrance settling at
  `combatY`, the tell stepping 1→2→3→fire→0 with real per-tick steps, core death clearing the
  remaining parts and winning with a life left, `DEFEATED` winning a same-tick tie), `BossReplayTest`
  (a full victory and a full defeat run through the real `Simulation` pipeline, each reproduced
  twice), plus `SpawnEvent`/`SpawnSystem` cases for the drop-slot fix (a two-slot formation with the
  drop tied to slot 1, and an out-of-range slot failing at spawn time) and `SystemPipelineTest`'s
  canonical-order fixture updated for the new stage.
- **256 tests total, all passing.**

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

## Blocked

Nothing on the `core` side. The health bar's rendering, the tell's visual beats, the boss art, the
music itself and the actual `level-01.json`/`assets/data` boss content are all outside `core`'s
boundary and are not part of this pass.

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
  `boss(String)` as the missing override.

## Notes for whoever comes next

### For `level-designer`, in `level-01.json`

- **The boss.** No boss content file/schema exists yet — `game-presentation` needs to add
  `ContentSource.boss("level-01")` support first (see below) and decide where the JSON lives (a new
  `boss-l1.json`, or a `"boss"` object inside `level-01.json` — either is fine, `core` only cares
  about the shape of `BossDefinition`). Fields needed, exactly: `id`, `entersAt` (seconds since the
  level started), `coreHealth`, `podHealth`, `armHealth`, `corePoints`, `podPoints`, `armPoints`,
  `entranceSpeed`, `combatY`, `patternCooldown`, `spreadProjectileSpeed`, `sweepProjectileSpeed`. None
  of these numbers are decided by this phase — `10-mvp-initial-values.md` has no boss row yet.
- **The strong encounter is a `SpawnEvent` with `formationId` naming a two-slot formation and
  `enemyId` = `"enemy-carrier"`**, no new archetype. Give it a `dropId` of `"attachment"` and a
  `dropSlot` naming exactly one of its two slots — `0` or `1` — so the encounter hands over exactly
  one attachment, not two. This is the fix for issue #23, landed in this phase specifically so this
  encounter can rely on it.
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
- **Music change on entry**: watch `WorldView.bossStatus().present()` go `false → true` to start the
  boss track, and `true → false` to stop it — check `outcome()` at that same moment to tell victory
  (`COMPLETED`) from the player dying mid-fight (`DEFEATED`), since the spec asks the music to
  "return correctly" only for the latter.
- **`boss-shot` needs a sprite.** Invented in this phase for the boss's projectile — nothing in
  `02-sprite-sizes.md` covers it, since no enemy fired before this phase. Same rough scale as the
  player's own `shot-p1`/`shot-p2` (`PROJECTILE_RADIUS = 2.0f` in `BossSystem`).
- **`boss-core`/`boss-pod`/`boss-arm` sprites**: `docs/design/06-boss-presentation.md` says these are
  "specified here and not yet drawn". `BossSystem` already assumes exactly those three ids, mirrored
  by the renderer for the right-hand pod/arm as that document specifies — `core` never creates a
  fourth or fifth sprite id.

### A gap this phase did not close

`enemy-shooter`'s "higher rate of fire" from `02-mvp-functional-spec.md`'s minimum roster, and the
carrier's "spawns basic enemies periodically" from the same list, are both still unbuilt on the
`core` side — no enemy `"weapon"`/`"spawner"` component factory exists yet, `12-architecture.md`
names both (`Spawner` in its component table) but neither phase 04 nor phase 05 built them, and this
phase did not either: the strong encounter only needs two stationary carriers with health high enough
to matter, not their periodic spawn, to satisfy "no new archetype, defeating it hands over the
attachment." Worth a follow-up issue if the strong encounter is meant to read as "sustained pressure"
rather than "two tough carriers standing still."
