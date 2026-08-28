---
name: spawnsystem-wave-migration
description: Migrating SpawnSystem from a flat WaveTimeline cursor to WavePlacement/WaveDefinition (#112) — the first-tick scheduling bug this produced, and how to trust an inherited uncommitted diff.
metadata:
  type: project
---

Phase 11b, task 4 (#112, PR #121 against `phase/11b-wave-system`). Picked up from a previous
`core-domain` run killed mid-work by a spend limit, with a large uncommitted diff already in the
worktree. `SpawnSystem` now walks `ContentSource.placements(levelId)` — an ordered list of
`WavePlacement`, each resolving a `WaveDefinition` via `ContentSource.wave(String)` — instead of a
single cursor over `WaveTimeline.events()`. See [[project_wave-content-contract]] for the content
types this builds on.

**A clamp meant for one case silently broke a different, earlier case — and only running the
inherited tests unmodified caught it.** `scheduleNext(previousEndTime)` clamps a placement's start
forward to `levelTime` so a negative offset following a late-detected `Cleared` wave never schedules
into the past — correct and necessary. But the inherited `update()` called `levelTime += step` *before*
scheduling the level's very first placement, so that same clamp also pushed the first wave's start
from 0 to one step, delaying every one of its spawn events by exactly one tick. Nothing in the new
code's own logic looked wrong in isolation; what surfaced it was running the *old*, unmodified
`SpawnSystemTest` (before rewriting it) against the *new* `SpawnSystem` — 16 of 24 tests failed with
entities not spawning at all. Fixed by scheduling the first placement before the step is added, so it
starts at level time zero, matching what the old flat-cursor system did. **Lesson: when inheriting
unfinished work whose own tests haven't been migrated yet, run the untouched old test file against the
new production code first, before rewriting the tests — it is a free regression check that a rewritten
test file can no longer give you, since a bug and its test can both be wrong the same way.**

**A type signature change (`WaveOrigin.waveId` from `int` to `String`) breaks compilation at every
call site, not just the ones the task's own file list names.** `SpawnerSystemTest` and `WorldTest` —
neither mentioned in the task brief's list of files to touch — both constructed `new WaveOrigin(1)`
directly and failed to compile once the field became a `String`. `./gradlew :core:compileTestJava`
(not just the target test class) is what caught these; a partial test run would have stayed green while
the whole module failed to build.

**`WaveTimeline`'s "superseded, not yet retired" state (see [[project_wave-content-contract]]) held
past this task too, for the same reason.** `game`'s `JsonContentSource` still populates it for
`level-01.json`, not yet migrated (issue #114) — `ContentSource.timeline(String)` stayed a `default`
throwing method rather than being deleted, since `core-domain` may not edit `game`. Checked this held
by grepping `game/src` for `WaveOrigin`/`SimpleWaveTimeline` usage and running a whole-repo
`./gradlew compileJava compileTestJava` before opening the PR — clean.

**A conditional final step in an issue comment needs the actual PR state checked, not just a local
branch grep.** #112's issue comment said to flip `ContentSource.wave(String)` to abstract only once
#113 is merged into `phase/11b-wave-system`. `git log phase/11b-wave-system | grep 113` found nothing
(no commit trailer names it), but the reliable check was `gh pr list --base phase/11b-wave-system`,
which showed #113's implementation (PR #120) still `DRAFT`, plus confirming `JsonContentSource` has no
`wave(String)` override yet. Left the default in place, per the comment's own instruction not to block
on it.
