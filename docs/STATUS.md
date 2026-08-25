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

The merges resolved the cross-branch dependencies that were the reason for merging together. They did
**not** finish the features, and the gap is easy to miss because every phase reads as done:

- **Nothing consumes `EnemyDestroyed`.** `CleanupSystem` emits it; no listener exists in `game`. The
  explosion sound is generated, loaded and silent.
- **Nothing reads `bossStatus()` for audio.** `MusicTrack.BOSS` exists and never plays.
- **No archetype declares a `"weapon"` component.** `EnemyWeaponSystem` and the `"weapon"` factory
  both exist, but `assets/data/enemies.json` gives the weapon to nobody, so **enemies still do not
  shoot in the shipped level**.
- **The sprites are not in the game.** This is the big one. `#30`'s diff touches only `docs/design/`.
  The 34 character sprites — archetypes, ship, boss parts, shots, pickups, HUD icons — live as pixel
  art in `docs/design/mockups/src/01-sprites.js`, and the PNGs that script renders are gitignored
  previews. `assets/` holds JSON and `startup-logo.png`, nothing else. `game` still draws through
  `PlaceholderAtlas`, which builds coloured rectangles into a `Pixmap` at runtime. Phase 06 delivered
  the art direction; it never delivered game-loadable art.

## What is left

Ordered by what blocks what.

1. **Give `enemy-shooter` a `"weapon"` in `assets/data/enemies.json`.** Small, `level-designer`'s
   lane, and it gates the balance pass: there is nothing to tune while nothing shoots.
2. **Export the sprites to a real atlas under `assets/` and make the renderer consume it** instead of
   `PlaceholderAtlas`. Unplanned, `game-presentation`'s lane, and the largest single item left. It is
   the difference between the MVP being playable with its own art and playable with rectangles.
3. **Wire the two dead audio cues** — an `EnemyDestroyed` listener and a `bossStatus().present()`
   edge. Both hooks are written and documented in `AudioSystem`'s javadoc.
4. **Balance.** Level 1's pacing was written before enemies could shoot, and enemy health is still
   placeholder — a heavy carrier dies in about 1.2 s against the 32 s its stretch reserves. Decided:
   tuned by playing, not by arithmetic. Blocked on item 1.
5. **Phase 09** — web, CI and deploy. Not started, and it is what turns this into a link a stranger
   can open. `assets/startup-logo.png` is already in place; `web/` is an empty skeleton by decision,
   and the TeaVM build was verified working once, in phase 03, before being reverted as out of scope.

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

The order from here is the five numbered items in "What is left". The three branches are merged; what
replaced them is smaller per item but there are more items than the phase table implies.

### Phase state

| # | Phase | State |
|---|---|---|
| 01 | Foundations | **done** — merged in #2 |
| 02 | Core mechanics | **done** — merged in #10 |
| 03 | First playable | **done** — merged in #14 |
| 04 | Content pipeline | **done** — core in #16, loader in #14 |
| 05 | Game systems | **done** — merged in #22 |
| 06 | Presentation | **art direction done** — #8, #26, sprites drawn in #30. The sprites are not yet game-loadable art; see "What is left", item 2 |
| 07 | Boss | **merged** — #29, with level 1 and the machinery for enemy fire. No content uses the weapon yet |
| 08 | Audio and polish | **merged** — #31. Two cues built and unwired; see "What is left", item 3 |
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
