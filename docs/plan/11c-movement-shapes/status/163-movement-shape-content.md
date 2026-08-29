# 163 — a movement shape is named content

**Half written by `core-domain`.** `level-designer` appends its own half below, for the
`assets/data/trajectories.json` entries, once this half has landed.

## What was built

`core/port/TrajectoryDefinition.java` is now a sealed interface, mirroring `WaveEndCondition`'s
pattern, permitting exactly two records:

- `SimpleTrajectoryDefinition(id, vx, vy)` — unchanged constructor and validation, now documented as
  the catalogue's `constant` shape. `velocityAt` ignores elapsed time.
- `ArcTrajectoryDefinition(id, vx, vy, ay)` — new, the catalogue's `arc` shape. Rejects a missing id
  and any non-finite `vx`/`vy`/`ay`.

Both implement a new method, `float verticalVelocityAt(float elapsedSeconds)`: `constant` returns
`vy` unconditionally, `arc` returns `vy + ay * elapsedSeconds` — the closed form the catalogue's "For
#163" section asked to be picked deliberately rather than accumulated tick by tick. `vx()` stays
time-independent for both kinds, per the catalogue's refusal of horizontal acceleration, so no
`horizontalVelocityAt` was added.

`ComponentFactoryRegistry.attachMotion` is unchanged in behaviour — it still snapshots a trajectory's
velocity once, at spawn, into `Motion`. Its javadoc now says explicitly that this snapshot does not
keep re-evaluating an `arc`'s vertical velocity after spawn, and names #164 as the issue that wires a
`Trajectory` component and a per-tick re-evaluation into `MotionSystem`. This PR does not touch
`MotionSystem` at all — no `SystemOrder` change, per invariant 5 and the acceptance criteria.

No `ContentSource` method signature changed: `trajectory(String id)` already returned
`TrajectoryDefinition`, so both kinds resolve through the one existing lookup.

Tests added to `ContentDefinitionsTest`: the constant shape ignores elapsed time; `ArcTrajectoryDefinition`
validates id and every parameter's finiteness; the closed-form formula is checked against the
catalogue's own `strike-run` numbers (turns at `t = -vy/ay = 4.07s`); `ay = 0` degenerates to a flat
line without being routed through `SimpleTrajectoryDefinition`.

## The exact JSON shape expected

For a `constant` entry (unchanged, the four that already ship):

```json
{ "id": "slow-descent", "vx": 0, "vy": -18 }
```

For an `arc` entry:

```json
{ "id": "strike-run", "type": "arc", "vx": 0, "vy": -110, "ay": 27 }
```

`"type"` is optional and defaults to `"constant"` — the four existing entries need no edit.

## What is NOT done, and why — a gap in the plan worth flagging

**`game/JsonContentSource.loadTrajectories` was not touched.** It still reads exactly `id`, `vx`,
`vy` and ignores an unknown key, so today an `arc` entry would load silently as a `constant` — the
exact trap the plan's "Watch out" section and the shape catalogue's own "For #163" section describe,
and the shape catalogue says outright: "The entries land in the same pull request as the parser that
reads them."

`JsonContentSource.java` lives in `game/`, which is `game-presentation`'s module, not `core-domain`'s
— my own agent boundary is "you write only inside `core/`", reinforced by three earlier memory entries
("not mine to touch") on this exact file. The task's own scope line ("`core/` only — the `core.port`
contract, the `ContentSource` lookup and the JSON reading") names JSON reading as in-scope, but the
only place `JsonReader`/`JsonValue` exist in this repository is `game/` — `core` has no libGDX
dependency, checked (`core/build.gradle` declares nothing beyond the JDK). I judged the module
boundary in `CLAUDE.md`'s agent table to outrank the task's own scope wording, which reads as
imprecise rather than a deliberate widening.

**Consequence:** the parser update — reading `"type"`, defaulting to `"constant"`, constructing
`ArcTrajectoryDefinition` for `"arc"`, and rejecting any other value loudly (naming the trajectory id
and the bad type) — still needs to land before `level-designer` adds `arc` entries to
`trajectories.json`, or those entries load silently wrong. This is the change needed, spelled out so
whoever picks it up does not have to re-derive it:

```java
private void loadTrajectories(JsonReader reader, FileHandle file) {
    inFile(file, () -> {
        for (JsonValue entry : reader.parse(file).get("trajectories")) {
            TrajectoryDefinition trajectory = parseTrajectory(entry);
            trajectories.put(trajectory.id(), trajectory);
        }
        return null;
    });
}

private static TrajectoryDefinition parseTrajectory(JsonValue entry) {
    String id = entry.getString("id");
    String type = entry.getString("type", "constant");
    if ("constant".equals(type)) {
        return new SimpleTrajectoryDefinition(id, entry.getFloat("vx"), entry.getFloat("vy"));
    }
    if ("arc".equals(type)) {
        return new ArcTrajectoryDefinition(
            id, entry.getFloat("vx"), entry.getFloat("vy"), entry.getFloat("ay"));
    }
    throw new IllegalArgumentException("trajectory '" + id + "' has an unknown type '" + type + "'");
}
```

I recommend the coordinator route this to `game-presentation` before `level-designer`'s entries land,
or fold it into whichever branch does #164 (also `core-domain`, but #164's own scope explicitly
excludes `game/` too — the same boundary question would recur there).

## Acceptance criteria — my half

- One archetype, two shapes: not demonstrable yet from this PR alone — `ComponentFactoryRegistry`
  still resolves one trajectory id per archetype, one snapshot at spawn; the override lives in #164's
  `SpawnEvent` change. Not this PR's criterion to close.
- A shape that is not a constant vector exists and is followed: `ArcTrajectoryDefinition` exists and
  its `verticalVelocityAt` is tested against the catalogue's own worked numbers. "Followed" by a
  running entity is #164's wiring; the shape itself is built and correct here.
- Determinism replays pass: `./gradlew :core:test` is green, unchanged from before this branch —
  nothing here is wired into any system's per-tick behaviour yet, so nothing new could have broken a
  replay.
- `DeterminismRulesTest` stays green: confirmed, part of the same green `:core:test` run.
- `SystemOrder` unchanged: confirmed, no file under `core/domain/system/` was touched.
- Javadoc: `TrajectoryDefinition`'s own class javadoc no longer claims curves are not built — the one
  sentence that became false by this diff was corrected. The fuller closing pass across `Motion.java`
  and the roadmap is #86's, explicitly not mine per the task dispatch.

## `pre-pr-check`

Run against `phase/11c-movement-shapes`, output pasted in the pull request once `level-designer` has
appended its half and the branch is ready.

---

## `game-presentation`'s half — the loader itself

Followed `core-domain`'s recommended diff, with one addition: `requireOnlyKeys` on each entry, matching
the strictness the rest of this class already applies to a wave's end condition and a level's top-level
blocks. Without it a `constant` entry carrying a stray `ay` would parse clean and silently drop the
value — the same "unrecognised key loads clean and does nothing" gap #163's own class javadoc says
this file exists to close.

`JsonContentSource.parseTrajectory` (`JsonContentSource.java:157-186`) now reads `"type"`, defaulting
to `"constant"` when absent — via `entry.getString("type", "constant")`, so the four entries shipped
before `"type"` existed need no edit — and switches on it: `"constant"` builds a
`SimpleTrajectoryDefinition` after checking the entry carries only `id`, `type`, `vx`, `vy`; `"arc"`
builds an `ArcTrajectoryDefinition` after checking `id`, `type`, `vx`, `vy`, `ay`. Any other value
throws `IllegalArgumentException`, naming both the trajectory id and the bad type — the loader used to
accept anything and silently discard unknown keys, which is the exact defect the task exists to close.

**Verified without a test source set** (`game/build.gradle.kts` declares no `testImplementation`; see
this agent's own memory on the classpath trick). Compiled `:game:compileJava` clean, then ran a
throwaway program against the real compiled classes and the real `gdx-1.14.2.jar`, invoking the
private `parseTrajectory(JsonValue)` through reflection so no `ContentSource` had to be fully
constructed (that needs `balance.json`, `enemies.json`, `formations.json`, `attachments.json` and a
level file, none of which this task touches):

- The four existing entries (`slow-descent`, `swoop`, `dive`, `crawl`, taken verbatim from
  `assets/data/trajectories.json`) load as `SimpleTrajectoryDefinition` with unchanged `vx`/`vy`.
- `{ "id": "strike-run", "type": "arc", "vx": 0, "vy": -110, "ay": 27 }` — the catalogue's own example
  — loads as `ArcTrajectoryDefinition` and `verticalVelocityAt(2f)` returns `-56.0`, matching the
  closed form `vy + ay·t = -110 + 27·2`.
- `{ "id": "bogus", "type": "spiral", ... }` throws `IllegalArgumentException: trajectory 'bogus' has
  an unknown type 'spiral'` — names the id and the bad type, per the task's own acceptance line.
- An `arc` entry missing `ay` throws (`getFloat` on a missing key), and a `constant` entry carrying a
  stray `ay` throws `"trajectory 'typo' has an unrecognised key 'ay'"` from the new `requireOnlyKeys`
  call.

Full transcript and the exact classpath-discovery steps are in this agent's memory, not repeated here.

**Not touched:** `core/`, `assets/data/` — no content entries added, per this task's scope. `game/`'s
other loaders (`loadFormations`, `loadEnemies`, etc.) are untouched; only `loadTrajectories` and its
new `parseTrajectory` helper changed.

**What was and was not checked about the web target:** `./gradlew build` was not run (issue #123 means
`compileTeavmJava` reports `NO-SOURCE` locally regardless of this change) — not checked. `JsonReader`/
`JsonValue` are the only JSON API touched, no reflection-based `Json` class, no new dependency added —
the two web-target rules this task calls out by name are both satisfied by inspection of the diff.

## Acceptance criteria — this half

- A `type` the loader does not know fails loudly, naming the trajectory id and the bad type: done,
  verified above.
- The four existing `constant` entries load exactly as before: done, verified above with the literal
  entries from `assets/data/trajectories.json`.
- An `arc` entry loads as `ArcTrajectoryDefinition`: done, verified above with the catalogue's own
  `strike-run` numbers.
- No `core/` or `assets/data/` file touched: confirmed, `git status --porcelain` shows only
  `JsonContentSource.java` changed for this half.

## `pre-pr-check` — `game-presentation`'s run

Not yet run for the pull request — the branch is not ready until `level-designer` adds the content in
the same branch, per the task dispatch. `pre-pr-check --base phase/11c-movement-shapes` was run once
during this task to confirm nothing outside `docs/` and `game/` was touched; see the coordinator's
final pull request for the run that accompanies it.
