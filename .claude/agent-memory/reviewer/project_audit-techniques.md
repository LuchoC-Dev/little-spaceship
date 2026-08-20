---
name: audit-techniques
description: Read-only ways to prove a finding in this repo without modifying it or re-running the full build
metadata:
  type: project
---

Techniques that turned suspicions into confirmed findings during the phase 01 and 02 audits, all of them non-mutating for the repository.

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

## For a second-round pass, where the question is "did anything change on the way to the fix"

- **Prove behavioural equivalence by iteration order, not by re-running.** `ComponentStore` is a dense array walked by index, so the sequence a detection loop emits is fully determined by insertion/removal history. Two implementations that both walk `colliders` in index order emit the same sequence, whatever their nesting. `git show <base>:<path>` next to the current file is enough to settle it — no build, no probe.
- **A shared-fixture refactor is audited by diffing the fixture's defaults against the inline fixture it replaced,** field by field. Test count staying flat proves nothing; matching values do. `core.testsupport.TestBalance`'s defaults matched the deleted inline `FixedContent` exactly, which is what made the 68-line deletion a no-op.
- **A stub that throws is weaker than a stub that works.** When a test swaps `throw new UnsupportedOperationException(...)` for a real fixture, that is strictly stronger coverage, not weaker — the throwing stub was a trap waiting for the first system that read the value.
- **Verify a corrected citation by opening the line it now points at.** `docs/planning/11-technical-prototype-results.md:84` really does say the 480×270 / 208 px playfield "is confirmed as the starting point", which is what made phase 02's re-pointed `PLAYFIELD_WIDTH` javadoc a genuine fix rather than a differently-wrong citation.

## For a phase that turns rules into data

- **Prove a "never iterates a hash" determinism claim in three greps, not by reasoning.** Grep the
  registry class for every use of its map (`get` only is the tell), grep the test `ContentSource` for
  the same, then confirm every ordered walk on the spawn path is over a type whose compact
  constructor calls `List.copyOf`. That settles replay ordering without running anything.
- **Audit mutability at the compact constructor, never at the javadoc.** A Java record that reassigns
  its component through `List.copyOf`/`Map.copyOf` inside `public Foo {}` really is immutable; one
  that only documents "never null" is not. `PublicContractTest` cannot tell them apart — it checks the
  declared return *type*, and `java.util.List` passes either way, so mutability across the boundary is
  a hand check every time.
- **Find unused boundary API by grepping the method name with its argument shape.** `\.number(|\.text(`
  over `src/main` then `src/test` separately exposed that two of `ComponentSpec`'s five accessors —
  precisely the two-argument overloads — have no production caller. Overload arity is the thing to
  look at; the one-argument form was used everywhere.
- **Reconstruct spawn geometry on paper against the branch's own fixture.** `y = 270 + 4.5 - 30 =
  244.5` inside a 0–270 playfield is a stronger finding than any test could have been, took one line
  of arithmetic, and the fixture that supplies the numbers is in the same PR.
- **Check whether a JDK API is new to `core/src/main` on this branch** with `git grep -n "List.copyOf"
  main -- core/src/main`. `core` is what TeaVM compiles, JUnit runs on the JVM, and `web/build.gradle.kts`
  is still commented out — so a first-time use of `ImmutableCollections` is unverifiable here and is a
  suspicion to hand forward, not a defect to block on.

Related: [[defect-patterns]], [[review-tooling-and-memory-placement]].
