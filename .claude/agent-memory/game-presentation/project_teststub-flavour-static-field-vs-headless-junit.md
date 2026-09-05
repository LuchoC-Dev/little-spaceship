---
name: teststub-flavour-static-field-vs-headless-junit
description: why TestScenarios.ALL had to become a method, and how a second -Ptests-only source directory lets a test compile against a class that does not exist outside that flavour
metadata:
  type: project
---

Issue #311 (11k task 3): `TestScenarios`'s hardcoded `List.of(...)` became a discovery call,
`FileHandle#list(".json")` on `Gdx.files.internal("data")`, sorted alphabetically by level id
(mtime is unusable for "newest" — it resets to checkout time on a fresh clone, so it is exactly the
kind of filesystem-dependent non-determinism invariant 2 rules out for the simulation, and the same
reasoning applies here). See [[trajectory-mirroring-and-core-exceptions-without-ids]] for the
`mirrorOf`/`speedOf`-following pattern this class's label derivation copies from
`JsonContentSource#resolveDerived`, read-only, against raw `JsonValue` rather than resolved
`TrajectoryDefinition` objects — resolved `PathTrajectoryDefinition` cannot tell a `segments`-authored
path from a `waypoints`-authored one apart, since both compile down to the same `PathSegment` list by
design (11j), so the `PATH:`/`ABS:` label distinction needs the raw JSON specifically.

**A static field's initializer runs at class load, not at first genuine use of that field.**
Turning `TestScenarios.ALL` into `static final List<Scenario> ALL = discover(Gdx.files.internal(...))`
broke every test in a new `TestScenariosTest`, even ones that only ever call the package-private
`discover(FileHandle)` method directly and never touch `ALL` — referencing *any* member of a class
loads it, which runs every static initializer in source order, and `Gdx.files` does not exist in a
headless JUnit process (no `Gdx.app` bootstrapped, see
[[headless-libgdx-verification]]). Fixed by making it a method, `all()`, called only from
`TestMenuScreen`; discovery itself stays a separate, directly-testable static method that never
touches `Gdx` except through the `FileHandle` a caller (real code or a test) already supplies.

**Extended the `-Ptests` flavour to the `test` sourceSet, not just `main`.** `TestScenariosTest`
references `TestScenarios`, a class that plain `./gradlew :game:test` never compiles (see
[[gradle-source-set-flavour-for-code-absence]] for why `main` is split this way already). Added a
second conditional `sourceSets { test { if (testsFlavour) java.srcDir("src/testsTest/java") } }` in
`game/build.gradle.kts`, mirroring the existing `main` toggle — an unconditional `src/test/java` file
referencing `TestScenarios` would have broken `./gradlew :game:test` for everyone not passing
`-Ptests`. Verified by `./gradlew :game:clean :game:test` (no property): builds green, and
`find game/build/classes -iname "*TestScenarios*"` prints nothing — the ordinary test run neither
compiles nor runs `TestScenariosTest`.

**`FileHandle#list()` on the web target: read the actual backend jar rather than trust the existing
"has no answer" comment blindly, then still sided with it.** Extracted
`backend-web-1.6.1-sources.jar` (`com.github.xpenatan.gdx-teavm:backend-web:1.6.1`, in the Gradle
module cache) and read `WebFiles`/`MemoryFileStorage`/`InternalStorage`: `FileType.Internal` is
backed by an `OrderedMap` that only `writeInternal` (the `FileType.Local`/browser-storage write path)
is ever seen populating — nothing in that jar populates it for a preloaded internal asset, so
`list()` on the web target most likely returns nothing there. This is one concrete piece of evidence
behind `JsonContentSource`'s existing javadoc claim (see [[filehandle-list-avoided-for-teavm]]); it
does not fully prove the negative (some other class in the dependency graph could populate it and
wasn't found), but it is enough to make the same call that class already made. Accepted the risk
here rather than avoiding `list()` altogether because this code is `-Ptests`-only, has never shipped
combined with `:web`, and the task's own acceptance criterion was "the `-Ptests` build compiles," not
"runs correctly under TeaVM."
