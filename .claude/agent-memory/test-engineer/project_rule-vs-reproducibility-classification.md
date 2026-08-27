---
name: rule-vs-reproducibility-classification
description: How the core test suite was classified into rule-asserting vs reproducibility-asserting vs infrastructure for phase 11a's baseline, and what did not match the roadmap's inherited claim.
metadata:
  type: project
---

For phase 11a task 1 (issue #96), all 289 tests in `core/src/test/` (35 files) were read by hand and
classified. Full method and per-file table: `docs/plan/11a-rule-asserting-tests/baseline.md`, taken at
commit `4e388067bf7ff01d527c72db9fa8828c79318b8f`.

**The result contradicted the roadmap's framing, and the correction matters if this measurement is
ever redone or cited.** `docs/plan/post-mvp-roadmap.md` says "the bulk asserts that a run reproduces
itself, not that a rule holds" as a claim about `core/src/test/` generally. Measured: only 9 of 289
tests assert reproducibility (2 of those also assert a rule); 165 assert a rule; 117 are
infrastructure/contract tests (ECS mechanics, port DTO validation, architecture checks) that are
neither. The roadmap's claim is accurate only for a narrow slice — the five cross-system replay files
(`BombReplayTest`, `BossReplayTest`, `DamageReplayTest`, `LevelScoreReplayTest`, `SpawnerReplayTest`;
9 tests, 6 reproducibility-leaning) — because `domain/system/*Test.java` (151 tests) is already
overwhelmingly rule-asserting. **Why:** a figure repeated across three documents (roadmap, 10c
assessment, this phase's plan) without anyone re-measuring it drifted from what a full read actually
shows. **How to apply:** when a plan cites a suite-wide count that traces back to an "inherited,
never measured" figure, expect it to be right about its own example and wrong about the aggregate —
measure the whole population before reusing the framing.

**A test name is a hint, not a classification.** `SimulationTest.seedChangesTheOutcome` and
`inputChangesTheOutcome` contain "outcome"/"seed" but assert that two runs *differ*, not that they
agree — the opposite of a reproducibility test. `RngTest.pinnedSequence` pins a literal expected value
(looks like a "rule" test) but the value being pinned is the RNG algorithm's own numeric contract, not
a decided game rule from `docs/planning/` — it landed in infrastructure, not rule. Every one of the 289
needed an actual read of the assertion, not a grep for "Deterministic" or "assertEquals" shape.

**The two tests that assert both a rule and reproducibility in a single method** share one pattern
worth reusing: `assertEquals(first, second)` for the self-comparison, plus `assertEquals(GOLDEN_FINGERPRINT,
first)` against a committed literal string built from world state. Only `BombReplayTest` and
`LevelScoreReplayTest` do this among the five replay files — `BossReplayTest`, `DamageReplayTest` and
`SpawnerReplayTest`'s determinism tests have no pinned value, which is exactly what makes them pure
reproducibility tests and nothing else: a broken rule that breaks identically on both runs stays green.
