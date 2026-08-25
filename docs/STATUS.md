# Project status

Last updated: 20/08/2026.

Read this first if you are picking the project up. It says where things stand and what comes next; `CLAUDE.md` says how to work here.

## Where we are

**The game can be finished, but not from `main`.** Phases 01 to 06 are merged: a ship, enemies, the
defensive chain, power-ups, a bomb, a score, the HUD and six screens. The boss, the real level, the
sprites and the audio are all built and all sitting in three unmerged branches — a full run from menu
to victory has been played on `feat/boss`, scoring 8700.

**Read "Work in flight" below before doing anything else.** `main` alone is not the picture.

What exists in the repository:

- `docs/planning/` — fourteen documents covering vision, MVP spec, game systems, campaign, progression, balance values, architecture and the agent workflow.
- `docs/sources/` — the verbatim ChatGPT transcript the planning came from. Spanish on purpose; it is evidence.
- `spikes/web-viability/` — a throwaway prototype that validated the platform. Not the base of the game. It can be deleted once it stops being useful.
- `docs/design/` — the visual direction: the closed `ls32` palette, sprite sizes in pixels per archetype, bitmap typography, HUD layout, legibility rules, and pixel-exact mocks. Synchronisation point 1, settled.
- `.claude/agents/` — six agent definitions with project-scoped persistent memory.
- `core/` — the ECS, the fixed-step loop, `Rng`, `InputFrame` and the ports, plus motion, collision, the defensive chain, cleanup, the content contracts, the spawner, and phase 05's systems: weapons and their upgrades, power-ups, the attachment, the bomb, `Health` and scoring. 236 tests, no libGDX on its classpath.
- `game/` and `desktop/` — the LWJGL3 launcher, the input adapter that sums keyboard and relative mouse, an allocation-free renderer reading through `WorldView`, integer-scaled viewport, placeholder art at the sizes the visual direction fixed, and the JSON content loader. No tests yet. `web/` is still an empty skeleton; phase 09 owns it.
- `assets/data/` — the content as JSON: six archetypes, four trajectories, three formations and one timeline.

## Work in flight

Three branches, none merged, each with a draft pull request. They are the MVP.

| Branch | Worktree | PR | Holds |
|---|---|---|---|
| `feat/boss` | `../little-spaceship-boss` | [#29](https://github.com/LuchoC-Dev/little-spaceship/pull/29) | phase 07's boss, the carrier's spawner, level 1 in full, `game`'s wiring for all of it, and enemy fire |
| `feat/sprite-production` | `../little-spaceship-visual` | [#30](https://github.com/LuchoC-Dev/little-spaceship/pull/30) | every sprite, both bitmap fonts, the explosions and the Skin |
| `feat/audio` | `../little-spaceship-audio` | [#31](https://github.com/LuchoC-Dev/little-spaceship/pull/31) | the procedural WAV generator, the audio runtime and the volume sliders |

**None has been reviewed.** The decision was to review and merge all three together, in that order —
sprites first since nothing depends on it, then audio, then boss, which touches the most.

Two things to know before merging:

- **The last commit on `feat/boss` was not reviewed and was committed by the coordinator, not its
  author.** `feat(core): let enemies fire and record an enemy's death` was finished but uncommitted
  when a spend limit killed the agent mid-run. 277 tests were green at that point. It brings
  `EnemyWeaponSystem` and the first real `GameEvent`, `EnemyDestroyed`, which the audio lane needs for
  the explosion sound it has already built and wired to nothing.
- **`feat/audio` and `feat/sprite-production` predate the boss.** The audio lane's music-change hook
  and the atlas both expect things that only exist on `feat/boss`.

## What is left after that

- **Balance.** Level 1's pacing was written before enemies could shoot, and enemy health is still
  placeholder — a heavy carrier dies in about 1.2 s against the 32 s its stretch reserves. Decided:
  tuned by playing, not by arithmetic.
- **Phase 09** — web, CI and deploy. Not started, and it is what turns this into a link a stranger
  can open. `assets/startup-logo.png` is already in place; `web/` is an empty skeleton by decision,
  and the TeaVM build was verified working once, in phase 03, before being reverted as out of scope.

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

The order from here: review and merge the three branches above, tune the balance by playing, then
phase 09. Nothing else blocks the MVP.

### Phase state

| # | Phase | State |
|---|---|---|
| 01 | Foundations | **done** — merged in #2 |
| 02 | Core mechanics | **done** — merged in #10 |
| 03 | First playable | **done** — merged in #14 |
| 04 | Content pipeline | **done** — core in #16, loader in #14 |
| 05 | Game systems | **done** — merged in #22 |
| 06 | Presentation | **done** — direction #8, integration #26, sprites in #30 (unmerged) |
| 07 | Boss | **built, unmerged** — #29, with level 1 and enemy fire |
| 08 | Audio and polish | **built, unmerged** — #31 |
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
