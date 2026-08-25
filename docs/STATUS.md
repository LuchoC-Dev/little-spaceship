# Project status

Last updated: 25/08/2026.

Read this first if you are picking the project up. It says where things stand and what comes next; `CLAUDE.md` says how to work here.

## Where we are

**`main` is now the whole picture.** The three branches that held the MVP were reviewed and merged on
25/08/2026, in the order that had been decided: sprites (#30), audio (#31), boss (#29). All three
were accepted with no blocking findings. `./gradlew build` is green and `core` carries 277 tests.

What that leaves is not "polish". Two capabilities landed **built but connected to nothing**, and one
deliverable turns out not to be what its phase status implied. Read "What is left" before planning
anything — the remaining work is larger than the phase table suggests.

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

1. **Balance.** Level 1's pacing was written before enemies could shoot, and enemy health is still
   placeholder — a heavy carrier dies in about 1.2 s against the 32 s its stretch reserves. Decided:
   tuned by playing, not by arithmetic. No longer blocked; enemies now shoot.
2. **Phase 09** — web, CI and deploy. Not started, and it is what turns this into a link a stranger
   can open. `assets/startup-logo.png` is already in place; `web/` is an empty skeleton by decision,
   and the TeaVM build was verified working once, in phase 03, before being reverted as out of scope.

### Known and deliberately left open

None of these block the MVP. Each is a real thing someone chose not to fix under a deadline.

- **Art and ECS disagree on five pickup ids** (`pickup-weapon` vs `pickup-weapon-upgrade`, and four
  more), bridged by alias regions in `sprites.atlas` — same pixels, second name — because renaming
  either side crosses an agent boundary. Settle it by picking a side, not by adding a third alias.
- **`boss-shot` has no authored art.** It was invented in phase 07, after the sprite pass, and is
  aliased to `shot-e-small`. It reads correctly; it is not its own sprite.
- **The HUD's icon squares overlap their text labels** — LIVES, BOMBS and POWER are drawn over by
  their own icons. Predates the atlas. The five `icon-*` sprites exist in the atlas and `HudRenderer`
  does not use them; it is still text-only.
- **Ramming the boss does not repeatedly damage the player** — one `IMPACT` on first contact, then
  nothing across continued overlap. Found while trying to force a fast defeat for verification. It is
  not known whether this is intended; nobody has looked at it as a rule.
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
| 06 | Presentation | **done** — direction #8, integration #26, sprites drawn in #30 and packed into `assets/atlas/` on 25/08 |
| 07 | Boss | **merged** — #29, with level 1 and the machinery for enemy fire. No content uses the weapon yet |
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
