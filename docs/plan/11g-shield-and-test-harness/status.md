# Phase 11g — The shield drop, and a test harness for `game` · status

**State:** in progress — opened on 01/09/2026 on `phase/11g-shield-and-test-harness`, branched from `dev` once 11f merged in [#232](https://github.com/LuchoC-Dev/little-spaceship/pull/232)
**Updated:** 01/09/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch.

## Done

Nothing yet. The phase opened on 01/09/2026, once the 11 group closed.

**It carries one write that belongs to 11f**: recording that phase as merged into `dev`, in its own `status.md` and in the phase table in `docs/STATUS.md`. Both writes go together or neither, and putting them here rather than in a separate pull request against `dev` keeps the project owner approving one pull request per phase — the same arrangement used when 11e's record was carried on the 11f branch in [#226](https://github.com/LuchoC-Dev/little-spaceship/pull/226).

## In progress

Two tasks, both from things the 11 group left behind.

| Task | Issue | Owner |
|---|---|---|
| 1 | [#230](https://github.com/LuchoC-Dev/little-spaceship/issues/230) — a `shield` drop in level 1 | `level-designer` |
| 2 | [#19](https://github.com/LuchoC-Dev/little-spaceship/issues/19), the unblocked half — a way to test `game` at all | `test-engineer`, with `game-presentation` |

**Neither was invented by a plan.** Task 1 exists because 11f wired `icon-shield` from art already sitting in the atlas, and level 1 then turned out to contain no `shield` drop at all — the icon is correct and unreachable, and nobody could have known until it was wired. Task 2 has been open since **phase 03**.

## Blocked

Nothing is blocked.

**But task 1 cannot close without a play session, and that is structural** — the same shape 11e and 11f both had. The verdict on a balance change comes from playing; `docs/STATUS.md` calls playing *"the only source this project trusts for balance"*, and the rule was decided on 22/08 and again on 25/08. An agent places the drop; the project owner confirms it falls, is collectable, and lights the icon.

## Decisions taken while implementing

**Why task 2 is here at all, since 11a scheduled #19 to the 12 group.** D5 of `docs/plan/11a-rule-asserting-tests/status.md` was amended after `reviewer` was asked to argue with it and did: #19 bundles two questions and **only one was ever blocked**. What to assert about `JsonContentSource`'s error messages depends on a format 11b was rewriting, and stays scheduled to the 12 group. **How to test anything depending on `FileHandle` without dragging LWJGL into the suite never depended on the format** — it is a harness design question, and #19's own text calls it *"the question to answer first"*.

D5 ended with *"Put to the project owner, because it adds scope to a group that is not planned yet."* **That was never actually put to them until 01/09/2026**, when it was, and they chose to answer the harness question soon rather than carry it into an unplanned group. This phase is that answer.

## Notes for whoever comes next

**Phase 03 already solved the harness question and threw the solution away.** Two programs verified input summing and a non-empty world using JDK dynamic proxies for `Gdx.input`/`Gdx.graphics`; `docs/plan/03-first-playable/status.md` transcribes their output. **They were never committed.** The technique was not the defect — the defect is that the evidence cannot be reproduced by anyone. Look there first, and this time commit it.

**Level 1 was signed off by the project owner on 01/09/2026**, after two play sessions, as correct with nothing further to modify. Task 1 adds one pickup to a 134.5 s level that already carries four. **That is the only balance change this phase is allowed to make**, and where it goes has to be argued against the intensity curve in `docs/levels/level-01.md`, not chosen for convenience.

**`docs/levels/level-01.md` is generated and CI fails if it drifts.** Any content change is followed by `node tools/build-level-docs.js` and the result committed.

**An agent may launch the game only to confirm it starts, never to play it.** Added to [`../how-to-run-a-phase.md`](../how-to-run-a-phase.md) by the project owner during 11f, when an agent began driving the game to see the shield — the same shield this phase makes reachable.
