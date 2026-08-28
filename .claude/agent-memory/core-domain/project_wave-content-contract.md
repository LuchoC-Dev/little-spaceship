---
name: wave-content-contract
description: Adding WaveDefinition/WaveEndCondition/ContentSource.wave(id) for #111 — why the new lookup is a default method, and why WaveTimeline was left untouched in code.
metadata:
  type: project
---

Phase 11b, task 3 (#111, PR #119 against `phase/11b-wave-system`). Added `WaveDefinition` (id, spawns,
endCondition, offsetSeconds), `WaveEndCondition` (sealed: `FixedDuration`/`Cleared`) and
`SimpleWaveDefinition` to `core.port` — the ninth content kind.

**A new `ContentSource` method has to be a `default` method, or it breaks a module you don't own.**
`ContentSource` is implemented in exactly two places: `core`'s own `TestContent` (test support, mine
to update) and `game`'s `JsonContentSource` (not mine to touch). `tools/pre-pr-check` runs
`./gradlew build` for the whole repo whenever `core/` changes — not just `:core:test` — so an abstract
method added to `ContentSource` fails the check by breaking `game`'s compile, even though the diff
that caused it never touched a file outside `core/`. `ContentSource.wave(String id)` is a `default`
method that throws `UnsupportedOperationException` naming the issue that will override it (#113), so
`game` keeps compiling untouched until that PR lands. Checked precedent: `hasBoss()`/`boss()` were
added as *abstract* methods in commit `ce48e9e`, and `game`'s `JsonContentSource` was fixed in a
separate commit (`2ddab6e`) — that gap must have broken the whole-repo build for whoever's `pre-pr-check`
ran in between, or that pairing was coordinated in the same PR window. Don't assume abstract-by-default
is safe just because it happened before; check whether `./gradlew build` (not just `:core:test`) stays
green before opening the PR.

**A task that says "you may not touch system X" can still be blocked by X being the only consumer of
the type you're asked to redesign.** `WaveTimeline` is `SpawnSystem`'s only consumer
(`world.content().timeline(levelId).events()`). The task asked me to decide whether `WaveTimeline`
"gains a layer above it or its name stops matching," but literally reshaping it would have broken
`SpawnSystem`'s compile — out of bounds for this task, in scope for #112. Resolved it as a doc-only
change: `WaveTimeline`'s javadoc now says explicitly that it's superseded-but-not-retired and names the
task that migrates it, instead of silently drifting out of sync with the new `WaveDefinition` contract
sitting next to it. `SpawnEvent` is reused by both the legacy `WaveTimeline` (level-relative `at`) and
the new `WaveDefinition.spawns()` (wave-relative `at`) — its doc was generalised to not assume a single
reference frame, since it's genuinely just "a timestamped spawn," and the container decides what zero
means.

**Issue #111's own wording settled a design question the plan's prose left ambiguous.** The plan says a
wave declares "an id, its spawns, one end condition and one offset" but doesn't say which type carries
the offset. The plan's prose ("a wave is placed relative to the end of the one before it") reads as if
placement were a property of *where a level puts a wave*, which would suggest a separate
placement/reference record. The issue text is explicit: "its placement: an offset relative to the end
of the wave before it" is listed as one of the four things *the wave itself* declares — so `offset()`
lives on `WaveDefinition`, and a level's sequence is nothing more than an ordered list of wave ids. When
the plan and the linked issue disagree on a structural detail, the issue is probably the more literal
source — it's what the acceptance criteria are checked against.

See [[project_content-pipeline-design]] for the general ComponentSpec/registry pattern this follows,
and [[project_game-systems-design]] for why an optional-per-archetype component (the same shape
`WaveEndCondition` avoids by being sealed instead) can drift the wrong way.
