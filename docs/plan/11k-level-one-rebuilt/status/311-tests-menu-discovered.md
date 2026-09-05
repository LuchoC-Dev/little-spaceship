# Task 3 — the TESTS list is discovered from `assets/data/test-*.json`

Issue [#311](https://github.com/LuchoC-Dev/little-spaceship/issues/311).

## What changed

`TestScenarios.ALL` (a hardcoded `List.of(...)` literal, fourteen entries by phase 11j) is gone.
`TestScenarios.all()` now discovers every `test-*.json` file directly under `assets/data/` at
runtime — through `FileHandle#list(".json")` on `Gdx.files.internal("data")` — and builds a
`Scenario` for each, sorted alphabetically by level id. `TestMenuScreen` calls `TestScenarios.all()`
instead of the old field. Adding a scenario file under `assets/data/` and nothing else now puts it
in the TESTS menu, closing the gap #301 needed a whole second pull request for.

`ALL` became a method (`all()`), not a field, because a field's initializer runs the moment the
class is loaded — which happens the moment a test references *any* member of `TestScenarios`,
including the package-private `discover(FileHandle)` the test exercises directly. An eager field
calling `Gdx.files` failed every test with `ExceptionInInitializerError` in the headless JUnit
process, regardless of which method the test actually called. Discovered by running the test suite
once with the field still eager (all ten failed with the same cause) and fixed by making it lazy.

## The label decision, and why

The plan's own text raised two ways to keep the `LINE:`/`PATH:`/`ABS:` prefix #301 decided: derive
it from a new level-schema key, or derive it from the trajectory the scenario places. Took the
second, as recommended: `JsonContentSource.requireOnlyKeys` needed no change, and the label comes
out right by construction from content that already exists. Concretely:

- If the level file has a `"boss"` key, the label is `BOSS`, no trajectory lookup needed.
- Otherwise, the first spawn (across the level's waves) that carries an explicit `"trajectory"` key
  decides the prefix: `LINE` for a trajectory with no `"type"`, `PATH` for `"type": "path"` with
  `"segments"`, `ABS` for `"type": "path"` with `"waypoints"`, and `ARC` for `"type": "arc"` — a
  fourth prefix #301 never needed because no scenario used an arc yet. `"mirrorOf"` and `"speedOf"`
  are followed to whatever they ultimately derive from, the same way
  `JsonContentSource#resolveDerived` does, with a cycle guard that gives up rather than looping.
- If no spawn overrides its trajectory (an ordinary wave, e.g. `test-wave-04`, which plays its
  archetypes' own default movement), the label falls back to the id itself, uppercased with dashes
  turned into spaces — `test-wave-04` reads `WAVE 04`, not the old hand-picked `WAVE 4`.
- Label derivation never throws. A malformed or missing reference falls back to the id-derived name
  instead of failing the whole menu — the scenario's actual content is still validated, loudly, by
  `JsonContentSource` the moment it is opened; this class only decides what to print on a button
  before that happens.

This reads raw `trajectories.json`/`waves.json` with `JsonReader`/`JsonValue` directly, not through
`JsonContentSource`'s resolved `TrajectoryDefinition` objects — a resolved `PathTrajectoryDefinition`
cannot tell `PATH` from `ABS` apart, since both authoring forms compile down to the same
`PathSegment` list by design (11j). The raw JSON's `"segments"` vs `"waypoints"` key is the only
place that distinction still exists.

Checked against every scenario the repository ships today (`node`-style manual trace, not a script):
`LINE: CROSS`, `PATH: TURN`, `PATH: MIRROR`, `PATH: WAIT`, `PATH: LOOP`, `PATH: OSCILLATE`,
`ABS: HOLD LINE`, `BOSS`, `WAVE 04`/`WAVE 09`/`WAVE 12` all come out identical or equivalent to the
old hand-picked labels; `PATH: SLIDE DESCEND`, `PATH: DIVE RETREAT` and `ABS: SWEEP WIDTH` are longer
than the old shorthand (`PATH: SLIDE`, `PATH: RETREAT`, `ABS: SWEEP`) but not wrong — this is a
finding for the project owner, not a defect: whether the longer, fully-derived names read worse on
screen is theirs to judge, not checked here.

## Ordering: alphabetical by level id, not "newest first"

#291 decided a hand-ordered stack, newest entry on top, precisely because a human was choosing where
each new line landed. Discovery removes that human, and nothing about a filesystem listing carries
"when was this added" in any form this project can read deterministically — file modification time
resets to checkout time on a fresh clone and is not the same across machines, which is exactly the
kind of filesystem-dependent non-determinism invariant 2 rules out for the simulation and this
sorts the same way. `FileHandle#list()`'s own iteration order is the filesystem's, unspecified and
different across platforms, so the result is explicitly sorted by `Scenario#levelId()` afterward
regardless of what order `list()` returns — confirmed by a test that writes fixture files in
z-a-m order and asserts the discovered list comes out a-m-z.

## The web-target risk, read and accepted rather than fixed

Read `backend-web-1.6.1-sources.jar` (`com.github.xpenatan.gdx-teavm:backend-web:1.6.1`) out of the
Gradle cache before writing any of this, per the plan's own advice. `WebFiles.getFileDB(FileType.Internal)`
returns an `InternalStorage extends MemoryFileStorage`, and `MemoryFileStorage#list()` walks an
internal `OrderedMap<String, FileData>` that only `writeInternal` (used by `FileType.Local`, i.e.
browser local storage) is ever seen populating in that jar — nothing populates it for a preloaded
internal asset. The actual asset-copy step observed during `:web:gdx_teavm_web_js_build` (every file
under `assets/` listed individually as `Copied [Internal] ...`) is a build-time manifest the TeaVM
plugin bakes in, not something `list()` reads back at run time. This matches `JsonContentSource`'s
own already-documented claim that `FileHandle#list()` "has no answer for the web target's asset
packaging" — I found nothing in the backend to contradict it and one more piece of evidence for it.

Accepted rather than avoided, for three reasons: this class is compiled only under `-Ptests`, which
has never been combined with a `:web` build; on desktop `FileHandle#list()` is backed by a real
`java.io.File#listFiles()` and behaves exactly as needed; and this task's own acceptance criterion is
that the `-Ptests` build *compiles*, not that it runs correctly under TeaVM. If a future phase wants
this flavour on the web target, discovery needs to move to build time (a generated source, the way
the level documents are generated) rather than trusting this backend's `list()` at run time — written
down as an open item below rather than solved here, since solving it is out of this task's scope.

## New test infrastructure

`game/build.gradle.kts` gained a second conditional source directory, `game/src/testsTest/java`,
added to the `test` sourceSet only when `-Ptests` is present — mirroring the existing `main`
sourceSet toggle for `src/tests/java`/`src/teststub/java`. Needed because `TestScenariosTest`
references `TestScenarios` directly, which does not exist as a compiled class outside `-Ptests`, so
an unconditional `src/test/java` file would break `./gradlew :game:test` for everyone else.

`TestScenariosTest` (`game/src/testsTest/java/.../screen/TestScenariosTest.java`) exercises
`TestScenarios.discover(FileHandle)` against a `@TempDir` fixture directory, never against the real
`assets/data/` — ten tests: prefix filtering, alphabetical ordering regardless of file creation
order, each label kind (`BOSS`, `LINE`, `PATH`, `ABS`, `ARC`), a mirrored trajectory taking its
target's kind, a wave with no trajectory override falling back to its id-derived name, and a
cyclical `mirrorOf` pair falling back rather than looping forever.

## Verified

- `./gradlew :game:compileJava -Ptests` — `BUILD SUCCESSFUL`.
- `./gradlew :game:test -Ptests` — `BUILD SUCCESSFUL`, all ten new tests plus the existing suite
  green.
- `./gradlew :game:clean :game:test` (no `-Ptests`) — `BUILD SUCCESSFUL`, and
  `find game/build/classes -iname "*TestScenarios*"` printed nothing: the ordinary build compiles
  none of it, exactly as before this change.
- `./gradlew :web:gdx_teavm_web_js_build` (no `-Ptests`, the shipped configuration) —
  `BUILD SUCCESSFUL`; `grep -c "TestMenuScreen\|TestScenarios" web/build/dist/js/webapp/app.js`
  printed `0`. The shipped web build still contains none of it, same proof 11h used.
- `./gradlew :desktop:run -Ptests` — launched, reached a running LWJGL3 window titled
  `little-spaceship`, no exception in the log (only the usual LWJGL/JDK native-access warnings seen
  on every desktop run in this environment). Killed once confirmed running, per "running the game is
  not playing it" — never navigated the menu, never opened a scenario.
- The discovered order and every label: read from the code and asserted by `TestScenariosTest`, not
  observed on screen, per this task's own instruction.

## Acceptance criteria

- [x] Adding a scenario file under `assets/data/` and nothing else puts it in the TESTS menu —
  `discover` reads the directory itself; no code names any scenario by id anymore.
- [x] The stack order #291 decided survives in spirit, not literally — replaced by an explicit,
  deterministic alphabetical order, argued above and asserted by a test with adversarial input
  order.
- [x] The order is read from the code / asserted by a test, not observed on screen.
- [x] The `-Ptests` build still compiles; the shipped build still contains none of it — proven by
  the real TeaVM compile and a grep of the emitted `app.js`, as above.

## Open items this leaves behind

- Discovery depends on `FileHandle#list()`, which is read-confirmed unreliable on the web target's
  `FileType.Internal` storage. Not a problem today because `-Ptests` is desktop-only in practice, but
  it would need to move to a build-time generated source before this flavour could ever target
  `:web`.
- The auto-derived labels for three existing scenarios (`test-slide-descend`, `test-dive-retreat`,
  `test-sweep-width`) are longer than the hand-picked ones they replace. Not fixed here: shortening
  them would mean either renaming the files (a `level-designer` concern) or reintroducing a
  hand-maintained short name (exactly what this task removes). Left for the project owner to judge
  on screen.
