# Phase 11b — The wave system · status

**State:** not started
**Updated:** 27/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

- Task 5 / #87 — `JsonContentSource` no longer hardcodes `LEVEL_ID = "level-01"`. The constructor now
  takes a `levelId` parameter and reads `<levelId>.json` from the data directory. Per review, the id
  itself lives on `LittleSpaceshipGame` (`levelId()`, exposed the same way `seed()` is) rather than on
  `PlayScreen` — both `PlayScreen` and `ShipSelectScreen` already read session-wide state off that
  object, so this avoids coupling one screen to a sibling's constant. No directory listing was added —
  `FileHandle.list()` has no answer under TeaVM's asset packaging, and there is still exactly one level
  file, so eager single-level loading stays. See `feat/level-by-id` PR against this branch.

## In progress

Nothing yet.

## Blocked

Waiting on 11a. Every task here is a behaviour change and 11a is the net.

## Decisions taken while implementing

Record here anything decided that the plan did not specify, and why. If it changes a game rule, it also belongs in `docs/planning/08-decisions-and-open-items.md`.

## Notes for whoever comes next

—
