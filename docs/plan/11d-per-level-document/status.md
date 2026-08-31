# Phase 11d — The per-level document · status

**State:** in progress — opened on 31/08/2026 on `phase/11d-per-level-document`
**Updated:** 31/08/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch. Read those for what each one did; this is what the phase amounts to.

## Done

Nothing yet. The phase opened on 31/08/2026, once 11c reached `dev` in [#174](https://github.com/LuchoC-Dev/little-spaceship/pull/174) and the release [#176](https://github.com/LuchoC-Dev/little-spaceship/pull/176) levelled `main` with it.

## In progress

Five tasks from `plan.md` plus one defect the phase inherits. The running order, who does each and why, is in `plan.md` under "The running order" — it is a decision about how the phase runs, not a record of progress, which is why it lives there.

[#177](https://github.com/LuchoC-Dev/little-spaceship/issues/177) is the inherited defect: `pr-check` fails every `dev` → `main` release, and there is no sanctioned branch to fix it on because rule 1 of that same workflow accepts only `phase/*` or `docs/*` against `dev`. A phase branch is the sanctioned path, so it runs here, as a defect found while the phase runs rather than as a task from the plan.

## Blocked

Nothing. The dependency this file recorded on 27/08/2026 — 11b and 11c, because a generator cannot be written against a format that is still changing — is satisfied: `assets/data/waves.json` and `assets/data/trajectories.json` both exist on `dev` and neither format is open.

## Decisions taken while implementing

- **The generator is a Node script in `tools/`, not a Gradle task.** Decided at the phase's opening, and the reasoning is in `plan.md` under "The running order". The short version: the project's only JSON parser is `game/adapter/content/JsonContentSource.java`, which drags libGDX in and belongs to `game-presentation`, whereas `tools/` belongs to no agent and Node already runs the two generators this phase's decision section cites as precedent — `docs/design/atlas/build-atlas.js` and `docs/design/fonts/build-fnt.js`.

Record here anything else decided that the plan did not specify, and why. If it changes a game rule, it also belongs in `docs/planning/08-decisions-and-open-items.md`.

## Notes for whoever comes next

—
