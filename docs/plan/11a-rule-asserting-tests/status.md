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

**Task 3 · the boss's rules** ([#98](https://github.com/LuchoC-Dev/little-spaceship/issues/98)) —
`test-engineer`, branch `test/boss-rules`. Read against `docs/planning/08-decisions-and-open-items.md`
("Level 1 climax and length"), `docs/plan/07-boss/status.md` and `BossSystem`'s and `BossStatus`'s own
class javadoc (which restate those decisions rather than invent new ones). Two of the three
acceptance-criteria clauses already had a test that fails when the rule breaks, both in the
pre-existing `BossSystemTest` (already counted as rule-asserting in the baseline): the third — the
roadmap's own example — had none, anywhere, and is what this task closes.

| Rule (quoted from planning / the code's own javadoc) | Test |
|---|---|
| "The boss has one phase, two alternating attack patterns and a clear tell before each" (08); tell timing from `06-boss-presentation.md`, three 0.25 s beats | `BossSystemTest.tellStepsThroughThreeBeatsThenFires` — asserts the beat progression 1→2→3→fire→0 via real per-tick steps, not a hardcoded duration |
| "spread and sweep alternate every cycle, never chosen" (`BossSystem` javadoc); fan of `FAN_COUNT` rays per side | `BossSystemTest.volleyFansThreeRaysPerSideAndAlternatesPattern` — asserts two consecutive volleys are SPREAD then SWEEP, six rays each, matching the fixed ratio tables |
| "the core is the only part whose death ends the fight... whatever keel, pods or arms remain are destroyed with it" (`BossSystem` javadoc, "Defeat"); the health bar "falls both when a part is hit and, at once, when a part dies and stops contributing anything" (`BossStatus` javadoc) — **the roadmap's own example: the ship destroys a module and the world reflects it** | **Gap, closed.** New `BossReplayTest.destroyingAPodDoesNotEndTheFightAndTheWorldReflectsIt`: through the real `Simulation` pipeline (`WeaponSystem` → `CollisionSystem` → `DamageSystem` → `CleanupSystem` → `BossSystem`), the ship shoots down the boss's left pod alone and the test asserts the fight stays `IN_PROGRESS`, the boss stays `present()`, and `hp` falls by exactly the destroyed pod's health — nothing at either the unit or the replay level tested a non-core part dying alone before this |

`BossSystemTest.defeatingTheCoreWinsTheRun` (pre-existing) covers the other half of the same rule — the
core's own death does end the fight and clears the remaining parts — at the unit level;
`BossReplayTest.victoryIsDeterministic` (pre-existing, counted "Both" in the baseline) pins that the
same thing holds through the full pipeline, but for the core, not for a module dying alone, which is
exactly the gap the new test closes.

**Red demonstrated.** Changed `BossSystem.updateSpawned`'s `if (!world.isAlive(core))` to
`if (!world.isAlive(core) || !world.isAlive(podLeft))`, so a pod's death alone ends the fight exactly
like the core's. Ran
`./gradlew :core:test --tests "...BossReplayTest.destroyingAPodDoesNotEndTheFightAndTheWorldReflectsIt"`,
got:

```
BossReplayTest > destroying a pod alone leaves the fight running, and the health bar reflects the loss FAILED
    org.opentest4j.AssertionFailedError at BossReplayTest.java:75
1 test completed, 1 failed
```

(line 75 is the `LevelOutcome.IN_PROGRESS` assertion, which is exactly the rule the change broke), then
reverted the one line. `git diff -- '*/src/main/*'` is empty on the branch that was committed.

**`./gradlew build` green** after the addition; `BossReplayTest` grew from two tests to three, none of
the earlier two changed.

**Coordinator's correction, and the finding under it.** The new test's javadoc originally sourced the
module rule to `BossSystem`'s and `BossStatus`'s own javadoc — the implementation, which is what this
phase exists to stop tests from asserting. It now cites `docs/plan/07-boss/status.md`, where phase 07
actually decided it: the `DEFEATED` state is reached on the core's destruction and "whatever keel, pods
or arms remain are marked for destruction with it", a part's "own death alone ends nothing", and the
bar "shortens the instant any part is hit or dies, not only the core". The javadocs restate that
decision; they are not its source.

**The finding: the boss's structural rules are not in `docs/planning/` at all.** They are in a phase's
`status.md`. `02-mvp-functional-spec.md:184` still reads "a simple, legible boss appropriate for a
first level; patterns and aesthetics still open", which 08's "Level 1 climax and length" answered on
21/08/2026 and phase 07 then built. `08-decisions-and-open-items.md` decides the *shape* of the fight
(one phase, two alternating patterns, a clear tell) and says nothing about parts, colliders or which
death ends it. That is not wrong — 07's status is a legitimate decision record and it is precise — but
it means "the rule comes from `docs/planning/`" is not always available, and a worker asked to obey it
literally will reach for a javadoc instead. **Left as a finding, not fixed:** correcting
`02-mvp-functional-spec.md` is a planning document edit and outside this phase's scope. It belongs
with 11e, which redesigns the boss and will have to write its rules down somewhere.

**Task 4 · level completion** ([#99](https://github.com/LuchoC-Dev/little-spaceship/issues/99)) —
`test-engineer`, branch `test/level-completion-rules`. Read `World.View.outcome()`'s bossless branch
(`waveTimelineExhausted && noEnemyLeft() && alive`) and `ScoreSystem.completionBonus` against
`docs/planning/02-mvp-functional-spec.md` ("Victory on defeating the boss while keeping at least one
life, or defeat on losing all lives") and `08-decisions-and-open-items.md`, "End-of-level completion
bonus" (`lives * 1000 + bombs * 300`, resolved against a flat-bonus reading). Two of the three
conjuncts, plus the bonus rule, already had a test that fails when broken; one conjunct had none.

| Rule (quoted from planning) | Test |
|---|---|
| The run does not complete before the wave timeline is exhausted (implicit in "Victory on defeating the boss"/completing the level; `waveTimelineExhausted` conjunct) | **Gap, closed.** New `WorldTest.outcomeStaysInProgressWhileTheWaveTimelineIsNotExhausted` |
| The run does not complete while an enemy survives (`noEnemyLeft()` conjunct) | Pre-existing `SpawnSystemTest.notCompleteWhileASpawnedEnemySurvives` — already red-when-broken: after its single `SpawnEvent` fires, `waveTimelineExhausted` is already true for the rest of the test, so this test isolates `noEnemyLeft()` specifically |
| "defeat on losing all lives" (02) wins over completion when both would apply the same tick (`alive` conjunct, in substance) | **Gap, closed.** New `WorldTest.outcomeReportsDefeatOverCompletionWhenTheLastLifeIsLostTheSameTick` |
| "1000 and 300 respectively... per-unit, not flat" (08, "End-of-level completion bonus") | Pre-existing `ScoreSystemTest.completionBonusScalesByRemainingLivesAndBombs` (unit level, calls `ScoreSystem.completionBonus` directly) and `WorldTest.completionBonusReflectsLivesAndBombs` (through `WorldView.completionBonus()`, the real boundary crossing) — both already use differing lives (3) and bombs (2) counts, which is exactly what makes a flat bonus distinguishable from the per-unit one; no gap |

**The `alive` conjunct, read literally, is dead code — a finding, not a gap this task could close by
writing a test for the literal conjunct.** `outcome()` returns `DEFEATED` in an early guard
(`state != null && state.lives <= 0`) *before* the bossless branch's `if (waveTimelineExhausted &&
noEnemyLeft() && alive)` is ever reached, and `alive` is computed right after that guard as
`state == null || state.lives > 0` — so at the point the bossless `if` runs, `alive` is provably always
`true`. Demonstrated directly: removed `&& alive` from that one line (nothing else), ran
`./gradlew :core:test`, and the full suite — 291 tests at the time, none touched — stayed green:

```
BUILD SUCCESSFUL in 6s
```

then reverted. No test, existing or new, can turn red from deleting that literal conjunct, because no
reachable state has `alive == false` there. What the decided rule ("defeat on losing all lives")
actually needs, and what the new `outcomeReportsDefeatOverCompletionWhenTheLastLifeIsLostTheSameTick`
test asserts and is red-when-broken for, is the early guard itself: broke it by loosening
`state.lives <= 0` to `state.lives < 0` (so zero lives no longer trip the guard) and ran
`./gradlew :core:test --tests "*WorldTest.outcomeReportsDefeatOverCompletionWhenTheLastLifeIsLostTheSameTick*"`:

```
WorldTest > a bossless level reports defeat, not completion, when the last life is lost on the same
tick the timeline empties with no enemy left FAILED
    org.opentest4j.AssertionFailedError at WorldTest.java:259
1 test completed, 1 failed
```

then reverted. Also demonstrated the `waveTimelineExhausted` conjunct going red the same way: dropped
it from the bossless `if`, ran
`./gradlew :core:test --tests "*WorldTest.outcomeStaysInProgressWhileTheWaveTimelineIsNotExhausted*"`,
got `AssertionFailedError at WorldTest.java:245`, reverted. And the per-unit bonus: changed
`ScoreSystem.completionBonus` to a capped/flat shape (`lives > 0 ? bonus : 0`), ran both bonus tests,
got both `FAILED`, reverted. `git diff -- '*/src/main/*'` is empty on the branch that was committed.

**The bossless branch's coverage gap for 11b, precisely.** Every test above that reaches
`LevelOutcome.COMPLETED` through the bossless branch — the pre-existing
`SpawnSystemTest.completesOnceTheTimelineIsExhaustedAndNothingIsAlive` (line 340 as of this branch) and
this task's two new tests — drives `noEnemyLeft()` to true by destroying the one spawned enemy by hand
(`world.destroyEntity(enemy)`) or by never spawning one at all, never by advancing a real wave clearing
through combat, drops or the collision pipeline. 11b changes what advances the spawn cursor and what
"cleared" means; nothing in this task's diff, or in the suite before it, exercises the bossless
`COMPLETED` path through the real spawn-and-clear flow `LevelScoreReplayTest` uses for score. That is
the gap task 4's own note in `plan.md` already named, confirmed by re-reading rather than widened.

**`./gradlew build` green** after the two additions.

**Coordinator's addition to that finding, and where it went.** The boss branch's
`bossDefeated && alive` is dead for exactly the same reason and was outside the demonstration above:
the early guard settles `alive` for the whole method, not for one branch of it. So `outcome()` carries
the redundancy twice. Opened as [#104](https://github.com/LuchoC-Dev/little-spaceship/issues/104)
against `core-domain` rather than fixed here — `test-engineer` does not modify production code, and
the choice between deleting both occurrences and restructuring the method so the defeat-beats-
completion priority is expressed once is a design call in `core`. Flagged as worth settling **before
11b**, which changes what advances the spawn cursor and what "cleared" means and will be reading this
method closely.

**Task 5a · forbidden-API check strips comments and literals** ([#53](https://github.com/LuchoC-Dev/little-spaceship/issues/53) = [#3](https://github.com/LuchoC-Dev/little-spaceship/issues/3)) —
`test-engineer`, branch `test/forbidden-api-strips-comments`. `JavaSource` gained `strip(String)`:
line comments, block comments and string/char literals (escaped quotes, an escaped backslash right
before a closing quote, char literals like `'"'`) are each blanked to a single space before
`containsToken` runs, so a name spelled out in a comment or a string literal no longer counts as a
call. `DeterminismRulesTest.noForbiddenApi` now strips each file before searching it. What `strip`
explicitly does not handle: text blocks (`"""`) — none exist under `core/src/main` today (checked
with `grep -rl '"""' core/src/main --include=*.java`, no output) — documented as a live limitation
in the method's own javadoc rather than guessed at.

**The risk named in the issue — stripping too much and blinding the check — is what most of the new
tests are for**, not the easy "comments are ignored" half. `DeterminismRulesTest` gained five fixture
tests (`stillCatchesARealForbiddenCallAfterStripping`, `ignoresAForbiddenNameInsideALineComment`,
`ignoresAForbiddenNameInsideABlockComment`, `ignoresAForbiddenNameInsideAStringLiteral`,
`catchesARealCallSharingALineWithAMentioningComment`) and a new `JavaSourceTest` gained four more for
the literal-scanning corners named in the plan (an escaped quote inside a string, an escaped
backslash right before a closing quote, a char literal holding a quote, an escaped quote inside a
char literal). All run the same `strip` → `containsToken` pipeline `noForbiddenApi` runs, on fixture
strings, so they do not depend on what `core` happens to contain.

**Red demonstrated, both directions.** First, the case where a defect removes stripping entirely:
temporarily made `strip` a no-op (`return content` before the real body). Ran
`./gradlew :core:test --tests "...DeterminismRulesTest" --tests "...JavaSourceTest"`, got 5 of 13
tests failing (the three comment/literal-ignoring fixtures plus two of the new `JavaSourceTest`
escape cases), then reverted. Second, and this is the one the issue calls the actual risk — a
stripper that strips too much and silently stops catching real calls: temporarily made the string
scanner also trigger on `.` (`c == '"' || c == '.'`), which eats real code after any period. Ran the
same two test classes and got exactly the two "still catches a real call" fixtures red
(`stillCatchesARealForbiddenCallAfterStripping`, `catchesARealCallSharingALineWithAMentioningComment`)
— the ones that exist specifically to catch this failure mode — then reverted. `./gradlew :core:test`
green after both reverts.

**Step 3, the one exception to this agent's boundary (D2).** Rewrote only the class javadoc of
`Rng.java`: it now names `Math.random()` and `java.util.Random` outright and drops the three lines
explaining why it was being coy, since the check no longer reads a javadoc comment as a call.
`git diff phase/11a-rule-asserting-tests -- '*/src/main/*'` shows only that one file, only comment
lines changed — pasted in the pull request.

**Ambiguity in the plan, worth flagging.** Neither the plan nor #53 says where the new fixture tests
should live. Put the five pipeline-level ones (strip + containsToken, mirroring `noForbiddenApi`
exactly) in `DeterminismRulesTest` next to the existing `searchesWholeWords`, and the four
literal-scanning corner cases in a new `JavaSourceTest`, since they are about `JavaSource` in
isolation rather than about the determinism rule itself. A different worker might have put all nine
in one place; naming the split here so it does not look accidental.

## In progress

The phase branch exists and the issues are open. The plan's seven tasks become eight pieces of work — task 5 splits, see D1 below — of which seven go to a worker:

| Task | Issue | Worker |
|---|---|---|
| 1 · baseline count | [#96](https://github.com/LuchoC-Dev/little-spaceship/issues/96) | `test-engineer` — done |
| 2 · defensive chain | [#97](https://github.com/LuchoC-Dev/little-spaceship/issues/97) | `test-engineer` — done |
| 3 · the boss's rules | [#98](https://github.com/LuchoC-Dev/little-spaceship/issues/98) | `test-engineer` — done |
| 4 · level completion | [#99](https://github.com/LuchoC-Dev/little-spaceship/issues/99) | `test-engineer` — done |
| 5a · forbidden-API check | [#53](https://github.com/LuchoC-Dev/little-spaceship/issues/53) (= [#3](https://github.com/LuchoC-Dev/little-spaceship/issues/3)) | `test-engineer` — done |
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
