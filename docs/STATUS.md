# Project status

Last updated: 20/08/2026.

Read this first if you are picking the project up. It says where things stand and what comes next; `CLAUDE.md` says how to work here.

## Where we are

**The game is playable. Phases 01 to 05 are merged: a ship that shoots, enemies that take damage and die, power-ups, an attachment, a bomb and a score. The art is still placeholder, and the level is a fixture rather than the designed one — there is no boss, and no strong encounter.**

What exists in the repository:

- `docs/planning/` — fourteen documents covering vision, MVP spec, game systems, campaign, progression, balance values, architecture and the agent workflow.
- `docs/sources/` — the verbatim ChatGPT transcript the planning came from. Spanish on purpose; it is evidence.
- `spikes/web-viability/` — a throwaway prototype that validated the platform. Not the base of the game. It can be deleted once it stops being useful.
- `docs/design/` — the visual direction: the closed `ls32` palette, sprite sizes in pixels per archetype, bitmap typography, HUD layout, legibility rules, and pixel-exact mocks. Synchronisation point 1, settled.
- `.claude/agents/` — five agent definitions with project-scoped persistent memory.
- `core/` — the ECS, the fixed-step loop, `Rng`, `InputFrame` and the ports, plus motion, collision, the defensive chain, cleanup, the content contracts, the spawner, and phase 05's systems: weapons and their upgrades, power-ups, the attachment, the bomb, `Health` and scoring. 236 tests, no libGDX on its classpath.
- `game/` and `desktop/` — the LWJGL3 launcher, the input adapter that sums keyboard and relative mouse, an allocation-free renderer reading through `WorldView`, integer-scaled viewport, placeholder art at the sizes the visual direction fixed, and the JSON content loader. No tests yet. `web/` is still an empty skeleton; phase 09 owns it.
- `assets/data/` — the content as JSON: six archetypes, four trajectories, three formations and one timeline.

## What is settled

Platform, architecture and MVP scope are decided, and the technical parts were **measured, not assumed** — see `docs/planning/11-technical-prototype-results.md`.

The short version lives in `CLAUDE.md`: invariants, web-target pitfalls, conventions, commit rules and the agent roster. The long version is in `docs/planning/`.

`docs/planning/08-decisions-and-open-items.md` is the one to check before deciding anything: it separates confirmed decisions from provisional ones and from what is still open.

## What comes next

The master plan is in `docs/plan/`, starting from [the overview](plan/00-overview.md). Each phase has its own folder with a `plan.md` and a `status.md`.

| Milestone | Date |
|---|---|
| MVP — level 1 playable in the browser with its own art | 26/08/2026 |
| Finish — polish, game feel, final audio | 09/09/2026 |

Two lanes run in parallel: **code** and **art**. The art lane starts on day one and never waits for the code, which is the only reason the week is feasible.

Next up, on the code lane, is [phase 07, the boss](plan/07-boss/plan.md), which depends on 05 and also owes two decisions the level cannot be written without: what the strong encounter is, and the boss itself. On the art lane, sprite production is what remains of phase 06 — the direction is written, nothing has been drawn, and it is now the longest pole.

Three things the running build hides, all tracked and none blocking: `game` has no test suite at all ([#19](https://github.com/LuchoC-Dev/little-spaceship/issues/19)); `assets/data/level-01.json` is the pipeline fixture rather than the fourteen-beat level the campaign document describes ([#20](https://github.com/LuchoC-Dev/little-spaceship/issues/20)); and a designed drop is handed to every slot of its formation, so a guaranteed upgrade on a three-enemy wave gives three ([#23](https://github.com/LuchoC-Dev/little-spaceship/issues/23)).

### Phase state

| # | Phase | State |
|---|---|---|
| 01 | Foundations | **done** — merged in #2 |
| 02 | Core mechanics | **done** — merged in #10 |
| 03 | First playable | **done** — merged in #14 |
| 04 | Content pipeline | **done** — core in #16, loader in #14 |
| 05 | Game systems | **done** — merged in #22 |
| 06 | Presentation | **visual direction done** — merged in #8; art production and integration pending |
| 07 | Boss | not started |
| 08 | Audio and polish | not started |
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

Five agents own different modules, so their boundaries come from the architecture rather than from goodwill. Each keeps its own memory under `.claude/agent-memory/`, and that memory records only what the agent learned that is not already written in `docs/`.
