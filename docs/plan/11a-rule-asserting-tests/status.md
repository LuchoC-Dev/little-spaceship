# Phase 11a — Tests that assert rules · status

**State:** in progress on `phase/11a-rule-asserting-tests`
**Updated:** 27/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

**Task 1 · baseline count** ([#96](https://github.com/LuchoC-Dev/little-spaceship/issues/96)) —
`test-engineer`, branch `docs/test-suite-baseline`. All 289 tests in `core/src/test/` read by hand
against a stated criterion. Headline: **167 assert a rule, 9 assert reproducibility, 4 overlap** (both
in the same test method), 117 are infrastructure/contract tests that are neither. Full method, per-file
table and the reproducible denominator command are in
[`baseline.md`](baseline.md). Taken at commit `4e388067bf7ff01d527c72db9fa8828c79318b8f`.

**The coordinator corrected two classifications before review**, by reading the file rather than by
re-reasoning: `BossReplayTest`'s two tests each pin a `LevelOutcome` as well as self-comparing, which
this document's own criterion calls "Both", so they moved out of reproducibility-only and the rule
headline went 165 → 167. The other five reproducibility-only tests were re-read at the same time and
all five hold. The per-file table was also verified mechanically against
`for f in $(find core/src/test -name "*.java"); do echo "$(grep -c '@Test' "$f") $f"; done` — all 35
rows match.

The roadmap's "the bulk asserts reproducibility" holds for the narrow slice it was actually about — the
five cross-system replay files (9 tests, 8 reproducibility-asserting) — not for `core/src/test/` as a
whole, where the 151 `domain/system/*Test.java` unit tests are already overwhelmingly rule-asserting.
`baseline.md` says so explicitly so tasks 2–4 do not read the roadmap's line as "most of the suite is
weak" when it is not.

**And the roadmap's flagship example is not quite what it says.** `BossReplayTest` was cited as
asserting nothing but determinism; it does pin the level's outcome. What the roadmap gets right is the
half that matters — nothing in that file asserts anything about the boss's modules, so breaking the
module rule leaves both tests green. Task 3 ([#98](https://github.com/LuchoC-Dev/little-spaceship/issues/98))
closes that, and should state the rule about modules rather than about the outcome, which is already
pinned.

**Task 2 · defensive chain** ([#97](https://github.com/LuchoC-Dev/little-spaceship/issues/97)) —
`test-engineer`, branch `test/defensive-chain-rules`. Every decided rule of the chain named in task 2
already had a test that fails when the rule is broken, in `DamageSystemTest` and `PickupSystemTest`
(both pre-existing, from phases 04/05, and already counted as rule-asserting in the baseline). One real
gap found and closed: `02-mvp-functional-spec.md` names losing the attachment, alongside the shield,
explicitly as a case that grants the shorter grace period — "Losing the shield or the attachment also
grants those grace frames" — but no test asserted it for the attachment path;
`shieldDamageGrantsTheShorterInvulnerability` covered the shield half only. Added
`attachmentDamageGrantsTheShorterInvulnerability`.

Rules checked against `DamageSystemTest`/`PickupSystemTest`, and where each already lives:

| Rule (quoted from planning) | Test |
|---|---|
| "invulnerability → shield → attachment → life" (03, 08) | `invulnerabilityAbsorbsTheHitEntirely`, `shieldAbsorbsBeforeAttachmentOrLife`, `attachmentAbsorbsBeforeLife` |
| "Any damage taken grants temporary invulnerability... shorter... than that of respawn" (02) | `shieldDamageGrantsTheShorterInvulnerability`, `lifeLossGrantsTheLongerInvulnerability`, and the new `attachmentDamageGrantsTheShorterInvulnerability` |
| "It absorbs the hit that destroys it, avoiding that life loss" (08) | `attachmentAbsorbsBeforeLife`, `attachmentSurvivesWhileDurabilityRemains` |
| "losing a life does not automatically remove persistent power-ups" (02, 08) | `losingALifeDoesNotClearPersistentPowerUps` |
| Life cap / weapon-upgrade cap (10) | `extraLifeRaisesLivesUpToTheCap`, `extraLifeAtMaximumGrantsPoints`, `weaponUpgradeRaisesShotLevel`, `weaponUpgradeAtMaximumGrantsPoints` (also `bombRechargeRaisesBombsUpToTheCap`/`AtMaximumGrantsPoints`, same cap-then-points shape) |
| "Picking up a power-up already at maximum... turns into points" (10) | one test per kind: `weaponUpgradeAtMaximumGrantsPoints`, `shieldAlreadyPresentGrantsPoints`, `extraLifeAtMaximumGrantsPoints`, `bombRechargeAtMaximumGrantsPoints`, `invulnerabilityAlreadyAtCapGrantsPoints`, `attachmentAlreadyEquippedGrantsPoints`, plus `maxedPickupIncreasesTheScoreOnceSwept` for the score side |
| "Weak enemies... are destroyed in that crash; tanks and heavy carriers are not" (02) | `weakEnemyDiesOnCollision`, `heavyEnemySurvivesCollision` |
| Attachment durability is data per attachment, not a constant (08) | `attachmentDurabilityComesFromDataNotAConstant` (in `PickupSystemTest`) |

**Red demonstrated for the new test.** Removed the `grantInvulnerability(...)` call from the attachment
branch of `DamageSystem.resolvePlayerHit`, ran
`./gradlew :core:test --tests "...DamageSystemTest.attachmentDamageGrantsTheShorterInvulnerability"`,
got:

```
DamageSystemTest > damage absorbed by the attachment also grants the shorter invulnerability, same as the shield FAILED
    java.lang.NullPointerException at DamageSystemTest.java:97
1 test completed, 1 failed
```

then reverted the production line. `git diff -- '*/src/main/*'` is empty on the branch that was
committed.

**One pre-existing test pins an open item; left untouched, out of scope.** `DamageSystemTest.
invulnerabilityAlsoProtectsAgainstConsequencesForTheOther` (phase 04) asserts that an invulnerable
player's crash does not destroy a weak enemy — exactly the behaviour `08-decisions-and-open-items.md`
lists as **open** ("Whether invulnerability should also suppress the consequences for the other
entity"). It predates this phase and the open-items list itself, matches the implementation, is
correctly named, and rewriting or deleting it is out of this task's scope ("rewriting tests that are
fine") and out of `test-engineer`'s boundary either way (the behaviour it pins lives in
`DamageSystem`, not the test). Naming it here so whoever settles that open item in 11e knows this test
will need to move with the decision, not be treated as a second vote for the current reading.

**Not tested, and not treated as a gap:** "the attachment disappears when taking damage and **when
losing a life**" (08, "Resolved contradictions") could read as a second, independent loss trigger
distinct from absorbing the hit. Checked against `DamageSystem.resolvePlayerHit`: the three branches
are mutually exclusive by construction (attachment absorbs before the life branch is ever reached), so
there is no code path today where a life is lost while an attachment is equipped — the "and when losing
a life" clause is satisfied structurally, not by a separate rule to assert. Worth revisiting only if a
future damage source bypasses this ordering.

## In progress

The phase branch exists and the issues are open. The plan's seven tasks become eight pieces of work — task 5 splits, see D1 below — of which seven go to a worker:

| Task | Issue | Worker |
|---|---|---|
| 1 · baseline count | [#96](https://github.com/LuchoC-Dev/little-spaceship/issues/96) | `test-engineer` — done |
| 2 · defensive chain | [#97](https://github.com/LuchoC-Dev/little-spaceship/issues/97) | `test-engineer` — done |
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
