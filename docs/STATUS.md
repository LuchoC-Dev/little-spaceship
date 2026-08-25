# Project status

Last updated: 25/08/2026.

Read this first if you are picking the project up. It says where things stand and what comes next; `CLAUDE.md` says how to work here.

## Where we are

**`main` is now the whole picture, and the game is finished except for shipping it.** The three branches
that held the MVP were reviewed and merged on 25/08/2026 — sprites (#30), audio (#31), boss (#29) — and
everything they left unwired was closed the same day, along with a balance pass driven by playing, two
defects and a UI pass. `./gradlew build` is green and `core` carries 289 tests.

**Phase 09 — web, CI and deploy — is the only thing between this and a link a stranger can open.**
Nothing else blocks the MVP.

A pattern worth knowing before you trust any phase marked done: **three times in one day, art that a
phase had "delivered" turned out to exist only under `docs/design/`, with nothing in `assets/` and no
code loading it** — the sprites, then the fonts. A phase status saying the art is drawn does not mean
the game can draw it. Check `assets/` and check what the loader actually asks for.

What exists in the repository:

- `docs/planning/` — fourteen documents covering vision, MVP spec, game systems, campaign, progression, balance values, architecture and the agent workflow.
- `docs/sources/` — the verbatim ChatGPT transcript the planning came from. Spanish on purpose; it is evidence.
- `spikes/web-viability/` — a throwaway prototype that validated the platform. Not the base of the game. It can be deleted once it stops being useful.
- `docs/design/` — the visual direction: the closed `ls32` palette, sprite sizes in pixels per archetype, bitmap typography, HUD layout, legibility rules, and pixel-exact mocks. Synchronisation point 1, settled.
- `.claude/agents/` — six agent definitions with project-scoped persistent memory.
- `core/` — the ECS, the fixed-step loop, `Rng`, `InputFrame` and the ports, plus motion, collision, the defensive chain, cleanup, the content contracts, the spawner, and phase 05's systems: weapons and their upgrades, power-ups, the attachment, the bomb, `Health` and scoring. 236 tests, no libGDX on its classpath.
- `game/` and `desktop/` — the LWJGL3 launcher, the input adapter that sums keyboard and relative mouse, an allocation-free renderer reading through `WorldView`, integer-scaled viewport, placeholder art at the sizes the visual direction fixed, and the JSON content loader. No tests yet. `web/` is still an empty skeleton; phase 09 owns it.
- `assets/data/` — the content as JSON: six archetypes, four trajectories, three formations and one timeline.

## The three merges, and what they did not do

Merged 25/08/2026 after one `reviewer` pass each, on Sonnet, one per pull request.

| PR | Branch | Verdict |
|---|---|---|
| [#30](https://github.com/LuchoC-Dev/little-spaceship/pull/30) | `feat/sprite-production` | accept — every measurable claim reproduced against the committed PNGs |
| [#31](https://github.com/LuchoC-Dev/little-spaceship/pull/31) | `feat/audio` | accept — `core` untouched, no threads, determinism intact |
| [#29](https://github.com/LuchoC-Dev/little-spaceship/pull/29) | `feat/boss` | accept — including the coordinator-committed last commit, whose "277 tests" is exact |

The merges resolved the cross-branch dependencies but did **not** finish the features. Three
capabilities landed connected to nothing, and one deliverable was not what its phase status implied.
All four were closed the same day, on their own branches, each merged after the coordinator audited
the diff:

- **`EnemyDestroyed` had no listener** — the explosion sound was generated, loaded and silent.
  `AudioDirector` is now the simulation's `GameEventSink`.
- **Nothing read `bossStatus()` for audio** — `MusicTrack.BOSS` never played. The `false→true` edge
  now swaps the music. The return-on-death path needed no new code: `LittleSpaceshipGame.setScreen`
  already stops the music for any screen that is not `PlayScreen`.
- **No archetype declared a `"weapon"`** — enemies did not shoot in the shipped level. `enemy-shooter`
  now carries `{ "pattern": "straight-single", "rate": 1.8, "speed": 90 }`. Those numbers are
  `level-designer`'s judgement, not a decided value; see `docs/plan/07-boss/status.md`.
- **The sprites were not in the game.** `#30`'s diff touched only `docs/design/`: the 34 character
  sprites lived as pixel art in `docs/design/mockups/src/01-sprites.js`, and the PNGs that script
  renders are gitignored previews. `PackedSpriteAtlas` and `SpriteAtlas` already existed and were
  falling back to `PlaceholderAtlas` for want of a file. `docs/design/atlas/build-atlas.js` now
  generates `assets/atlas/sprites.png`/`.atlas` from that one source, and the game loads it.

## What is left

1. ~~**Balance.**~~ **Closed on 25/08 by the player's own judgement: good enough to ship.** What is
   left is in "Post-MVP backlog" below, not here. The history below is kept because it records how the
   numbers got where they are.

   Also fixed the same day, both defects rather than balance: the player could fly off the top and
   bottom of the screen for ever (`MotionSystem.clampPlayerToPlayfield` clamped x only — there was no
   y clamp at all), and the HUD's labels sat on top of their own icon rows.

   Level 1's pacing was written before enemies could shoot, and enemy health is still
   placeholder — a heavy carrier dies in about 1.2 s against the 32 s its stretch reserves. Decided:
   tuned by playing, not by arithmetic.

   Two play sessions on 25/08 drove a round of fixes, all merged the same day.

   **What was wrong and is now fixed.** Only `enemy-shooter` was ever armed, and it does not appear
   in `level-01.json` until 166 s — so the player met no enemy fire at all. Worse, `EnemyWeapon`
   initialised `cooldownRemaining` to the full cooldown, so an enemy had to survive an entire cooldown
   before its first shot, while these archetypes carry no `health` component and die to one player
   projectile. Nothing ever fired.

   `EnemyWeapon` now takes an optional `"firstShotDelay"`, independent of `"rate"` and defaulting to it
   (zero rejected: an enemy firing the frame it spawns is unreadable). The roster in
   `02-mvp-functional-spec.md` was then honoured properly — Basic, Fast light, shooter and Super-fast
   all shoot; Tank and carrier do not, as specified. The boss's `patternCooldown` went 1.3 → 0.7,
   making its attack cycle 2.05 s → 1.45 s against the fixed 0.75 s tell. Verified on screen: six
   enemy projectiles in flight from three archetypes at once, and the player losing a life to them.

   **The open tension, deliberately not resolved.** The spec asks the basic for "low health and a slow
   shot", and those fight each other: dying to one hit, a basic rarely lives to fire twice, so its
   rate-of-fire contrast against the shooter is mostly invisible. The honest fix is a small `Health`
   (2–3 points) on `enemy-basic` and `enemy-light`, which is a real balance change — it lengthens clear
   time and introduces chip damage against `weaponProjectileDamage: 10`. Left for a play session to
   decide, not for arithmetic.

   **Watch first, next session:** whether the basic reads as firing less often than the shooter or just
   dies too fast to tell; whether the boss at 0.7 now feels like a boss; whether `enemy-rush`'s single
   likely shot reads as "shoots little" rather than "does not shoot"; and whether `enemy-light`'s
   130 u/s projectile is dodgeable — it is the fastest enemy shot in the game.
2. **Phase 09 is the only thing left before the MVP.** — web, CI and deploy. Not started, and it is what turns this into a link a stranger
   can open. `assets/startup-logo.png` is already in place; `web/` is an empty skeleton by decision,
   and the TeaVM build was verified working once, in phase 03, before being reverted as out of scope.

### Post-MVP backlog, from real play on 25/08

The player judged the gameplay good enough to ship and asked that none of these be changed before the
MVP. They are written down here because they came from playing, which is the only source this project
trusts for balance, and because two of them are defects rather than tuning.

- **The shooting sound glitches under sustained fire.** Appears after firing continuously for a while.
  Most likely `Sound` instance exhaustion in libGDX — every shot starts a new one and nothing bounds
  them. Not diagnosed.
- **Frame rate drops about 20 FPS for 5–10 seconds at certain points.** Cause unknown. The obvious
  hypothesis — projectiles accumulating off screen — was **checked and refuted**: `LifetimeSystem`
  expires both projectile layers past the playfield edge with a 16 unit margin, so they do not pile
  up. Do not start from that theory again.
- **The boss is still too easy, and the reason is its aim, not its volume.** Tripling the fan to three
  rays per part barely moved it. The player's diagnosis is better than the fix: the spread always
  points *outward* and the sweep *inward*, so a player who parks in the centre is never threatened.
  The fight is a positioning problem solved once, not a dodge. What to try: five rays, and aim the fan
  at the player rather than at fixed outward angles. That changes the nature of the attack instead of
  its quantity — and it must be weighed against the tell staying honest, which is what makes this boss
  fair.
- **Every enemy projectile uses `shot-e-small`.** `EnemyWeaponSystem` hardcodes it. The tank's shot
  should be visibly heavier — `shot-e-heavy` already exists in the atlas, unused — which needs
  `EnemyWeapon` to carry a sprite id per weapon.

### The UI pass, 25/08 — closed

The player called the screens "not well made": buttons with no padding or padding on one side only,
text overflowing its frames, a BACK button clipped off the bottom of Options. None of it was padding.

**`GameSkin.build()` was calling `new BitmapFont()` — libGDX's bundled Arial — and registering it under
the names `"font-mini"` and `"font-title"`.** Every screen and `HudRenderer` asked the Skin for the
project's own fonts by name and got Arial. Smooth, proportional, wrong metrics, inside layouts drawn
pixel-exactly around a fixed 6/8 px advance. The frames were right; the glyphs were wrong. The hand-drawn
sheets had existed since #30 and had never been made loadable — **the same gap the sprites had**: art
produced in `docs/design/`, never packaged into `assets/`. That pattern appeared three times in one day;
it is worth checking for a fourth before assuming a phase is done.

`docs/design/fonts/build-fnt.js` now emits `assets/fonts/*.fnt` + page from the sheets, and `GameSkin`
loads them at scale 1. Fixed alongside it: the BACK button (the placeholder's line height overflowed the
480×270 viewport — `BaseUiScreen.content` has no clip), absent button minimums (`04-hud-layout.md`'s
60×12 was never enforced), one-sided padding, menu entries now centred so a short label's slack splits
evenly, and the focus marker no longer flush against the frame.

**One thing deliberately re-added:** the focused entry grows 6 px. That was never an animation — with the
placeholder font each button sized to its own text, so swapping `"  "` for `"> "` widened it for free.
A fixed advance makes that swap cost zero pixels, so the movement vanished when the real font landed. It
is now driven from the cell's width, not the button's padding, because the 60 px plate minimum absorbs
padding on short labels and nothing moves.

### Known and deliberately left open

None of these block the MVP. Each is a real thing someone chose not to fix under a deadline.

- **Art and ECS disagree on five pickup ids** (`pickup-weapon` vs `pickup-weapon-upgrade`, and four
  more), bridged by alias regions in `sprites.atlas` — same pixels, second name — because renaming
  either side crosses an agent boundary. Settle it by picking a side, not by adding a third alias.
- **`boss-shot` has no authored art.** It was invented in phase 07, after the sprite pass, and is
  aliased to `shot-e-small`. It reads correctly; it is not its own sprite.
- ~~The HUD's icon squares overlap their text labels.~~ **Fixed on 25/08.** `HudRenderer` fed
  `BitmapFont.draw` through the rect helper `yGdx(yDown, height)`, but a font's `y` is already a top
  edge ("the top of most capital letters"), not a bottom-left corner, so the cap height was subtracted
  twice and every label landed one line down, on its own icon row. A separate `yGdxTop` helper now
  serves text. The score value was silently mispositioned by the same bug. Still open from this entry:
  the five `icon-*` sprites exist in the atlas and `HudRenderer` does not use them; it is still
  text-only.
- ~~Ramming the boss does not repeatedly damage the player.~~ **Withdrawn on 25/08 after a play
  session.** Ramming damages the player normally. The "one hit then nothing" an agent saw under
  sustained overlap is `03-game-systems.md`'s decided rule — all damage taken grants temporary
  invulnerability — working exactly as specified. Kept here as a record that a guess reached this
  document as an open unknown; check `docs/planning/` before writing down that the game fails to do
  something.
- **`game` now imports `core.domain.event`** (`EnemyDestroyed`, `GameEvent`), the first such import in
  the project, and the mechanical boundary grep in `.claude/agent-memory/game-presentation/` will flag
  it. It is not a violation: `core.port.GameEventSink` already imports `GameEvent`, and `GameEvent`'s
  own javadoc says the events "are the whole contract" towards presentation. The event package is a
  boundary package that happens to be filed under `domain`. If that keeps causing false alarms, move
  it to `core.port` rather than weakening the check.
- **`ship-bank`, `ship-tilt`, `ship-hit`, the thrust and muzzle effects, `module-satellite`,
  `structure-tower` and the five `icon-*` glyphs** are in the atlas and referenced by nothing. Art
  waiting for a system.

Two things phase 09 must check, both surfaced by these reviews and neither a problem today:

- `game/src/main/java/.../tools/audio/{Wav,Synth,GenerateAudio}.java` use `java.nio.file` and sit
  inside the module TeaVM compiles. Harmless only while `web/build.gradle.kts`'s `gdxTeaVM {}` block
  stays commented out. When it comes back, exclude `tools.audio` from the TeaVM source set or move it
  to its own source set.
- `docs/design/07-skin.md` commits the Skin integration to `Skin(FileHandle, TextureAtlas)` /
  `skin.load(...)`, which is libGDX's reflective `Json` class underneath — the exact mechanism
  `CLAUDE.md`'s web-pitfalls section warns about. scene2d has no non-reflective Skin loader, so this
  is likely the project's one legitimate exception, but the TeaVM reflection declarations for every
  style class named in `skin.json` must exist before that call ships.

## What is settled

Platform, architecture and MVP scope are decided, and the technical parts were **measured, not assumed** — see `docs/planning/11-technical-prototype-results.md`.

The short version lives in `CLAUDE.md`: invariants, web-target pitfalls, conventions, commit rules and the agent roster. The long version is in `docs/planning/`.

`docs/planning/08-decisions-and-open-items.md` is the one to check before deciding anything: it separates confirmed decisions from provisional ones and from what is still open.

## What comes next

The master plan is in `docs/plan/`, starting from [the overview](plan/00-overview.md). Each phase has
its own folder with a `plan.md` and a `status.md`.

| Milestone | Date |
|---|---|
| MVP — level 1 playable in the browser with its own art | 26/08/2026 |
| Finish — polish, game feel, final audio | 09/09/2026 |

The order from here is the two numbered items in "What is left": balance by playing, then phase 09.
Everything the three merged branches left unwired is closed.

### Phase state

| # | Phase | State |
|---|---|---|
| 01 | Foundations | **done** — merged in #2 |
| 02 | Core mechanics | **done** — merged in #10 |
| 03 | First playable | **done** — merged in #14 |
| 04 | Content pipeline | **done** — core in #16, loader in #14 |
| 05 | Game systems | **done** — merged in #22 |
| 06 | Presentation | **done** — direction #8, integration #26, sprites packed into `assets/atlas/` and the real bitmap fonts into `assets/fonts/` on 25/08 |
| 07 | Boss | **merged** — #29, with level 1 and enemy fire. Four archetypes shoot; the boss fans 6 projectiles a volley |
| 08 | Audio and polish | **merged** — #31, with both dead cues wired on 25/08. Nobody has heard it yet; phase 09's browser pass is the first listen |
| 09 | Web, CI and release | not started |

## Open items that do not block

None of these stop step one. Each is due when its moment arrives:

- the intensity-curve tooling — the level's length and climax are now decided, the tool to shape the curve is not;
- pointer capture for relative mouse, and browsers other than Chrome.

## Traps that already cost hours

All of these are in `CLAUDE.md`, but they are worth repeating because they are silent:

- `assets/startup-logo.png` is mandatory or the web build crashes on preload, with an error that never mentions the logo.
- Headless Chrome cannot validate the web runtime. It fails under SwiftShader even when a real browser works.
- `ExecutorService`, `CompletableFuture` and `ReentrantLock` do not exist in TeaVM: they break the build. The core is single-threaded by decision, and it was measured that concurrency would buy nothing.
- Breaking determinism does not fail loudly. It silently invalidates every replay.

## How work is organised

One issue per task, one branch per issue, merged through a pull request that closes it. Commits go through the `/git-commit` skill. `reviewer` accepts or rejects a phase against the acceptance criteria in its `plan.md`.

Repository: <https://github.com/LuchoC-Dev/little-spaceship> — private. Several parallel Claude sessions each get their own git worktree.

Six agents own different modules, so their boundaries come from the architecture rather than from goodwill. Each keeps its own memory under `.claude/agent-memory/`, and that memory records only what the agent learned that is not already written in `docs/`.
