# Phase 11h — A test mode, so a wave can be looked at without playing to it · status

**State:** **open.** Branch `phase/11h-test-mode`, created from `dev` on 03/09/2026. Three tasks; nothing merged yet.
**Updated:** 03/09/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch.

## Why this phase exists

The project owner decided on 03/09/2026 to keep going on level 1 and give it **more depth**, and defined that as **movement and grouping** — how enemies enter, move and combine. Three things were ruled out in the same conversation: the boss moves as little as possible, no new enemy archetypes, and obstacles wait until the story and the final background exist.

**The tool comes first, and that was the owner's call.** Every iteration of movement work has to be judged by playing — the rule this project has now decided three times — and today judging one wave costs playing the level up to it. The boss costs 134.5 s. A phase that makes that cost five seconds pays for itself inside the next phase.

## What it is

A build flavour, not a hidden feature: `./gradlew :desktop:run -Ptests` carries a fourth main-menu entry, TESTS, listing named scenarios. The ordinary build does not have it and neither does anything that reaches `main`.

**The design turned out cheaper than expected, and the finding is that nothing needed inventing.** A level file is `{ "boss", "events", "waves" }` and nothing more — `game/adapter/content/JsonContentSource.java:349` — so a file naming one wave placement already *is* "start at that wave", and a file with a boss block already *is* "start at the boss". Which level runs is one field, `game/LittleSpaceshipGame.java:42`, whose own javadoc predicted this: *"the day a level-select flow exists, this field is the one place that changes"*. Both halves of the flavour mechanism also already exist in the repository — the `tools` source set in `game/build.gradle.kts`, kept out of `main` because TeaVM compiles everything reachable from it, and the `-Prelease` property in `web/build.gradle.kts`.

## Tasks

| Task | Issue | Owner | PR |
|---|---|---|---|
| 1 | the test flavour and the TESTS menu | `game-presentation` | — |
| 2 | the four scenarios | `level-designer` | — |
| 3 | the record in `08-decisions-and-open-items.md` | coordinator | — |

## What is open

Everything. The phase has just opened.
