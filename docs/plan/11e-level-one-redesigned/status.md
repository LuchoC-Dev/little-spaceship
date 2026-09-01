# Phase 11e — Level 1 redesigned, balance and the boss · status

**State:** in progress — opened on 31/08/2026 on `phase/11e-level-one-redesigned`
**Updated:** 31/08/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch. Read those for what each one did; this is what the phase amounts to.

## Done

Nothing yet. The phase opened on 31/08/2026, once 11d reached `dev` in [#195](https://github.com/LuchoC-Dev/little-spaceship/pull/195).

**Task 7 is answerable now and is recorded here rather than being re-derived**: [#23](https://github.com/LuchoC-Dev/little-spaceship/issues/23) — a designed drop attached to every slot of its formation — is **closed, and the rule is tested**. `core/domain/system/SpawnSystem.java` attaches the `Drop` only when `i == event.dropSlot()`, and `SpawnSystemTest.designedDropAttachesToExactlyOneSlot` asserts `world.drops().size() == 1` and checks the surviving entity's x against the named slot's own offset. `dropSlotOutsideFormationFails` covers the typo. Beat 11, the difficult encounter that hands over the attachment, rests on this and it holds.

## In progress

Seven tasks from [`plan.md`](plan.md). The running order and who does each is in that file under "The running order".

## Blocked

**Nothing is blocked, but the phase cannot *close* without a play session, and that is structural rather than a scheduling problem.**

The acceptance criteria say it in as many words: *"The verdict comes from playing. A session played, what it felt like, what changed as a result. A balance change justified by arithmetic alone does not satisfy this criterion."* That rule was decided twice, on 22/08 and 25/08, and `docs/STATUS.md` calls playing *"the only source this project trusts for balance"*.

**No agent can satisfy it.** All four watch-items task 5 inherits from `docs/STATUS.md` are questions about how something reads in play:

1. does `enemy-basic` read as firing less often than `enemy-shooter`, or does it just die too fast to tell?
2. does the boss at `patternCooldown 0.7` feel like a boss?
3. does `enemy-rush`'s single likely shot per pass read as "shoots little" rather than "does not shoot"?
4. is `enemy-light`'s 130 u/s projectile dodgeable?

**How the phase is therefore run — decided by the project owner on 31/08/2026:** the coordinator and `level-designer` build a *candidate* — the fourteen waves, the stat changes, the boss change, the regenerated document — and hand the project owner a play session with those four questions plus the length question. The phase tunes from the answers and only then closes. **The candidate is not the deliverable and must not be merged to `dev` as though it were**: an unplayed batch of numbers reaching the published site is exactly what the four-level branch regime exists to prevent.

## Decisions taken while implementing

Record here anything decided that the plan did not specify, and why. If it changes a game rule, it also belongs in `docs/planning/08-decisions-and-open-items.md`.

## Notes for whoever comes next

**Two warnings this phase inherits, both from earlier phases and both concrete:**

- From 11c, and it constrains where a wave may place a veer: **the veers must spawn on the side they veer away from** — `veer-left` at `atX >= 0.75`, `veer-right` at `atX <= 0.25` — or the shape happens off screen. `docs/plan/11c-movement-shapes/shape-catalogue.md` also names the eight shapes it refused and why, and the rule that **every shape must leave the playfield unattended in finite time**, or a `cleared` wave deadlocks behind it.
- From 11b: `LifetimeSystem`'s safety box sits 128 units past every playfield edge, sized against today's formations, and a movement shape that leaves the playfield and re-enters is exactly what that box must not eat ([#117](https://github.com/LuchoC-Dev/little-spaceship/issues/117)).

**Two spawns in the level as it stands are already flagged by the generated document and were left for this phase.** `l1-carrier-pair` and `l1-finale-a` each place `enemy-light` in `diagonal-mirror` on `swoop` far enough left that 53% and 63% of the swept width sits outside `0 .. 208`. They read in range at the spawn instant and are not. See the Checks section of `docs/levels/level-01.md`.

**`docs/levels/level-01.md` is generated and CI fails if it drifts.** Every content change in this phase must be followed by `node tools/build-level-docs.js` and the result committed. `docs/levels/waves.md` regenerates with it.

**This phase is the first honest test of 11d's document.** Its `level-designer` is the first reader who did not help build it. Whatever the document fails to say, say so — that is a finding, and it goes back to `docs/plan/11d-per-level-document/document-contract.md`, which is where 11d put the last round of them.
