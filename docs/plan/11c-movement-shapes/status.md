# Phase 11c — Movement as a described thing · status

**State:** in progress — opened 29/08/2026 on `phase/11c-movement-shapes`
**Updated:** 29/08/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever does that task on their own branch, before review. That split is what phase 10d built: in 11b every parallel agent edited one shared `status.md`, which produced a merge conflict, a forbidden force-push to escape it, and two silent gaps in the record. `tools/pre-pr-check` now fails a branch that does work and writes no fragment, and rejects a fragment filed in another phase's directory.

## The tasks

Five, one issue each. The plan's task 5 is [#86](https://github.com/LuchoC-Dev/little-spaceship/issues/86) itself — the handover from 10c that this whole phase exists to close.

| # | Task | Owner |
|---|---|---|
| [#161](https://github.com/LuchoC-Dev/little-spaceship/issues/161) | Per-entity movement state: a path is a function of the entity's own elapsed time | `core-domain` |
| [#162](https://github.com/LuchoC-Dev/little-spaceship/issues/162) | Decide which movement shapes exist, and refuse the rest | `level-designer` |
| [#163](https://github.com/LuchoC-Dev/little-spaceship/issues/163) | A movement shape is named content, loaded from `assets/data` | `core-domain` + `level-designer` |
| [#164](https://github.com/LuchoC-Dev/little-spaceship/issues/164) | `SpawnEvent` carries an optional shape id, the archetype supplies the default | `core-domain` |
| [#86](https://github.com/LuchoC-Dev/little-spaceship/issues/86) | Close the handover, and correct every javadoc this phase falsifies | `core-domain` |

## Done

Nothing yet.

## In progress

Nothing yet. The phase branch exists and the issues are open.

## Blocked

Nothing. 11a merged in [#109](https://github.com/LuchoC-Dev/little-spaceship/pull/109) and 11b in [#131](https://github.com/LuchoC-Dev/little-spaceship/pull/131), so the wave a shape gets placed in exists.

## Decisions taken while implementing

Record here anything decided that the plan did not specify, and why. If it changes a game rule, it also belongs in `docs/planning/08-decisions-and-open-items.md`.

**Already decided before the phase opened**, by the project owner on 27/08/2026: the shape is chosen in the spawn event, with the archetype supplying the default. **Which shapes exist is not decided** — that is #162, and it is the one place in this phase where invariant 6 does the most work.

## Notes for whoever comes next

This is the first phase to run agents in parallel over `core/` and `assets/data/` at once, which is the scenario 10d built its tools for. Every worker gets its own worktree, created by the coordinator, and agent memory is written in the main checkout — `tools/agent-memory-path <agent>` prints the one correct directory.
