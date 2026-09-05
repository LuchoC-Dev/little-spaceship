# Phase 11j — Paths written where they happen, and a speed that does not resize them · status

**State:** **complete on `phase/11j-absolute-paths`, and open as a pull request against `dev`.** Seven issues closed through seven pull requests, all merged into the phase branch on 04–05/09/2026, CI green. **Not merged into `dev`** — that waits on the project owner's direct approval on that pull request, **and on their playing the five new trajectories**, which is this phase's last acceptance criterion and the one no test can stand in for.
**Updated:** 05/09/2026

This file holds the phase's `State:` line and its narrative, and the coordinator writes it — at the phase's opening and at its close.

**Per-task progress does not live here.** It lives in `status/`, one file per task, written by whoever did that task on its own branch. Six fragments.

## Why this phase existed

**Its agenda came out of the project owner playing phase 11i**, not out of a plan — the second time in this group that a play session wrote the next phase.

Having approved the path vocabulary, they worked out the consequence of the format themselves: a segment is velocity × duration, so raising speed **scales the shape** — an L becomes a bigger L with the same angles. Useful as a source of variations, and it makes "enter at the top centre and leave to the right" expensive to author, because the values have to be computed backwards before they can be written.

Their proposal was to keep the system relative underneath and put an absolute form on top. **That turned out to cost nothing in `core`**, exactly as predicted.

## Done

| Task | Issue | What | PR |
|---|---|---|---|
| 1 | [#287](https://github.com/LuchoC-Dev/little-spaceship/issues/287) | The absolute authoring syntax | [#290](https://github.com/LuchoC-Dev/little-spaceship/pull/290) |
| 2 | [#296](https://github.com/LuchoC-Dev/little-spaceship/issues/296) | The speed multiplier | [#298](https://github.com/LuchoC-Dev/little-spaceship/pull/298) |
| 3 | [#297](https://github.com/LuchoC-Dev/little-spaceship/issues/297) | Seven trajectories and five scenarios | [#299](https://github.com/LuchoC-Dev/little-spaceship/pull/299) |
| — | [#291](https://github.com/LuchoC-Dev/little-spaceship/issues/291) | The TESTS menu as a stack, newest first | [#292](https://github.com/LuchoC-Dev/little-spaceship/pull/292) |
| — | [#293](https://github.com/LuchoC-Dev/little-spaceship/issues/293) | The menu losing its first entry after scrolling | [#295](https://github.com/LuchoC-Dev/little-spaceship/pull/295) |
| — | [#301](https://github.com/LuchoC-Dev/little-spaceship/issues/301) | The five new scenarios listed in the menu | [#302](https://github.com/LuchoC-Dev/little-spaceship/pull/302) |
| — | — | A criterion that needs playing is badly written | [#294](https://github.com/LuchoC-Dev/little-spaceship/pull/294) |

`reviewer` audited the two planned tasks that ran in this session — #298 and #299 — and accepted both, each with corrections applied by the coordinator before merge because the workers were already closed. #287 and the two menu defects were audited by the coordinator; each fragment says which.

## What the phase built

**An absolute authoring syntax.** A `path` entry declares **exactly one** of `"segments"` (relative: a velocity held for a duration) or `"waypoints"` (absolute: an entry point, then destinations with a speed, plus the same `wait` shorthand). Declaring both or neither fails at load naming file and id. Resolved in `game/adapter/content/JsonContentSource.java`, handing `core` the same `PathSegment(vx, vy, duration)` it already took — **no new `core` API**, exactly as the reparameterization predicted.

**The kind of a path is answered by its key, once, at the top**, rather than leg by leg. Mixing the two forms inside one path is refused structurally, not merely validated — the risk the plan named as the single biggest thing this phase could get wrong.

**A speed multiplier, resolved at load.** `{"id": "x-fast", "speedOf": "x", "multiplier": 2}` — a trajectory declared as another one traversed faster. Velocities up **and** durations down, so the geometry is identical and only the traversal time changes, which was the project owner's decision and not the other meaning. It costs one named entry per speed. `mirrorOf` and `speedOf` compose in either order, and a cycle is refused naming the chain.

**The spawn-event alternative was argued and not built.** It buys a genuinely different capability — one shape at two speeds inside one wave — and it would need a `SpawnEvent` field and a `SpawnSystem` change, i.e. `core`'s module. Nothing written asks for it, so invariant 6 refused it. The two would compose later: it is the same arithmetic at a different moment.

**Seven trajectories, five of them authored shapes and two `mirrorOf` lines**, plus a scenario each — five scenarios, since a mirror shares its original's, as 11i decided. **One entry per capability the existing twelve could not express, not one per beat.** Two are `constant`, two are absolute `waypoints`, none is a new `arc`. **Nothing was deleted and level 1 was not touched**: the diff over `trajectories.json` and `waves.json` removes no line at all.

## The result worth keeping

**An `arc`'s `ay` takes the square of the multiplier while its velocities take the first power.** `verticalVelocityAt` is `vy + ay·t`, integrating to `vy·t + ay·t²/2`; matching the fast trajectory at `t` to the original at `k·t` needs `vy_f = k·vy` and `ay_f = k²·ay`. Scaling `ay` linearly gives a **different parabola that still looks plausible** — the kind of error that survives a careful reading and only a traced geometry test catches. `reviewer` reproduced the mutation: turning `faster` into a `scale` reddens exactly the five geometry tests, and no others.

## Two defects the audits found, both in prose, both corrected before merge

Neither changed a line of JSON or a rule, and both are recorded in their own fragments.

- **A javadoc naming a guard that never fires.** #298 claimed an absurd multiplier is refused because it *underflows a duration to zero*. It is not: `vy * multiplier` overflows to infinity and trips `PathSegment`'s finiteness check on the **velocity**, long before a duration could underflow. The test asserting it was named after the wrong mechanism too. **The behaviour was always right; only the explanation was wrong** — and the syntax comment posted on the issue had hedged it correctly. The narrow, false version appeared only when it was written into the code.
- **Five removal times early by exactly one collider radius.** #299's descriptions applied the radius correctly to spawn heights and edge crossings and dropped it from `isPastSafetyBox`, which tests `transform.x + radius` against the margin — an entity is removed once its *edge* clears the box. `reviewer` found it by closed-form algebra; the coordinator re-derived it by integrating the real content at the game's own 1/60 step. **Every corrected number is later than the one it replaced**, so no wave's `fixedDuration` claim was weakened — there was more slack than the file said, not less.

**Both were caught because task 3 was required to write down what each trajectory should look like.** That instruction has now produced a finding in two consecutive phases, and in both the JSON was right and the prose was wrong.

## Decisions taken while implementing

- **The multiplier applies to all three kinds**, not only to `path`. "The same trajectory run faster" names no kind, and `arc` is the only curved motion in the game. Neither the plan nor the issue said which — a gap the task reported rather than papered over.
- **A derived trajectory is a named entry, not a modifier on an existing one.** That is the actual syntax decision, and it fell out of the load-time choice rather than being stated anywhere. At this scale it is a feature: 11k reads `trajectories.json` as a vocabulary, and "this wave uses `dive-fast`" reads better than a number on a spawn event.
- **A menu label's prefix names the authoring form**, not the word "path": `LINE:` for a `constant`, `PATH:` for relative segments, `ABS:` for absolute waypoints. `test-cross` is a `constant` on purpose, and the two absolute entries are what the phase exists to build; a shared prefix hid both facts in the one place the project owner reads while choosing what to open.

## What is open

- **[#300](https://github.com/LuchoC-Dev/little-spaceship/issues/300) — nothing checks that an absolutely-authored path is placed at the `atX` it was written for.** Raised by task 1, which argued it and deliberately did not build it, then answered by task 3, which hit it in practice: **it belongs in `tools/build-level-docs.js`**, which already reads waves and trajectories together and already fails a pull request on a bad swept extent — not in `JsonContentSource`, which parses the two files with no cross-reference. It costs nothing today, because every absolute path was authored and placed by the same task. **It starts costing in 11k**, which is exactly the phase where one person writes them and another places them.
- **The absolute form is absolute in `y` only modulo the collider radius.** A slot is born at `270 + radius`, so a waypoint written `y: 190` is flown at 196.5 on `enemy-shooter`. Measured, documented in every description, unchecked by anything.
- **[#280](https://github.com/LuchoC-Dev/little-spaceship/issues/280) — a loop is always a path's tail.** Unchanged by this phase; still no drawn shape needs it.
- **`JsonBalanceValues`'s other twenty fields** are still unverified against a parsed fixture. Carried from 11i.
- **Whether `tools/pre-pr-check` should run `node tools/build-level-docs.js`.** Carried from 11i, where four red runs proved the gap. It did not bite this phase: task 3 regenerated, and the generator prints `unchanged` for both documents on the closing tree.
- **Whether the TESTS list should be discovered from `assets/data/test-*.json`.** The plan asked whether this phase changed that arithmetic. **It did.** Nine entries became fourteen; a content task shipped five scenarios it could not list, needing [#301](https://github.com/LuchoC-Dev/little-spaceship/issues/301) — a whole issue, branch and pull request in a second module to finish work already done. And **no test can cover the list**: `TestScenarios` lives only in the `-Ptests` source set, which `game`'s test source set is not compiled against, so "every id in `ALL` has a file" had to be run by hand as a shell loop. Three costs measured now, not one.

## What has not been checked, and by whom it must be

**The project owner has not played the five new trajectories.** Every mechanical criterion passes — `./gradlew build`, CI, the document generator printing `unchanged`, the traced geometry test, the fourteen menu ids each resolving to a file. **None of that is the criterion.** Whether the shapes read the way their descriptions say they do is the owner's verdict, and this phase itself wrote the rule that says so: `how-to-run-a-phase.md`'s "And the half that is the coordinator's", added after 11j broke the no-playing rule twice — the second time because a coordinator wrote a criterion that could not be met without scrolling a menu.

Open the game with `-Ptests` and look at `LINE: CROSS`, `PATH: SLIDE`, `PATH: RETREAT`, `ABS: HOLD LINE` and `ABS: SWEEP`, at the top of the TESTS menu. `status/297-level-one-trajectories.md` says what each should look like, written from the JSON rather than from intent — that file is what the screen is checked against.

## What comes after

**Phase 11k — level 1 rebuilt on these trajectories**, which is what the project owner wanted from the start and what all of 11h, 11i and 11j were built to make possible.
