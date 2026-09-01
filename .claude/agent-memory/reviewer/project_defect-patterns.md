---
name: defect-patterns
description: The recurring shapes of defect found when auditing this repo — where to look first in a phase review, including second-round, data-driven, seam, design-fidelity, workflow-enforcement and cross-agent-handoff passes
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

## Six more from phase 06 (`feat/hud-and-screens`, PR #26), where a design document became screens

This phase's family is the *fidelity* one: the code runs, the owner played it, and what drifted is
the distance between the document that was drawn and the thing that was built. Nothing here fails a
test either, and "the owner confirmed it works" does not touch any of it.

19. **A game rule reimplemented in the presentation layer while `core`'s tested copy stays uncalled.**
    `PlayScreen` computed `lifeCompletionBonus() * lives + bombCompletionBonus() * bombs` inline the
    same pass that *edited* `ScoreSystem.completionBonus`'s javadoc to explain why it has no caller.
    Two implementations of one rule in two modules, and the tested one is dead. Whenever `game` reads
    a `PlayerStatus` field and then does arithmetic on it, ask which document that arithmetic comes
    from and grep `core` for the same formula — this is pattern 2 (accessor with no call site) seen
    from the other end, and the giveaway is a `static` method in `core` whose only callers are tests.
20. **A domain field justified by a design document, consumed by something that document assigns
    elsewhere.** `Invulnerable.source` was added to `core` because "`04-hud-layout.md` asks the HUD to
    draw the three sources differently *on the ship*". The ship treatment was never built; the only
    consumer is a plate widget the same document assigns to the power-up alone. The field is still
    the least-bad option — read the *consumer* before accepting the justification, and check whether
    the cited section is the one the code implements.
21. **A spec bullet list ticked as a whole when one bullet is unbuilt.** `02-mvp-functional-spec.md`'s
    HUD section has eight bullets; seven were drawn and "clear feedback for hits and for losing
    upgrades" was not, while the criterion "the HUD shows everything the spec requires" was marked
    **earned**. Read the source list item by item against the implementation; do not accept a
    criterion that quotes a document as evidence that the whole document was satisfied. In this case
    the missing half was not blocked on `core` at all — it is a frame-over-frame diff of
    `PlayerStatus` — which is worth checking before believing "deferred".
22. **"Everything else is unchanged from the first pass's assessment", where the first pass assessed
    two rows of seven.** A criteria table can be dodged by reference as well as by overclaim. When a
    status pass defers to an earlier one, open the earlier one and count the rows it actually
    evaluated.
23. **Batching broken by texture alternation, in the phase whose criterion is batching.** `new
    BitmapFont()` twice produces two distinct `Texture`s even for the same bundled font, and a HUD
    that alternates label -> filled rects -> label rebinds once per switch. ~12-14 binds per frame
    from `HudRenderer` alone. Absolutely trivial at this scale and *not* worth optimising — but it is
    the literal task-12 criterion, and it is the finding that matters more than any allocation count
    under this project's own "drawing is the cost" rule. Reconstruct the bind sequence by reading
    the draw method top to bottom; no profiler needed.
24. **A pixel-exact mock is a checkable artefact, and nobody checks it.** `docs/design/mockups/src/
    05-screens.js` draws all six screens as explicit calls. Diffing those calls against the shipped
    screens found six omissions (title rule, credits entry, pause plate, menu subtitle/footer,
    unpadded defeat score, stat bars turned into raw numbers) that "the owner played the build and
    it works" cannot surface, because reachability and fidelity are different claims. The
    counter-example in the same branch: the omitted second ship slot *is* documented with a reason,
    which is how a deviation should look.

Calibration from this phase, and it matters: **the per-frame allocation the parent flagged as the
invariant most at risk turned out not to be a defect.** `WorldView.player()`'s fresh record plus the
zero-padded score come to ~6-10 short-lived objects per frame, constant in entity count.
`12-architecture.md:159` states the intent as "not one object per *entity* per frame", which is
intact; only `03-first-playable/plan.md:45`'s absolute phrasing is now literally false. The honest
finding was the stale criterion, not the allocation. Do not manufacture the O(1) case into a defect.

## Round 2 on PR #26: presentation timers, and the limit of a snapshot contract

The verdict was accept. What is worth keeping is not the findings — they were all notes — but four
shapes that will recur the moment presentation grows.

25. **A snapshot contract can report *what changed*, never *what happened*.** Phase 06 derives five
    of the six feedback events from a frame-over-frame diff of `PlayerStatus`, which works only
    because they are monotone state transitions. The sixth (pickup collected at maximum) is
    indistinguishable from an ordinary kill, because `maxedPickupBonus` and `enemy-tank`'s score are
    both 500. Whenever presentation is asked to react to an *event*, check whether the port hands it
    a state or an occurrence. The seam for occurrences already exists and is empty:
    `core.port.GameEventSink`, `core.domain.event.GameEvent` (a marker interface with **zero**
    implementations) and `GameEventQueue` (buffers, drains in emission order after the tick), with
    `PlayScreen` passing `event -> { }`. Audio in phase 08 needs the same distinction, so the first
    concrete `GameEvent` closes both at once.
26. **Presentation timers counted in `draw()` calls, against a design table written in simulation
    ticks.** `HudRenderer`'s flash counters and `WorldRenderer.sourceTicks` advance once per render
    frame, while `04-hud-layout.md` specifies ticks of 1/60. Desktop pins the two together
    (`DesktopLauncher` sets `useVsync(true)` **and** `setForegroundFPS(60)`); the web target paces
    off `requestAnimationFrame`, so a high-refresh display runs every blink and flash fast while the
    durations, which come from `BalanceValues` in seconds, do not move. Constants named `*_TICKS`
    that a render loop decrements are the tell.
27. **An identity duplicated as a literal across modules fails silently in both directions.**
    `WorldRenderer` matches the player by `"ship-basic"`, copied from `Simulation`'s
    `private static final SpriteId PLAYER_SPRITE`, because `SpriteVisitor.accept` carries no player
    flag. Rename either side and every ship-side treatment stops drawing with no compile error and
    no failing test. Distinguish this from a string that genuinely *crosses* the boundary
    (`PlayerStatus.attachmentId`), which cannot drift.
28. **A domain method that goes `public` to serve `World.View` escapes `PublicContractTest`
    entirely.** Its `BOUNDARY_PACKAGES` are `core.port` and `core.application` only, so
    `ScoreSystem.completionBonus(BalanceValues, Player)` — public, taking a mutable domain component
    — passes untouched. Not a breach in practice (`game` cannot obtain a `Player`), but the guard
    becomes the javadoc. Pattern 1 in its newest form: check the test's package filter every time a
    fix widens visibility inside `domain`.

Calibration, and it is the point of the round: the two claims the parent asked me to distrust —
hand-traced tick counts and a deliberate omission — both held. The counters set-draw-decrement in
one method, so a timer of N is visible for exactly N frames, no off-by-one anywhere; the omission
was the correct call and was documented rather than guessed. What the write-up does instead, twice,
is state an omission once in prose while the summary sentence beside it reads as complete. That is
the residual shape in this author's status documents, and it is a note, never a rejection.

## PR #30 (`feat/sprite-production`), art-only, no code — the clean-branch calibration case

Nine commits, docs/design only (`git diff --stat main...HEAD -- core game desktop web` is empty), so
invariants 1-6 could not be at risk structurally and the review became fidelity-checking a design
document against itself and against the PNGs it describes. Worth recording because this branch
passed almost everything, and knowing what "clean" looks like here matters as much as the defect
catalogue.

29. **A frozen sizing table not updated when a later document in the *same PR* supersedes its
    granularity.** `02-sprite-sizes.md`'s boss row still reads one sprite, `boss-l1`, 119x87; the
    same branch's `06-boss-presentation.md` splits the boss into `boss-core`/`boss-pod`/`boss-arm`
    (47x87/25x25/31x45) and says explicitly "five sprites, not one image". The radii and offsets
    agree byte-for-byte between the two documents — only the sprite-id/dimension row was left
    stale. A note, not a blocker: whenever a phase adds a document that supersedes part of an
    earlier frozen one, diff the specific row, not just the shared numbers (radius, offset), since
    those are what people check first and the id/dimension column is what silently drifts.
30. **A Skin's `skin.json` loaded through `Skin(FileHandle, TextureAtlas)` is exactly the reflective
    `Json` class the web-pitfalls section warns about**, and the design doc for it
    (`docs/design/07-skin.md`) commits to that loading contract (`skin.load(Gdx.files.internal(...))`)
    without mentioning TeaVM reflection config. Not a defect on an art-only branch with no code, and
    scene2d's own Skin format has no non-reflective loader — but it is a suspicion worth handing to
    whichever phase writes the `Skin.load` call: confirm the TeaVM reflection declarations exist for
    every style class named in the JSON before that call ships, since CLAUDE.md's own JSON rule reads
    as an absolute and this is the one legitimate exception to it.
31. **Numeric self-consistency across a design PR is checkable in full, cheaply.** Every measurable
    claim in this branch's `status.md` table checked out against the committed PNGs: frame counts
    (29 = 5+6+8+10), per-frame sprite sizes (21/31/47/95, read via `struct.unpack` on each PNG's IHDR),
    font sheet dimensions (96x60, 128x78), and the boss's ±59 px arm reach recomputed by hand from
    odd-width sprite geometry (half-width to edge is 15, not 15.5, because rule 1 makes every
    dimension odd with a true centre pixel). Zero mismatches. This is the counter-example to keep
    calibration honest: "read every number, trust none of them until reconstructed" does not always
    find something, and this branch is the proof it does not have to.

## Phase 08 (`feat/audio`, PR #31): clean, and worth recording *why* nothing fired

Five commits, 29 files, a synthesised-WAV generator, a runtime, and volume sliders. Verdict was
accept — no invariant violation, no misclaimed criterion. Recording this one because "I found
nothing" needs the same rigor as a rejection, and because two shapes from earlier phases were
present here in their *safe* form, which is the useful contrast to keep.

- **Pattern 27 (identity duplicated as a literal across modules) recurred, in the form the project
  already has precedent for accepting.** `AudioDirector.SHOT_P1`/`SHOT_P2` copy `WeaponSystem`'s
  private `SpriteId("shot-p1"/"shot-p2")` literals verbatim, exactly the way `WorldRenderer.
  PLAYER_SPRITE_ID` already does for the ship. Confirmed both sides still match by grep. Still a
  real fragility (rename either side and the shoot cue goes silent with no compile error and no
  failing test) — worth a note every time, not a blocker, since the project already made this
  trade-off once and it holds for the same reason: no `core.port` type currently carries a role tag,
  and adding one without a second real case would trip invariant 6.
- **A "blocked, waiting on another branch" claim is a citation, and checks out the same way any
  other citation does.** `status.md` asks `core-domain` for an `EnemyDestroyed` event and says
  `WorldView.bossStatus()` "does not exist on `main` yet — it is on `feat/boss`". Both grepped to
  zero hits in `core/src/main` and `game/src/main` on this branch. The task's own framing (context
  section) said not to report this as a defect if true; verifying it cheaply is still worth doing
  rather than taking the framing on faith — it would have been a real finding if the grep had come
  back non-empty.
- **A design-time-only tool living inside a module that TeaVM compiles is not a risk *while the web
  build stays commented out*.** `game/.../tools/audio/{Wav,Synth,GenerateAudio}.java` use
  `java.nio.file`, which has no TeaVM guarantee, but `web/build.gradle.kts`'s `gdxTeaVM {}` block is
  entirely commented out (phase 03 reverted it, phase 09 owns bringing it back). So nothing compiles
  those classes for the web target *yet*. This is a suspicion to hand forward for phase 09's audit,
  not a defect now — the distinction is the same one `audit-techniques.md` already draws for
  `List.copyOf`: unverifiable is not the same as wrong. Check whether `tools.audio` gets excluded
  from the TeaVM source set (or moved to a separate Gradle source set entirely) when phase 09 lands.
- **The counter-example to go with pattern 21/22:** the acceptance criterion "audio starts without
  an error in the browser, verified on a real browser" is *not* claimed met — `status.md` says
  outright "did not confirm by ear; this sandboxed environment has no audio capture... phase 09's
  real-browser pass is still the first time anyone actually listens to this." Same for task 5
  (animations): marked "in progress" with the exact missing pieces named, not folded into "done".
  This is what an honest partial-completion entry looks like next to patterns 21/22's dishonest one.

## PR #29 (`feat/boss`), phase 07 — a coordinator-committed last commit that held up, and a citation gap in what a replay actually covers

The largest branch reviewed so far (46 files, 16 commits) and the cleanest on the invariants: no
`com.badlogic.gdx` in `core`, no `Math.random`/clock read, no `Thread`/`ExecutorService`, JSON read
through `JsonReader`/`JsonValue` only, `SystemOrder` extended with two new stages (`SPAWNER`,
`ENEMY_WEAPON`, `BOSS`) each placed with a written reason and none out of order. Worth recording
because it is the reference case for a good last-minute, unreviewed commit rather than a bad one.

32. **A commit finished by the coordinator after a spend-limit cut, with a self-reported test count,
    checked out exactly.** `b33f302`'s message claims "277 tests green at the point of the cut";
    `grep -rn "@Test" core/src/test --include=*.java | wc -l` on the tree as committed returns exactly
    277. The commit itself is otherwise unremarkable: a new component (`EnemyWeapon`), a new stateless
    system (`EnemyWeaponSystem`) mirroring `SpawnerSystem`'s own shape, and `CleanupSystem` emitting
    `EnemyDestroyed` — the first concrete `GameEvent` this codebase has built, closing the empty seam
    pattern 25 named two phases ago. No invariant violated, no shortcut taken under pressure. The
    lesson is not "trust a coordinator-committed cut" but "the self-reported number is free to check
    and worth checking every time" — `grep -c` here, not a rebuild.
33. **A status document can go stale from a commit that lands *after* it, not just from one that
    precedes it.** `docs/plan/07-boss/status.md` (dated the same day) lists "no enemy `weapon`
    component factory exists yet" as an open gap in its own "gap this phase did not close" section —
    true when written, and then the branch's own next and final commit closed exactly that gap.
    Pattern 4 usually fires because memory or status stops being updated; here the *order* inverted
    it — check commit timestamps against a status doc's own date before treating a documented gap as
    still open, in either direction.
34. **A criterion's replay evidence and its unit-test evidence can point at different scenarios, only
    one of which is boss-specific.** The acceptance table cites `BossReplayTest.defeatIsDeterministic`
    for "the boss can... kill the player, to the right screen", but `defeatContent()` in that file
    never calls `.withBoss(...)` — `hasBoss("level-01")` is false for that scenario, so it exercises
    the ordinary, pre-phase-07 rammer-kills-the-player path, not a boss fight. The actual boss-specific
    rule (`DEFEATED` wins a same-tick tie against `bossDefeated`) is genuinely covered, just at the
    unit level (`BossSystemTest.defeatWinsATieWithBossDefeat`, which does call `.withBoss(...)`), not
    by the full-pipeline replay the criteria table cites for it. Worth a follow-up, not a blocker:
    whenever a criteria table cites a "replay" test for a rule, open the fixture builder and check it
    actually configures the subsystem the rule is about, separately from checking that a unit test
    somewhere does.
35. **Content built ahead of a real consumer, correctly labelled as such, in the same commit that
    builds the capability.** `EnemyWeaponSystem` and its `"weapon"` `ComponentFactoryRegistry` entry
    ship in `b33f302`, but `assets/data/enemies.json`'s `enemy-shooter` carries no `"weapon"`
    component — the capability exists and nothing in shipped content uses it yet. Not a defect: this
    is `game`/content's job in a later step, not `core-domain`'s in this commit, and nothing in the
    commit or `status.md` overclaims it as wired. Recorded as the calibration case for pattern 14 (an
    extension seam wired to nothing) — the difference here is the seam *is* reachable through the
    ordinary content pipeline (`ComponentFactoryRegistry.withDefaults()`, not a private static test
    fixture), so it is a gap to close later, not a boundary hole.

Calibration from this phase: the drop-slot fix (issue #23, `SpawnEvent`'s sixth field) and the
`level-01.json` content pass both cross-checked cleanly against their own stated verification —
every `atX`, `dropSlot` and drop-kind claim in `status.md`'s "What was verified" section reproduces
by reading the file. The one place content underclaimed rather than overclaimed: `status.md` flags
`:game:compileJava` as failing before the boss-loading fix landed, and it genuinely did (confirmed by
reading `JsonContentSource` before and after `2ddab6e`) — an honest, checkable "not yet" rather than a
premature "done".

## PR #35 (`ci/github-actions`), phase 09 task 7 — a status document falsified by its own branch's later commit

The workflow itself was clean: no `core`/`game`/`desktop`/`web` source touched, no dependency
added, commit hygiene perfect, and the desktop-build and core-test criteria were confirmed by
reading real GitHub Actions run logs (`:desktop:compileJava/:jar/:assemble/:build`,
`:core:test`, both `BUILD SUCCESSFUL`), not assumed from the YAML. The one defect was in prose,
and it is a new variant of pattern 4/33 worth naming on its own.

36. **A status document's claim is falsified by a *later commit on the same branch*, and the
    correction is skipped even though it was possible.** `docs/plan/09-web-ci-release/status.md`
    said "[the workflow] has never been run on an actual GitHub Actions runner — that only happens
    once the PR is opened", written in commit `4e11d87`. By the time that commit's own push
    triggered a run, the run had already failed (`gradlew: Permission denied`, exit 126) on a real
    runner. The branch's next and *final* commit, `8542034`, fixed exactly that failure — proving
    the claim wrong from inside the same branch — and two more real runs then succeeded. Nothing
    after `8542034` corrected `status.md`. This is pattern 33 in its worse form: that pattern is
    about a gap closing silently after a status doc is written (unavoidable, order is just what it
    is); here the branch's own subsequent commit *disproved* the doc's claim and the author had
    every opportunity to fix the sentence and didn't. The tell is generic and cheap: whenever a
    status doc makes a claim about "has/hasn't happened yet", check it against every commit that
    lands *after* that doc on the same branch, not just against commits that precede it — `git log
    --format="%H %ai %s" <the doc's commit>..HEAD` on the branch is enough, no run logs needed.
    The same sentence, near-verbatim, was also carried into
    `.claude/agent-memory/game-presentation/project_github-actions-ci-shape.md` ("the workflow
    itself is untested until GitHub Actions actually runs it on the opened PR") — confirming that
    pattern 4 (memory contradicting `status.md`) and this new variant are often the *same* wrong
    sentence, copy-pasted into both stores, not two independent errors. Check both files for the
    identical claim, not just one.

Calibration: the coordinator had independently pushed a deliberate failing-test run on a
throwaway branch specifically to demonstrate "CI fails when tests fail" — a criterion this review
had flagged as inferred rather than observed. `290 tests completed, 1 failed`,
`Task :core:test FAILED`, `BUILD FAILED`, confirmed on a real runner before the throwaway branch
was deleted. Recorded here because it is the calibration case in the other direction from pattern
36: a criterion that looked like a reasonable inference from Gradle's own behaviour is still
worth demonstrating directly when the cost of doing so is one throwaway push, and "the tool
guarantees this" is a weaker form of evidence than this project otherwise insists on.

## PR #37 (`docs/readme`), phase 09 task 9 — the README itself was clean; a coordinator's own fix commit left a sibling document stale

Docs-only branch (`README.md` + `docs/plan/09-web-ci-release/status.md`, confirmed by
`git diff main...HEAD --stat`), and every checkable claim in the README reproduced exactly:
289 tests (summed `tests="..."` across `core/build/test-results/test/*.xml` after
`./gradlew core:test --rerun`), port 8080 (not `spikes/web-viability/README.md`'s stale 8181 —
its own `build.gradle.kts` hardcodes `serverPort.set(8181)`, confirming *that* file is wrong, not
the README), every Gradle task name (`desktop:run`, `web:gdx_teavm_web_js_run`,
`web:gdx_teavm_web_js_build -Prelease`) existing verbatim, the controls table matching
`InputAdapter.java` line for line including "Escape releases pointer lock" and the additive
keyboard+mouse rule, and all six architectural invariant claims (no `com.badlogic.gdx` / no
`Math.random` / no `Thread`-family / no `Json` reflection class in `core` or `game`, `game` never
importing `core.domain`, `core/build.gradle.kts` declaring zero dependencies).

37. **A coordinator's correction commit fixed the claim in the document a stranger reads and left
    the identical stale claim in the document only the team reads.** Commit `9406151` (the final
    commit on the branch) rewrote README.md's License section from "no license file yet" to
    "[MIT](LICENSE)" — correct, `LICENSE` (MIT, added by `5c5d610`) really does exist. But
    `docs/plan/09-web-ci-release/status.md`, edited by an *earlier* commit on the same branch
    (`f70c6e3`, timestamped 3 minutes before `9406151`), still asserts in present tense "**Not
    done in this issue: no LICENSE file exists**" after the merge — and `9406151` touched only
    `README.md`, never revisiting the sibling doc that made the same now-false claim. This is
    pattern 33/36 with the fix and the miss in the *same commit*: the author demonstrably knew
    the claim was wrong (that is what `9406151` is) and fixed it in exactly one of the two places
    it appeared. The generic tell from pattern 36 still applies and would have caught this in
    seconds: `git log --format="%H %ai %s" <doc's commit>..HEAD` on the branch, then grep the doc
    for anything that commit range's diff touches.

Calibration, in the direction that matters for judgement: the *other* correction in the same
commit (downgrading "Chrome, Firefox, Edge" to "Chrome and Firefox... verified by hand") is
accurate and complete in the one place it needed to be — the README, the public-facing document —
and status.md never made the Edge claim to begin with, so there was nothing to leave stale there.
One correction was fully propagated, the sibling one was not; the difference was not effort, the
Edge fix and the License fix are the same size, so treat "the coordinator already fixed X" as a
claim to verify per-document, not a fact that generalises across the branch's two docs once
confirmed in one.

## Phase 11a (`phase/11a-rule-asserting-tests`, PRs #100–#107) — a clean branch, and the shape a legitimate "argue with a routing decision" prompt takes

38. **A phase can hold up on every axis at once, and the honest report is "accept."** Checked: the
    `git diff dev..phase -- '*/src/main/*'` claim (exactly `Rng.java` comment lines + the new
    `rngparity` module, confirmed by running the diff myself); the boss-replay geometry in
    `BossReplayTest.podKillSimulation` (recomputed the pod/arm/x-distance arithmetic by hand —
    `[[project_boss-replay-geometry]]` in `test-engineer`'s own memory independently derives the
    same numbers, which is a second, unprompted confirmation rather than trusting one narrative);
    the `alive`-conjunct-is-dead-code finding (read `World.View.outcome()` directly — the early
    `state.lives <= 0` guard really does make the later `&& alive` unreachable-false in both
    branches); every rule-to-test table row in `status.md` for tasks 2–4 against the actual test
    bodies; both architecture-test narrowings (`ALLOWED_JAVA_UTIL_TYPES` against a grep of every
    `java.util.*` import in `core/port`+`core/application`, `DOMAIN_CONTRACT` whitelist against the
    one real `GameEventSink` import) for both under- and over-narrowing. Nothing failed. Do not
    manufacture a finding to justify review effort — this phase's report was "accept, one thing
    worth arguing with" and that was the true state.
39. **A "route this open item to a later phase" decision (D5) can be correct about the concrete
    assertions and still bundle in a separable, format-independent sub-question.** #19 asks two
    things at once: what to assert about `JsonContentSource`'s error messages (genuinely blocked on
    11b's rewrite of that class) and how to unit-test anything depending on `FileHandle` without
    dragging LWJGL into the suite (a test-harness design question, independent of which JSON shape
    11b lands on). D5 routed the whole issue to the 12 group on the first ground without addressing
    that the second question could be resolved earlier. Worth naming as "worth arguing with" rather
    than a defect: nobody had written the harness question down as separable before this reading.

## Calibration from phase 11b task 1 (`feat/entity-lifetime`, PR #116), a clean branch

Recorded so "accept, nothing new" stays calibrated against a real example, not just the rejections.

Every checkable claim in the branch held: the `SAFETY_MARGIN` derivation (`column-3` 44-unit spread x
`enemy-carrier` 15-unit radius -> y=329, 314 from its own edge) reproduced exactly by hand from
`assets/data/formations.json`/`enemies.json` and `SpawnSystem.positionSpawned`'s formula; the escape
rule (no score, no drop, no `EnemyDestroyed`) verified by reading `ScoreSystem`, `CleanupSystem` and
`ComponentStore.remove` and confirmed by a full-pipeline unit test in the same PR
(`anEscapedEnemyGivesNothing`); the two-pass structure in `expireEnemies` genuinely needed (`Component
Store.remove` is a swap-remove that reorders the dense array mid-iteration — confirmed by reading the
class) and the two-pass version has no version of the same bug (pass 2 iterates a separate `ArrayList`,
never the store pass 1 walked); `CleanupSystem`'s "converges uniformly" claim stayed true with zero
change to that class, because the strip happens upstream and both consumers were already conditional
on component presence. Full clean build (`./gradlew clean build --rerun-tasks`) green across all five
modules including `web`; 310 core tests, 0 failures/skipped/errors.

The one thing worth naming as a *pattern to watch for, not a defect here*: a worst-case measurement
combining the extreme value of two independent data files (largest formation spread, largest enemy
radius) is only a real bound if the spawn system actually allows that combination to occur — checked
by reading `SpawnSystem.spawnWave`, which assigns one `enemyId` to every slot of a formation, so any
(formation, enemy) pairing the schema allows is reachable by a future wave even if no current level
uses it. Worth re-deriving on every phase that touches `formations.json`, `enemies.json`, or the
spawn-positioning formula, since the bound silently goes stale if either file's extremes move.

Also worth naming: the `new ArrayList<>()` allocated once per tick in `LifetimeSystem.expireEnemies`
(populated in the near-totality of ticks with nothing) is the same shape as the phase 05 boxed-`Integer`
finding calibrated as noise — thousands of tiny, short-lived allocations across a multi-minute level
against ~10ms/frame of drawing cost. Named, not blocking.

## PR #121 (`feat/wave-spawning`, phase 11b task 4), a hand-off between two agents on the same branch

`SpawnSystem` migrated off a flat timeline cursor onto `ContentSource.placements(String)` +
`wave(String)`, with a genuine mid-task hand-off (first author killed by a spend limit, second
author inherited the uncommitted diff). Verdict: accept. Full clean build
(`./gradlew clean build --console=plain`, `little-spaceship-wave-spawn` worktree) green across all
five modules; `core/build/test-results/test` aggregated to 322 tests, 0 failures/skipped/errors.
`pre-pr-check --base phase/11b-wave-system` reproduced the PR's own pasted output verbatim.

40. **A test can be named after a rule, assert something, and never touch the system that rule
    governs.** `SpawnSystemTest.movingAPlacementEarlierChangesNoOtherOffset` builds two
    `List<WavePlacement>` by hand, reusing the *same* `WavePlacement` object instance in both lists,
    and asserts `assertEquals(untouched, originalOrder.get(1))` — trivially true by object identity,
    regardless of anything `SpawnSystem` does. Confirmed vacuous by scaling `scheduleNext`'s offset
    arithmetic by 1000x in a scratch worktree: the test stayed green. The other three rule-asserting
    tests in the same PR (`fixedDurationEndsExactlyAtItsOwnDuration`, `clearedWaveWaitsForEvery
    EntityToBeGone`, `negativeOffsetOverlapsTwoWaves`) all genuinely exercise `SpawnSystem.update`
    and all went red under a matching one-line break (`>=`→`>` on the duration check, dropping the
    `noEntityCarries` guard, and replacing the do-while re-check loop with a single pass,
    respectively) — so this is not a blanket problem with the phase's tests, just this one. The
    "rule" it half-demonstrates (offset is relative-only, no absolute-position field) is actually a
    true structural property of `WavePlacement` being an immutable record with no such field, and
    needs no runtime test at all — the test should either construct genuinely distinct objects and
    assert on `SpawnSystem`'s emitted spawn order, or be deleted as redundant with the record's own
    javadoc guarantee.
41. **A `default`-with-throw contract method copies the justification of a sibling method
    word-for-word without checking it still applies.** `ContentSource.timeline(String)` was demoted
    from abstract to `default` in this PR, with javadoc citing the same reasoning as `wave(String)`
    ("kept only because `game`'s `JsonContentSource` still overrides it") — but `JsonContentSource`
    *does* override `timeline()` on this branch (confirmed by reading the file), so nothing forces
    the demotion; the module-boundary need that justifies `wave(String)` and the new
    `placements(String)` (grep `implements ContentSource`: `JsonContentSource` has no override of
    either, so an abstract method would break `game`'s compile, a module `core-domain` may not edit)
    genuinely does not apply to `timeline()`. Net effect: this single PR takes `ContentSource` from
    one defaulted method to three, not two as its own PR description counts ("the second defaulted
    contract method") — `placements(String)` is real and justified, `timeline(String)`'s demotion is
    an unforced, minor widening dressed in the justification of its neighbours. Worth flagging every
    time a `default`-throw method's javadoc cites "the same reason as X" — reread whether the actual
    production implementer(s) already provide an override; if they do, the default adds a silent-
    failure mode for a case that cannot currently occur.

Also confirmed correct, worth recording as the positive case: the claimed inherited bug (`update()`
scheduling the level's first placement *after* `levelTime += step`, so `scheduleNext`'s clamp-forward
logic delayed every one of the level's first-wave spawns by one tick) is real in the sense that the
described mechanism would produce exactly that symptom, and the shipped fix — scheduling the first
placement before the tick's own step is added — resolves it without touching the separate `Cleared`
clamp path (`scheduleNext(world, levelTime)` from `resolveEnded`, called only after detection, is
untouched code, confirmed by diff). Reconstructed by hand rather than trusting the commit body: with
the fix, `scheduleNext(world, 0f)` runs while `levelTime` is still its initial `0f`, giving
`start = max(0 + offset, 0)`; only then does `levelTime += step` run, so the first tick's `spawnDue`
computes `localTime = step - 0 = step`, matching the old flat-cursor system's own first-tick check
(`events.at() <= levelTime` after the same increment). Reversing the two statements (increment first,
schedule second) reproduces the claimed bug exactly: `start` would clamp to `step`, not `0`.

## For a second-round PR whose top-level GitHub description predates the fix commit

- **The PR body (`gh pr view --json body`) is not updated automatically when new commits land** —
  it is prose the author wrote once and can leave stale. PR #120 (task 7, phase 11b) round 2 deleted
  the load-time flattening and the `Cleared`-placement rejection its round-1 self had; the fix commit,
  a fresh PR *comment*, and `status.md` all say so correctly, but the PR's top **description**
  (`## What changed` / the JSON example / the verification log) still describes the deleted
  round-1 behaviour verbatim — it was never edited after the round-2 commits. Read `gh pr view --json
  body` *and* `gh pr view --json comments` separately and diff what each claims against the code;
  the top description is what a reader sees first and is not guaranteed to be the current claim.
  Not a code defect here (status.md and the latest comment are accurate, code matches them), but
  exactly the shape of "false statement in a document" the project's own precedent (phase 09's two
  rejections) warns about — flag it for correction even when a later comment already fixes the record,
  because the top description is the part most likely to be read alone. **Recurred in PR #168, phase
  11c** (see below): same shape, same verdict (note, not blocker), so it is a real recurring pattern
  in this project's workflow, not a one-off.
- **`./gradlew build` never runs the actual TeaVM JS/Wasm compile** on this repo — `web:build`
  finishes with `compileTeavmJava NO-SOURCE` and no `dist/js` output. The real check is a specific
  task, `./gradlew :web:gdx_teavm_web_js_build` (list them with `./gradlew :web:tasks --all | grep -i
  teavm`), which does the asset copy (confirms `startup-logo.png` ships) and the actual compile. A
  claim of "the web target still builds" backed only by `:web:build` or the top-level `build` is
  weaker than it sounds; run the JS build task directly when the PR touches anything under `web/` or
  makes a TeaVM-compatibility claim.

## Phase 11b closing group: PRs #124, #125, #127 — the migration's real math checked out, two claims did not

Merged all three into a scratch worktree (`git worktree add ../ls-review-final --detach origin/phase/11b-wave-system`, three plain `git merge --no-edit`s, no conflicts). `./gradlew build` green, 322 core tests / 0 failures, `./gradlew :web:gdx_teavm_web_js_build` green (`waves.json` and `startup-logo.png` both copied into `dist/js/webapp/assets`). Verdict: #124 accept (clean, matches #122 exactly, both implementers confirmed to override both methods, no third implementer). #125 accept on the migration itself, with one real false claim to correct. #127 accept, the falsification claims reproduced exactly.

42. **A "checked every pair, nothing else repeats" claim is disprovable by one `Counter` over the same
    92 events the author had in front of them.** #125/`status.md` claims `enemy-tank`/`single`/`atX
    0.5` (no drop) is "the *only* exact spawn-composition duplicate anywhere in the 92 events." It
    is not: `enemy-rush`/`column-3`/`atX 0.5` (no drop) repeats **six** times (118.5, 195.5, 215.0,
    224.0, 276.5, 293.0), `enemy-basic`/`line-5`/`atX 0.5` five times, plus four more pairs/triples.
    None of this touches the acceptance criterion (still genuinely reused 3x, still an honest,
    correct reuse) or the reconstructed timing (my own independent Python re-simulation, using only
    the placement list's offsets and the waves' durations — no access to the author's script —
    reproduced all 92 original absolute times exactly, `diff` clean). The lesson: "checked every
    block pairwise" is a claim about a search that was run, not about the result being correct;
    counting duplicates by `(spawn, formation, atX, drop, dropSlot)` tuple across the whole file is
    three lines of Python and should be run every time a content migration claims uniqueness, even
    when the *consequence* of the false claim is zero (a stronger reuse example existed and was not
    used).
43. **The acceptance criterion "the determinism replay of level 1 still passes" has no committed
    test that loads real content.** `LevelScoreReplayTest` and `LevelContentIntegrationTest` both
    build content through `TestContent` (grepped, confirmed); `game/src/test` does not exist at all
    (`find game -path "*/test/*"` empty). What actually verified the migration was the author's own
    uncommitted scratch program (`FileHandle` over real `assets/data`, ticking a real `Simulation`
    for 310s) and my own independent reconstruction — neither is a regression net that runs again on
    the next content edit. Worth naming every time a phase's central acceptance criterion is a
    behavioural claim about real JSON content: ask what test the CI actually runs that would fail if
    a future edit to that JSON broke it, separately from asking whether the migration was correct
    *this time*.
44. **A predictive-scheduling rewrite can make an existing boundary check untestable through the
    path that used to expose it.** #127 rewrote `FixedDuration`→`FixedDuration` follower scheduling
    from reactive (`hasEnded` must return true before the next wave is scheduled) to predictive
    (`start + duration.seconds()` computed once, at chain-build time). Mutating `hasEnded`'s
    `FixedDuration` boundary (`>=` → `>`) leaves **all 24** `SpawnSystemTest` cases green —
    `--rerun-tasks` confirmed, not a stale-cache false negative. The reason: once a `FixedDuration`
    chain is scheduled predictively, `hasEnded` for that condition only gates removal from
    `activeWaves` (harmless — the wave's own spawn cursor is already exhausted) and the exact tick
    `world.markWaveTimelineExhausted()` fires; nothing in the suite pins that exact tick for a
    `FixedDuration`-only chain (the one exact-boundary completion test, `completesOnceThe
    TimelineIsExhaustedAndNothingIsAlive`, gives the boundary 0.5s of slack, not zero). Meanwhile the
    two DisplayNames that read as if they test that boundary
    (`fixedDurationEndsExactlyAtItsOwnDuration`, `movingAPlacementEarlierChangesNoOtherOffset`) *do*
    genuinely fail under a **different** mutation — breaking the chain arithmetic itself
    (`start + duration.seconds()` → `start + duration.seconds() - 1f`) fails both. So the tests are
    not vacuous, they just protect a different thing than their exact-boundary framing suggests.
    Whenever a system moves from reactive to predictive scheduling, mutate the *old* boundary check
    on its own, separately from the arithmetic that replaced its job — they can diverge.
45. **A phase's own unmet acceptance-criterion bullet, honestly flagged inside the very PR that
    doesn't close it, still has to be caught at phase-boundary time.** `.claude/agents/
    level-designer.md`'s "a level is a timeline of timestamped spawn events" paragraph is still
    "Not built yet" as of all three PRs (`git diff ... --stat -- .claude/agents/level-designer.md`
    empty on all three) — `status.md`'s own #114 entry says so ("Not this task's to fix, flagged for
    whoever owns it"), correctly. Not a false claim anywhere, but it is a real, plan-listed
    acceptance-criterion bullet with no owning PR in the closing group. When a coordinator says "I
    close the phase after these," re-check the full acceptance list against the merge, not just
    against each PR's own stated scope — an honestly-deferred item from three PRs ago is still open.
- **`status.md` on the phase branch does not gain an entry for #124 or #127.** Neither branch
  touches `docs/plan/11b-wave-system/status.md` (confirmed by `git diff <phase>...<branch> --stat`);
  the only trace of the negative-offset bug and its fix on the phase branch itself is two agent-memory
  commit *messages* ("record the negative-offset overlap fix" / "note wave lookup defaults retired"),
  not a status.md line. Since `status.md` is supposed to be "the only place phase progress is
  recorded" and the plan's own decision text already asserts negative offsets work ("A negative
  offset overlaps them"), a reader of `status.md` alone would not learn that this was broken until
  28/08/2026 and had to be fixed by #126/#127. Not a false statement (status.md says nothing wrong,
  it says nothing at all), but the closing PR set should add the entry rather than leave it to two
  memory-commit subject lines to carry the fact.

## Phase 10d — auditing rules written from inside one lived case (workflow-enforcement phase)

46. **Two sibling scripts that enforce "the same rule" almost never enforce the identical set of
    exceptions, and only running both against the same commit proves it.** `tools/hooks/commit-msg`
    exempts `Merge `, `Revert "`, `fixup!`, `squash!` and an empty/comment-only subject (case
    statement, `tools/hooks/commit-msg:32-34`). `tools/pre-pr-check`'s own commit-hygiene loop
    (step 2, around line 80) only skips `"Merge "*`. A plain `git revert --no-edit` — which the hook
    explicitly documents as legitimate — produces a subject the hook accepts at commit time and
    `pre-pr-check` then rejects before the pull request, with a generic "not a conventional subject"
    error that gives no hint the two checks disagree. Confirmed by actually running
    `git revert --no-edit <sha>` on a scratch branch and then `tools/pre-pr-check`: `FAIL 1 commit
    subject(s) break the convention`. Worse: `docs/plan/10d-enforced-workflow/status/
    137-issue-contract.md:13` states outright that `commit-msg` checks "the same rule
    `tools/pre-pr-check` applies… Merges, reverts, `fixup!`, `squash!` and an empty or comment-only
    message are skipped" — a claim about parity between the two scripts that is false for four of
    its five listed exemptions, written the same way phase 09's "never run on a runner" claim was:
    plausible, unverified, and contradicted by one command. This is the shape to check first
    whenever two enforcement scripts are said to share a rule: read each one's actual exemption list
    side by side, don't trust the sentence that says they agree.
47. **A status-fragment naming check that verifies "the basename matches the issue number" is not
    the same as verifying "the fragment lives under the phase actually being worked."** Neither
    `tools/pre-pr-check` (which only checks presence — by its own comment, deliberately) nor
    `.github/workflows/pr-check.yml` (which matches `docs/plan/[^/]+/status/[^/]+\.md` against any
    phase directory and only checks the basename against the cited issue number) confirms that the
    `[^/]+` phase segment matches the phase branch the pull request targets. Confirmed by
    constructing a branch against `phase/10d-enforced-workflow` that adds
    `docs/plan/11c-movement-shapes/status/999-misplaced.md` and citing "Closes #999" in the body: both
    the real `tools/pre-pr-check` and a line-for-line extraction of `pr-check.yml`'s logic pass it.
    Weaker than #46 (the gap is explicitly narrowed-by-comment in `pre-pr-check`, "checking the name
    against the issue… is pr-check's job" — but `pr-check` doesn't close that gap either), still
    worth naming as a second instance of the same phase's blind spot: a check specified from the one
    case its author tested (issue number matches fragment number) rather than the full shape of
    "illegitimate" (issue number matches, but wrong phase directory).

## Phase 11c round 1 (PRs #168 `feat/movement-state`, #169 `content/movement-shapes`) — two agents in parallel over disjoint files, and a clean pair

The first phase to run `core-domain` and `level-designer` in parallel over `core/` and
`docs/`+`assets/data/` at once, with a real mid-round correction crossing between them (#169's
catalogue landed while #168 was open; #168's author had already added `Trajectory.originX/originY`
on a defensible guess, the coordinator asked for them to come out once the catalogue settled the
question the other way, and they did, same branch, same PR — confirmed via `git log` on the branch
showing the follow-up commits `fix(core): drop Trajectory origin now the shape catalogue refuses
it` immediately after the design landed). Both branches touched only their own module (`git diff
<phase>...<branch> --stat` for #168: `core/` + its own status fragment only; for #169: `docs/`
only, no `assets/data/` written — deliberately, since #163 owns the parser). Both green on real
CI runs (`gh run view <id> --log`, `BUILD SUCCESSFUL` on both `build` jobs). Verdict: accept both,
nothing blocking on either.

48. **A PR's top-level description can describe a design the branch's own later commits reversed,
    while every other record (PR comment, status fragment, code) agrees on the current state.**
    #168's `gh pr view --json body` still says `Trajectory` "carries origin (`originX`/`originY`)
    alongside elapsed time" under a "Decision made where the plan was silent" heading — that was
    true of the branch's first 5 commits, false after the next 3 (`fix(core): drop Trajectory
    origin now the shape catalogue refuses it` and its sibling test/doc commits). The correction is
    recorded faithfully in a PR *comment* ("Update: per the coordinator's instruction, dropped
    `Trajectory.originX`/`originY`...") and in the status fragment, which additionally narrates
    *why* the guess was made and *why* it was reversed rather than hiding the churn — the actual
    `Trajectory.java` on the branch has one field, `elapsed`. This is exactly the shape named as a
    recurring pattern by PR #120's round 2 (see the note above), now confirmed to recur a second
    time in a different phase: the top description is written once and nobody is in the habit of
    revisiting it after a follow-up commit, even when a comment two clicks below it does. Calibrated
    as a note, not a blocker, both times — the description is stale, nothing downstream (code,
    status.md, the latest comment) is.
49. **A status fragment's component-store ordinal claim can be off by a small, harmless amount and
    still be a checkable numeric error worth naming.** #168's `status/161-...md` calls `Trajectory`
    "a fifteenth `ComponentStore<Trajectory>` field" — `grep -c "private final ComponentStore<"
    World.java` on the branch returns 19 (18 pre-existing + this PR's one). Costs nothing
    functionally (the test that actually guards store count, `WorldTest`'s `stores.size() >= 15`
    floor, does not depend on the ordinal claimed in prose), but it is exactly the kind of
    quantitative claim this project's own citation-checking habit exists to catch — count the
    `private final ComponentStore<` lines whenever a status fragment numbers a store by ordinal, it
    is one grep.
50. **A design-fidelity document's derived-quantity arithmetic (turn time, apex depth, screen-exit
    time) is worth reconstructing by hand even when it spans several entries, because the discipline
    generalises and the author's habit of citing exact source lines (radius from `enemies.json`,
    `SAFETY_MARGIN` from `LifetimeSystem.java`) makes it cheap.** Recomputed all of #169's
    `shape-catalogue.md` claims independently: `strike-run`'s turn time (`110/27 = 4.074s`, matches
    "4.07 s") and apex depth (`110²/(2·27) = 224.07`, matches "224 below spawn"); `veer-left`'s
    (`95/20 = 4.75s`, `95²/40 = 225.6 ≈ 226`, both match) and its crossing-x arithmetic at `atX 0.9`
    (`187 - 32·4.75 = 35`, matches "apex x ≈ 35" exactly); the safety-box bounds cited (`x ∈
    [-128, 336]`, `y ∈ [-128, 398]`) against `LifetimeSystem.SAFETY_MARGIN = 128f` and
    `PLAYFIELD_WIDTH`/`PLAYFIELD_HEIGHT` (208/270) — exact match. One inexactness found and judged
    not worth blocking: the stated screen-exit times for `strike-run`/`veer-left` (9.15s/9.85s)
    solve `y = 398`/`x = -128` rather than `y - radius = 398`/`x - radius = -128`, which is what
    `LifetimeSystem.isPastSafetyBox` actually checks — the true exit is a few hundredths of a second
    later (recomputed: `y=402` gives `t≈9.18s`, not `9.15s`). Immaterial to the rule the document is
    proving ("every shape leaves in finite time"), and this project's own calibration (pattern 31,
    the phase 03 sprite-production branch) already established that a design document can check out
    in full — do not manufacture the rounding gap into a defect it doesn't support.
51. **A "phase 12's levels give us no beat list to point a shape at" finding can be reported by both
    halves of a parallel round without either one copying the other, and both halves agreeing is
    itself evidence, not just a coincidence to note.** The coordinator's brief for this round flagged
    this exact gap as "already known, don't spend the audit rediscovering it" — and #169's own
    `shape-catalogue.md` and its status fragment both state it independently and identically ("Phase
    12's levels 2 and 3 justify nothing here, and could not... only half of the issue's... could be
    answered honestly, so all seven entries rest on level 1"), which is the correct, honest response
    to an unanswerable half of an issue's acceptance criterion — the alternative (silently building a
    shape "for phase 12" anyway) is exactly what invariant 6 exists to refuse, and this branch didn't
    take it.

## PR #170 (`feat/movement-shape-content`, phase 11c round 2, issue #163) — a sealed interface actually proven safe under TeaVM, and a clean three-agent branch

The round the parent most wanted a second opinion on: `TrajectoryDefinition` became a `sealed
interface` with a `permits` clause, `core` is what TeaVM compiles, and CI's `build` job runs
`compileTeavmJava` as `NO-SOURCE` (see the closing-group entry above and [[audit-techniques]]'s
"For auditing a TeaVM web dist directory") — so nothing in the PR's own green checks proves the web
target still builds. Settled by actually running the real compile, not by reasoning about the
language feature: `git worktree add` off `origin/feat/movement-shape-content`, then `./gradlew.bat
:web:gdx_teavm_web_js_build` (the task name from the earlier note above), `BUILD SUCCESSFUL`, and
`grep -c ArcTrajectoryDefinition web/build/dist/js/webapp/app.js` returned 20 — the sealed type and
its new permitted record are both really in the compiled JS, not just theoretically compatible.
`WaveEndCondition` (`core/src/main/java/.../core/port/WaveEndCondition.java:17`) really is already
`sealed`, confirming the PR's "mirrors an existing pattern" claim, and this PR is the first time
anyone has actually proven a sealed `core.port` type survives the TeaVM compile rather than assuming
it from `WaveEndCondition` shipping unexamined. **Worth remembering directly: sealed interfaces with
`permits` compile through this project's TeaVM plugin (`com.github.xpenatan.gdx-teavm:1.6.1`) — this
no longer needs re-litigating on a future PR, only re-confirming if the plugin version changes.**

52. **Nothing wrong found, and every checkable claim reproduced.** The eight refused shapes from
    round 1's catalogue (`diagonal`, `logarithmic`, `sine`, `enterAndHold`, `ax`, waypoints,
    formation-relative, player-reading) shipped nothing — no field, no parser branch, no
    `horizontalVelocityAt` (grepped for it directly, zero hits) — and the "nothing points at the new
    entries" claim held (`grep -rln "strike-run\|veer-left\|veer-right" assets/data/*.json` returns
    only `trajectories.json`). `requireOnlyKeys` strictness is pre-existing (already used for waves'
    end conditions), not new machinery invented for this PR, and the four pre-existing trajectory
    entries parse unchanged through it. Full clean `./gradlew build`: 328 core tests, 0
    failures/skipped/errors. `pre-pr-check --base phase/11c-movement-shapes` reproduced the PR body's
    pasted "PASS — 5 commit(s), 8 file(s) changed" exactly (had to `git checkout -b` inside the
    detached worktree first — the branch-name check fails on a bare detached HEAD, a tooling quirk
    worth remembering, not a defect). The `-110 → -56 → -0.002` sequence in both the PR body and the
    status fragment reproduces by hand (`-110+27·2=-56`, `-110+27·4.074≈-0.002`). Recorded as
    calibration alongside PR #22/#26/#31/#08's clean verdicts: an "accept, nothing new" report is
    only worth as much as the TeaVM compile actually run to support it.

## PR #171 (`feat/spawn-shape-id`, phase 11c task 4/issue #164) — the phase's own task, clean

Two agents on one branch, `core-domain` for the binding and evaluation, `game-presentation` for the
loader key, same split as #163. Verdict: accept, nothing found. Recorded as the calibration case for
"velocity becomes a function of elapsed time for the first time" not actually breaking determinism.

53. **A uniform-attach claim ("every entity that gets a Motion now also gets a Trajectory") is
    verified by grepping the one call site of the shared factory method, not by reading its own
    javadoc.** `ComponentFactoryRegistry.attachMotion`/`attachTrajectory` is registered exactly once,
    under `"motion"` in `ComponentFactoryRegistry.withDefaults()`, and only `SpawnSystem` and
    `SpawnerSystem` ever call `attachComponents` at all — the player (`Simulation`), the boss
    (`BossSystem`) and both weapon systems all call `world.motions().set(...)` directly, bypassing the
    registry entirely, so none of them ever gains a `Trajectory` and MotionSystem's per-tick
    re-evaluation loop (which walks `world.trajectories()`, not `world.motions()`) never touches their
    hand-set `Motion`. Confirmed by grepping `attachMotion|attachTrajectory` and `new Motion(` across
    `core/src/main` separately. The "is it really a no-op for constants" half of the same claim is
    provable by mutation-free reasoning once `TrajectoryDefinition`'s two implementations are read: a
    `constant` shape's `verticalVelocityAt` ignores its argument by construction, so re-evaluating it
    every tick is definitionally the same write every time — no probe needed, unlike the boundary
    mutations pattern 44 required.
54. **An "unknown id fails loudly, one level later, by design" claim for a *new* content field is
    fully checkable end to end with two greps, no probe.** `SpawnEvent.trajectoryId` skips validation
    in its own compact constructor (confirmed by reading it — same four checks as before this PR,
    nothing added for the new field) and `JsonContentSource.parseSpawnEvent` never resolves it either
    — the actual throw is `JsonContentSource.require`'s `IllegalArgumentException("unknown " + kind +
    " id '" + id + "'")`, reached through `world.content().trajectory(id)` inside
    `MotionSystem.advanceTrajectories`/`ComponentFactoryRegistry.attachTrajectory`, and
    `Simulation.tick` has no try/catch anywhere in the file — so the exception is genuinely uncaught,
    not merely "loud" in the sense of a logged line. This is the same treatment `enemyId`/`formationId`
    already get (grepped `require(` calls in `JsonContentSource` to confirm the pattern is pre-existing,
    not invented for this field), so the precedent claim holds too.
55. **A phase's own worked numbers (turn time, exact velocity at a stated tick) are the strongest test
    oracle available, and a test that asserts against them is proof against silent accumulation
    drift.** `MotionSystemTest.arcShapeIsFollowedAndCurves` asserts `Motion.vy` at t=5s equals
    `-110 + 27*5 = 25` exactly (0.01f tolerance) — this is `TrajectoryDefinition#verticalVelocityAt`'s
    closed form, and it would NOT match if the implementation instead accumulated `ay * step` every
    tick with float error, or evaluated against a stale `elapsed` (before vs. after the tick's own
    increment). Whenever a "closed form, not accumulation" claim appears, check that the test's
    asserted value is the closed-form result and not just "some value the code currently produces."

Full clean-branch confirmation: `./gradlew :core:test --rerun-tasks` from a fresh worktree, 331 tests
0 failures/skipped/errors (aggregated from `core/build/test-results/test/*.xml`); `DeterminismRulesTest`
and every `*ReplayTest` present and green; `SystemOrder.java` untouched (`git diff` empty for that
file); `./gradlew :web:gdx_teavm_web_js_build` green from the same worktree (this PR touches `game/`,
so worth re-running even though the PR itself only claims "not checked" for the web target — the
claim was appropriately conservative, not wrong); `tools/pre-pr-check --base phase/11c-movement-shapes`
reproduced the PR's pasted "PASS — 4 commit(s), 11 file(s) changed" exactly, after `git checkout -b`
inside the detached worktree (pattern from PR #170's memory entry, still needed). No wave/level/enemy
content references any new shape id (`grep -rln "strike-run\|veer-left\|veer-right" assets/data/*.json`
returns only `trajectories.json`), confirming the "no wave points at a shape yet" claim.

Related: [[audit-techniques]].

56. **Phase 11e's two branches (#203 boss-aimed-attack, #204 enemy-health-numbers) are a second
    clean-branch calibration point, and both status fragments self-flagged their own biggest risk
    instead of waiting for review to find it.** #203's fragment names, unprompted, that collapsing
    spread/sweep into one `fireAimedFan` method "narrows what makes them feel like two" and routes it
    back to task 5's play session rather than asserting the decided "two alternating patterns" rule
    is still intact — a claim I could not fully settle either (it is a rules judgement, not a fact),
    so the honest move was to relay the same open question rather than resolve it either way. #204's
    fragment flags `LevelScoreReplayTest.java:32`'s javadoc as now false without touching the file
    (correctly, since it's `core-domain`'s), and separately flags `bombDamage 50` against the new
    carrier `Health 1000` as dropping the bomb from removing 62% to 5% of a carrier's health — a real
    numeric consequence of the change that the branch's own scope didn't ask it to fix. Both claims
    checked out exactly as stated: `DamageSystem.java:93` genuinely never reads `Collider#fragile` on
    the player-projectile branch (only `:149` ramming and `BombSystem.java:115` detonation do), and
    regenerating `docs/levels/level-01.md` via `node tools/build-level-docs.js` in the branch's own
    worktree produced zero diff. Pattern to reuse: when a fragment volunteers a limitation or a
    downstream consequence unprompted, that is a strong signal of genuine care — verify it like any
    other claim, but it more often survives verification than an unprompted claim of *correctness*
    does.
57. **`Math.sqrt` for vector renormalisation in `core` has a real precedent worth checking rather than
    trusting.** `MotionSystem.java:116` (`scale = cap / Math.sqrt(lengthSquared)`) already does exactly
    this for the velocity cap, so a new use for aim-direction renormalisation in `BossSystem` citing it
    as precedent is checkable with one grep rather than a determinism argument from first principles.
    `Math.sin`/`cos` remain the thing to look for instead — still absent, still the actual TeaVM/JVM
    float-parity risk this project treats as real.

## PR #207 (`feat/fourteen-beat-level-one`, phase 11e task 1) — a content-only branch, clean, and the calibration case for "candidate not verdict"

`assets/data/waves.json` + `level-01.json` + two generated docs + status fragment only (confirmed by
`git diff --stat`), so invariants 1-6 are structurally out of reach. Verdict: accept. Worth recording
because this is a strong example of the honest-partial-completion shape (patterns 21/22/36's positive
case) and because it sharpens when a stated out-of-scope item is a real overstep versus a labelled,
disclosed judgement call.

46. **A per-wave "adapted" claim is checkable by diffing the spawn-tuple list, not by trusting the
    table's prose.** Extracted `(spawn, formation, atX, trajectory)` per wave from both sides of the
    diff with a five-line Python script (`json.load` on `git show <rev>:assets/data/waves.json`,
    compare by wave id). Of nine "adapted" waves, seven were genuine trims (formations and order kept,
    duplicate entries dropped, `atX` raised where the fragment says so). Two went further than a trim
    without the fragment flagging it specifically: `l1-tanks-and-priority` reordered its spawns and
    swapped `vee-5`→`diagonal` for its light entry, and `l1-evolved-shooters` dropped `enemy-light`
    entirely and added an `enemy-basic line-5` in its place. Neither is a violation — the fragment
    never claims "shape kept intact" for either of them the way it does for beat 3, and the beat's
    thematic content survives — but it is the check to run every time a status fragment tables
    kept/adapted/new: extract the tuples, diff them, and see whether the *specific* wave that claims
    a bare "adapted" (no shape-preservation sentence attached) is doing more than trimming.
47. **A candidate branch changing a number an issue's own "out of scope" section assigns to a later
    task is not automatically an overstep — check whether the later task can even be evaluated without
    it.** #198 says "the length... is task 2's and fixed by playing, not here," and the branch moved
    `level-01.json`'s `boss.entersAt` from 302 to 139.5. `boss.entersAt` is independent of the wave
    chain (confirmed by reading the generated doc's own note and `BossSystem`), so leaving it at 302
    was a real option — the branch chose not to. What makes this a defensible judgement call rather
    than a violation: the status fragment states the new number as a proposal with its derivation
    (budget backward from "~3 min, boss included" against the fourteen beats' target durations),
    labels it explicitly "not a verdict: task 2 owns the length," and the plan's own workflow frames
    tasks 1-4 as landing together into one playable candidate for a single play session (#201) — a
    13-wave-then-155-seconds-of-nothing level would not let that session answer the length question at
    all. Recorded as the case to reuse: when a branch touches a value an issue nominally reserves for
    a sibling task, check (a) whether the value is structurally independent of what this issue actually
    builds, (b) whether the branch's own record calls it a proposal or a decision, and (c) whether the
    sibling task could function without this branch having picked *some* value. All three favour
    "accept, note for the coordinator" over "revert before merge" here.
- Also confirmed clean by direct reproduction, not by trusting the write-up: `node
  tools/build-level-docs.js` in the branch's own worktree produced zero diff against the committed
  `docs/levels/{level-01,waves}.md`; every veer placement (`0.88`/`0.12`) satisfies the 11c
  `atX >= 0.75` / `<= 0.25` rule; the swoop-drift arithmetic for the two previously-open Checks
  findings (`l1-carrier-pair`, `l1-finale-a`) recomputed clean inside `0..208` for every remaining
  `enemy-light`/`swoop` spawn in the new content; the per-beat start/end times in the fragment's own
  table match the generated doc to the tenth of a second; `enemies.json` genuinely untouched
  (`git diff --stat`); tank 300 / carrier 1000 / `weaponProjectileDamage 10` confirmed in
  `assets/data/{enemies,balance}.json`, and zero `"cleared"`-type waves confirmed by grep, backing the
  fragment's "every wave is `fixedDuration` on purpose" reasoning. `tools/pre-pr-check --base
  phase/11e-level-one-redesigned` reproduced the PR body's pasted output verbatim, including the real
  `./gradlew build` pass.

## PRs #221 (`fix/pointer-lock-recovery`, #41) and #224 (`feat/shield-attachment-visible`, #43), phase 11f — both accept, one real second-guess finding

Both branches touch `PlayScreen.java` in disjoint regions (#221 the `render()` body, #224 one line in
`show()`); merged both into a scratch worktree (`git worktree add ... --detach phase/11f-web-defects`,
two plain `git merge --no-edit`s) with zero conflicts, and `./gradlew clean build -q` on the merge
result was green across all five modules. Worth recording as the technique: don't reason about whether
two diffs "should" combine, actually merge them.

46. **A same-call read-after-write of a platform-reported flag, right after requesting the change that
    flag reports.** `InputAdapter.managePointerCapture` requests pointer capture
    (`Gdx.input.setCursorCatched(true)`) and, in the same method invocation, falls through to `else if
    (pointerCaptureRequested && !Gdx.input.isCursorCatched())` — which, if the platform's capture grant
    is not synchronous (Pointer Lock in a real browser is asynchronous; a JS `requestPointerLock()`
    call resolves via a later `pointerlockchange` event, not before the calling frame returns), would
    read the not-yet-confirmed state on the very same frame and immediately fire "unexpectedly lost",
    pausing the game the instant the player clicks to engage the mouse. Confirmed the code shape by
    reading the method top to bottom (`if (mouseEnabled && !pointerCaptureRequested && click) { ... }`
    falls through with no `return`/`else`, straight into the `if (escapeJustPressed...) else if
    (pointerCaptureRequested && !isCursorCatched())` pair). Could not confirm or refute the TeaVM/GWT
    backend's actual synchrony for `setCursorCatched`/`isCursorCatched` from this environment (would
    need the gdx-teavm backend source, not present in this repo; searching the Gradle cache for the jar
    timed out) — this is the "unverifiable is not the same as wrong" shape from
    [[audit-techniques]], reported as a suspicion worth a one-line guard (skip the unexpected-loss
    check on the same call that just requested capture) rather than a confirmed defect.
47. **A design document read narrowly by a PR turned out to be read correctly, confirmed by checking
    both the doc and the atlas.** #224 argues `04-hud-layout.md`'s "Invulnerability is shown on the
    ship, not in the plate" is scoped to the three `InvulnerabilitySource` grace periods (respawn,
    damage-absorbed, power-up) and not to `shieldActive`'s own persistent HUD icon, and that no
    shield-ring sprite exists to draw on the ship instead. Both checked out: the document's own "on the
    ship" table lists exactly those three named sources and nothing else, and `assets/atlas/
    sprites.atlas` has `pickup-shield` (the drop capsule) and `icon-shield` (the HUD glyph, now wired)
    but no third, on-ship shield-ring id. A deliberate omission argued from a document is worth
    re-reading against the document before accepting *or* rejecting it — this one held.

Calibration in both directions: keyboard-only play cannot trigger the pointer-loss pause at all
(`pointerCaptureRequested` only ever becomes `true` inside the `mouseEnabled` branch), Escape's
deliberate release and the unexpected-loss branch are mutually exclusive on the same call (`if`/`else
if`, no case where both fire), and skipping `loop.advance` for the lost-lock frame is exactly the
existing pause semantics (`audioDirector.update`/outcome-check already sit inside the same `if
(!paused)` block) — not a new hole in accumulated fixed-step time or the outcome check. Also confirmed:
all six atlas ids the two PRs newly reference exist in `assets/atlas/sprites.atlas` at the lines
`module-satellite:153`, `icon-life:209`, `icon-bomb:216`, `icon-shield:223`, `icon-invuln:230`,
`icon-module:237`; the null-region HUD fallback is whole-widget (icon or full old rect+outline, never
a mix); `WorldRenderer.drawAttachment` adds no new mutable field, reusing the same call's `x`/`y`.

48. **A `pauseGameplay()` guarded by `if (!paused)` makes a second entry point (browser pointer-lock
    loss) safe to call repeatedly without extra guards.** #227 (phase 11f, in-game options) added a
    second panel state (`buildPauseOptionsPanel()`) swapped in place on the same pause `Stage`, and its
    own fragment flagged "does the pointer-lock-loss path re-entering pause while options is open
    double-attach listeners or strand the panel" as unchecked. Traced it: `PlayScreen.render()` only
    calls `input.sample()`/checks `pointerCaptureLostUnexpectedly()` inside `if (!paused)`, and
    `pauseGameplay()` itself no-ops when `paused` is already `true` — so once paused (menu or options
    state), the unexpected-loss branch is never even evaluated again. No double-build, no doubled
    `MenuNavigator`, no dead end. Worth checking this shape on any future PR that adds a second trigger
    for an existing state-guarded method: read the guard first, then trace whether the second trigger's
    call site is itself gated on the same flag.
49. **An excluded feature's justification checked out because of a *pre-existing* gap the author didn't
    even cite.** #227 excluded a live mouse-control toggle from the in-game options panel, arguing it
    "interacts with live pointer-lock state" without pointing at code. Reading `InputAdapter
    .managePointerCapture` found the actual mechanism: pointer-capture release is gated solely on
    Escape (`escapeJustPressed && pointerCaptureRequested`), never on `mouseEnabled` going false — so a
    live toggle-off mid-run would leave the cursor captured/hidden with no release path but pause. The
    exclusion was sound, but the fragment's own reasoning was vaguer than the code justifies; citing the
    actual gate would have made "not decided lightly" into "here is the line that would break."
50. **Octopus-merging two same-phase sibling branches with disjoint file sets in a throwaway detached
    worktree is a fast, real conflict/build check** — `git worktree add --detach <tmp> <phase-branch>`,
    then `git merge --no-edit <branchA> <branchB>`, then `./gradlew build -q`, then
    `git worktree remove --force <tmp>`. Confirms both "do these PRs conflict" and "does the combination
    still build" in one pass without touching either sub-branch's own worktree. `git worktree remove`
    can transiently fail with a Windows file-lock ("Permission denied") right after Gradle touched the
    tree; retrying (or just re-running `git worktree list`) after a couple seconds resolves it — it is
    not a sign the worktree needs manual cleanup.

## PRs #234/#235, phase 11g — a clean pair, and the calibration case for a fully-argued content placement

Merged both into a scratch worktree (`git worktree add ../ls-review-11g --detach phase/11g-shield-and-test-harness`, two plain `git merge --no-edit`s, zero conflicts even though both touch `docs/plan/11g-shield-and-test-harness/status/`, because each writes its own filename). `./gradlew build` green across all modules. Verdict: accept both.

46. **A design-placement argument that cites a pacing table can be checked to the decimal, and here every number was right.** #234's reasoning (drought length, which placement is the first density spike, which slot is the formation's centre, which x that resolves to relative to the player's start x) all reproduced exactly from `docs/levels/level-01.md`'s own generated tables — density 1.77/s at placement #4 genuinely is the highest of the level up to that point and genuinely isn't exceeded again until 88.0s (1.91/s); `dropSlot 1` on a `line-3` genuinely is the middle slot (`(-20,0)(0,0)(20,0)`); atX 0.30 genuinely resolves left of the player's x=104 start. Worth naming as the counter-example to keep pattern 31's calibration point alive in the content-authoring family too, not just the art-only branch it was first observed on.
47. **The two mechanical claims behind a design rejection (`Shield` has no durability; `LifetimeSystem` strips `Drop` from an escaping enemy) are one grep and one class-read each, and are worth doing even when the prose reads as obviously true** — both held exactly as stated (`Shield` is a bare marker class; `LifetimeSystem.strip` removes `ScoreValue`/`Drop`/`Collider` before `markForDestruction`). A rejection argument stands or falls on claims like these, not on the placement chosen.
48. **A harness's "no dependency added, single-threaded, no clock" claim and its "the test can actually fail" claim are independently and cheaply checkable without rebuilding the author's steps.** Reading `FakeInput`'s `invoke()` confirmed the proxy answers only named methods and returns the type's JDK default (false/0/null) for everything else — the exact shape the project's own memory warns can make a test pass for the wrong reason, but here the two real assertions (`keyboardAloneReachesTopSpeed`, `keyboardAndMouseCancelExactly`) both route through methods the proxy actually implements, and the arithmetic behind "cancel exactly" (`mouseX = -140*(208/208)/1 = -140`, `keyboardX(140) = +140`) reproduces on paper from `InputAdapter.sample`'s real formula, not from the test's own comment. The 140f→999f falsification claim is plausible on inspection (the assertion is a direct `assertEquals`, no rounding or pooling in the way) — not independently rerun this session, so that half stays "plausible, not independently reproduced" rather than "confirmed."

Calibration in the other direction, worth keeping: no invariant-1 hit (`grep -rn "com.badlogic.gdx" core/src/main` first returned a spurious hit against `Rng.java` from a truncated `head` pipe; a clean, untruncated regrep of that exact file found nothing — when a grep result looks surprising, rerun it without piping through `head` before trusting it as a finding).
