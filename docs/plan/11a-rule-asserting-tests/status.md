# Phase 11a — Tests that assert rules · status

**State:** in progress on `phase/11a-rule-asserting-tests`
**Updated:** 27/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

**Task 1 · baseline count** ([#96](https://github.com/LuchoC-Dev/little-spaceship/issues/96)) —
`test-engineer`, branch `docs/test-suite-baseline`. All 289 tests in `core/src/test/` read by hand
against a stated criterion. Headline: 165 assert a rule, 9 assert reproducibility, 2 overlap (both in
the same test method), 117 are infrastructure/contract tests that are neither. Full method, per-file
table and the reproducible denominator command are in
[`baseline.md`](baseline.md). Taken at commit `4e388067bf7ff01d527c72db9fa8828c79318b8f`.

The roadmap's "the bulk asserts reproducibility" holds for the narrow slice it was actually about — the
five cross-system replay files (9 tests, 6 reproducibility-leaning) — not for `core/src/test/` as a
whole, where the 151 `domain/system/*Test.java` unit tests are already overwhelmingly rule-asserting.
`baseline.md` says so explicitly so tasks 2–4 do not read the roadmap's line as "most of the suite is
weak" when it is not.

## In progress

The phase branch exists and the issues are open. The plan's seven tasks become eight pieces of work — task 5 splits, see D1 below — of which seven go to a worker:

| Task | Issue | Worker |
|---|---|---|
| 1 · baseline count | [#96](https://github.com/LuchoC-Dev/little-spaceship/issues/96) | `test-engineer` — done |
| 2 · defensive chain | [#97](https://github.com/LuchoC-Dev/little-spaceship/issues/97) | `test-engineer` |
| 3 · the boss's rules | [#98](https://github.com/LuchoC-Dev/little-spaceship/issues/98) | `test-engineer` |
| 4 · level completion | [#99](https://github.com/LuchoC-Dev/little-spaceship/issues/99) | `test-engineer` |
| 5a · forbidden-API check | [#53](https://github.com/LuchoC-Dev/little-spaceship/issues/53) (= [#3](https://github.com/LuchoC-Dev/little-spaceship/issues/3)) | `test-engineer` |
| 5b · `PublicContractTest` scope | [#54](https://github.com/LuchoC-Dev/little-spaceship/issues/54) (= [#4](https://github.com/LuchoC-Dev/little-spaceship/issues/4)) | `test-engineer` |
| 6 · `Rng` parity as a Gradle task | [#52](https://github.com/LuchoC-Dev/little-spaceship/issues/52) | `test-engineer` |
| 7 · where [#19](https://github.com/LuchoC-Dev/little-spaceship/issues/19) goes | — | the coordinator |

## Blocked

Nothing.

## Decisions taken while implementing

Four, all taken by the coordinator **before any worker was launched**, because each of them would
otherwise have been resolved by an agent in the middle of a task.

### D1 — Task 5 is two workers, not one

The plan's task 5 covers two independent pieces of work duplicated as four issues. They share nothing
but the phase that carries them: [#53](https://github.com/LuchoC-Dev/little-spaceship/issues/53) changes
`JavaSource` and `Rng`'s javadoc, [#54](https://github.com/LuchoC-Dev/little-spaceship/issues/54) changes
`PublicContractTest` and `LayerDependencyTest`. One worker for both would carry two unrelated diffs into
one pull request, and only one of them needs D2's exception.

### D2 — `test-engineer` may rewrite `Rng`'s class javadoc, and nothing else in `core/src/main`

`test-engineer`'s definition says it does not modify production code, and
`core/src/main/java/dev/luchoc/littlespaceship/core/domain/rng/Rng.java` is `core-domain`'s file.
[#53](https://github.com/LuchoC-Dev/little-spaceship/issues/53) step 3 is nevertheless part of the same
piece of work: the javadoc is coy *because* the check greps raw text, so fixing the check is what makes
the plain wording possible, and the two land together or the reason for the change is invisible.

**The exception and its limit:** comment text inside `Rng.java` only. No executable line, no signature,
no annotation, no import, no other file under any module's `src/main`. `git diff -- '*/src/main/*'` on
that branch must show changes to comment lines of that one file and nothing else.

`docs/plan/agent-prompts.md` says to split rather than grant an exception. Splitting here would spend a
whole worker — phase 09 measured a worker at 1.8–6.6 M cached tokens just to start
(`docs/planning/13-working-with-agents.md`) — on four lines of comment that cannot change behaviour and
whose limit a one-line `git diff` enforces.

### D3 — the `Rng` parity check gets its own Gradle subproject; `core` is not touched

[#52](https://github.com/LuchoC-Dev/little-spaceship/issues/52) asks for the real
`dev.luchoc.littlespaceship.core.domain.rng.Rng` to be compiled through TeaVM. Applying the TeaVM plugin
to `:core` would put a libGDX-adjacent toolchain on the module whose whole point is not having one —
invariant 1 says the compiler is what enforces that, and `core/build.gradle.kts` says so in a comment.

So the check lives in a **new subproject** that depends on `:core`, applies the TeaVM plugin itself, and
is registered in `settings.gradle.kts`. `game/src/tools/java` is the precedent: `tools/audio` moved into
its own source set for exactly this reason, and it was verified against the jar that `game.jar` carries
no `tools` classes.

**The limit:** the worker may create that subproject and add one line to `settings.gradle.kts`. It may
not add a dependency to `core/build.gradle.kts` and may not touch anything under `core/src/main`.

### D4 — task 7 is the coordinator's, not a worker's

Deciding where [#19](https://github.com/LuchoC-Dev/little-spaceship/issues/19) — `game` has no tests —
belongs is a routing decision about phases, not test engineering. It is written below and put to the
project owner rather than handed to an agent.

## Notes for whoever comes next

—
