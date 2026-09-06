---
name: boss-front-clock-cycle-fraction
description: why N=2 (the numerically closest cadence to a suggested period) failed a hard "fires during MOVING" requirement, why N=3 was needed instead, and how the cycle's real segment order was first misread and had to be corrected after review
metadata:
  type: project
---

For #329 (front weapon on its own clock, synced to the rear cycle by `frontPeriod =
rearCycleDuration / FRONT_SHOTS_PER_CYCLE`), the naive choice of `N` — closest numeric match to the
owner's suggested period — was wrong, and the reason only shows up when you check *where in the cycle*
each independent shot's fixed time-fraction lands, not just its absolute period.

**Why N=2 fails, N=3 works:** with a single independent shot per cycle (`N=2`), that shot always lands
at exactly the 50% mark of the cycle, deterministically, every single cycle — nothing varies it. If the
segment the code guarantees a `MOVING`-shot requirement against doesn't reach past that 50% mark, the
shot can never be observed in `MOVING`, no matter how long you trace. This isn't a probabilistic
near-miss fixable by more ticks — it's structural. `N=3` puts shots at 1/3 and 2/3 instead, and one of
those two landed inside `MOVING`.

**The mistake I actually made, caught by `reviewer` after the PR was up:** I assumed the cycle's steady
-state segment order was `COOLDOWN → TELLING → MOVING` (the same order the *very first* cycle runs,
since it has no leading move) and generalised that onto every cycle. The real order, from
`updateTelling`'s own code (`beginMove(world)` called immediately after the front-clock reset, in the
same tick the rear volley fires) is **`MOVING → COOLDOWN → TELLING`** in steady state — `MOVING` is
first, not last. This flips which independent shot (first vs. second) lands in `MOVING`, and which
segment catches the other one, though it did not change which `N` was the right answer. `reviewer`
caught it with a reflection probe tracing real ticks against the real compiled classes and
`level-01.json`'s actual `patternCooldown`, not by re-reading the prose.

**How to apply:**
1. When synchronising an independent periodic clock to phases of a state machine with a "must fire
   during phase X" requirement, check the *phase boundary as a fraction of the whole cycle* against
   each candidate `N`'s shot fractions (`k/N` for `k=1..N-1`), not just how close the resulting period
   is to a target number. A period can be numerically ideal and still structurally incapable of landing
   in the required phase.
2. **Don't infer a state machine's steady-state segment order from its first iteration.** A state
   machine's first cycle is often shaped by what precedes it (here: an entrance with no move yet) and
   is not representative of every cycle after it. Trace the actual reset/transition call sites
   (`grep` the method that resets your clock and see what it calls right next to that reset) rather
   than reasoning about "which phase comes first" from the enum's declaration order or an assumed
   narrative.
3. When a reviewer specifically distrusts an explanation but accepts the code and the conclusion,
   re-derive the numeric argument from the code's actual call order before touching the comment text —
   the fix is in the math, not the wording.

See [[boss-star-movement]] for the move-duration and cycle-length context this built on.
