# 329 — the boss's front weapons get their own clock, and a move takes a fixed time

**Branch:** `feat/boss-front-weapons`. **Closes:** [#329](https://github.com/LuchoC-Dev/little-spaceship/issues/329).
`core-domain`, `core/` only: `core/src/main/java/.../domain/system/BossSystem.java` and its own test,
`BossSystemTest.java`.

## What changed

**1. A move now takes a fixed duration, whatever the distance.** `beginMove` no longer computes
`moveDuration` from `distance / MOVE_SPEED`; it is now the constant `MOVE_DURATION = 0.84f`, chosen to
reproduce exactly the slowest hop a player had actually seen under #325's old constant speed (45
units/second): the longest *legal* three-step hop, about 37.6 units, took about 0.84 s. `MOVE_SPEED`
is gone — `updateMoving`'s linear interpolation (`t = moveElapsed / moveDuration`) needed no other
change, since it was already distance-agnostic. One line to change, per the plan's own instruction.

**2. The pattern alternation is gone.** Before this change, the pods (spread) and the arms (sweep)
alternated through the same cooldown → tell → fire → move cycle, one pattern per cycle. Now the pods
are the **rear** weapon and keep exactly that cycle to themselves — nothing about their cooldown,
tell, standstill or aim-locking changed. The arms are the **front** weapon and fire on an independent
clock, `updateFrontWeapons`, run every tick of `FIGHT` from `updateFight` regardless of `fightStage` —
during `COOLDOWN`, `TELLING` and `MOVING` alike. Each front shot has no tell of its own: it computes a
fresh aim point at the instant it fires (`computeAimPoint`, extracted from what `lockAim` already did),
rather than reading a point frozen earlier — "each front shot locks the player's position at the
instant it fires," per the issue.

**3. The synchronisation rule is arithmetic, not a runtime correction.** `rearCycleDuration` (cached
once `BossDefinition` is known) is `patternCooldown + TELL_DURATION + MOVE_DURATION` — a constant for
the whole run, which change 1 is what makes possible. `frontPeriod = rearCycleDuration /
FRONT_SHOTS_PER_CYCLE`. `updateFrontWeapons` fires at most `FRONT_SHOTS_PER_CYCLE − 1` shots
independently per cycle, at `frontPeriod`, `2 * frontPeriod`, …; the cycle's own last shot always comes
from `updateTelling`, fired together with the rear volley the instant it resolves, and that is also
where `frontElapsed`/`frontShotsThisCycle` reset to zero. Since `FRONT_SHOTS_PER_CYCLE` shots, evenly
spaced, always span exactly `rearCycleDuration`, the front weapon always coincides with the rear volley
by construction — no correction, no drift to guard against.

## How the front cadence is driven while `fightStage == MOVING`

`updateFrontWeapons` never reads or writes `fightStage`. It is called unconditionally, once per tick,
from `updateFight`, right after the `switch` that dispatches to whichever of `updateCooldown` /
`updateTelling` / `updateMoving` is current. So the front weapon's clock advances identically whether
the boss is cooling down, telling or travelling, and a front shot can and does land mid-move.
`FightStage.MOVING` is still assigned in exactly one place — the tail of `updateTelling`, after
`fireRearVolley` has run — so the rear weapon's exclusion from #325 is untouched: no second call site
was added, and `updateFrontWeapons` is the only new caller of anything, and it calls neither
`beginMove` nor any state-machine transition.

## `FRONT_SHOTS_PER_CYCLE = 3`, and why not the numerically closer 2

Against real content (`patternCooldown` 0.7 s in `level-01.json`), the cycle is
0.7 + 0.75 + 0.84 = **2.29 s**. The owner's suggestion was a front period near 1.2 s.

- `N = 2` → period 1.145 s, only 0.055 s off the suggestion — numerically the best fit.
- `N = 3` → period 0.763 s, 0.437 s off — a worse fit on paper.

But `N = 2`'s one independent shot always lands at exactly half the cycle, and the tell alone
(`patternCooldown + TELL_DURATION = 1.45 s`) already covers 63.3% of it — so that shot would fire
during `TELLING` on *every* cycle, never during `MOVING`, failing the plan's own hard requirement that
a front shot be provably fired while the boss travels. Reaching that requirement with `N = 2` would
need `MOVE_DURATION > patternCooldown + TELL_DURATION = 1.45 s`, which is not "near 0.84 s" by any
reading. `N = 3` places its second independent shot at two-thirds of the cycle (0.667), past the 0.633
mark where the tell ends, so it lands inside `MOVING` on every single cycle — the smallest `N` for
which that holds at this `MOVE_DURATION`. `FRONT_SHOTS_PER_CYCLE` is a one-line constant, named and
commented with this reasoning in place, for the owner to retune by playing.

## Fire rate, measured rather than impressed

Both computed from the class's own constants, not observed in a play session.

- **Before this change** (per `docs/plan/11k-level-one-rebuilt/status/325-boss-star-movement.md`):
  spread and sweep alternated, one ten-projectile volley per cycle of `patternCooldown + 0.75 s +` a
  move of 0.05–0.84 s. At the fixture's real content values (`patternCooldown` 0.7 s) and the move
  range's midpoint (~0.445 s), that is one volley every ~1.895 s ≈ **0.53 fire events/s**, ~5.3
  projectiles/s.
- **After this change:** the rear still fires one ten-projectile volley every `rearCycleDuration` =
  2.29 s (≈ 0.437 events/s, ~4.4 projectiles/s from the rear alone), and the front fires
  `FRONT_SHOTS_PER_CYCLE` = 3 ten-projectile volleys in that same 2.29 s (≈ 1.31 events/s, ~13.1
  projectiles/s from the front alone — one of those three ticks coincides with the rear's own).
  Combined, **3 distinct fire ticks every 2.29 s ≈ 1.31 fire ticks/s**, about **2.5× the tick rate**
  and, in raw projectile count, ~17.5 projectiles/s against ~5.3 before — **about 3.3× as many
  projectiles per second.**

## Tests

`BossSystemTest.java`, 8 new or rewritten:

- `movesTakeTheSameFixedDurationRegardlessOfDistance` — two seeded runs land on star points at very
  different distances from the entrance's own landing spot (`Rng(1)` → point 5, `Rng(6)` → point 0,
  distances 67.8 and 112 units, a 1.65× ratio) and both take the same number of ticks,
  `Math.round(MOVE_DURATION / STEP)`, within one tick of tolerance.
- `rearVolleyFansFiveRaysPerPodAndFiresTogetherWithTheFront` (rewrite of the old
  `volleyFansFiveRaysPerSideAndAlternatesPattern`, which asserted alternation that no longer exists) —
  the first event containing a spread-speed (180) projectile is twenty projectiles total, ten at 180
  and ten at 160, in the same tick.
- `frontFiresMoreOftenAndCoincidesWithEveryRearVolley` — traces every fire event over 320 ticks
  (~3 cycles) and asserts each is either a 20-projectile coincidence or a pure 10-projectile,
  all-160-speed front shot, and that exactly `FRONT_SHOTS_PER_CYCLE − 1` (2) front-only events fall
  between any two coincidences — the test that would fail if the ratio drifted.
- `frontFiresWhileMoving` — asserts at least one pure front-only event in that same trace has
  `event.moving() == true`, read through a new package-visible `BossSystem.isMoving()` (the same
  precedent `STAR_X`/`STAR_Y` set for #325: visible only for the test, no accessor added to any port).
- The four pre-existing movement tests from #325 (`bossVisitsOnlyStarPointsWithinThreeSteps`,
  `samePatternForTheSameSeed`, `starPointsKeepTheWholeBossOnScreen`, `firesFromStandstillAfterMoving`)
  pass unmodified — they never depended on how `moveDuration` was computed, only on where the boss
  ends up and when it fires. `entranceDescendsToCombatY` and `tellStepsThroughThreeBeatsThenFires` also
  pass unmodified.

**No replay test reaches this code**, exactly as the plan warned: all three `BossReplayTest` fixtures
set `patternCooldown: 1000f` or configure no boss, so `TELLING` (and now `updateFrontWeapons`'s
coincident branch) is never reached in a full-pipeline replay. Their green (below) is not evidence for
this change; the proof lives entirely in `BossSystemTest`.

## Acceptance criteria

- [x] A move takes the same time whatever the distance, asserted on a short hop (67.8 units) and a
  long one (112 units) — `movesTakeTheSameFixedDurationRegardlessOfDistance`.
- [x] Every rear volley coincides with a front shot, and exactly `FRONT_SHOTS_PER_CYCLE − 1` (2) front
  shots fall between two rear volleys — `frontFiresMoreOftenAndCoincidesWithEveryRearVolley`, which
  would fail if the ratio drifted.
- [x] A front shot fires while `fightStage == MOVING` — `frontFiresWhileMoving`.
- [x] Each front shot aims at the player's position at the instant it fires — `computeAimPoint` is
  called fresh inside `fireFrontVolley`, never reading a field frozen earlier; covered indirectly by
  every test that moves the player and checks a front shot's direction.
- [x] The rear volley still fires from a standstill and still aims at the position frozen at the tell —
  `volleyAimsAtThePlayerLockedAtTellStart` (unmodified) and `firesFromStandstillAfterMoving`
  (unmodified) both pass.
- [x] The boss fires strictly more often than before: ~2.5× the fire-tick rate, ~3.3× the projectile
  rate — measured above from the class's own constants, not observed in play.
- [x] `./gradlew build` green across every module (`core`, `game`, `web`, `desktop`, `rngparity`); all
  five replay tests still pass; `grep -rn "com.badlogic.gdx\|Math.random"
  core/src/main/java/.../BossSystem.java` prints nothing.
- [ ] Whether the fight is hard enough, whether the front weapons read as pressure rather than noise,
  whether the fixed move duration or `N` are right — the project owner's, not attempted here.

## Commands run

- `./gradlew :core:test --tests "dev.luchoc.littlespaceship.core.domain.system.BossSystemTest"` —
  green, 18 tests (10 pre-existing from before #325, 4 from #325 unmodified, 1 rewritten, 3 new).
- `./gradlew :core:test --tests "*ReplayTest"` — green, all five replay tests.
- `./gradlew build` — green across every module.
- `grep -rn "com.badlogic.gdx\|Math.random\|System.currentTimeMillis\|new Thread\|ExecutorService\|CompletableFuture" core/src/main/java/.../BossSystem.java` — no matches.
