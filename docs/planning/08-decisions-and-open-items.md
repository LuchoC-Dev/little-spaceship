# Record of decisions, contradictions and open items

## Confirmed decisions

### Product

- Platform: Java + libGDX + Gradle + gdx-teavm, publishing to the browser, with desktop sharing the core. Validated by the prototype on 18/08/2026.
- Build tool: Gradle. It replaces Maven for being the path supported by the stack.
- Web publication target: JavaScript, keeping Wasm available at no cost.
- The core is **single-thread** with a deterministic loop. Multithreading was evaluated and discarded: the web target offers no real parallelism, and the measurements show there is nothing to gain. Closed decision, not to be reopened without a new case.
- Agreed optimisation order: batching and atlases first, spatial structures for collision afterwards, concurrency never.
- Architecture: a hand-written, in-house ECS, without a library. Balanceable content in external JSON read without reflection. Tests of pure systems plus deterministic replays. Manual dependency injection by constructor. Detail in `12-architecture.md`.
- `core` is pure Java and does not depend on libGDX; presentation lives in `game`.
- Strict contract rule: no module exposes concrete classes to another. Every boundary crossing goes through an interface defined by the consumer, and whatever crosses is immutable or read-only. It is verified with an architecture test.
- All code is written in English, including comments, logs, JSON keys and content identifiers.
- Repository `little-spaceship`, root package `dev.luchoc.littlespaceship`, Java 17, Gradle with wrapper and JUnit 5.
- Hexagonal architecture, ports and adapters, with the dependency rule pointing towards the domain. From Clean we take that rule and the domain/infrastructure separation, without forcing use cases inside the game loop.
- Language: everything that lives in the repository is written in English, including agent definitions and `CLAUDE.md`. Only `docs/planning/` stayed in Spanish during this stage, and it was translated at the start of implementation. Conversation with the user is always in Spanish.
- A new game, not a remake.
- A complete level-based vertical shoot 'em up.
- Single-player and local initially.
- Java as the main language.
- Retro/pixel-art.
- One complete, publishable level as the MVP.
- Campaign as the main mode.
- 3–5 levels per stage and five stages as the vision for the first campaign.
- A boss normally at the end of each level, with valid exceptions.

### MVP

- One basic ship.
- Automatic sustained shot.
- Fast movement and slow/precise movement.
- Bomb as the special ability.
- Lives system; three initial lives as a provisional value.
- Arcade score without currency.
- Simple power-ups.
- A single active attachment.
- Play/Options/Quit menu.
- Keyboard and optional mouse.
- Simple pause.
- Level music and a change for the boss.
- No difficulty levels and no checkpoints.
- Intermediate density: neither bullet hell nor purely traditional arcade. Hitbox smaller than the sprite, not a single point and not visible.
- Crashing into an enemy damages the player; weak enemies are destroyed in the crash, heavy ones are not.
- All damage taken grants temporary invulnerability, not only death; the respawn one lasts longer than the one from absorbed damage.
- Decorative scenario without collision, except for a few destructible structures that drop resources.
- The level 1 attachment is delivered by the strong encounter before the rest.
- Keyboard and mouse work at the same time additively: their movement vectors are summed and opposite directions cancel out.
- The mouse is relative, not positional.
- Picking up a power-up already at maximum grants points instead of being wasted.
- The MVP persists audio and mouse preferences, even though it does not save progress.
- The MVP includes a minimal credits and licences screen in Options.
- Integer scaling with nearest-neighbour and letterbox over a fixed logical resolution.
- The starting numeric values live in `10-mvp-initial-values.md` and in configuration, not in the code.

### Enemy fire, and how the level gets balanced

Decided on 22/08/2026 by the project owner, once level 1 was written and the gaps became visible.

- **Enemy weapons get built before the MVP.** *(Done. `EnemyWeapon`, `EnemyWeaponSystem` and the
  `ENEMY_WEAPON` stage landed in phase 07; the four archetypes the spec arms — basic, light, shooter
  and super-fast — carry a `weapon` component as of 25/08/2026, tank included. The state described
  below is the one that produced this decision, not the current one.)* Nothing but the boss shot when
  this was written: `WeaponSystem` armed the player, and enemy firing patterns were deferred through
  phases 04, 05 and 07 without anyone deciding they were out of scope. The consequence was that
  `enemy-shooter` — the "evolved basic" the spec describes as the archetype that shoots — read as a
  larger, slower basic, and the only danger in the level was collision. The palette already reserves a value and hue no background may repeat
  for enemy bullets, and `05-legibility-rules.md` is built around them, so the presentation side is
  specified and waiting.
- **Enemy health is tuned by playing, not by arithmetic.** At the placeholder numbers a heavy carrier
  dies in about 1.2 s against the 32 s the timeline reserves for the strong encounter. The numbers
  have been open since phase 04 and the level now states how long each stretch should last, so there
  is something to calibrate against — but the calibration happens with the game in hand.

### Level 1 climax and length

Decided on 21/08/2026 by the project owner, at the start of phase 07. All three were deliberately left
open during planning and could not be deferred further: the level's timeline cannot be written without
them, and phase 05's guaranteed drops have nowhere to anchor.

- **The boss has one phase, two alternating attack patterns and a clear tell before each.** This is the
  recommendation `docs/plan/07-boss/plan.md` makes on schedule grounds, and it is the version taken: a
  simple, readable boss beats an ambitious one that ships broken. Two phases were the alternative and
  are the first thing the master plan lists for cutting anyway.
- **The strong encounter is two heavy carriers at once.** It is built from what already exists — the
  carrier spawns basic enemies periodically, so two of them produce sustained pressure with no new
  archetype to draw or balance — and it teaches target prioritisation, which is what
  `04-campaign-and-levels.md` asks of that moment. Defeating it hands over the attachment.
- **Level 1 runs four minutes or more, boss included.** Long enough for the full fourteen-beat sequence
  with its calm, escalation, rest and climax rather than a compressed version of it. The cost is
  content to produce and balance, and that is accepted.

### Architecture review, 27/08/2026

Decided by phase 10c, the architecture review, against the code rather than against the design
documents. The evidence is in `docs/plan/10c-architecture-review/assessment.md`, the backlog triage in
`issue-triage.md` and the reasoning including what was rejected in `decision.md`.

- **The architecture holds for the 11 group, with four named additive extensions and no change to its
  shape.** Nothing in the module boundary, the hexagonal layering, the content contracts, the fixed
  system order or the determinism rules changes. The four extensions are issues
  [#84](https://github.com/LuchoC-Dev/little-spaceship/issues/84),
  [#85](https://github.com/LuchoC-Dev/little-spaceship/issues/85),
  [#86](https://github.com/LuchoC-Dev/little-spaceship/issues/86) and
  [#87](https://github.com/LuchoC-Dev/little-spaceship/issues/87), ordered behind
  [#44](https://github.com/LuchoC-Dev/little-spaceship/issues/44).
- **A wave may end on world state rather than on a clock without breaking determinism.** This is not
  an inference from the invariants: `World.View.outcome()` already decides `COMPLETED` by reading the
  world, through `noEnemyLeft()`. Two mechanisms are missing before "cleared" means anything — nothing
  removes an enemy that leaves the playfield (`LifetimeSystem` expires only the two projectile layers),
  and no entity records which wave spawned it.
- **`SPAWN` stays fifth in `SystemOrder`.** A world-state trigger there resolves one tick after the
  last kill, deterministically. Moving the stage after `CLEANUP` to make it immediate was rejected:
  it would spawn waves after the tick's collision pass, which is a different and worse rule.
- **`CollisionLayer.PLAYER` stays**, resolving [#11](https://github.com/LuchoC-Dev/little-spaceship/issues/11).
  It is written by `Simulation` and never matched on, and that asymmetry is the design: the player is
  resolved through `World.playerEntity()` because there is exactly one of it. Deleting the constant
  would force a "none" layer or an unrelated one onto the player's collider, which is worse.
- **Movement as a described thing is the one real gap.** `Motion` is a bare velocity and a trajectory
  is a constant vector resolved once at spawn, so the same archetype cannot enter differently at two
  points of a level without being duplicated. Mechanism only; the shapes and the format are the 11
  group's design. **Built on 29/08/2026 by phase 11c**, which is why this paragraph is left as the
  dated record of what the review found rather than rewritten — see the 11c entry below.
- **Not decided here, on purpose:** what a wave is, and what form the per-level document takes. The
  architecture permits every arrangement of the second except two hand-maintained artefacts, so the
  choice is a design and process decision rather than an architectural one.
- **Open, and put to the project owner:** invariant 6 in `CLAUDE.md` reads "no abstraction without a
  real case in the MVP", and the MVP has shipped. `CLAUDE.md` was not edited;
  [#91](https://github.com/LuchoC-Dev/little-spaceship/issues/91) carries the proposed wording.
  **Resolved on 27/08/2026** — see "The 11 group, 27/08/2026" below.

### The 11 group, 27/08/2026

Decided by the project owner in the planning conversation that produced the six phase plans under
`docs/plan/11a-rule-asserting-tests/` through `11f-web-defects/`. Every one of these was left open on
purpose by `docs/plan/post-mvp-roadmap.md`, and phase 10c refused to close them because they are
design decisions rather than architectural ones — see the "What was considered and rejected" section
of `docs/plan/10c-architecture-review/decision.md`. **Not built:** nothing below is implemented yet.

- **What ends a wave: either, chosen per wave — a fixed duration or being cleared — and fixed duration
  is the default.** Unless a level says otherwise, a wave behaves as `SpawnSystem` does today.
  "Cleared" means every entity the wave spawned has been destroyed **or has left the playfield**, so
  it depends on both [#84](https://github.com/LuchoC-Dev/little-spaceship/issues/84) and
  [#85](https://github.com/LuchoC-Dev/little-spaceship/issues/85).

  **The children a carrier spawns inherit their parent's wave — decided by the project owner on
  28/08/2026, closing [#85](https://github.com/LuchoC-Dev/little-spaceship/issues/85).** A child
  carries the same wave id as the carrier that created it, and a `cleared` wave is not cleared until
  those children have also been destroyed or have left the playfield. The reason is what the player
  reads on screen: a wave ends when the screen is clear of what that wave brought, not when the thing
  that carried them happens to die. A carrier that escapes leaving live children no longer stalls the
  wave for ever, because #84's lifetime and safety box remove anything that leaves the playfield.

  **Built:** `core/domain/component/WaveOrigin.java` records the id of the wave instance that spawned
  an entity, attached by `SpawnSystem.spawnWave` to every entity a wave creates. `SpawnerSystem`
  copies the holder's `WaveOrigin` onto every child it spawns from a carrier, so a carrier with no
  `WaveOrigin` of its own — one built outside a wave, such as by a test — produces children with none
  either. Nothing else that creates an entity as a side effect of a wave-spawned entity — a dropped
  pickup (`CleanupSystem`), an enemy's own projectile (`EnemyWeaponSystem`), a boss part or its
  projectiles (`BossSystem`) — inherits a `WaveOrigin`: the decided rule names carriers and their
  `Spawner`-spawned children specifically, not every downstream entity a wave's enemy happens to
  produce, and nothing consumes the wave id yet — the `cleared` end condition itself is a later task
  ([#112](https://github.com/LuchoC-Dev/little-spaceship/issues/112)).
- **A wave is placed relative to the end of the one before it**, with an offset; a negative offset
  overlaps them. A level carries no absolute timestamps. This removes the guarantee
  `SimpleWaveTimeline`'s constructor gives today by sorting on a timestamp.
- **A wave takes no parameters, in the 11 group.** Level 1 is rebuilt from the fourteen beats of
  `04-campaign-and-levels.md` and reuse only appears once level 2 exists. Invariant 6. Revisited in
  phase 12, which is the phase that will name the concrete case. The risk is accepted: if 12 asks for
  parameters, it is a format change with one level built on top.
- **Waves live in their own file as named content** — `assets/data/waves.json`, beside
  `formations.json` and `trajectories.json` — and a level references them by id. Formations stay the
  grouping below; the two do not blur.
- **An enemy leaves the simulation by two mechanisms, and neither may remove it on screen.** A
  **lifetime** per archetype, expressed as data rather than as a constant in code, which removes the
  enemy only once it is off screen — an enemy still visible when its lifetime expires waits. And a
  **safety box** well outside the playfield that removes anything touching it at once, as the backstop
  for whatever a lifetime does not catch. **The box's coordinates must be written down and must clear
  every legitimate spawn and trajectory**, because enemies are spawned outside the playfield today:
  `SpawnSystem.positionSpawned` places a formation's lowest slot at `PLAYFIELD_HEIGHT + radius`, and
  the largest vertical spread in `assets/data/formations.json` is `column-3` at 44 units, against the
  16-unit margin `LifetimeSystem` uses for projectiles. This **activates a deferral phase 10c had
  closed**: `assessment.md`'s Part 3 evaluated a `Lifetime` timer component and concluded "No.
  Nothing in the 11 group needs a timer-expiring entity." The project owner named the case on
  27/08/2026, which is the standard invariant 6 now sets.

  **Built, closing [#84](https://github.com/LuchoC-Dev/little-spaceship/issues/84):
  `core/domain/component/Lifetime.java`, `core/domain/system/LifetimeSystem.java`.** The safety box
  is 128 logical units past every playfield edge (`x` in `[-128, 336]`, `y` in `[-128, 398]`) —
  `LifetimeSystem.SAFETY_MARGIN`. It clears the worst legitimate spawn measured against
  `assets/data/formations.json` and `assets/data/enemies.json` on this date (`column-3`'s 44-unit
  spread carrying `enemy-carrier`'s 15-unit radius, born at `y = 329`, `314` from its own edge) by 84
  units, deliberately more than that 44-unit floor for the movement shapes phase 11c adds. `Lifetime`
  is an optional per-archetype component (a `"lifetime": {"seconds": N}` spec, read the same way
  `"health"` is), so no existing archetype needs a content change for the safety box alone to fix the
  defect.

  **An enemy that leaves the simulation without being defeated gives the player nothing: no score, no
  drop, no `EnemyDestroyed` — decided by the project owner on 28/08/2026.** Score rewards killing, not
  letting something through, and the un-emitted `EnemyDestroyed` matters beyond the score: `game`'s
  `AudioDirector` is the simulation's `GameEventSink`, so emitting it for an escape would have played
  an explosion sound for an enemy the player never hit, off screen. `LifetimeSystem` is the only place
  that knows an entity is escaping rather than being defeated, so it strips that entity's `ScoreValue`,
  `Drop` and `Collider` before calling `World.markForDestruction` — `ScoreSystem`, `CleanupSystem`'s
  drop resolution and its `EnemyDestroyed` emission are each already conditional on the component they
  read being present, so all three naturally do nothing once it is gone. `CleanupSystem` needed no
  change and its "converges every destruction path uniformly, regardless of what killed its holder"
  stays exactly true: no second, source-aware branch was added to it, an escaping entity simply no
  longer carries anything a uniform sweep would find interesting. This supersedes the non-decision
  originally recorded here (before this date, the intermediate build awarded the score and drop
  uniformly and left the question open).
- **A movement shape is chosen in the spawn event, with the archetype supplying the default.** This is
  the half of [#86](https://github.com/LuchoC-Dev/little-spaceship/issues/86) that 10c named and left
  open. The other half — **which shapes exist** — was decided on 29/08/2026 by
  [#162](https://github.com/LuchoC-Dev/little-spaceship/issues/162), against the beats that ask for
  them: **two kinds, `constant` `{vx, vy}` and `arc` `{vx, vy, ay}`**, and seven named entries — the
  four existing constant vectors plus `strike-run`, `veer-left` and `veer-right`. A shape is a
  function of the entity's own elapsed time and its spawn state, carries no randomness, and must leave
  the playfield unattended in finite time; otherwise a `cleared` wave can deadlock behind it, because
  `LifetimeSystem` removes an enemy only once it is off screen. The reasoning, the parameters and the
  eight refusals are in
  [`docs/plan/11c-movement-shapes/shape-catalogue.md`](../plan/11c-movement-shapes/shape-catalogue.md).
  **Built on 29/08/2026**, and this paragraph replaces a "Not built" that was true when it was
  written. `core/port/TrajectoryDefinition.java` is a sealed interface permitting
  `SimpleTrajectoryDefinition` (`constant`) and `ArcTrajectoryDefinition` (`arc`), whose
  `verticalVelocityAt(float)` returns `vy + ay·t` in closed form;
  `game/adapter/content/JsonContentSource.java` reads the `"type"` key and refuses an unknown one;
  the three `arc` entries are in `assets/data/trajectories.json`; `SpawnEvent` carries an optional
  `trajectoryId` and `MotionSystem` re-evaluates it every tick from `Trajectory`'s `elapsed`.
  [#163](https://github.com/LuchoC-Dev/little-spaceship/issues/163) and
  [#164](https://github.com/LuchoC-Dev/little-spaceship/issues/164) are closed.
  **No wave points at a shape yet** — pointing one would redesign level 1, which is
  [11e](../plan/11e-level-one-redesigned/plan.md).
- **The per-level document is generated from the JSON, and CI fails when they disagree.** Of the three
  arrangements area G of `docs/plan/10c-architecture-review/assessment.md` says the architecture
  permits, this is the one taken. It follows the two generators that already work here,
  `docs/design/atlas/build-atlas.js` and `docs/design/fonts/build-fnt.js`. The fourth arrangement, two
  hand-maintained artefacts, is refused.
- **Level 1 targets around three minutes including the boss, and the number is fixed by playing.**
  **This reopens "Level 1 climax and length" above**, decided on 21/08/2026 as "four minutes or more",
  after the project owner played the shipped build and found five minutes too long. The other two
  decisions in that subsection — the boss's single phase with two alternating patterns and a clear
  tell, and the strong encounter being two heavy carriers — are untouched.
- **The boss's redesign travels with the balance pass**, in the same phase, because tuning a fight
  against a pacing that no longer exists is doing the work twice.
- **`level-designer`'s boundary is widened to all level content under `assets/data/`** — levels,
  waves, formations and movement shapes — from `assets/data/level-*.json` and nothing else. The old
  line left the wave and movement-shape content that phases 11b and 11c create belonging to no agent,
  and put the migration of `level-01.json` into a phase owned by `core-domain`, which would have been
  an agent writing outside its module. **The line did not move in the direction that matters:**
  content is `level-designer`'s, and the systems that read it stay `core-domain`'s. Applied to
  `.claude/agents/level-designer.md` and to the agent table in `CLAUDE.md`.
- **Invariant 6 is reworded, resolving
  [#91](https://github.com/LuchoC-Dev/little-spaceship/issues/91).** The wording 10c proposed is
  accepted and applied to `CLAUDE.md`: *"No abstraction without a real case you can point at. A case
  is a written design or a shipped need, not an expectation."* The standard did not change when the
  MVP shipped; only the sentence's subject did.

### Campaign and progression

- Permanent ship/attachment unlocks.
- Free management of what is unlocked in hangars.
- Hangar currency temporary to the run.
- Survival unlocked after stage 1.
- Endless unlocked after completing the campaign.
- Three profile slots.
- Audio/control configuration global across the slots.
- Autosave on finishing levels/stages and at safe points.
- Continue button.
- On normal difficulty, losing a life does not automatically remove persistent power-ups; each one is consumed by its own rule.
- Defensive priority: invulnerability → shield → attachment → life.
- The attachment is lost when taking damage and when losing a life; it absorbs that hit to avoid the life loss.
- Attachment durability is data configurable per attachment, not a constant in code.
- Attachments operate automatically or semi-automatically.
- Power-ups and the attachment are kept when moving to the next level within the same run.
- Save and quit resumes from the last safe checkpoint, not from the exact position.
- Continue recovers the run's saved state; starting from the checkpoint creates a new run with a default loadout.
- The portfolio must demonstrate architecture, tests, CI, performance, documentation, art and deployment.
- ~~The repository stays private initially; making it public will be evaluated upon reaching the MVP or the final product.~~ **Resolved 25/08/2026: made public on shipping the MVP**, under MIT, with a description and topics.

## Provisional decisions

- Three initial lives.
- Roster and approximate order of appearance for level 1.
- The basic ship improves its shot by increasing the number of projectiles.
- Power-ups controlled by the level design.
- Hangar at the end of each stage and occasional resupply.
- Minimum loadout when starting from advanced checkpoints.
- HP for Survival and a hybrid HP + lives for Endless.

## Resolved contradictions

### Power-ups on losing a life

The initial definition of total loss was replaced for normal difficulty: persistent power-ups do not disappear automatically when a life is lost. Each power-up is removed by its own condition; for example, the shield when absorbing damage. Higher difficulties may impose a greater loss once they are designed.

### Attachment and damage

Resolved: the attachment disappears when taking damage and when losing a life. It absorbs the hit that destroys it, avoiding that life loss, and it sits after the shield in the defensive priority. It is kept when moving to the next level within the same run.

Durability: the same for all attachments by default, but modelled as data configurable per attachment and not as a constant in code, so that a more resistant protection attachment can be supported later.

### End-of-level completion bonus

`10-mvp-initial-values.md`'s score table says the bonus for remaining lives and bombs is "1000 and
300 respectively... to reward finishing in good shape". That sentence is ambiguous between a flat
bonus and one scaled by how much remains, and the second reading is the one that actually rewards
"finishing in good shape" — a flat bonus would pay the same whether the player has one life left or
five. Resolved by phase 05 as per-unit: `lives * 1000 + bombs * 300`. Implemented as
`ScoreSystem.completionBonus(BalanceValues, Player)`. It had no caller when phase 05 wrote it,
since nothing in the core detected "the level is complete" before a boss and a victory condition
existed; phase 07 supplied both, and the result now crosses the boundary as
`WorldView.completionBonus()`.

### Build tool

Maven was an initial decision and a user preference. Gradle became the recommendation for libGDX + gdx-teavm.

Resolved by the prototype, as planned: **Gradle**. The gdx-teavm plugin is a Gradle plugin that resolves backend, assets, `index.html` and local server, and generates the JS and Wasm tasks. Reproducing it with Maven would be manual integration with no gain.

## Open gameplay items

- Rule for losing power-ups on higher difficulties and at the end of the run.
- Exact behaviour with several simultaneous attachments post-MVP: it should not create conflicts since they are automatic, but it has to be seen with real gameplay.
- Intensity curve tool/format.
- **Whether invulnerability should also suppress the consequences for the other entity.** The phase 02 implementation reads "invulnerability → shield → attachment → life" as four layers, so an active invulnerability absorbs a hit with no side effect at all: a weak enemy is not destroyed by ramming an invulnerable player, and an enemy projectile is not consumed. `02-mvp-functional-spec.md` states "weak enemies … are destroyed in that crash" with no condition attached, so this is a narrower reading than the letter of the spec, chosen because it is what the four-layer framing implies and what the genre does. It has not been validated against real gameplay. If it reads wrong once there is a playable build, it is a one-line change in `DamageSystem`.
- **A respawn gap left open by "the ship reappears near where it was destroyed."** Phase 02 implements respawn as never destroying the player entity, so it is already exactly where it died — cheaper than tracking a separate spawn point, and within the letter of the spec. The gap: a slow, non-fragile enemy (tank, heavy carrier) that is overlapping the player at the moment of death is still overlapping when the following invulnerability expires, so the player must actively move away during the grace period or take a second hit immediately. Playable, but not a decided rule — worth checking once there is a playtestable level.
- **Which archetypes count as "weak" for the crash rule was read broadly.** `02-mvp-functional-spec.md` names "basic, light and fast" as destroyed by a crash and only excludes tank and heavy carrier explicitly; it does not say which side of that line the evolved-basic/shooter archetype falls on. Phase 04 reads the rule as "everything except tank and heavy carrier is weak" and marks basic, light, shooter and super-fast fragile — the closing clause ("tanks and heavy carriers are not") reads as the operative rule, with the three named archetypes as examples rather than an exhaustive list. Worth confirming once there is a playtestable level; a one-line change per archetype in `enemies.json` if it reads wrong.
- **The player's starting position has no number in `10-mvp-initial-values.md`.** Phase 04 added `BalanceValues.playerStartX()`/`.playerStartY()` with placeholder values (104, 30 — bottom-centre of the 208x270 playfield), the same "open, not decided" status as `playerSpeed()`/`playerSlowFactor()`. Replace with a real number once there is a playable build to check it against.

## Open campaign and narrative items

- Name of the game, world, factions and characters.
- The exact place defended in level 1.
- Presentation format between stages.
- Detail of bosses and sub-bosses.
- Frequency and rules for multi-boss levels.
- Final number of levels per stage.
- The exact narrative of the supership and the entity in the 3→4 transition.
- The real scope of a possible second campaign.

## Open progression and saving items

- Exact technical fields and serialisation format of the run snapshot.
- Restoration rules in the face of a corrupt save or an incompatible version.
- Names and amounts of the run currency.
- Currency/system for the permanent shop.
- Which stats and records are saved per profile.
- Whether a slot can be copied, renamed or deleted.
- Handling of save-scumming.
- Hangars in Endless mode.
- Final HP/lives design per mode.
- Rules and format for cheat codes.

## Technical items to verify

- ~~Real compatibility in Firefox, Edge and Safari; Chrome already verified.~~ **Chrome and Firefox verified by hand on 25/08/2026 against the live site. Edge was dropped by the project owner's decision. Safari remains unverified.**
- Pointer capture for the relative mouse — built and shipped, and it has a defect: losing the lock breaks mouse control until the page is refocused ([#41](https://github.com/LuchoC-Dev/little-spaceship/issues/41)).
- Measurement with definitive art and audio; the spike generates its textures in code.
- Compatibility of Java dependencies.
- Final hosting.
- Testing, CI and deployment strategy.
- Resizing policy for the web canvas: the backend needs an explicit size.

## Original questions still without an explicit answer

- What concrete difference must be visible with respect to the old game: code, finish, depth, publication or all of it?
- Is there a target date for the MVP?
- Will a gamepad be a post-MVP requirement?
- Is mobile/touch support expected at some point?

## Implementation start-up tasks

- ~~Translate the planning documents into English.~~ Done on 19/08/2026, together with the rename to `docs/planning/`.
- All new documentation —ADRs included— is written directly in English.

## Recommended order for resolving open items

1. Confirm/correct this functional package.
2. Resolve the rules that directly affect the MVP.
3. Run the web/desktop technical prototype.
4. Decide stack and build tool.
5. Define architecture and data/configuration strategy.
6. Create a backlog and begin implementation.
