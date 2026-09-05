# Phase 11k — Level 1 rebuilt on a vocabulary written for it · status

**State:** **open.** Branch `phase/11k-level-one-rebuilt` cut from `dev` at `9edd44c` on 05/09/2026,
after [#308](https://github.com/LuchoC-Dev/little-spaceship/pull/308) recorded 11j as done and on
`dev`. The plan is written; no task has started.
**Updated:** 05/09/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the
phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by
whoever did that task on its own branch.

## Why this phase exists

**Three phases built for it and none of them used it.** 11h built a test mode so a wave can be looked
at without playing to it, 11i made a movement shape an ordered list of bounded segments with waits,
repeats and mirroring, and 11j let a path be written where it happens and run faster without changing
size. Each ended with level 1 deliberately untouched, and the project owner asked at the end of 11j
why nothing new appears while playing. Nothing does, by design. This is where it does.

## What the project owner decided before the plan was written

Asked on 05/09/2026, before anything was opened, and answered in one pass. The plan's "What the
project owner decided" section carries them in full; in short: the whole level changes, the beats may
move, "simple movements" is a rule **per archetype** (two or three that resemble each other, mirrors
free), **the vocabulary is written new** — the seven from 11c change and the twelve from 11i/11j are
test material — ~2.5 minutes is the right length, the boss keeps its difficulty but gains movement,
#255 is in scope, the TESTS list gets discovered, [#300](https://github.com/LuchoC-Dev/little-spaceship/issues/300)
is a first task, and **the play session gates the phase**.

## What the plan found before any task started

**`tools/build-level-docs.js` cannot read a `path` trajectory.** Verified on 05/09/2026:
`grep -n "path\|segments\|waypoints\|mirrorOf\|speedOf" tools/build-level-docs.js` returns only
Node's own `path` module. `sweptExtent` (`:197`) reads `traj.vx`, `screenTime` (`:241`) reads
`traj.vy`, and the shapes table (`:618`) prints `vx`, `vy` and `ay` — a `path` entry has none of them
at the top level. The geometry runs only inside `buildLevel`, so only for `level-01.json`, which
places no `path` today; `waves.md` never touches geometry, which is why the ten test scenarios from
11i and 11j pass through it unnoticed.

**The blindness is real and dormant, and this phase is what wakes it.** It became task 1, ahead of
[#300](https://github.com/LuchoC-Dev/little-spaceship/issues/300), because both live in the same file
and because every content task after them depends on the document telling the truth.

## Tasks

Eight, in three groups. Issues are opened as each group starts rather than all at once.

| # | Task | Agent | Issue | PR |
|---|---|---|---|---|
| 1 | The generator understands a `path` trajectory | `game-presentation` | — | — |
| 2 | An absolute path is checked against its `atX` | `game-presentation` | [#300](https://github.com/LuchoC-Dev/little-spaceship/issues/300) | — |
| 3 | The TESTS list discovered from `assets/data/test-*.json` | `game-presentation` | — | — |
| 4 | Content can place a standalone pickup | `core-domain` | [#255](https://github.com/LuchoC-Dev/little-spaceship/issues/255) | — |
| 5 | The trajectory vocabulary level 1 is rebuilt from | `level-designer` | — | — |
| 6 | Level 1 rebuilt on it | `level-designer` | — | — |
| 7 | The boss moves | `core-domain` | — | — |
| 8 | The play session, and the tuning that follows | the project owner | — | — |

## Open

Everything. The phase has just been opened.
