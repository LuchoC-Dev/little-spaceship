---
name: defect-patterns
description: The recurring shapes of defect found when auditing this repo — where to look first in a phase review, including second-round passes
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

See [[audit-techniques]] for how to confirm these cheaply without touching the repo, and [[review-tooling-and-memory-placement]] for the operational traps around posting the verdict.
