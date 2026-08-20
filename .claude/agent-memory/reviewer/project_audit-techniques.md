---
name: audit-techniques
description: Read-only ways to prove a finding in this repo without modifying it or re-running the full build
metadata:
  type: project
---

Techniques that turned suspicions into confirmed findings during the phase 01 audit, all of them non-mutating for the repository.

**Why:** the reviewer role changes nothing, and re-running `./gradlew build` is slow and usually already done by the author. These get certainty faster and leave no trace in the working tree.

**How to apply:**

- **Compile a throwaway probe against the already-built classes.** `core/build/classes/java/main` exists after any build. `javac -cp <that> -d <scratchpad> Probe.java` then run it with `-cp "<classes>;<scratchpad>"` (semicolon — Windows). This is how a latent hazard gets demonstrated as output instead of argued from reading. Write the probe in the scratchpad directory, never in the repo.
- **Read `core/build/test-results/test/*.xml` instead of re-running tests.** Aggregating `tests=`/`skipped=`/`failures=` from the XML confirms the claimed count and, more usefully, that nothing was skipped.
- **Check module build outputs on disk** (`ls */build`) to confirm a module actually configures, before doubting a build-script construct you think is illegal.
- **Diff duplicated source against its original** when a spike copies a core class for cross-runtime verification: `diff <(sed -n '/^public final class X/,$p' copy) <(sed -n '/^public final class X/,$p' original)`. Confirms the parity claim is about the real class, and exposes drift.
- **Grep a member across main and test separately** to find API with no production caller.
- **`git diff main...HEAD -- <path>`** to decide whether an offending line is pre-existing or introduced by the branch under review. This is the difference between a blocking finding and a note.
- **Count operations on paper instead of timing them.** For a nested-loop performance finding, the exact iteration count for a named scenario is stronger evidence than a microbenchmark and immune to JIT noise — and it survives the "but TeaVM is different" objection, because the *count* is runtime-independent. Reconstruct the scenario from the benchmark that was cited (entity counts per layer), multiply, and compare against the pair count the benchmark reports. No probe, no build.
- **Read the cited benchmark's data structures, not its result table.** `spikes/web-viability/collisionbench/src/main/java/colbench/Main.java` is the source of the 0.028 ms collision figure the repo quotes repeatedly; it uses one flat `float[]` per layer. Any ECS-shaped implementation quoting it is quoting a number measured on different data. The spike sources are in Spanish and pre-date the English-only rule — that is expected there, not a finding.
- **Check `SystemOrder`'s ordinals whenever a note claims a future system can consume something.** Stage order is the enum's declaration order, and it is not the order the phases are built in. `WEAPON` (2) runs before `COLLISION` (5); `PICKUP` (7) and `SCORE` (8) run after `DAMAGE` (6) and before `CLEANUP` (9).
- **Check for the module that would hold the other half of a rule.** `find game desktop web -name "*.java"` returning nothing is a fast way to prove that a criterion involving input, rendering or audio cannot yet be met inside `core`, whatever the test claims.

Related: [[defect-patterns]].
