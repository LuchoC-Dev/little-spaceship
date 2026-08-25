---
name: defect-patterns
description: The recurring shapes of defect found when auditing this repo — where to look first in a phase review, including second-round, data-driven, seam and design-fidelity passes
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
is state an omission once in prose while the summary sentence beside it reads as complete. That is
the residual shape in this author's status documents, and it is a note, never a rejection.

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
is state an omission once in prose while the summary sentence beside it reads as complete. That is
the residual shape in this author's status documents, and it is a note, never a rejection.

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
