---
name: audit-techniques
description: Read-only ways to prove a finding in this repo without modifying it or re-running the full build, including batching, design-fidelity and TeaVM dist-size checks
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

## For a phase that adds systems consuming input or acting at range

- **Trace the input path end to end before trusting any "the adapter debounces it" javadoc.** Three
  files settle it: `InputAdapter.sample` (level vs edge per key), `LittleSpaceshipGame.render` (one
  sample per frame, handed to `advance`), and `GameLoop.advance`'s while-loop (the same frame given
  to every tick). Any system that *spends* something on `input.x()` being true is wrong unless `x` is
  a level. No build, no probe — three greps.
- **Reconstruct the exact position at a scripted tick from the branch's own replay fixture.** The
  strongest form of the "spawn geometry on paper" technique: take the fixture's spawn time, the
  trajectory's speed, the collider radius, and the tick the test scripts the action on, and multiply.
  For PR #22: wave due at t=1.0 → tick 60 → y = 270+5.5 = 275.5; crawl −9/s; bomb at tick 65 → 5
  steps × 0.15 = 0.75 → y = 274.75, bottom edge 269.25 inside a 270-tall playfield. That turned "the
  bomb probably hits off-screen enemies" into a defect the phase's own test demonstrates.
- **Count the stores declared in `World` against the assertions in `WorldTest`.** Two greps
  (`private final ComponentStore` in `World.java`, `assertEquals(0, world.` in `WorldTest.java`).
  Phase 05: thirteen vs four.
- **To find what a stage sees that it did not before, read forward through the ordinals, not the
  javadoc.** A stage that only calls `markForDestruction` leaves its victims fully alive — collider
  included — for every later stage in the same tick. Check whether `CollisionSystem` filters
  `pendingDestruction`; it does not.
- **`grep -P` fails in this Git Bash with "supports only unibyte and UTF-8 locales".** For the
  English-only sweep use a bracket class instead: `grep -n '[^ -~<TAB>]' file`. Piping that through
  `sed` in a `while read` loop also produces spurious "couldn't flush stdout: Permission denied"
  lines on Windows — the matches printed before it are still valid.

## For the round-2 pass that follows a rejection

- **Decide whether a rewritten test fixture is a fix or a cover-up by arithmetic, not by reading the
  new javadoc.** Recompute the scripted moment under the *new* rule: PR #22's presses moved from
  ticks 65/185 to 110/230, i.e. 50 ticks after each spawn, so y = 275.5 - 50 x 0.15 = 268 — inside a
  270-tall playfield by 2 units, therefore the scenario is real and the old one had become vacuous.
  Same multiplication as the technique that produced the original finding, run forwards.
- **Read a golden fingerprint as a list of fields and ask which of them the fix moves.** If deleting
  the fix leaves every field unchanged, the golden guards drift, not the fix — say so rather than
  crediting it with more than it does.
- **Verify a reflective "guards everything" test by looking for the populated-before assertion.**
  Empty-after alone passes vacuously for a store nobody filled. `WorldTest` asserts `store.size() > 0`
  for every discovered field first, plus a `>= 13` floor — that is the complete shape.
- **`git log --format='%s' <base>..HEAD | awk '{print length, $0}'`** checks the 72-character subject
  rule for a whole round in one line; useful whenever `status.md` promises commit hygiene going
  forward.
- **A "no error appeared in the manual run" claim is worth reconstructing against the level data.**
  For PR #22 the level's `line-3` wave anchors at `atX 0.5` -> x = 104, which is the player's exact
  start x, and `slow-descent` (-18/s) brings it into contact around t = 14 s — so a ~15 s hands-off
  run may well have rammed the player and produced a pickup after all. The status text under-claims
  here, which costs nothing, but the reconstruction is what tells you whether an "inferred, not
  verified" note is being conservative or is simply wrong.

## For a phase that turns a design document into screens

- **Count texture binds by reading the draw method top to bottom, not by profiling.** A `SpriteBatch`
  flushes on every texture change, so the bind sequence is fully determined by the source order of
  the draw calls. `HudRenderer` alternates a private 1x1 `pixel` `Texture` with its fonts; that plus
  the next point gives an exact per-frame count with no run.
- **`new BitmapFont()` twice creates two distinct `Texture` objects**, even for the same bundled
  default font. Any Skin that builds `font-mini` and `font-title` separately cannot batch text of the
  two together. Check the constructor, not the font name.
- **Separate O(1) from O(n) before calling an allocation a defect here.** List the draw path's
  allocations and ask whether each scales with entity count. This project's real rule is
  `12-architecture.md:159` ("not one object per entity per frame"), not
  `03-first-playable/plan.md:45`'s absolute phrasing; a constant handful of short-lived objects
  against ~10 ms of drawing is not a finding, and the stale criterion wording is.
- **`BitmapFont.draw` and `GlyphLayout` pool internally**, so text drawing is not the allocation
  source it looks like — `Integer.toString`/`StringBuilder`/`String.replace` in the same method are.
- **A design document can be internally ambiguous, and the two readings live in different sections.**
  `04-hud-layout.md` lists the invulnerability icon and timer unconditionally in its coordinate
  table, then assigns them to the power-up alone in a later prose section. Read both before calling
  an implementation wrong or right; quote the section the code actually contradicts.
- **The mockup sources are the fidelity oracle.** `docs/design/mockups/src/05-screens.js` is a list
  of explicit draw calls per screen — read it as a checklist against the shipped screen classes.
  `grep -n "setBackground\|n2-panel"` across `game/screen` settles which panels exist in seconds.
- **"The owner played the build" verifies reachability, not fidelity, and not the criteria table.**
  Treat a play-tested branch as having its *flow* confirmed and nothing else; the criteria rows still
  have to be earned one at a time.
- **Grep a `core` `static` helper for callers in `src/main` and `src/test` separately when the same
  formula might have been rewritten in `game`.** Test-only callers on a rule method is the tell that
  the production copy moved to the other side of the boundary.

Related: [[defect-patterns]], [[review-tooling-and-memory-placement]].

## For auditing presentation feedback nobody could capture on screen

- **Read a flash timer's visible length off the *call order* inside `draw()`, not off the constant.**
  `HudRenderer` runs `updateFeedback` (sets N) -> draw -> `decrementFeedback`, which makes a timer of
  N visible on exactly N consecutive frames. Set-then-decrement-then-draw would cost one frame,
  draw-then-set would cost the first. Three lines settle a whole table of tick counts.
- **For a two-phase flash, enumerate the counter's values rather than reasoning about the
  threshold.** `ruleFlashTicks` from 6 with `> 4` selecting `N7` gives 6,5 -> `N7` and 4,3,2,1 ->
  `W3`: the table's "2 then 4" without a probe.
- **To answer "what if two events land on the same tick", ask the domain which pairs can co-occur
  first.** `DamageSystem` never destroys the player entity (it decrements `lives` in place), so
  `WorldView.player()` never returns `PlayerStatus.NONE` mid-run — which is the only thing that
  would have produced a spurious "weapon level gained" flash off a `0 -> N` recovery step. The
  co-occurring pairs that remain (death removing shield and attachment with the life) touch disjoint
  plate regions and share no state.
- **A new `Skin` lookup is a runtime failure the compiler cannot see.** `getDrawable(name)` and
  `newDrawable(name, ...)` throw if the name was never registered — and `Skin.add(name, resource)`
  files it under its runtime class, so a nine-patch added implicitly is invisible to `getDrawable`.
  Confirm each new name against `GameSkin`, and check whether the screen that uses it is built in a
  constructor (so a manual run exercised it) or lazily (so nobody has).

## For auditing a TeaVM web dist directory

- **`du -sh` overstates a TeaVM debug/sourcemap dist by roughly 4x on this filesystem.** A non-release
  (or leftover-debug) build copies the sourcemap's original `.java` sources alongside `app.js`
  (`sourceFilePolicy.set(SourceFilePolicy.COPY)`), which for this project is ~580 small files under
  `webapp/src/`. Each gets rounded up to a filesystem allocation unit, so `du -sh web/build/dist/js/webapp`
  reported 8.5 MB against a true sum of ~2.2 MB (`find <dir> -type f -printf "%s %p\n"` added by hand,
  or `find <dir> -type f -exec du -ch {} + | tail -1` which sums apparent size instead of block size).
  Whenever a size claim for a TeaVM dist needs reproducing, sum file bytes directly — don't trust `du`
  on a tree with many small files, and check `app.js.map`/`app.js.teavmdbg`/`src/` are excluded from
  what you compare against a "what a visitor downloads" figure, since a release build (`-Prelease`)
  should not ship them but a stale incremental build directory can still contain them from an earlier
  non-release run.

## For auditing a single-task PR against a phase branch with no worktree left

- **`git show <rev>:<path>` on Windows Git Bash mangles a `origin/branch:path/to/file` argument** into
  `origin\branch;path\to\file` (MSYS's path-conversion heuristic fires on the colon+slash mix) and
  fails with "unknown revision or path not in the working tree" even though the object exists. Prefix
  the command with `MSYS_NO_PATHCONV=1` and it resolves correctly. Cheaper than re-adding a worktree
  just to read one file.
- **A worst-case geometric claim ("formation X's spread carrying enemy Y's radius") does not need X
  and Y to co-occur anywhere in current level content to be a valid bound**, when the spawn system
  uses one enemy id for every slot of a formation in a given wave (confirmed by reading
  `SpawnSystem.spawnWave`/`positionSpawned`: `event.enemyId()` is shared across all slots). Nothing
  stops a *future* wave from pairing them, so the bound is over all (formation, enemy) pairs the
  content schema allows, not over pairs actually present in `level-01.json`. Verified for PR #116
  (`SAFETY_MARGIN`/issue #84): `column-3` (44-unit `offsetY` spread, the largest of eight formations)
  paired with `enemy-carrier` (15-unit radius, the largest of six enemies) gives `y = 329`, exactly
  the class javadoc's figure, even though `level-01.json` never actually spawns `enemy-carrier` in
  `column-3`.
- **A golden fingerprint that goes A → B → A across a PR's commit history is stronger evidence than
  one that never moved.** Reading the intermediate commit (`git show <sha>` on the test file) proved
  the score path was genuinely exercised by the fix — B was the bug's real value before the rule was
  implemented, not a typo — which is the difference between "the golden pins the fix" and "the golden
  never had a chance to catch the fix's absence." PR #116: `entities=12→11` alone across the PR, but
  `score=1350→1600→1350` across three commits, only the middle one uncommitted-and-reverted.

## For auditing two sibling PRs that must be judged as one design

- **Actually build the merge instead of reasoning about whether two branches "should" combine
  cleanly.** `git worktree add <dir> --detach <phase-branch>` then two plain `git merge --no-edit
  <other-branch>` calls answers the conflict question with a real merge result, not a guess from
  reading two diffs that touch the same file region. Both branches of #118/#119 inserted a new
  paragraph into `docs/plan/11b-wave-system/status.md` right after the same anchor line; `git merge`
  with the `ort` strategy resolved it without a conflict marker because the two insertions were at
  different post-anchor offsets by the time the second merge ran — worth checking by eye afterward
  (`sed -n` over the merged file), because a clean auto-merge can still interleave two paragraphs into
  a reading order that contradicts itself even when git sees no conflict.
- **A stopgap type mismatch (`int` id now, `String` id in a sibling PR's contract) is cheap to promote
  later specifically when its only production writers are inside `core` and it is never serialized.**
  Grep every read/write site of the field (three, for `WaveOrigin.waveId`: `World`'s store, one
  `SpawnSystem` write, one `SpawnerSystem` copy) and confirm none of them round-trips through JSON or
  crosses to `game`. If both hold, changing the field's type later is a same-module, no-format-change
  edit — call it a real bridge rather than a baked-in assumption, and say why using the site count.
- **A `default` interface method that throws is judged against two things, not one:** whether its
  javadoc names the concrete task that retires it (a real issue number, not "later"), and whether any
  *other* implementer of the same interface would silently inherit the throwing behavior without
  noticing. `git grep -ln "implements ContentSource"` found exactly one production implementer
  (`JsonContentSource`, in `game`, a different agent's module) — which is what makes "an abstract
  method would force an edit outside this PR's module" a real constraint and not an excuse.
- **Counting test XML files under a merged worktree needs the right depth.** `find . -path
  "*/build/test-results/test" -name "*.xml"` can return zero on Windows Git Bash for a path that
  exists — `cd` straight into `core/build/test-results/test` and glob `*.xml` there instead of trusting
  a `find -path` pattern across a multi-module Gradle tree.
