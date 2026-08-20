# Project status

Last updated: 19/08/2026.

Read this first if you are picking the project up. It says where things stand and what comes next; `CLAUDE.md` says how to work here.

## Where we are

**Planning is finished. Phase 01 is merged: the simulation core exists, with no game visible yet.**

What exists in the repository:

- `docs/planning/` — fourteen documents covering vision, MVP spec, game systems, campaign, progression, balance values, architecture and the agent workflow.
- `docs/sources/` — the verbatim ChatGPT transcript the planning came from. Spanish on purpose; it is evidence.
- `spikes/web-viability/` — a throwaway prototype that validated the platform. Not the base of the game. It can be deleted once it stops being useful.
- `.claude/agents/` — five agent definitions with project-scoped persistent memory.
- `core/` — the ECS, the fixed-step loop, `Rng`, `InputFrame` and the ports, with 91 tests and no libGDX on its classpath. `game`, `desktop` and `web` exist as empty module skeletons.

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

Next up: [phase 02, core mechanics](plan/02-core-mechanics/plan.md), on the code lane. The art lane can start at any time and blocks phase 03.

### Phase state

| # | Phase | State |
|---|---|---|
| 01 | Foundations | **done** — merged in #2 |
| 02 | Core mechanics | not started |
| 03 | First playable | not started |
| 04 | Content pipeline | not started |
| 05 | Game systems | not started |
| 06 | Presentation | not started |
| 07 | Boss | not started |
| 08 | Audio and polish | not started |
| 09 | Web, CI and release | not started |

## Open items that do not block

None of these stop step one. Each is due when its moment arrives:

- the level 1 boss: phases, patterns and look;
- what exactly the "strong encounter" is — the fight that hands over the attachment;
- the level's target duration and the intensity-curve tooling;
- pointer capture for relative mouse, and browsers other than Chrome;
- sprite sizes, which the visual direction has to settle before real art is drawn.

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
