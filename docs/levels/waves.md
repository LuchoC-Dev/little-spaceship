# Every wave, and who places it

**This file is generated. Do not edit it by hand.** `tools/build-level-docs.js` writes it from
`assets/data/waves.json` and every `assets/data/level-NN.json`, and `.github/workflows/ci.yml`
fails if it drifts.

`waves.json` is **one shared file across every level** and its ids are global. A new wave needs an
id nothing here already uses, and a wave already here can be placed again instead of copied — an
edit to it then lands on every placement below.

| wave | lasts | spawns | entities | archetypes | placed by |
|---|---|---|---|---|---|
| `l1-opening-calm` | 8.0 s | 1 | 1 | `enemy-basic` | `level-01` #1 at 0.0 s |
| `l1-first-basics` | 14.0 s | 5 | 13 | `enemy-basic` | `level-01` #2 at 8.0 s |
| `l1-light-and-fast` | 11.0 s | 5 | 13 | `enemy-light` | `level-01` #3 at 22.0 s |
| `l1-combined-formations` | 13.0 s | 7 | 23 | `enemy-basic` `enemy-light` | `level-01` #4 at 33.0 s |
| `l1-tanks-and-priority` | 12.0 s | 5 | 13 | `enemy-tank` `enemy-basic` `enemy-light` | `level-01` #5 at 46.0 s |
| `l1-super-fast` | 9.0 s | 5 | 7 | `enemy-rush` | `level-01` #6 at 58.0 s |
| `l1-heavy-carrier` | 13.0 s | 3 | 7 | `enemy-carrier` `enemy-basic` `enemy-light` | `level-01` #7 at 67.0 s |
| `l1-evolved-shooters` | 10.0 s | 5 | 13 | `enemy-shooter` `enemy-basic` | `level-01` #8 at 80.0 s |
| `l1-high-pressure` | 11.0 s | 7 | 21 | `enemy-basic` `enemy-light` `enemy-shooter` `enemy-rush` | `level-01` #9 at 88.0 s |
| `l1-twin-carriers-attachment` | 16.0 s | 5 | 8 | `enemy-carrier` `enemy-rush` | `level-01` #10 at 97.5 s |
| `l1-brief-rest` | 6.0 s | 1 | 1 | `enemy-basic` | `level-01` #11 at 113.5 s |
| `l1-final-escalation` | 15.0 s | 12 | 32 | `enemy-basic` `enemy-light` `enemy-rush` `enemy-shooter` `enemy-tank` | `level-01` #12 at 119.5 s |

**`unplaced`** is a wave no level uses. Not an error — `waves.json` is a library — but it is dead
content until something places it, and nothing else in the repository would tell you.

Archetypes come from `assets/data/enemies.json` (6 of them) and formations from `assets/data/formations.json`.
Each level's own document has the rest: the pacing, the roster, the checks.
