# level-01

**This file is generated. Do not edit it by hand — CI will fail before your edit reaches anyone.**
`tools/build-level-docs.js` writes it from the content listed below, and `.github/workflows/ci.yml`
regenerates it on every push and fails if the result differs.

Generated from:

- `assets/data/level-01.json`
- `assets/data/waves.json`
- `assets/data/enemies.json`
- `assets/data/formations.json`
- `assets/data/trajectories.json`
- `assets/data/attachments.json`
- `assets/data/balance.json`

It carries no generation date, git hash or tool version on purpose: any of those would make the
check above fail on every run, which is how a mechanism becomes noise somebody switches off.

**A level file on its own is not playable.** `game/LittleSpaceshipGame.java:42` holds
`private static final String LEVEL_ID = "level-01";`, so which level runs is a code change in
`game/`, not a content change. A correct second level file loads and cannot be reached until that
line moves.

**What it does not carry is why any of this is the way it is.** JSON admits no comments, so
design intent has nowhere to live in the source and cannot be generated from it. The intent for
level 1 is in `docs/planning/04-campaign-and-levels.md`, and which wave serves which beat is in
`docs/plan/11c-movement-shapes/shape-catalogue.md` under "What points at what". The reasoning
behind the gap is section 14 of `docs/plan/11d-per-level-document/document-contract.md`.

## The format

Every key, because the rest of this document prints values and would otherwise leave you guessing
them. The lists come from `game/adapter/content/JsonContentSource.java`, which **rejects any key
its schema does not name** — `requireOnlyKeys`, `:431` — so a key that is not below is a level
that fails to load rather than a key that is quietly ignored.

```jsonc
// assets/data/level-NN.json — requireOnlyKeys(root, "level file", ...) at :349
{
  "boss": { ... },        // optional; the block is below
  "waves": [              // ordered list of placements. An empty list is a level with no waves
    { "wave":   "l1-basic-intro",  // required, an id in waves.json
      "offset": 8.0 }              // required, seconds AFTER THE PREVIOUS PLACEMENT ENDS,
                                   // not from level start. NEGATIVE OVERLAPS the two:
                                   // -6.0 starts this one 6 s before the last one ends,
                                   // and overlap is the one lever in this format that
                                   // produces pressure nothing else can
  ]
  // "events" is also accepted at the top level: the pre-11b flat spawn list. Do not write one
}
```

```jsonc
// assets/data/waves.json — requireOnlyKeys(root, "wave file", "waves") at :253
{ "waves": [
  { "id":     "l1-basic-intro",   // required, and GLOBAL: waves.json is one shared file across
                                  // every level, so an id collides with every other level's
    "end":    { "type": "fixedDuration", "seconds": 27.5 },
                                  // required. Two kinds, and nothing else:
                                  //   {"type":"fixedDuration","seconds":N}  ends at N
                                  //   {"type":"cleared"}                    ends when every
                                  //     entity it spawned is gone. From the first cleared wave
                                  //     onwards every later time in this document is a lower
                                  //     bound rather than a value
    "spawns": [                   // required, and each entry is:
      { "at":        0.0,         // required, seconds FROM THIS WAVE'S OWN START.
                                  //   A spawn past the wave's duration never fires
        "spawn":     "enemy-basic",  // required, an id in enemies.json
        "formation": "single",       // required, an id in formations.json
        "atX":       0.5,            // required, 0..1 of the 208-wide playfield, applied to
                                     //   the formation's CENTRE. Nothing clamps the result
        "trajectory": "dive",        // optional; omit to use the archetype's own default
        "drop":       "weapon-upgrade",  // optional, one of the six kinds below
        "dropSlot":   1 }            // optional, defaults to 0. Index into the formation's
                                     //   slots; past the slot count is fatal at spawn time
    ] }
] }
```

The same wave id may be placed **any number of times**, in one level or in several. That is the
point of the split, and it means an edit to a wave lands on every placement of it — the "Placed
N times" line under each wave below is where to check.

```jsonc
// assets/data/trajectories.json — requireOnlyKeys at :184 and :188
{ "trajectories": [
  { "id": "slow-descent", "vx": 0, "vy": -18 },
                                  // no "type": a constant velocity, units per second, y up
  { "id": "strike-run", "type": "arc", "vx": 0, "vy": -110, "ay": 27 },
                                  // "type":"arc" adds "ay", and only then. It turns after
                                  //   -vy/ay seconds and bottoms out vy^2/(2*ay) below spawn
  { "id": "turn-out", "type": "path",
    "segments": [ { "vx": 0, "vy": -45, "duration": 3.0 },
                  { "wait": 1.0 },
                  { "vx": -55, "vy": 0, "duration": 6.0 } ] },
                                  // "segments": velocity held for a duration, leg after leg;
                                  //   {"wait": s} is a leg with no velocity. Optional "loopStart"
                                  //   / "loopCount" repeat a trailing range; the last leg (the
                                  //   last of the repeated range, if there is one) must move —
                                  //   a path that ends at rest is refused at load
  { "id": "hold-then-slide", "type": "path",
    "waypoints": [ { "x": 104, "y": 270 }, { "x": 104, "y": 190, "speed": 45 },
                   { "x": 208, "y": 190, "speed": 70 } ] },
                                  // "waypoints": destinations and a speed instead of a velocity
                                  //   and a duration — "segments" and "waypoints" are mutually
                                  //   exclusive on one entry. ABSOLUTE: the first waypoint is
                                  //   the authoring origin, not a position the engine reads —
                                  //   the wave placing this must use atX = (that x) / 208
  { "id": "cross-right", "mirrorOf": "cross-left" },
                                  // negates every horizontal component of "cross-left"
  { "id": "dive-fast", "speedOf": "dive", "multiplier": 1.5 }
                                  // the same shape traversed 1.5x sooner — velocities scale by
                                  //   the multiplier, and an arc's "ay" by its square
] }
```

```jsonc
// the "boss" block of a level file — requireOnlyKeys(value, "boss block", ...) at :402.
// Every key is required; the names are BossDefinition's accessors. Values for this level are
// in "The boss" below, with what each one does to the fight.
{ "id": "boss-l1", "entersAt": 302.0,
  "coreHealth": 1800, "podHealth": 500, "armHealth": 500,
  "corePoints": 1500, "podPoints": 500, "armPoints": 500,
  "entranceSpeed": 25.0, "combatY": 175.0, "patternCooldown": 0.7,
  "spreadProjectileSpeed": 95.0, "sweepProjectileSpeed": 140.0 }
```

`enemies.json` and `formations.json` have **no strict key check** — an unknown component key in an
archetype is rejected by `ComponentFactoryRegistry` instead, at spawn time. Their fields are in
the Roster and Formations sections below, printed per entry rather than as a schema.

## At a glance

|  |  |
|---|---|
| placements | 12 |
| distinct waves | 12 |
| spawn events | 61 |
| entities spawned directly | 152 |
| the waves end at | 134.5 s |
| the boss enters at | 134.5 s (2.2 min) |
| gap between them | 0.0 s |

**Every wave ends on `fixedDuration`, so every time below is exact arithmetic.** The moment one
wave uses `{"type": "cleared"}` (`core/port/WaveEndCondition.java`), every absolute time after
it becomes a lower bound, because a cleared wave ends when the player finishes it. This
document would then say so here rather than print numbers that look exact.

## The pacing table

One row per placement, in level order. **Density is entities per second of that placement's own
duration** — `entities / duration` — and it is one axis of difficulty, not a difficulty score.
`docs/planning/01-vision-and-scope.md` names eight axes at once; this is one. A slow carrier that
keeps producing children reads low here and plays hard.

| # | wave | offset | start | end | lasts | entities | density | archetypes | drops |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `l1-opening-calm` | 0.0 | 0.0 | 8.0 | 8.0 s | 1 | 0.13/s | `enemy-basic` | — |
| 2 | `l1-first-basics` | 0.0 | 8.0 | 22.0 | 14.0 s | 13 | 0.93/s | `enemy-basic` | `weapon-upgrade` |
| 3 | `l1-light-and-fast` | 0.0 | 22.0 | 33.0 | 11.0 s | 13 | 1.18/s | `enemy-light` | — |
| 4 | `l1-combined-formations` | 0.0 | 33.0 | 46.0 | 13.0 s | 23 | 1.77/s | `enemy-basic` `enemy-light` | `shield` |
| 5 | `l1-tanks-and-priority` | 0.0 | 46.0 | 58.0 | 12.0 s | 13 | 1.08/s | `enemy-tank` `enemy-basic` `enemy-light` | `weapon-upgrade` |
| 6 | `l1-super-fast` | 0.0 | 58.0 | 67.0 | 9.0 s | 7 | 0.78/s | `enemy-rush` | — |
| 7 | `l1-heavy-carrier` | 0.0 | 67.0 | 80.0 | 13.0 s | 7 | 0.54/s | `enemy-carrier` `enemy-basic` `enemy-light` | — |
| 8 | `l1-evolved-shooters` | 0.0 | 80.0 | 90.0 | 10.0 s | 13 | 1.30/s | `enemy-shooter` `enemy-basic` | `weapon-upgrade` |
| 9 | `l1-high-pressure` | -2.0 | 88.0 | 99.0 | 11.0 s | 21 | 1.91/s | `enemy-basic` `enemy-light` `enemy-shooter` `enemy-rush` | `extra-life` |
| 10 | `l1-twin-carriers-attachment` | -1.5 | 97.5 | 113.5 | 16.0 s | 8 | 0.50/s | `enemy-carrier` `enemy-rush` | `attachment` |
| 11 | `l1-brief-rest` | 0.0 | 113.5 | 119.5 | 6.0 s | 1 | 0.17/s | `enemy-basic` | `bomb-recharge` |
| 12 | `l1-final-escalation` | 0.0 | 119.5 | 134.5 | 15.0 s | 32 | 2.13/s | `enemy-basic` `enemy-light` `enemy-rush` `enemy-shooter` `enemy-tank` | — |

## The curve

The same numbers as a shape, because a column of numbers is not one. The bar is scaled to the
densest placement in this level, so it compares beats within a level and not between levels.

```
    0.0  l1-opening-calm         0.13/s  ##
    8.0  l1-first-basics         0.93/s  #################
   22.0  l1-light-and-fast       1.18/s  ######################
   33.0  l1-combined-formations  1.77/s  #################################
   46.0  l1-tanks-and-priority   1.08/s  ####################
   58.0  l1-super-fast           0.78/s  ###############
   67.0  l1-heavy-carrier        0.54/s  ##########
   80.0  l1-evolved-shooters     1.30/s  ########################
   88.0  l1-high-pressure        1.91/s  ####################################
   97.5  l1-twin-carriers-attachment  0.50/s  #########
  113.5  l1-brief-rest           0.17/s  ###
  119.5  l1-final-escalation     2.13/s  ########################################
```

## Wave by wave

Each wave this level places, once, in the order it first appears. **Placed at** lists every
absolute time the level starts it: a wave is reusable, so editing one for one beat edits every
placement of it, and nothing in `assets/data/waves.json` says so.

### `l1-opening-calm`

**Ends:** `fixedDuration`, 8.0 s

**Placed 1 time:** #1 at 0.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 5.0 | `enemy-basic` | `single` (1) | 0.20 | `slow-descent` | 36.1 .. 47.1 | same | — |

### `l1-first-basics`

**Ends:** `fixedDuration`, 14.0 s

**Placed 1 time:** #2 at 8.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `single` (1) | 0.50 | `slow-descent` | 98.5 .. 109.5 | same | — |
| 3.0 | `enemy-basic` | `line-3` (3) | 0.50 | `slow-descent` | 78.5 .. 129.5 | same | `weapon-upgrade` slot 1 |
| 6.5 | `enemy-basic` | `line-3` (3) | 0.28 | `slow-descent` | 32.7 .. 83.7 | same | — |
| 9.5 | `enemy-basic` | `line-3` (3) | 0.72 | `slow-descent` | 124.3 .. 175.3 | same | — |
| 12.0 | `enemy-basic` | `column-3` (3) | 0.50 | `slow-descent` | 98.5 .. 109.5 | same | — |

### `l1-light-and-fast`

**Ends:** `fixedDuration`, 11.0 s

**Placed 1 time:** #3 at 22.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-light` | `single` (1) | 0.70 | `swoop` | 141.1 .. 150.1 | 72.5 .. 150.1 | — |
| 2.0 | `enemy-light` | `single` (1) | 0.55 | `swoop` | 109.9 .. 118.9 | 41.3 .. 118.9 | — |
| 4.5 | `enemy-light` | `diagonal` (3) | 0.75 | `swoop` | 136.5 .. 175.5 | 67.9 .. 175.5 | — |
| 7.0 | `enemy-light` | `diagonal-mirror` (3) | 0.60 | `swoop` | 105.3 .. 144.3 | 36.7 .. 144.3 | — |
| 9.0 | `enemy-light` | `vee-5` (5) | 0.65 | `swoop` | 98.7 .. 171.7 | 30.1 .. 171.7 | — |

### `l1-combined-formations`

**Ends:** `fixedDuration`, 13.0 s

**Placed 1 time:** #4 at 33.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | same | — |
| 1.5 | `enemy-light` | `diagonal` (3) | 0.80 | `swoop` | 146.9 .. 185.9 | 78.3 .. 185.9 | — |
| 4.0 | `enemy-basic` | `line-3` (3) | 0.30 | `slow-descent` | 36.9 .. 87.9 | same | `shield` slot 1 |
| 5.5 | `enemy-light` | `diagonal-mirror` (3) | 0.55 | `swoop` | 94.9 .. 133.9 | 26.3 .. 133.9 | — |
| 8.5 | `enemy-basic` | `column-3` (3) | 0.15 | `slow-descent` | 25.7 .. 36.7 | same | — |
| 9.0 | `enemy-basic` | `column-3` (3) | 0.85 | `slow-descent` | 171.3 .. 182.3 | same | — |
| 11.0 | `enemy-light` | `diagonal` (3) | 0.60 | `swoop` | 105.3 .. 144.3 | 36.7 .. 144.3 | — |

### `l1-tanks-and-priority`

**Ends:** `fixedDuration`, 12.0 s

**Placed 1 time:** #5 at 46.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-tank` | `single` (1) | 0.35 | `crawl` | 62.3 .. 83.3 | same | — |
| 2.0 | `enemy-basic` | `line-3` (3) | 0.70 | `slow-descent` | 120.1 .. 171.1 | same | `weapon-upgrade` slot 1 |
| 5.0 | `enemy-tank` | `single` (1) | 0.65 | `crawl` | 124.7 .. 145.7 | same | — |
| 7.5 | `enemy-light` | `diagonal` (3) | 0.80 | `swoop` | 146.9 .. 185.9 | 78.3 .. 185.9 | — |
| 10.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | same | — |

### `l1-super-fast`

**Ends:** `fixedDuration`, 9.0 s

**Placed 1 time:** #6 at 58.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-rush` | `single` (1) | 0.30 | `dive` | 58.4 .. 66.4 | same | — |
| 1.5 | `enemy-rush` | `single` (1) | 0.70 | `dive` | 141.6 .. 149.6 | same | — |
| 3.5 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | same | — |
| 6.0 | `enemy-rush` | `single` (1) | 0.40 | `strike-run` *(override)* | 79.2 .. 87.2 | same | — |
| 7.0 | `enemy-rush` | `single` (1) | 0.60 | `strike-run` *(override)* | 120.8 .. 128.8 | same | — |

### `l1-heavy-carrier`

**Ends:** `fixedDuration`, 13.0 s

**Placed 1 time:** #7 at 67.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-carrier` | `single` (1) | 0.50 | `crawl` | 89.0 .. 119.0 | same | — |
| 5.0 | `enemy-basic` | `line-3` (3) | 0.25 | `slow-descent` | 26.5 .. 77.5 | same | — |
| 9.0 | `enemy-light` | `diagonal-mirror` (3) | 0.70 | `swoop` | 126.1 .. 165.1 | 57.5 .. 165.1 | — |

### `l1-evolved-shooters`

**Ends:** `fixedDuration`, 10.0 s

**Placed 1 time:** #8 at 80.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-shooter` | `single` (1) | 0.40 | `slow-descent` | 76.7 .. 89.7 | same | — |
| 2.0 | `enemy-shooter` | `single` (1) | 0.60 | `slow-descent` | 118.3 .. 131.3 | same | — |
| 4.5 | `enemy-shooter` | `line-3` (3) | 0.50 | `slow-descent` | 77.5 .. 130.5 | same | — |
| 6.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | same | `weapon-upgrade` slot 2 |
| 8.5 | `enemy-shooter` | `line-3` (3) | 0.30 | `slow-descent` | 35.9 .. 88.9 | same | — |

### `l1-high-pressure`

**Ends:** `fixedDuration`, 11.0 s

**Placed 1 time:** #9 at 88.0 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | same | — |
| 1.0 | `enemy-light` | `single` (1) | 0.88 | `veer-left` *(override)* | 178.5 .. 187.5 | -125.5 .. 187.5 **leaves** | — |
| 2.5 | `enemy-light` | `single` (1) | 0.12 | `veer-right` *(override)* | 20.5 .. 29.5 | 20.5 .. 333.5 **leaves** | — |
| 4.5 | `enemy-shooter` | `line-3` (3) | 0.50 | `slow-descent` | 77.5 .. 130.5 | same | `extra-life` slot 1 |
| 6.0 | `enemy-rush` | `column-3` (3) | 0.20 | `dive` | 37.6 .. 45.6 | same | — |
| 6.5 | `enemy-rush` | `column-3` (3) | 0.80 | `dive` | 162.4 .. 170.4 | same | — |
| 9.0 | `enemy-light` | `vee-5` (5) | 0.60 | `swoop` | 88.3 .. 161.3 | 19.7 .. 161.3 | — |

### `l1-twin-carriers-attachment`

**Ends:** `fixedDuration`, 16.0 s

**Placed 1 time:** #10 at 97.5 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-carrier` | `pair` (2) | 0.50 | `crawl` | 45.0 .. 163.0 | same | `attachment` slot 0 |
| 4.0 | `enemy-rush` | `single` (1) | 0.35 | `strike-run` *(override)* | 68.8 .. 76.8 | same | — |
| 5.0 | `enemy-rush` | `single` (1) | 0.65 | `strike-run` *(override)* | 131.2 .. 139.2 | same | — |
| 9.0 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | same | — |
| 12.0 | `enemy-rush` | `single` (1) | 0.88 | `veer-left` *(override)* | 179.0 .. 187.0 | -125.0 .. 187.0 **leaves** | — |

### `l1-brief-rest`

**Ends:** `fixedDuration`, 6.0 s

**Placed 1 time:** #11 at 113.5 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `single` (1) | 0.50 | `slow-descent` | 98.5 .. 109.5 | same | `bomb-recharge` slot 0 |

### `l1-final-escalation`

**Ends:** `fixedDuration`, 15.0 s

**Placed 1 time:** #12 at 119.5 s

| at | archetype | formation | atX | shape | x at spawn | x swept | drop |
|---|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | same | — |
| 1.0 | `enemy-light` | `vee-5` (5) | 0.55 | `swoop` | 77.9 .. 150.9 | 9.3 .. 150.9 | — |
| 2.5 | `enemy-rush` | `single` (1) | 0.12 | `veer-right` *(override)* | 21.0 .. 29.0 | 21.0 .. 333.0 **leaves** | — |
| 3.0 | `enemy-rush` | `single` (1) | 0.88 | `veer-left` *(override)* | 179.0 .. 187.0 | -125.0 .. 187.0 **leaves** | — |
| 5.0 | `enemy-shooter` | `line-3` (3) | 0.30 | `slow-descent` | 35.9 .. 88.9 | same | — |
| 5.5 | `enemy-shooter` | `line-3` (3) | 0.70 | `slow-descent` | 119.1 .. 172.1 | same | — |
| 7.5 | `enemy-tank` | `single` (1) | 0.30 | `crawl` | 51.9 .. 72.9 | same | — |
| 8.0 | `enemy-tank` | `single` (1) | 0.70 | `crawl` | 135.1 .. 156.1 | same | — |
| 10.0 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | same | — |
| 11.0 | `enemy-light` | `diagonal` (3) | 0.75 | `swoop` | 136.5 .. 175.5 | 67.9 .. 175.5 | — |
| 12.5 | `enemy-rush` | `single` (1) | 0.45 | `strike-run` *(override)* | 89.6 .. 97.6 | same | — |
| 13.5 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | same | — |

**`x at spawn`** is `atX * 208 + slot.offsetX`, plus and minus the archetype's collider radius
(`SpawnSystem.spawnWave`, `SpawnSystem.positionSpawned`). **Nothing clamps it** — a formation
whose extent leaves `0 .. 208` spawns partly off screen and nobody is told at runtime.

**`x swept` is where it goes**, and for any shape with a `vx` it is the column that matters. A
spawn-instant extent is a snapshot: `swoop` carries `vx -10` for 6.9 s, so a formation on it ends
69 units left of where it started, and a `veer-right` placed on the right edge spends its whole
arc past it. `same` means the shape has no horizontal velocity and the two are identical.

**`shape` is resolved, not copied.** A spawn's own `trajectory` key overrides the archetype's
`motion.trajectory` and is marked *(override)*; every other row is the archetype default.

## Roster

Every archetype this level spawns, plus any an archetype spawns itself. The derived columns are
the point: `rate 4.0` is a number, "one shot per pass" is what it means.

| archetype | sprite | radius | fragile | health | shots to kill | score | default shape | screen time | weapon | shots per pass | spawner | children per pass | lifetime |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `enemy-basic` | `enemy-basic` | 5.5 | yes | 20 | 2 | 100 | `slow-descent` | 15.3 s | `straight-single`, every 3.2 s from 1.0 s, speed 70.0 | 5 | none | — | none |
| `enemy-light` | `enemy-light` | 4.5 | yes | none | 1 | 150 | `swoop` | 6.9 s | `straight-single`, every 2.4 s from 0.9 s, speed 130.0 | 3 | none | — | none |
| `enemy-tank` | `enemy-tank` | 10.5 | no | 200 | 20 | 500 | `crawl` | 31.2 s | `straight-single`, every 4.8 s from 1.6 s, speed 60.0 | 7 | none | — | none |
| `enemy-rush` | `enemy-rush` | 4.0 | yes | none | 1 | 250 | `dive` | 3.4 s | `straight-single`, every 4.0 s from 1.4 s, speed 120.0 | 1 | none | — | none |
| `enemy-carrier` | `enemy-carrier` | 15.0 | no | 700 | 70 | 1000 | `crawl` | 31.7 s | none | — | `enemy-basic` every 3.0 s | 10 | none |
| `enemy-shooter` | `enemy-shooter` | 6.5 | yes | 30 | 3 | 200 | `slow-descent` | 15.4 s | `straight-single`, every 1.8 s from 0.7 s, speed 90.0 | 9 | none | — | none |

**shots to kill** is `ceil(health / weaponProjectileDamage)` against `weaponProjectileDamage 10`
from `assets/data/balance.json`. An archetype with no `health` dies to one projectile — and so
does one with `health` at or below 10, which is how a "slightly tougher" enemy becomes a
no-op (`core/domain/system/DamageSystem.java`).

**screen time** is `(270 + radius) / |vy|`: the playfield height (`core/domain/system/SpawnSystem.java:92`) plus the
radius the entity spawns above the edge (`SpawnSystem.positionSpawned`). It is `varies` on an
`arc`, whose speed changes as it flies, and on a `path` for the same reason plus one more: this
table has no spawn to measure a `path` from, so it cannot say where a horizontal leg would carry
it off the sides. A `path`'s real screen time, for the spawn it is actually placed at, is the
"x swept" column above.

**shots per pass** is `1 + floor((screen time - firstShotDelay) / rate)`, and it is the number
that matters: an archetype whose rate exceeds its screen time fires once whatever the rate says.
**children per pass** is `floor(screen time / interval)`.

**`lifetime` is printed even though nothing carries one.** `core/domain/system/LifetimeSystem.java`
reads an optional per-archetype `Lifetime`; the column says `none` rather than being omitted, so
the lever stays visible.

## Movement shapes this level uses

Only the shapes this level reaches. The full catalogue, including the eight shapes that were
refused and why, is `docs/plan/11c-movement-shapes/shape-catalogue.md`.

| shape | kind | vx | vy | ay | turns after | apex depth | legs (path only) |
|---|---|---|---|---|---|---|---|
| `slow-descent` | `constant` | 0.0 | -18.0 | — | — | — | — |
| `swoop` | `constant` | -10.0 | -40.0 | — | — | — | — |
| `crawl` | `constant` | 0.0 | -9.0 | — | — | — | — |
| `dive` | `constant` | 0.0 | -80.0 | — | — | — | — |
| `strike-run` | `arc` | 0.0 | -110.0 | 27.0 | 4.1 s | 224.1 below spawn | — |
| `veer-left` | `arc` | -32.0 | -95.0 | 20.0 | 4.8 s | 225.6 below spawn | — |
| `veer-right` | `arc` | 32.0 | -95.0 | 20.0 | 4.8 s | 225.6 below spawn | — |

An `arc` turns at `-vy / ay` and bottoms out `vy² / (2·ay)` below where it spawned, evaluated in
closed form from the entity's own elapsed time (`core/port/ArcTrajectoryDefinition.java`).
The player flies at `playerStartY 30.0` in a 270-tall playfield, so a shape whose apex sits
far above that band is scenery.

**The veers spawn on the side they veer away from** — `veer-left` at `atX >= 0.75`, `veer-right`
at `atX <= 0.25` — or the shape happens off screen. That constraint is the catalogue's.

## Formations this level uses

| formation | slots | offsets (x, y) | span | occupied width, per archetype |
|---|---|---|---|---|
| `single` | 1 | (0, 0) | 0.0 | `enemy-basic` 11.0, `enemy-light` 9.0, `enemy-tank` 21.0, `enemy-rush` 8.0, `enemy-carrier` 30.0, `enemy-shooter` 13.0 |
| `line-3` | 3 | (-20, 0) (0, 0) (20, 0) | 40.0 | `enemy-basic` 51.0, `enemy-shooter` 53.0 |
| `column-3` | 3 | (0, 0) (0, 22) (0, 44) | 0.0 | `enemy-basic` 11.0, `enemy-rush` 8.0 |
| `diagonal` | 3 | (-15, 0) (0, -15) (15, -30) | 30.0 | `enemy-light` 39.0 |
| `diagonal-mirror` | 3 | (15, 0) (0, -15) (-15, -30) | 30.0 | `enemy-light` 39.0 |
| `vee-5` | 5 | (-32, 16) (-16, 8) (0, 0) (16, 8) (32, 16) | 64.0 | `enemy-light` 73.0 |
| `line-5` | 5 | (-40, 0) (-20, 0) (0, 0) (20, 0) (40, 0) | 80.0 | `enemy-basic` 91.0 |
| `pair` | 2 | (-44, 0) (44, 0) | 88.0 | `enemy-carrier` 118.0 |

The playfield is 208 units wide (`core/domain/system/MotionSystem.java:57`). Occupied width is what decides whether
an `atX` is legal, and it is the arithmetic every new spawn needs.

**A slot's `offsetY` is a head start in pixels, not a delay in seconds** — the whole formation
clears the bottom edge together and the slot furthest back arrives later only because it has
further to travel (`SpawnSystem.positionSpawned`). So one formation is a stream on a slow shape
and a burst on a fast one:

| formation | archetype | shape | y spread | first to last |
|---|---|---|---|---|
| `column-3` | `enemy-basic` | `slow-descent` | 44 | 2.44 s |
| `column-3` | `enemy-rush` | `dive` | 44 | 0.55 s |
| `diagonal` | `enemy-light` | `swoop` | 30 | 0.75 s |
| `diagonal-mirror` | `enemy-light` | `swoop` | 30 | 0.75 s |
| `vee-5` | `enemy-light` | `swoop` | 16 | 0.40 s |

## Projectiles

| fired by | speed | damage | radius |
|---|---|---|---|
| the player | 220.0 | 10 | not in content |
| `enemy-basic` | 70.0 | contact | 2.0 |
| `enemy-light` | 130.0 | contact | 2.0 |
| `enemy-tank` | 60.0 | contact | 2.0 |
| `enemy-rush` | 120.0 | contact | 2.0 |
| `enemy-shooter` | 90.0 | contact | 2.0 |

The player fires every `weaponFireCooldown 0.15` s across `weaponLevels 4` shot levels
(`assets/data/balance.json`).

**Not in `assets/data/`:** the enemy projectile's radius is `2.0` in `core/domain/system/EnemyWeaponSystem.java:35`,
and `straight-single` is the **only** `pattern` string that system builds
(`core/domain/system/EnemyWeaponSystem.java:37,86`). Any other value is content naming a shape nothing draws, and it throws the
moment that enemy first fires. Those two sentences are quoted from code, and **regenerating this
document cannot keep them honest** — if those lines move, this text does not change.

## Drops and rewards

| at | kind | wave | carried by |
|---|---|---|---|
| 11.0 | `weapon-upgrade` | `l1-first-basics` (#2) | `enemy-basic` in `line-3`, slot 1 |
| 37.0 | `shield` | `l1-combined-formations` (#4) | `enemy-basic` in `line-3`, slot 1 |
| 48.0 | `weapon-upgrade` | `l1-tanks-and-priority` (#5) | `enemy-basic` in `line-3`, slot 1 |
| 86.0 | `weapon-upgrade` | `l1-evolved-shooters` (#8) | `enemy-basic` in `line-5`, slot 2 |
| 92.5 | `extra-life` | `l1-high-pressure` (#9) | `enemy-shooter` in `line-3`, slot 1 |
| 97.5 | `attachment` | `l1-twin-carriers-attachment` (#10) | `enemy-carrier` in `pair`, slot 0 |
| 113.5 | `bomb-recharge` | `l1-brief-rest` (#11) | `enemy-basic` in `single`, slot 0 |

**A drop is delivered only if the player destroys the carrier.** `core/domain/system/LifetimeSystem.java`
strips `ScoreValue`, `Drop` and `Collider` from an enemy that leaves the screen, so a drop placed
on a fast, fragile archetype can be lost entirely.

**The six kinds are code, not content** (`core/domain/system/PickupSystem.java:39-71`). There is no `drops.json`, and
`SpawnSystem.requireRecognisedDrop` rejects anything outside this closed set the moment a wave
carrying it spawns:

| kind | what it does |
|---|---|
| `weapon-upgrade` | raises the player's shot level by one, up to `weaponLevels` |
| `shield` | grants a shield, if the player does not already have one |
| `extra-life` | one more life, up to `maxLives` |
| `bomb-recharge` | one more bomb, up to `maxBombs` |
| `invulnerability` | grace time set — not added — to `invulnerabilityPickupDuration` |
| `attachment` | equips an attachment; the only kind that is content-driven, through `assets/data/attachments.json` |

Attachment durability, the one content-driven part: `attachment` 1 (`assets/data/attachments.json`).

## The boss

| field | value |
|---|---|
| `id` | boss-l1 |
| `entersAt` | 134.5 |
| `coreHealth` | 1800.0 |
| `podHealth` | 500.0 |
| `armHealth` | 500.0 |
| `corePoints` | 1500.0 |
| `podPoints` | 500.0 |
| `armPoints` | 500.0 |
| `entranceSpeed` | 25.0 |
| `combatY` | 175.0 |
| `patternCooldown` | 0.7 |
| `spreadProjectileSpeed` | 85.0 |
| `sweepProjectileSpeed` | 125.0 |

| derived | value | how |
|---|---|---|
| entrance duration | 5.4 s | `(310.0 - combatY) / entranceSpeed`, the core spawning at `CORE_SPAWN_Y` |
| health of the kill target | 3600 | `2 x coreHealth` — `core-keel` carries the core's health independently |
| health the bar shows | 5600 | the sum across all 6 parts, so killing pods shortens the bar without shortening the fight |

**Where each ray goes**, from `combatY` and the fixed velocity ratios. `combatY` alone decides
whether this boss can hit anything, so the two columns that matter are which edge a ray leaves
through and how far it is from the boss when it crosses the height the player flies at.

| pattern | vx ratio | vx | vy | leaves through | x from the boss at y 30.0 |
|---|---|---|---|---|---|
| spread | 0.25 | 21.3 | -76.5 | the floor, 2.3 s | 40.3 |
| spread | 0.45 | 38.3 | -76.5 | the floor, 2.3 s | 72.5 |
| spread | 0.70 | 59.5 | -76.5 | a side, 1.7 s | **off the playfield already** |
| sweep | 0.55 | 68.8 | -81.3 | a side, 1.5 s | **off the playfield already** |
| sweep | 0.75 | 93.8 | -81.3 | a side, 1.1 s | **off the playfield already** |
| sweep | 0.95 | 118.8 | -81.3 | a side, 0.9 s | **off the playfield already** |

The ratios and `CORE_SPAWN_Y` are in `core/domain/system/BossSystem.java:74-89,140-151`, not in content.

**The last column is the one to read.** It is how far to either side of the boss a ray has
travelled by the time it reaches `playerStartY 30.0`, the height the player starts at, measured
from the boss's own centre — so a ray whose figure is larger than the half-width, 104, has left
the playfield before it ever gets down there and threatens nobody. **A boss every one of whose
rays reads that way is unlosable, with no error anywhere.** Where the player actually stands is
theirs to choose; this document says only where the rays are.

**`entersAt` is absolute level time**, compared against `BossSystem`'s own clock, which is
independent of the wave chain. The waves end at 134.5 s and the boss enters at 134.5 s: a 0.0 s gap.

## Designing against the player

The same for every level, repeated here so no lookup leaves this document.

|  | value |
|---|---|
| playfield width | 208 (`core/domain/system/MotionSystem.java:57`) |
| playfield height | 270 (`core/domain/system/SpawnSystem.java:92`) |
| `bombCompletionBonus` | 300 |
| `bombDamage` | 50 |
| `damageInvulnerability` | 1 |
| `initialBombs` | 2 |
| `initialLives` | 3 |
| `invulnerabilityPickupDuration` | 3 |
| `lifeCompletionBonus` | 1000 |
| `maxBombs` | 3 |
| `maxLives` | 5 |
| `maxedPickupBonus` | 500 |
| `pickupFallSpeed` | 20 |
| `pickupRadius` | 6 |
| `playerSlowFactor` | 0.45 |
| `playerSpeed` | 140 |
| `playerStartX` | 104 |
| `playerStartY` | 30 |
| `respawnInvulnerability` | 2 |
| `weaponFireCooldown` | 0.15 |
| `weaponLevels` | 4 |
| `weaponProjectileDamage` | 10 |
| `weaponProjectileSpeed` | 220 |

The two dimensions are fixed properties of the logical resolution rather than balance values,
which is why they live in code and everything under them lives in `assets/data/balance.json`.

## Checks

Every check below is a failure that is either silent at runtime or fatal only on the tick it
happens, and each one costs a run of `./gradlew :desktop:run` to find by hand. That is the part of
this document a generator can do and a human reliably will not.

**What was checked**, so you can tell what is still yours to verify:

- a spawn whose `at` is past its wave’s duration, which never fires
- a formation whose extent at the spawn instant leaves `0 .. 208`
- **a spawn whose swept extent is mostly outside `0 .. 208`**, which the spawn-instant extent cannot see, and the veer-side rule when a veer is the cause
- a `dropSlot` past its formation’s slot count
- a drop kind outside the six
- a `cleared` wave holding a shape that never leaves the playfield, so it can never end
- a negative `offset`, and what it overlaps
- a boss entering over a running wave, against a lower bound when the wave chain is inexact

**Not checked, and still yours:** whether the level is any good. Density is not difficulty and
this project tunes balance by playing.

- placement #9 `l1-high-pressure` has `offset -2.0`, overlapping `l1-evolved-shooters` by 2.0 s. Overlap is the one thing in this format that produces pressure nothing else can, and it is the thing a reader misreads first.
- placement #10 `l1-twin-carriers-attachment` has `offset -1.5`, overlapping `l1-high-pressure` by 1.5 s. Overlap is the one thing in this format that produces pressure nothing else can, and it is the thing a reader misreads first.

## The beat map

**Not generated, and it cannot be.** Which of the fourteen beats of
`docs/planning/04-campaign-and-levels.md` a placement serves, and why it exists, is design intent,
and there is no field for it in `assets/data/`. JSON admits no comments; area G of
`docs/plan/10c-architecture-review/assessment.md` predicted this exactly as the price of
generating the document from the JSON, and section 14 of
`docs/plan/11d-per-level-document/document-contract.md` decided to pay it rather than guess.

The mapping exists, written by hand, in `docs/plan/11c-movement-shapes/shape-catalogue.md` under
"What points at what".
