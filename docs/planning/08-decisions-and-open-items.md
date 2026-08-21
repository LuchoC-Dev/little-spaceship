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
- The repository stays private initially; making it public will be evaluated upon reaching the MVP or the final product.

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
`ScoreSystem.completionBonus(BalanceValues, Player)`, a pure function with no caller yet, since
nothing in the core detects "the level is complete" before a boss and a victory condition exist
(phase 07).

### Build tool

Maven was an initial decision and a user preference. Gradle became the recommendation for libGDX + gdx-teavm.

Resolved by the prototype, as planned: **Gradle**. The gdx-teavm plugin is a Gradle plugin that resolves backend, assets, `index.html` and local server, and generates the JS and Wasm tasks. Reproducing it with Maven would be manual integration with no gain.

## Open gameplay items

- Rule for losing power-ups on higher difficulties and at the end of the run.
- Exact behaviour with several simultaneous attachments post-MVP: it should not create conflicts since they are automatic, but it has to be seen with real gameplay.
- Whether the level 1 boss will have phases and what patterns/aesthetics it will use.
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

- Real compatibility in Firefox, Edge and Safari; Chrome already verified.
- Pointer capture for the relative mouse.
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
