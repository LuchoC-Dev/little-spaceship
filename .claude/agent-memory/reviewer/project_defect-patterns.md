---
name: defect-patterns
description: The recurring shapes of defect found when auditing this repo — where to look first in a phase review, including second-round, data-driven and seam-defect passes
metadata:
  type: project
---

Defect patterns observed auditing phase 01 (`feat/core-foundations`, PR #2). None of them are visible to a grep, and none of them failed a test.

**Why:** this project defends its invariants with self-written architecture tests and with a status/PR narrative. Both can be *narrower or staler than they claim* while everything stays green, so the audit has to check the claim against the check, not just run the check.

**How to apply:** on any phase review, walk these before reading the implementation line by line.

1. **An architecture test whose scope is narrower than the criterion it is cited for.** Read the test's package/class filter, not its javadoc. Phase 01's boundary check inspected only two of the three core layers while the plan's criterion and the PR table said "no public type in core". The test was still useful; the *claim* was overstated.
2. **Public accessors with zero call sites that hand out a mutable internal.** Grep the accessor name across `src/main` and `src/test`; a getter used by nobody is usually a boundary hole that a later phase will walk into. Cross-check it against the "no abstraction without a real case in the MVP" invariant, which catches it on two grounds at once.
3. **Two ways to destroy/mutate the same state, only one of which is safe.** Whenever a class documents "the caller always does X", check whether the API also allows not-X and what happens then. Ghost state that survives destruction is the shape this takes here.
4. **Agent memory under `.claude/agent-memory/` contradicting `docs/plan/*/status.md`.** Memory gets committed mid-phase and then the phase moves on; the memory file is not updated. Diff the two whenever both changed on the same branch.
5. **Docs conventions.** The English-only rule is absolute outside `docs/sources/`, and `spikes/` is already fully Spanish, so new files added there inherit the violation. Check whether an offending line is pre-existing on `main` before treating it as introduced by the PR — `git diff main...HEAD -- <file>` settles it and changes whether it can block.

Three more, from phase 02 (`feat/core-mechanics-02`, PR #10). Same family: the code was sound, the *claims attached to it* were not.

6. **A quoted measurement whose benchmark has a different data shape than the code citing it.** This repo justifies design choices with numbers from `spikes/web-viability/` and `docs/planning/11-technical-prototype-results.md`, and those numbers get copied into javadoc, commit bodies and PR descriptions. Open the benchmark source before accepting the citation: the collision figure was measured on flat per-layer `float[]` arrays, while the ECS implementation scans a whole `ComponentStore` per pair — same algorithm class, ~50× the operation count and a much costlier inner iteration. The *decision* was right and the *evidence* did not cover it.
7. **A criterion whose enforcement is split between `core` and an adapter that does not exist yet.** The test then asserts the trivial half and the criterion gets a green tick. "Keyboard and mouse in opposite directions cancel" was tested by feeding a zero vector and asserting no movement. Ask, for every criterion: which module actually contains the rule? If the answer is a module with no code yet, the criterion is deferred, not met.
8. **Forward-looking notes about how a *future* phase will reuse something.** They are written from intent and never compiled. Verify them against the declaration they depend on — `SystemOrder`'s ordinals are the usual one, and `WEAPON` sits *before* `COLLISION`, which invalidated a note saying a future `WeaponSystem` could read the collision buffer unchanged. These notes live in both `status.md` and agent memory, so the same wrong claim usually appears twice.

Also worth checking every time: whether the branch edited its own `plan.md` acceptance criteria. Phase 01 did, and the edit *raised* the bar rather than lowering it, which is fine — but it has to be read, not assumed.

And the counter-pattern, so the review stays calibrated: phase 02's agent memory did **not** repeat pattern 4 in the dangerous direction — it recorded the invented rules honestly, including one that contradicts a literal line of the spec. What it did instead was absorb phase progress from `status.md`. Both are worth naming, but only one of them is a defect in the code.

## Second-round passes have their own two shapes

From the round-2 pass on PR #10, where every finding had been addressed and the honest result was "accept, nothing new".

9. **A fix that relocates the identity of a subject, leaving the old identifier guarding nothing.** Phase 02's collision fix stopped matching the player by `CollisionLayer.PLAYER` and started resolving it through the `Player` component. Correct, faster, and it left `CollisionLayer.PLAYER` referenced by no production code at all — so a player collider on the wrong layer is now caught by nothing, and a future player-side collidable that is a *separate entity* would not be detected. On any round-2 pass, ask of each fix: what did the old mechanism used to catch that nothing catches now? Grep the old identifier across `src/main` and `src/test` separately — production-empty is the tell.
10. **A refactor sold as cost-only that also reorders an output buffer.** `CollisionSystem` writes `World.collisionHits()` and `DamageSystem` consumes it *in order*, so append order is behaviour, not an implementation detail. The reorder was safe here only because the two pairs `DamageSystem` consumes kept their relative order and the moved pair has no consumer yet. Nothing in the suite pinned that — every `CollisionSystemTest` case emits exactly one hit, so cross-pair order is untested. When a detection or event loop is restructured, reconstruct the emitted sequence by hand for the fixture population; do not expect a test to catch it.

Related to 10: **`DamageReplayTest` is not a regression net.** It runs the same build twice and compares fingerprints, with no golden value committed, so it proves determinism *within* a build and cannot detect a refactor that changes the outcome. Do not treat "the replay test passes" as evidence that behaviour was preserved across a diff.

## Four more from phase 04 (`feat/content-pipeline`, PR #16), where content became data

This phase's defects are the *data-driven* family: the code is generic and correct, and what leaks is
a rule that used to be in Java and is now a JSON field nobody validates.

11. **A geometric guarantee in a class javadoc that only holds for the zero case the tests use.**
    `SpawnSystem` promised every enemy "starts fully off-screen regardless of its size" while adding
    only the *entity's* radius, not the *formation's* vertical extent — so any slot with a negative
    `offsetY` spawns on-screen, and the phase's own `diagonal` fixture had two. The tests asserted
    position only for flat formations. Whenever a javadoc says "always" about a computed position,
    reconstruct the value by hand for the least flat fixture in the branch, not the first one.
12. **An optional content field whose default is the minority value of the roster it configures.**
    `spec.flag("fragile", false)` decides the crash rule for six archetypes, four of which are
    fragile. Omitting the field in JSON silently inverts a spec rule, with the symptom appearing in
    `DamageSystem`. For every optional field a factory reads, ask: what fraction of the real content
    wants the default, and what game rule flips when it is wrong?
13. **Typed optional accessors that treat "present but wrongly typed" as "absent".** `MapComponentSpec`
    fails loudly for required fields and silently defaults for optional ones, while its javadoc claims
    the loud behaviour for all of them. This directly defeats the "fails naming the offending id, not
    an NPE" criterion, and only for the half nobody tests. Read the *optional* overload, not the
    required one — the required one is always the correct one.
14. **An extension seam wired to a `private static final`.** `ComponentFactoryRegistry.register` is
    public with zero production callers; `SpawnSystem` holds one registry as a private static built
    from `withDefaults()`, so nothing outside `core` can substitute or extend it. Pattern 2's
    "accessor with no call site" in its builder form: the seam exists, is tested against a registry
    the test constructs itself, and no production path can ever be that registry.

Also from phase 04, and worth keeping the calibration honest: **every citation in the branch checked
out** — `docs/design/04-hud-layout.md:25` for the 270 playfield height, `02-sprite-sizes.md:38` and
`:77-84` for radii and ids, `10-mvp-initial-values.md:105-115` for the score table, and the
`SystemOrder` ordinals in `status.md`. After two phases where patterns 6 and 8 both fired, the
citation check came back clean. Run it anyway; do not assume it will fail.

The criteria-table defect (pattern 7) reappeared exactly once, narrowed to a single row: "all content
ids are in English" ticked met on the strength of test fixtures, in a branch with no content files.
The other five rows hedged honestly. Also new in that document: a plan **task** half-built
(trajectories yes, firing patterns no) with the reason recorded in prose but the task surfaced as
outstanding nowhere. Check `plan.md`'s task list against the branch, not only its criteria list —
the criteria table is not a superset of the tasks.

## Four more from phase 05 (`feat/game-systems`, PR #22), where systems started consuming input and killing at range

This phase's family is the *seam* one: every piece was individually correct and the defect lived in
the join between two of them, which is why nothing failed and nobody noticed.

15. **An edge-shaped input consumed once per tick, under a loop that replays one frame's input across
    N ticks.** `GameLoop.advance` feeds the *same* `InputFrame` to every tick a frame produces, and
    says so in its own javadoc; `InputAdapter` debounces with `isKeyJustPressed`, which is an edge
    per *render frame*. `BombSystem` spends a charge on every tick where `input.bomb()` is true, so
    one press costs two charges at 30 fps and up to the whole cap after a stall. Neither layer is
    wrong on its own and neither owns the tick-level edge. For any new system, ask whether the field
    it reads from `InputFrame` is a level or an edge, and if it is an edge, whether the consumer is
    idempotent across a repeated frame. `fire()` is a level and is fine; `bomb()` is the first edge.
16. **A stage inserted before the detection stage, when all it does is *mark* destruction.**
    `markForDestruction` does not remove the collider and `CollisionSystem` does not filter
    `pendingDestruction`, so `BOMB` at ordinal 3 means everything the bomb "cleared" still generates
    a `CollisionHit` at ordinal 6 and still costs the player a life at 7. The stage javadoc argued
    its placement against `SPAWN` and never mentioned `COLLISION`. Whenever a stage is added, list
    what it *marks* rather than destroys, then read forward through every later stage that iterates
    the same store — the answer is almost never in the javadoc that justified the placement.
17. **A whole-screen effect with no screen bound, in a world that deliberately spawns off-screen.**
    `BombSystem.detonate` walks every enemy collider; `SpawnSystem` puts every wave at
    `y = 270 + radius` and above. The spec says "on screen". The branch's own `BombReplayTest`
    scores off enemies 93% above the playfield, and its `bombActuallyScoredSomething` guard asserts
    on exactly that score. Any system whose scope is "the screen" needs a bound: grep it for
    `PLAYFIELD_HEIGHT`/`PLAYFIELD_WIDTH`, and if neither appears the bound is missing.
18. **A hand-enumerated completeness test that quietly stopped being complete.**
    `WorldTest.destroyStripsEveryComponent` sets four components and asserts four stores are empty,
    while its javadoc claims to be the guard against forgetting a store in `destroyEntity`. `World`
    now has thirteen. `destroyEntity` was correct anyway — by hand, not by check — and has been
    since phase 02. Count the stores declared in `World` against the assertions in that test on every
    phase that adds a component; the drift is invisible and the failure mode (a `Health` left on a
    recycled slot) is silent.

Calibration from this phase, in both directions: the author's verified/inferred split in `status.md`
held up on every point I checked, and the criteria table hedged honestly on the two rows that
deserved it (the completion-bonus reading, the replay's missing golden). But pattern 7's cousin fired
again — plan **task** 8 (guaranteed drops) is unbuilt, `level-01.json` carries two of the four drops,
and `status.md` says "the phase's task list is complete". Read the task list against the content
files, not against the criteria table.

## Round 2 on PR #22: what a *correct* response to a rejection looks like

Recorded because "accept, nothing new" is hard to calibrate and this round is a good reference for
the shape of an honest fix.

- **A replay fixture rewritten after a fix is not automatically a whitewash.** The test to apply:
  (a) did the fix make the old scenario *vacuous* rather than *failing* — here the old bomb presses
  at ticks 65/185 landed on enemies now correctly out of reach, so the run would have scored zero
  and proved nothing; (b) is the behaviour the old fixture accidentally exposed now pinned
  somewhere deliberate — here three unit tests, above/exactly-at/inside the 270 edge; (c) did the
  rewritten fixture gain a non-vacuity assertion. Three yeses is a correct response, not a
  disappeared failure.
- **A golden fingerprint is worth what its fields encode, and it usually does not pin the fix that
  prompted it.** `BombReplayTest`'s `score=200 lives=3 bombs=0 entities=4` would not move if the
  on-screen bound were deleted (the scripted presses now land on visible enemies either way), and
  `LevelScoreReplayTest`'s does not encode shield/attachment state. That is fine — the golden is a
  net for *unintended* drift, the unit tests are the net for the rule — but do not accept "the
  golden pins the fix" as an argument.
- **Boxed-`Integer` set lookups in a per-tick O(n^2) loop are within budget here.** `CollisionSystem`
  builds `Set.of()` when nothing is pending (no allocation) and only boxes at the `contains(int)`
  call sites. At MVP collider counts that is a few thousand short-lived `Integer`s per tick against
  ~10 ms of drawing — not a finding under this project's own "drawing is the cost" rule. If it ever
  matters, ordering the layer filter before the `contains` removes most of it for free.
- **A log-once `Set<String>` keyed on sprite ids is bounded** as long as every id is either a
  compile-time constant or composed from a value validated against a closed set — after round 2,
  `SpawnSystem` rejects an unrecognised `drop` id at spawn time, so `"pickup-" + drop.pickupId`
  cannot invent new strings. Check the validation, not the `Set`.
- **A statement of intent inside `status.md` becomes checkable in the next round.** F11 promised
  "every commit from this point on respects the limit"; one round-2 subject is 75 characters. Cheap
  to verify (`git log --format='%s' <base>..HEAD | awk '{print length}'`) and exactly the class of
  claim this repo tends to leave behind.
- **Pattern 4 again, in its slow form:** `core-domain`'s memory closes by pointing at "the exact
  `game`-module compile breakage this phase leaves behind", which round 2's `status.md` records as
  closed. The memory line was true when written and nobody revisits it — diff memory against
  `status.md` whenever both changed on the branch, in both directions.

See [[audit-techniques]] for how to confirm these cheaply without touching the repo, and [[review-tooling-and-memory-placement]] for the operational traps around posting the verdict.
