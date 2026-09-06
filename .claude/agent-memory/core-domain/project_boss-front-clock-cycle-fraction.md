---
name: boss-front-clock-cycle-fraction
description: why N=2 (the numerically closest cadence to a suggested period) failed a hard "fires during MOVING" requirement, and why N=3 was needed instead
metadata:
  type: project
---

For #329 (front weapon on its own clock, synced to the rear cycle by `frontPeriod =
rearCycleDuration / FRONT_SHOTS_PER_CYCLE`), the naive choice of `N` — closest numeric match to the
owner's suggested period — was wrong, and the reason only shows up when you check *where in the cycle*
each independent shot's fixed time-fraction lands, not just its absolute period.

**Why:** with a single independent shot per cycle (`N=2`), that shot always lands at exactly the
50% mark of the cycle, deterministically, every single cycle — nothing varies it. If `TELLING` +
`COOLDOWN` together cover more than 50% of the cycle (true here: 63.3% at real content values), that
lone shot fires during `TELLING` on *every* cycle and can never be observed firing during `MOVING`,
no matter how long you trace. This isn't a probabilistic near-miss fixable by more ticks — it's
structural. Increasing `N` to 3 puts a shot at the two-thirds mark instead, past where the tell ends,
landing inside `MOVING` on every cycle.

**How to apply:** when synchronising an independent periodic clock to phases of a state machine with a
"must fire during phase X" requirement, check the *phase boundary as a fraction of the whole cycle*
against each candidate `N`'s shot fractions (`k/N` for `k=1..N-1`), not just how close the resulting
period is to a target number. A period can be numerically ideal and still structurally incapable of
landing in the required phase. See [[boss-star-movement]] for the move-duration and cycle-length
context this built on.
