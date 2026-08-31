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

**What it does not carry is why any of this is the way it is.** JSON admits no comments, so
design intent has nowhere to live in the source and cannot be generated from it. The intent for
level 1 is in `docs/planning/04-campaign-and-levels.md`, and which wave serves which beat is in
`docs/plan/11c-movement-shapes/shape-catalogue.md` under "What points at what". The reasoning
behind the gap is section 14 of `docs/plan/11d-per-level-document/document-contract.md`.

## At a glance

|  |  |
|---|---|
| placements | 15 |
| distinct waves | 13 |
| spawn events | 92 |
| entities spawned directly | 261 |
| the waves end at | 298.0 s |
| the boss enters at | 302.0 s (5.0 min) |
| gap between them | 4.0 s |

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
| 1 | `l1-basic-intro` | 8.0 | 8.0 | 35.5 | 27.5 s | 15 | 0.55/s | `enemy-basic` | `weapon-upgrade` |
| 2 | `l1-light-intro` | 0.0 | 35.5 | 59.5 | 24.0 s | 21 | 0.88/s | `enemy-light` | — |
| 3 | `l1-basic-light-mix` | 0.0 | 59.5 | 86.0 | 26.5 s | 41 | 1.55/s | `enemy-basic` `enemy-light` | — |
| 4 | `l1-tank-solo` | 0.0 | 86.0 | 87.0 | 1.0 s | 1 | 1.00/s | `enemy-tank` | — |
| 5 | `l1-tank-intro-b` | 5.0 | 92.0 | 112.0 | 20.0 s | 18 | 0.90/s | `enemy-basic` `enemy-tank` `enemy-light` | — |
| 6 | `l1-rush-intro-a` | 0.0 | 112.0 | 126.5 | 14.5 s | 11 | 0.76/s | `enemy-rush` | — |
| 7 | `l1-tank-solo` | 0.0 | 126.5 | 127.5 | 1.0 s | 1 | 1.00/s | `enemy-tank` | — |
| 8 | `l1-rush-intro-b` | 1.5 | 129.0 | 138.0 | 9.0 s | 5 | 0.56/s | `enemy-rush` | `weapon-upgrade` |
| 9 | `l1-carrier-intro` | 0.0 | 138.0 | 166.0 | 28.0 s | 10 | 0.36/s | `enemy-carrier` `enemy-basic` `enemy-light` | — |
| 10 | `l1-shooter-intro` | 0.0 | 166.0 | 183.0 | 17.0 s | 11 | 0.65/s | `enemy-shooter` `enemy-light` | — |
| 11 | `l1-veteran-mix` | 0.0 | 183.0 | 208.0 | 25.0 s | 34 | 1.36/s | `enemy-shooter` `enemy-rush` `enemy-basic` `enemy-light` `enemy-tank` | `extra-life` `shield` |
| 12 | `l1-carrier-pair` | 0.0 | 208.0 | 245.0 | 37.0 s | 14 | 0.38/s | `enemy-carrier` `enemy-rush` `enemy-light` | `attachment` |
| 13 | `l1-rest-basic` | 0.0 | 245.0 | 256.0 | 11.0 s | 1 | 0.09/s | `enemy-basic` | `bomb-recharge` |
| 14 | `l1-finale-a` | 0.0 | 256.0 | 297.0 | 41.0 s | 77 | 1.88/s | `enemy-basic` `enemy-light` `enemy-rush` `enemy-shooter` `enemy-tank` `enemy-carrier` | `weapon-upgrade` |
| 15 | `l1-tank-solo` | 0.0 | 297.0 | 298.0 | 1.0 s | 1 | 1.00/s | `enemy-tank` | — |

## The curve

The same numbers as a shape, because a column of numbers is not one. The bar is scaled to the
densest placement in this level, so it compares beats within a level and not between levels.

```
    8.0  l1-basic-intro          0.55/s  ############
   35.5  l1-light-intro          0.88/s  ###################
   59.5  l1-basic-light-mix      1.55/s  #################################
   86.0  l1-tank-solo            1.00/s  #####################
   92.0  l1-tank-intro-b         0.90/s  ###################
  112.0  l1-rush-intro-a         0.76/s  ################
  126.5  l1-tank-solo            1.00/s  #####################
  129.0  l1-rush-intro-b         0.56/s  ############
  138.0  l1-carrier-intro        0.36/s  ########
  166.0  l1-shooter-intro        0.65/s  ##############
  183.0  l1-veteran-mix          1.36/s  #############################
  208.0  l1-carrier-pair         0.38/s  ########
  245.0  l1-rest-basic           0.09/s  ##
  256.0  l1-finale-a             1.88/s  ########################################
  297.0  l1-tank-solo            1.00/s  #####################
```

## Wave by wave

Each wave this level places, once, in the order it first appears. **Placed at** lists every
absolute time the level starts it: a wave is reusable, so editing one for one beat edits every
placement of it, and nothing in `assets/data/waves.json` says so.

### `l1-basic-intro`

**Ends:** `fixedDuration`, 27.5 s

**Placed 1 time:** #1 at 8.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `single` (1) | 0.50 | `slow-descent` | 98.5 .. 109.5 | — |
| 4.5 | `enemy-basic` | `single` (1) | 0.28 | `slow-descent` | 52.7 .. 63.7 | — |
| 8.5 | `enemy-basic` | `single` (1) | 0.72 | `slow-descent` | 144.3 .. 155.3 | — |
| 13.0 | `enemy-basic` | `line-3` (3) | 0.50 | `slow-descent` | 78.5 .. 129.5 | `weapon-upgrade` slot 1 |
| 17.5 | `enemy-basic` | `line-3` (3) | 0.25 | `slow-descent` | 26.5 .. 77.5 | — |
| 21.0 | `enemy-basic` | `line-3` (3) | 0.75 | `slow-descent` | 130.5 .. 181.5 | — |
| 24.0 | `enemy-basic` | `column-3` (3) | 0.50 | `slow-descent` | 98.5 .. 109.5 | — |

### `l1-light-intro`

**Ends:** `fixedDuration`, 24.0 s

**Placed 1 time:** #2 at 35.5 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-light` | `single` (1) | 0.70 | `swoop` | 141.1 .. 150.1 | — |
| 3.5 | `enemy-light` | `single` (1) | 0.85 | `swoop` | 172.3 .. 181.3 | — |
| 7.0 | `enemy-light` | `diagonal` (3) | 0.75 | `swoop` | 136.5 .. 175.5 | — |
| 10.5 | `enemy-light` | `diagonal` (3) | 0.40 | `swoop` | 63.7 .. 102.7 | — |
| 14.0 | `enemy-light` | `diagonal-mirror` (3) | 0.35 | `swoop` | 53.3 .. 92.3 | — |
| 17.5 | `enemy-light` | `vee-5` (5) | 0.60 | `swoop` | 88.3 .. 161.3 | — |
| 20.5 | `enemy-light` | `vee-5` (5) | 0.30 | `swoop` | 25.9 .. 98.9 | — |

### `l1-basic-light-mix`

**Ends:** `fixedDuration`, 26.5 s

**Placed 1 time:** #3 at 59.5 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | — |
| 1.5 | `enemy-light` | `diagonal` (3) | 0.85 | `swoop` | 157.3 .. 196.3 | — |
| 5.0 | `enemy-basic` | `line-3` (3) | 0.30 | `slow-descent` | 36.9 .. 87.9 | — |
| 6.5 | `enemy-light` | `diagonal-mirror` (3) | 0.20 | `swoop` | 22.1 .. 61.1 | — |
| 9.5 | `enemy-light` | `vee-5` (5) | 0.50 | `swoop` | 67.5 .. 140.5 | — |
| 11.5 | `enemy-basic` | `column-3` (3) | 0.15 | `slow-descent` | 25.7 .. 36.7 | — |
| 12.0 | `enemy-basic` | `column-3` (3) | 0.85 | `slow-descent` | 171.3 .. 182.3 | — |
| 16.0 | `enemy-basic` | `line-5` (5) | 0.40 | `slow-descent` | 37.7 .. 128.7 | — |
| 17.5 | `enemy-light` | `diagonal` (3) | 0.90 | `swoop` | 167.7 .. 206.7 | — |
| 20.5 | `enemy-light` | `vee-5` (5) | 0.45 | `swoop` | 57.1 .. 130.1 | — |
| 21.5 | `enemy-basic` | `line-3` (3) | 0.50 | `slow-descent` | 78.5 .. 129.5 | — |

### `l1-tank-solo`

**Ends:** `fixedDuration`, 1.0 s

**Placed 3 times:** #4 at 86.0 s, #7 at 126.5 s, #15 at 297.0 s — **reused: an edit here lands on all of them.**

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-tank` | `single` (1) | 0.50 | `crawl` | 93.5 .. 114.5 | — |

### `l1-tank-intro-b`

**Ends:** `fixedDuration`, 20.0 s

**Placed 1 time:** #5 at 92.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `line-3` (3) | 0.20 | `slow-descent` | 16.1 .. 67.1 | — |
| 3.0 | `enemy-basic` | `line-3` (3) | 0.80 | `slow-descent` | 140.9 .. 191.9 | — |
| 7.0 | `enemy-tank` | `single` (1) | 0.30 | `crawl` | 51.9 .. 72.9 | — |
| 8.0 | `enemy-tank` | `single` (1) | 0.70 | `crawl` | 135.1 .. 156.1 | — |
| 12.0 | `enemy-light` | `vee-5` (5) | 0.50 | `swoop` | 67.5 .. 140.5 | — |
| 15.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | — |

### `l1-rush-intro-a`

**Ends:** `fixedDuration`, 14.5 s

**Placed 1 time:** #6 at 112.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-rush` | `single` (1) | 0.25 | `dive` | 48.0 .. 56.0 | — |
| 3.0 | `enemy-rush` | `single` (1) | 0.75 | `dive` | 152.0 .. 160.0 | — |
| 6.5 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | — |
| 10.0 | `enemy-rush` | `column-3` (3) | 0.20 | `dive` | 37.6 .. 45.6 | — |
| 11.0 | `enemy-rush` | `column-3` (3) | 0.80 | `dive` | 162.4 .. 170.4 | — |

### `l1-rush-intro-b`

**Ends:** `fixedDuration`, 9.0 s

**Placed 1 time:** #8 at 129.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-rush` | `single` (1) | 0.35 | `dive` | 68.8 .. 76.8 | — |
| 1.5 | `enemy-rush` | `single` (1) | 0.65 | `dive` | 131.2 .. 139.2 | — |
| 4.5 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | `weapon-upgrade` slot 0 |

### `l1-carrier-intro`

**Ends:** `fixedDuration`, 28.0 s

**Placed 1 time:** #9 at 138.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-carrier` | `single` (1) | 0.50 | `crawl` | 89.0 .. 119.0 | — |
| 13.0 | `enemy-basic` | `line-3` (3) | 0.15 | `slow-descent` | 5.7 .. 56.7 | — |
| 19.0 | `enemy-basic` | `line-3` (3) | 0.85 | `slow-descent` | 151.3 .. 202.3 | — |
| 22.0 | `enemy-light` | `diagonal` (3) | 0.70 | `swoop` | 126.1 .. 165.1 | — |

### `l1-shooter-intro`

**Ends:** `fixedDuration`, 17.0 s

**Placed 1 time:** #10 at 166.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-shooter` | `single` (1) | 0.40 | `slow-descent` | 76.7 .. 89.7 | — |
| 3.0 | `enemy-shooter` | `single` (1) | 0.60 | `slow-descent` | 118.3 .. 131.3 | — |
| 6.5 | `enemy-shooter` | `line-3` (3) | 0.50 | `slow-descent` | 77.5 .. 130.5 | — |
| 10.0 | `enemy-shooter` | `line-3` (3) | 0.25 | `slow-descent` | 25.5 .. 78.5 | — |
| 11.0 | `enemy-light` | `diagonal-mirror` (3) | 0.80 | `swoop` | 146.9 .. 185.9 | — |

### `l1-veteran-mix`

**Ends:** `fixedDuration`, 25.0 s

**Placed 1 time:** #11 at 183.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-shooter` | `line-3` (3) | 0.50 | `slow-descent` | 77.5 .. 130.5 | `extra-life` slot 1 |
| 1.5 | `enemy-rush` | `column-3` (3) | 0.15 | `dive` | 27.2 .. 35.2 | — |
| 2.5 | `enemy-rush` | `column-3` (3) | 0.85 | `dive` | 172.8 .. 180.8 | — |
| 5.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | — |
| 7.0 | `enemy-light` | `vee-5` (5) | 0.30 | `swoop` | 25.9 .. 98.9 | — |
| 9.0 | `enemy-tank` | `single` (1) | 0.35 | `crawl` | 62.3 .. 83.3 | — |
| 9.5 | `enemy-tank` | `single` (1) | 0.65 | `crawl` | 124.7 .. 145.7 | — |
| 12.5 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | — |
| 14.0 | `enemy-basic` | `line-3` (3) | 0.20 | `slow-descent` | 16.1 .. 67.1 | — |
| 14.5 | `enemy-basic` | `line-3` (3) | 0.80 | `slow-descent` | 140.9 .. 191.9 | — |
| 17.0 | `enemy-light` | `diagonal` (3) | 0.90 | `swoop` | 167.7 .. 206.7 | — |
| 18.5 | `enemy-basic` | `single` (1) | 0.50 | `slow-descent` | 98.5 .. 109.5 | `shield` slot 0 |

### `l1-carrier-pair`

**Ends:** `fixedDuration`, 37.0 s

**Placed 1 time:** #12 at 208.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-carrier` | `pair` (2) | 0.50 | `crawl` | 45.0 .. 163.0 | `attachment` slot 0 |
| 7.0 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | — |
| 16.0 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | — |
| 23.0 | `enemy-light` | `diagonal` (3) | 0.85 | `swoop` | 157.3 .. 196.3 | — |
| 25.0 | `enemy-light` | `diagonal-mirror` (3) | 0.15 | `swoop` | 11.7 .. 50.7 | — |

### `l1-rest-basic`

**Ends:** `fixedDuration`, 11.0 s

**Placed 1 time:** #13 at 245.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `single` (1) | 0.50 | `slow-descent` | 98.5 .. 109.5 | `bomb-recharge` slot 0 |

### `l1-finale-a`

**Ends:** `fixedDuration`, 41.0 s

**Placed 1 time:** #14 at 256.0 s

| at | archetype | formation | atX | shape | x extent | drop |
|---|---|---|---|---|---|---|
| 0.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | — |
| 2.0 | `enemy-light` | `vee-5` (5) | 0.20 | `swoop` | 5.1 .. 78.1 | — |
| 3.5 | `enemy-light` | `vee-5` (5) | 0.80 | `swoop` | 129.9 .. 202.9 | — |
| 5.0 | `enemy-rush` | `column-3` (3) | 0.35 | `dive` | 68.8 .. 76.8 | — |
| 5.5 | `enemy-rush` | `column-3` (3) | 0.65 | `dive` | 131.2 .. 139.2 | — |
| 8.0 | `enemy-shooter` | `line-3` (3) | 0.50 | `slow-descent` | 77.5 .. 130.5 | `weapon-upgrade` slot 1 |
| 11.0 | `enemy-basic` | `column-3` (3) | 0.10 | `slow-descent` | 15.3 .. 26.3 | — |
| 11.5 | `enemy-basic` | `column-3` (3) | 0.90 | `slow-descent` | 181.7 .. 192.7 | — |
| 14.0 | `enemy-tank` | `single` (1) | 0.25 | `crawl` | 41.5 .. 62.5 | — |
| 14.5 | `enemy-tank` | `single` (1) | 0.75 | `crawl` | 145.5 .. 166.5 | — |
| 17.0 | `enemy-light` | `diagonal` (3) | 0.90 | `swoop` | 167.7 .. 206.7 | — |
| 18.0 | `enemy-light` | `diagonal-mirror` (3) | 0.10 | `swoop` | 1.3 .. 40.3 | — |
| 20.5 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | — |
| 23.0 | `enemy-shooter` | `line-3` (3) | 0.30 | `slow-descent` | 35.9 .. 88.9 | — |
| 24.0 | `enemy-shooter` | `line-3` (3) | 0.70 | `slow-descent` | 119.1 .. 172.1 | — |
| 26.5 | `enemy-basic` | `line-5` (5) | 0.45 | `slow-descent` | 48.1 .. 139.1 | — |
| 28.0 | `enemy-rush` | `column-3` (3) | 0.20 | `dive` | 37.6 .. 45.6 | — |
| 28.5 | `enemy-rush` | `column-3` (3) | 0.80 | `dive` | 162.4 .. 170.4 | — |
| 31.0 | `enemy-carrier` | `single` (1) | 0.50 | `crawl` | 89.0 .. 119.0 | — |
| 33.0 | `enemy-light` | `vee-5` (5) | 0.35 | `swoop` | 36.3 .. 109.3 | — |
| 34.5 | `enemy-light` | `vee-5` (5) | 0.65 | `swoop` | 98.7 .. 171.7 | — |
| 37.0 | `enemy-rush` | `column-3` (3) | 0.50 | `dive` | 100.0 .. 108.0 | — |
| 39.0 | `enemy-basic` | `line-5` (5) | 0.50 | `slow-descent` | 58.5 .. 149.5 | — |

**`x extent`** is `atX * 208 + slot.offsetX`, plus and minus the archetype's collider radius
(`SpawnSystem.spawnWave`, `SpawnSystem.positionSpawned`). **Nothing clamps it** — a formation
whose extent leaves `0 .. 208` spawns partly off screen and nobody is told at runtime.

**`shape` is resolved, not copied.** A spawn's own `trajectory` key overrides the archetype's
`motion.trajectory` and is marked *(override)*; every other row is the archetype default.

## Roster

Every archetype this level spawns, plus any an archetype spawns itself. The derived columns are
the point: `rate 4.0` is a number, "one shot per pass" is what it means.

| archetype | sprite | radius | fragile | health | shots to kill | score | default shape | screen time | weapon | shots per pass | spawner | children per pass | lifetime |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `enemy-basic` | `enemy-basic` | 5.5 | yes | none | 1 | 100 | `slow-descent` | 15.3 s | `straight-single`, every 3.2 s from 1.0 s, speed 70.0 | 5 | none | — | none |
| `enemy-light` | `enemy-light` | 4.5 | yes | none | 1 | 150 | `swoop` | 6.9 s | `straight-single`, every 2.4 s from 0.9 s, speed 130.0 | 3 | none | — | none |
| `enemy-tank` | `enemy-tank` | 10.5 | no | 40 | 4 | 500 | `crawl` | 31.2 s | `straight-single`, every 4.8 s from 1.6 s, speed 60.0 | 7 | none | — | none |
| `enemy-rush` | `enemy-rush` | 4.0 | yes | none | 1 | 250 | `dive` | 3.4 s | `straight-single`, every 4.0 s from 1.4 s, speed 120.0 | 1 | none | — | none |
| `enemy-carrier` | `enemy-carrier` | 15.0 | no | 80 | 8 | 1000 | `crawl` | 31.7 s | none | — | `enemy-basic` every 4.0 s | 7 | none |
| `enemy-shooter` | `enemy-shooter` | 6.5 | yes | none | 1 | 200 | `slow-descent` | 15.4 s | `straight-single`, every 1.8 s from 0.7 s, speed 90.0 | 9 | none | — | none |

**shots to kill** is `ceil(health / weaponProjectileDamage)` against `weaponProjectileDamage 10`
from `assets/data/balance.json`. An archetype with no `health` dies to one projectile — and so
does one with `health` at or below 10, which is how a "slightly tougher" enemy becomes a
no-op (`core/domain/system/DamageSystem.java`).

**screen time** is `(270 + radius) / |vy|`: the playfield height (`core/domain/system/SpawnSystem.java:92`) plus the
radius the entity spawns above the edge (`SpawnSystem.positionSpawned`). It is `varies` on an
`arc`, whose speed changes as it flies.

**shots per pass** is `1 + floor((screen time - firstShotDelay) / rate)`, and it is the number
that matters: an archetype whose rate exceeds its screen time fires once whatever the rate says.
**children per pass** is `floor(screen time / interval)`.

**`lifetime` is printed even though nothing carries one.** `core/domain/system/LifetimeSystem.java`
reads an optional per-archetype `Lifetime`; the column says `none` rather than being omitted, so
the lever stays visible.

## Movement shapes this level uses

Only the shapes this level reaches. The full catalogue, including the eight shapes that were
refused and why, is `docs/plan/11c-movement-shapes/shape-catalogue.md`.

| shape | kind | vx | vy | ay | turns after | apex depth |
|---|---|---|---|---|---|---|
| `slow-descent` | `constant` | 0.0 | -18.0 | — | — | — |
| `swoop` | `constant` | -10.0 | -40.0 | — | — | — |
| `crawl` | `constant` | 0.0 | -9.0 | — | — | — |
| `dive` | `constant` | 0.0 | -80.0 | — | — | — |

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
| 21.0 | `weapon-upgrade` | `l1-basic-intro` (#1) | `enemy-basic` in `line-3`, slot 1 |
| 133.5 | `weapon-upgrade` | `l1-rush-intro-b` (#8) | `enemy-rush` in `column-3`, slot 0 |
| 183.0 | `extra-life` | `l1-veteran-mix` (#11) | `enemy-shooter` in `line-3`, slot 1 |
| 201.5 | `shield` | `l1-veteran-mix` (#11) | `enemy-basic` in `single`, slot 0 |
| 208.0 | `attachment` | `l1-carrier-pair` (#12) | `enemy-carrier` in `pair`, slot 0 |
| 245.0 | `bomb-recharge` | `l1-rest-basic` (#13) | `enemy-basic` in `single`, slot 0 |
| 264.0 | `weapon-upgrade` | `l1-finale-a` (#14) | `enemy-shooter` in `line-3`, slot 1 |

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
| `entersAt` | 302.0 |
| `coreHealth` | 1800.0 |
| `podHealth` | 500.0 |
| `armHealth` | 500.0 |
| `corePoints` | 1500.0 |
| `podPoints` | 500.0 |
| `armPoints` | 500.0 |
| `entranceSpeed` | 25.0 |
| `combatY` | 175.0 |
| `patternCooldown` | 0.7 |
| `spreadProjectileSpeed` | 95.0 |
| `sweepProjectileSpeed` | 140.0 |

| derived | value | how |
|---|---|---|
| entrance duration | 5.4 s | `(310.0 - combatY) / entranceSpeed`, the core spawning at `CORE_SPAWN_Y` |
| health of the kill target | 3600 | `2 x coreHealth` — `core-keel` carries the core's health independently |
| health the bar shows | 5600 | the sum across all 6 parts, so killing pods shortens the bar without shortening the fight |

**How low a shot leaves the playfield**, from `combatY` and the fixed velocity ratios. Every
ratio is shallower than 45 degrees, so every projectile exits through a side edge and how far
down it gets is a pure function of `combatY`:

| pattern | vx ratio | vx | vy | y at the side edge |
|---|---|---|---|---|
| spread | 0.25 | 23.8 | -85.5 | -199.4 |
| spread | 0.45 | 42.8 | -85.5 | -33.0 |
| spread | 0.70 | 66.5 | -85.5 | 41.3 |
| sweep | 0.55 | 77.0 | -91.0 | 52.1 |
| sweep | 0.75 | 105.0 | -91.0 | 84.9 |
| sweep | 0.95 | 133.0 | -91.0 | 103.8 |

The ratios and `CORE_SPAWN_Y` are in `core/domain/system/BossSystem.java:74-89,140-151`, not in content. **A `y at the side
edge` above the player's band around `playerStartY 30.0` means that ray never reaches them**,
which is how a boss becomes unlosable with no error anywhere.

**`entersAt` is absolute level time**, compared against `BossSystem`'s own clock, which is
independent of the wave chain. The waves end at 298.0 s and the boss enters at 302.0 s: a 4.0 s gap.

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

**No issues found.**

## The beat map

**Not generated, and it cannot be.** Which of the fourteen beats of
`docs/planning/04-campaign-and-levels.md` a placement serves, and why it exists, is design intent,
and there is no field for it in `assets/data/`. JSON admits no comments; area G of
`docs/plan/10c-architecture-review/assessment.md` predicted this exactly as the price of
generating the document from the JSON, and section 14 of
`docs/plan/11d-per-level-document/document-contract.md` decided to pay it rather than guess.

The mapping exists, written by hand, in `docs/plan/11c-movement-shapes/shape-catalogue.md` under
"What points at what".
