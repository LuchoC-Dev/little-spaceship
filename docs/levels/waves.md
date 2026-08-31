# Every wave, and who places it

**This file is generated. Do not edit it by hand.** `tools/build-level-docs.js` writes it from
`assets/data/waves.json` and every `assets/data/level-NN.json`, and `.github/workflows/ci.yml`
fails if it drifts.

`waves.json` is **one shared file across every level** and its ids are global. A new wave needs an
id nothing here already uses, and a wave already here can be placed again instead of copied — an
edit to it then lands on every placement below.

| wave | lasts | spawns | entities | archetypes | placed by |
|---|---|---|---|---|---|
| `l1-basic-intro` | 27.5 s | 7 | 15 | `enemy-basic` | `level-01` #1 at 8.0 s |
| `l1-light-intro` | 24.0 s | 7 | 21 | `enemy-light` | `level-01` #2 at 35.5 s |
| `l1-basic-light-mix` | 26.5 s | 11 | 41 | `enemy-basic` `enemy-light` | `level-01` #3 at 59.5 s |
| `l1-tank-solo` | 1.0 s | 1 | 1 | `enemy-tank` | `level-01` #4 at 86.0 s, `level-01` #7 at 126.5 s, `level-01` #15 at 297.0 s |
| `l1-tank-intro-b` | 20.0 s | 6 | 18 | `enemy-basic` `enemy-tank` `enemy-light` | `level-01` #5 at 92.0 s |
| `l1-rush-intro-a` | 14.5 s | 5 | 11 | `enemy-rush` | `level-01` #6 at 112.0 s |
| `l1-rush-intro-b` | 9.0 s | 3 | 5 | `enemy-rush` | `level-01` #8 at 129.0 s |
| `l1-carrier-intro` | 28.0 s | 4 | 10 | `enemy-carrier` `enemy-basic` `enemy-light` | `level-01` #9 at 138.0 s |
| `l1-shooter-intro` | 17.0 s | 5 | 11 | `enemy-shooter` `enemy-light` | `level-01` #10 at 166.0 s |
| `l1-veteran-mix` | 25.0 s | 12 | 34 | `enemy-shooter` `enemy-rush` `enemy-basic` `enemy-light` `enemy-tank` | `level-01` #11 at 183.0 s |
| `l1-carrier-pair` | 37.0 s | 5 | 14 | `enemy-carrier` `enemy-rush` `enemy-light` | `level-01` #12 at 208.0 s |
| `l1-rest-basic` | 11.0 s | 1 | 1 | `enemy-basic` | `level-01` #13 at 245.0 s |
| `l1-finale-a` | 41.0 s | 23 | 77 | `enemy-basic` `enemy-light` `enemy-rush` `enemy-shooter` `enemy-tank` `enemy-carrier` | `level-01` #14 at 256.0 s |

**`unplaced`** is a wave no level uses. Not an error — `waves.json` is a library — but it is dead
content until something places it, and nothing else in the repository would tell you.

Archetypes come from `assets/data/enemies.json` (6 of them) and formations from `assets/data/formations.json`.
Each level's own document has the rest: the pacing, the roster, the checks.
