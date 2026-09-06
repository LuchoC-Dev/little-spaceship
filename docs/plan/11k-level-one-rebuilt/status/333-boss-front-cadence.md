# 333 — lower the boss's front-weapon cadence from N=3 to N=2

**Branch:** `fix/boss-front-cadence`. **Closes:** [#333](https://github.com/LuchoC-Dev/little-spaceship/issues/333).
`core-domain`, `core/` only: `core/src/main/java/.../domain/system/BossSystem.java` and its own test,
`BossSystemTest.java`. Built on top of #329 (merged as PR #332 into `phase/11k-level-one-rebuilt`).

## What changed

**`FRONT_SHOTS_PER_CYCLE` goes from 3 to 2.** One line. `frontPeriod = rearCycleDuration /
FRONT_SHOTS_PER_CYCLE` goes from ≈0.763 s to ≈1.145 s against real content
(`patternCooldown` 0.7 s), and the front fires two shots per cycle (one independent, one coincident
with the rear volley) instead of three. Nothing else about the mechanism changed: the coincidence
still happens the same way, in `updateTelling`, at the instant the rear volley fires; the independent
clock still runs in `updateFrontWeapons`, called every tick of `FIGHT` regardless of `fightStage`.

## The argument that inverts, and why it does not mean the front weapon is now gated

#329 chose `N = 3` over the numerically closer `N = 2` specifically because `N = 2`'s one independent
shot lands at exactly half the cycle, which — in the steady-state order `MOVING → COOLDOWN → TELLING`
that #329's own review correction established — falls inside `COOLDOWN` (36.7%–63.3% of the cycle),
never inside `MOVING` (the first 36.7%). That was written as a hard acceptance criterion: "a front shot
fires while `fightStage == MOVING`, proven by a test."

**The owner played `N = 3`, found it too hard, and asked for the cadence to be lowered — and that
instruction overrides the criterion, not the other way around.** The criterion was the coordinator's
own hardening of the owner's original ask, which was narrower and remains true at `N = 2`: the front
weapon must not be *gated* by movement — it must fire on its own clock whether the boss is stopped or
travelling, never waiting for a move to end or refusing to run during one. That property is untouched:
`updateFrontWeapons` is still called unconditionally every tick of `FIGHT`, still never reads or writes
`fightStage`, and its clock still advances through the entire `MOVING` window without pausing — proven
below. What is given up is only the *stronger*, coordinator-added claim that a shot demonstrably lands
inside `MOVING` at this specific ratio — which was never something the owner asked for by name, only a
way of making "not gated" checkable that stopped being achievable once the owner chose a slower
cadence.

`BossSystem`'s own `FRONT_SHOTS_PER_CYCLE` javadoc is rewritten to say this — not deleted, corrected —
including that the front period at `N = 2` (≈1.145 s against real content) lands closer to the owner's
original "around 1.2 s" suggestion from #329 than `N = 3`'s 0.763 s ever did.

## The test that had to fail, and what replaced it

`frontFiresWhileMoving` (from #329) asserted a pure front-only shot with `isMoving() == true` appears
in a 320-tick trace. At `N = 2` this is now false — the independent shot lands in `COOLDOWN` for this
fixture — and the test failed exactly as expected once `FRONT_SHOTS_PER_CYCLE` changed. **It is
deleted, not weakened or commented out.** Asserting its negation ("never during `MOVING`") would have
been a fact about this one ratio, not about the design, and would silently start passing again the
moment `FRONT_SHOTS_PER_CYCLE` is retuned back toward 3 — telling a future reader nothing true about
what the design actually guarantees either way.

**What replaces it:** `frontClockAdvancesThroughTheWholeMovingWindowRegardlessOfFightStage`, which
asserts the property that survives — the front clock is never paused by `fightStage`, `MOVING`
included — without needing a shot to actually land inside `MOVING`. The trick: at this fixture's
numbers (cooldown 0.2 s, tell 0.75 s, move 0.84 s, cycle 1.79 s), the independent shot's own period
(0.895 s) is *longer* than `MOVE_DURATION` (0.84 s). So reaching that shot at all requires the entire
`MOVING` window to have already elapsed, tick by tick, with the front clock still counting. If
`updateFrontWeapons` were gated by `fightStage` — paused during `MOVING` and resumed after — the
independent shot would arrive roughly `MOVE_DURATION` later than it does. The test finds the first
rear/front coincidence, finds the very next fire event (the independent shot, confirmed to be a pure
ten-projectile, 160-speed volley), and asserts the tick delta between them exceeds `MOVE_DURATION` —
which would be false if the clock had paused. It also records, as a fact rather than a requirement,
that this shot lands in `COOLDOWN` rather than `MOVING` at this ratio.

## Fire rate, measured

All computed from the class's own constants, not observed in a play session — the same method #329's
fragment used.

- **Before this change** (`N = 3`, PR #332): cycle 2.29 s (real content), rear 1 volley/cycle, front 3
  volleys/cycle → ≈1.31 fire ticks/s, ≈17.47 projectiles/s.
- **After this change** (`N = 2`): same 2.29 s cycle, rear 1 volley/cycle, front 2 volleys/cycle (one
  independent, one coincident) → ≈0.873 fire ticks/s, **≈13.10 projectiles/s** — a 25% reduction from
  `N = 3`.
- For context, against the pre-#329 baseline (alternating spread/sweep, ≈5.28 projectiles/s): `N = 2`
  is still ≈2.48× that baseline, against `N = 3`'s ≈3.31× — lower, but still a real increase over what
  the owner approved before #329, consistent with the front weapon still existing and still firing
  independently of movement.

## The lever not taken: fewer rays per front volley

Lowering `FAN_COUNT` for the front weapon alone (below five rays) would reduce front damage while
keeping `N = 3`'s faster rhythm and keeping a shot provably inside `MOVING`. The coordinator raised
both levers with the owner; the owner's instruction was the cadence, not the ray count. Recorded here,
not attempted: `FAN_COUNT` is currently one constant shared by both the rear and front weapons
(`fireAimedFan` takes no per-weapon ray count), so splitting it would need its own constant, not a
value change — worth knowing before the next tuning pass reaches for it.

## Tests

`BossSystemTest.java`:

- `frontFiresWhileMoving` (#329) — **deleted**, per the reasoning above.
- `frontClockAdvancesThroughTheWholeMovingWindowRegardlessOfFightStage` — **new**, replaces it.
- `frontFiresMoreOftenAndCoincidesWithEveryRearVolley` (#329) — unmodified, still passes: it reads
  `BossSystem.FRONT_SHOTS_PER_CYCLE` generically, so it now checks "exactly 1 (N−1) independent shot
  between two coincidences" without any source change, and would still fail if the ratio drifted.
- `rearVolleyFansFiveRaysPerPodAndFiresTogetherWithTheFront`, `movesTakeTheSameFixedDurationRegardlessOfDistance`
  and every other #329/#325 test — unmodified, still pass: none of them depended on the value of
  `FRONT_SHOTS_PER_CYCLE`.
- `FireEvent` gained a `tick` field (1-based, from `traceFireEvents`) to support the delta-timing
  assertion above; every existing caller of `FireEvent`/`traceFireEvents` needed no other change.

**No replay test reaches this code**, unchanged from #329: all three `BossReplayTest` fixtures set
`patternCooldown: 1000f` or configure no boss, so `TELLING` is never reached in a full-pipeline replay.

## Acceptance criteria

- [x] `FRONT_SHOTS_PER_CYCLE` is 2, and exactly one front shot falls between two rear volleys —
  `frontFiresMoreOftenAndCoincidesWithEveryRearVolley` (unmodified, parametric on the constant).
- [x] They still fire together at every rear volley, with no drift over many cycles — same test.
- [x] The front clock still runs regardless of `fightStage`, asserted by a test —
  `frontClockAdvancesThroughTheWholeMovingWindowRegardlessOfFightStage`.
- [x] The reasoning in the javadoc and this fragment describes the real steady-state cycle order
  (`MOVING`, then `COOLDOWN`, then `TELLING`) and says where the shot now lands (`COOLDOWN`, for this
  fixture and for real content).
- [x] The new fire rate is measured and stated: ≈17.47 → ≈13.10 projectiles/s (a 25% reduction).
- [x] The rear volley, the fixed move duration, the star movement and the single assignment site of
  `FightStage.MOVING` are unchanged — no edit touched `fireRearVolley`, `beginMove`, `updateMoving`,
  `STAR_X`/`STAR_Y`, or the `fightStage = FightStage.MOVING` line.
- [x] `./gradlew build` green; no replay cites this code as evidence.
- [ ] Whether the fight is now right — the project owner's, not attempted here.

## Commands run

- `./gradlew :core:compileJava` — green.
- `./gradlew :core:test --tests "dev.luchoc.littlespaceship.core.domain.system.BossSystemTest"` —
  green, 18 tests (17 pre-existing unmodified, 1 deleted, 1 new).
- `./gradlew build` — green across every module (`core`, `game`, `web`, `desktop`, `rngparity`).
