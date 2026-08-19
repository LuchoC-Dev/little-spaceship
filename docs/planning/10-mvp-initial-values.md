# MVP initial values and operational decisions

This document gathers the starting values needed to build the MVP. **None of them is definitive**: they are starting points chosen so that implementation and first play can begin. All of them must live in configuration, not embedded in the code, because their purpose is to change during balancing.

When a value changes after playtesting, it is updated here.

## Ship and resources

| Concept | Initial value | Note |
|---|---|---|
| Initial lives | 3 | Already confirmed in the specification. |
| Maximum lives | 5 | Prevents stacking lives until tension becomes trivial. |
| Initial bombs | 2 | Enough to use them without hoarding them. |
| Maximum bombs | 3 | Value for the basic ship; each ship defines its own. |
| Shot levels | 4 | Base + 3 upgrades, distinguishable by shape and count. |

### Invulnerability

| Situation | Initial duration |
|---|---|
| After respawn | 2.0 s |
| After damage absorbed by shield or attachment | 1.0 s |

Invulnerability must be communicated visually in both cases, although the respawn blinking may be more pronounced.

### Picking up a power-up already at maximum

The pickup is **not wasted**: it turns into points. This avoids the dead drop and keeps the incentive to pick everything up. The proposed initial bonus is 500 points.

## Controls

When the mouse is enabled in Options, **keyboard and mouse work simultaneously and additively**. There is no priority device and no switching between one and the other.

Both produce a **movement vector** per frame and those vectors are **summed**. If the mouse pushes to the right and the keyboard to the left with the same intensity, the result is zero and the ship does not move: they cancel out. The result is clamped to the ship's maximum speed, so that combining the two devices never allows going faster than using just one.

This forces a concrete decision: the mouse is **relative**, not positional. It contributes the cursor's displacement between frames, instead of teleporting the ship to the pointer's position. It is the only way for summing and cancelling to make sense.

Shooting and bomb have no conflict: either device triggers them.

### Technical consequence to validate

A relative mouse needs to capture the pointer —Pointer Lock in the browser— because otherwise the cursor reaches the window edge and stops generating displacement even though the player keeps moving it. Pointer Lock requires a prior user click and hides the system cursor.

This goes into the technical prototype, which already had input validation planned.

## Presentation

### Resolution and scaling

The policy, more important than the concrete number:

- Fixed logical resolution, independent of the window size.
- **Integer** scaling (×2, ×3, ×4) so that pixel-art is never deformed.
- **Nearest-neighbour** filtering, without smoothing.
- Leftover space is resolved with letterbox, not by stretching the image.

Proposed starting point: **480×270 logical** (exact integer scale to 1920×1080), with the vertical playfield centred —208 px wide— and the HUD occupying the side margins, as is usual in a vertical shoot 'em up shown on a landscape screen.

The definitive value is set during the technical prototype, which already includes this validation, and in coordination with the real sprite size.

### Credits

The MVP includes a minimal credits and licences screen, accessible from Options. It is cheap to build and necessary as soon as any external asset with required attribution is used.

## Persistence in the MVP

The MVP does **not** save progress: there are no profiles, checkpoints or continuation.

It does save the **preferences**: master volume, music, effects and mouse enabling. Losing the chosen volume on every launch feels like a defect, and the cost is a single configuration entry. This is coherent with the decision that configuration is global and does not belong to any profile.

## Level 1 pacing

| Section | Target duration |
|---|---|
| Introduction and initial calm | 5-10 s |
| Body of the level up to the strong encounter | 3-4 min |
| Rest | 5-10 s |
| Final escalation | 45-60 s |
| Boss | 60-90 s |
| **Total** | **5-6 min** |

## Guaranteed drops

So that the MVP feels designed and not random, level 1 guarantees:

- a weapon upgrade in the first third, so the player understands the system early;
- a shield before the strong encounter;
- the attachment on defeating the strong encounter;
- a bomb recharge before the boss.

The rest of the drops are placed in the wave design as suits the pacing.

## Score

Base starting values:

| Source | Points |
|---|---|
| Basic enemy | 100 |
| Fast light | 150 |
| Evolved basic | 200 |
| Super-fast | 250 |
| Tank | 500 |
| Heavy carrier | 1000 |
| Destructible structure | 300 |
| Boss | 5000 |
| Power-up picked up at maximum | 500 |

On completing the level a bonus for remaining lives and bombs is added —1000 and 300 respectively— to reward finishing in good shape without introducing combos or multipliers, which are out of scope for the MVP.
