# 334 — the loader reads a formation slot's delay

**Branch:** `feat/loader-reads-slot-delay`. **Closes:** [#334](https://github.com/LuchoC-Dev/little-spaceship/issues/334).
`game`, `game/adapter/content/JsonContentSource.java` and `game`'s tests only. Nothing under
`core/`, `assets/data/` or `tools/` touched — the content that uses this (a single-file column in
level 1) is a separate branch, per the issue.

## What was built

`JsonContentSource.loadFormations` now reads an optional `"delaySeconds"` on each slot in
`formations.json` and constructs the three-argument `FormationSlot(offsetX, offsetY, delaySeconds)`
`core`'s #330 half added. A slot with no `"delaySeconds"` key defaults to `0f` — every formation
shipped before this issue has no such key, so every one of them loads to the identical
`FormationSlot` it always did.

`requireOnlyKeys(slot, "formation '" + id + "' slot", "offsetX", "offsetY", "delaySeconds")` was
added to each slot (there was no per-slot key check before this branch — only `offsetX`/`offsetY`
were ever read, so a typo'd key already loaded clean and was silently ignored). A construction
failure from `FormationSlot`'s own compact constructor (negative, `NaN` or infinite delay) is caught
and rethrown with the formation's id prefixed, then `inFile` adds the file name on top — matching the
pattern every other loader failure in this class already follows.

## The quantisation decision: quantise on parse, in seconds

Chose **option 1** from the `core` fragment: `delaySeconds` stays authored in seconds — consistent
with every other timestamp this content already authors that way (`SpawnEvent.at`, a pickup's
`at`) — and the loader rounds it to the nearest whole tick before it ever reaches `FormationSlot`:

```java
float delaySeconds = Math.round(rawDelaySeconds * 60f) * TICK_SECONDS;
```

where `TICK_SECONDS` is `1f / 60f`, duplicated in `JsonContentSource` from `core.application.GameLoop
.STEP` for the same reason `PLAYFIELD_WIDTH`/`PLAYFIELD_HEIGHT` already are — `game` does not depend
on `core.application`.

**Why this over "author a tick count":** a `"delayTicks"` key removes the rounding question by
construction, but it is a different unit from every other timestamp a level or a formation already
authors, and this content has only ever asked a designer to think in seconds. Quantising on parse
keeps the authoring surface uniform and pushes the actual float-precision problem to the one place
that already knows about it — the loader, not the person writing the JSON.

**What this means for what a designer actually gets:** the seconds typed in `formations.json` are, in
effect, a label for a tick count from the moment they are parsed — `0.3` and `0.31` both mean
"18 ticks", `0.283` and `0.3` mean different things only if they round to different tick counts. This
is deliberate: it is exactly what closes the drift the `core` fragment measured. It is not currently
surfaced anywhere content authors read besides this fragment and the loader's own javadoc; if a
future task adds authored delays to a shipped level, that javadoc is the one place documenting the
rounding, and a generated doc for formations (there is none yet) would be the natural place to state
it explicitly once one exists.

**The drift this closes, quoting the `core` fragment's own reproduction:** for the raw literal `0.3`,
its table reports `active_pass=18` against `ideal_active_pass=19` — the slot turns active one tick
earlier than a whole-tick reading of "0.3 seconds = 18 ticks" would predict. I did not re-derive that
number; I re-ran the same reproduction to confirm what the loader now produces instead of it. What
the loader passes to `FormationSlot` after quantising `0.3` is `18 * TICK_SECONDS`, computed as
`Math.round(18) * (1f/60f)` — a single multiplication, the same expression `core`'s own test fixtures
use to construct an exact `N`-tick delay directly. This is the value `SpawnSystemTest`'s
`delayedSlotTracesLeaderPositionsExactlyDelayTicksBehind` and
`delayedSlotWithOneTickDelayTracesLeaderExactly` already exercise and prove exact against a leader's
own recorded positions — so quantising here is what lets an authored `0.3`, `0.29` or `0.305` (all
three round to `18`) reach `core` as precisely the value those tests already cover, rather than as an
untested literal. I did not independently re-verify the tick-for-tick position match for a
non-quantised delay; that is `core`'s claim and `core`'s test, not this branch's.
`aDecimalDelayIsQuantisedToTheNearestWholeTick` pins the loader's own half: an authored `0.3` loads to
`18 * TICK_SECONDS`, not to the raw `0.3f` literal.

## Tests

New file `JsonContentSourceFormationDelayTest`, kept separate from the other `JsonContentSource*Test`
files for the same reason `JsonContentSourceSpeedMultiplierTest` already is — a parallel task landing
in `JsonContentSource` should not also collide on a test file.

- `aSlotWithNoDelayKeyProducesZeroDelay` — the back-compat case the acceptance criteria name
  explicitly: a slot with no `"delaySeconds"` produces exactly what it produced before this branch.
- `aDecimalDelayIsQuantisedToTheNearestWholeTick` — the quantisation decision itself, using `0.3`
  (one of the `core` fragment's five measured values) and asserting the loaded `delaySeconds` equals
  `18 * TICK_SECONDS`, not the raw literal.
- `aWholeTickDelayIsUnchangedByQuantisation` — a delay that is already an exact multiple of the tick
  (`0.2` → 12 ticks) round-trips to the same value.
- `aNegativeDelayFailsAtLoadNamingFileAndFormation` — `FormationSlot`'s own guard surfaces through
  `inFile`'s wrapping, naming both `formations.json` and the formation's id.
- `anUnrecognisedSlotKeyFailsAtLoadNamingFileAndFormation` — the new `requireOnlyKeys` call on each
  slot, naming both the file and the formation.

## Acceptance criteria

- [x] A formation slot in `assets/data/formations.json` can carry a delay, and the loader constructs
  a `FormationSlot` that carries it — not exercised against the shipped file (no level uses this yet,
  per the issue's scope), exercised against fixture JSON in the new test file instead.
- [x] A slot with no delay produces exactly what it produces today — `aSlotWithNoDelayKeyProducesZeroDelay`;
  `node tools/build-level-docs.js` prints `unchanged` for both `docs/levels/level-01.md` and
  `docs/levels/waves.md`; `./gradlew build` green, all five `core` replay test classes
  (`BombReplayTest`, `BossReplayTest`, `DamageReplayTest`, `LevelScoreReplayTest`,
  `SimulationTest`) included and unchanged.
- [x] A malformed or negative delay is refused at load with a message naming the file and the
  formation — `aNegativeDelayFailsAtLoadNamingFileAndFormation`.
- [x] The quantisation decision is implemented, tested, and stated above with the drift measurement
  behind it.
- [x] `./gradlew build` green.

## Commands run

- `./gradlew :game:compileJava --console=plain -q` — clean, no output.
- `./gradlew :game:test --tests "*FormationDelayTest*" --console=plain` — `BUILD SUCCESSFUL`, all 6
  new tests passed.
- `./gradlew build --console=plain` — `BUILD SUCCESSFUL`, every module (`core`, `game`, `web`,
  `desktop`, `rngparity`) built, including `core:test` (the five replay suites) and `game:test`.
- `node tools/build-level-docs.js` — printed `unchanged  docs/levels/level-01.md` and
  `unchanged  docs/levels/waves.md`.
- `tools/pre-pr-check --base phase/11k-level-one-rebuilt` — output pasted into the pull request.
