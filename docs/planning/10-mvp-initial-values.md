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

### Movement speed — missing

This document fixes the **policy** for player movement (additive devices, clamped result, slow movement as a multiplier — see Controls below) but no concrete number for the ship's top speed or the slow-movement multiplier. Phase 02 needed both to implement the clamp and added them to `BalanceValues` as `playerSpeed()` and `playerSlowFactor()`, with placeholder values (140 logical units/s, ×0.45) that exist only in test fixtures — there is no production `BalanceValues` implementation yet for them to live in instead. **Open, not decided:** replace the placeholders with real numbers once there is a playable build to tune them against, and record the result here.

### Player starting position — missing

Nothing here fixes where the ship starts a run, only that it is inside the 208x270 playfield. Phase 04 needed a concrete point so a run never starts with an empty world, and added `BalanceValues.playerStartX()`/`.playerStartY()` with placeholder values (104, 30 — bottom-centre) that, same as the movement speed above, exist only in test fixtures. **Open, not decided:** replace with real numbers once there is a playable build.

### Enemy health and weapon/bomb damage — missing

`12-architecture.md` names `Health` as a component ("health points, enemies and boss") and shows `{"points": 40}` as a tank's value in its JSON schema example, but that `40` is illustrative there, not a decided balance number — no enemy hit-point value appears anywhere else in this document. Phase 05 needed `Health` to exist for a weapon upgrade to mean anything beyond more projectiles and for the bomb to be able to damage a tank or a heavy carrier instead of leaving them untouched, so it built the component and added `BalanceValues.weaponProjectileDamage()` and `.bombDamage()`, with placeholder values (10 and 50) that, same as the movement speed and starting position above, exist only in test fixtures. **Open, not decided:**

- per-archetype `Health` points for the level 1 roster (basic, light, shooter, rush, tank, carrier) and the boss. `game`'s `enemies.json` gives the two non-fragile archetypes a placeholder value each (tank 40, carrier 80, tank's number matching `12-architecture.md`'s illustrative example) — the four fragile archetypes carry no `"health"` entry, since a fragile hit destroys them outright regardless of it (see `Health`'s javadoc);
- `weaponProjectileDamage` and `bombDamage`.

Per `01-vision-and-scope.md`'s "difficulty through pressure" principle, these are **not** meant to become difficulty dials — difficulty "must not depend only on raising health and damage." They are fixed per-shot/per-detonation constants, tuned once against real gameplay, not values that scale with a difficulty setting that does not exist yet in the MVP anyway.

### Weapon and pickup values — missing

Phase 05 built `WeaponSystem`, `BombSystem` and `PickupSystem`, none of which had a concrete number
anywhere in this document. `game`'s `JsonBalanceValues`/`balance.json` carry placeholder values so the
game actually runs: `weaponFireCooldown` (0.15 s between volleys), `weaponProjectileSpeed` (220 logical
units/s), `pickupRadius` (6.0, larger than a pickup's sprite per `02-sprite-sizes.md`'s "magnetic" feel)
and `invulnerabilityPickupDuration` (3.0 s). **Open, not decided:** replace with real numbers once
there is a playable build to tune them against, and record the result here.

### Boss numbers — missing

`BossDefinition` needs thirteen values and this document had no boss row when phase 07 wrote them.
`level-01.json` carries a starting set so the fight exists at all: `entersAt 302`, `coreHealth 1800`,
`podHealth 500`, `armHealth 500`, `corePoints 1500`, `podPoints 500`, `armPoints 500`,
`entranceSpeed 25`, `combatY 175`, `patternCooldown 1.3`, `spreadProjectileSpeed 95`,
`sweepProjectileSpeed 140`. **Open, not decided**, with two things worth knowing before they are
retuned:

- The fight ends when the core dies, and `core-keel` carries the core's own health, so its length is
  governed by `2 * coreHealth` divided by whatever damage the player lands on the central column —
  not by the total the health bar shows. 1800 was chosen against an assumed 80 effective damage per
  second at weapon level 3, for a fight of roughly 45 s inside the 60–90 s the pacing table asks for.
  It moves the moment `weaponProjectileDamage` stops being a placeholder.
- `combatY` decides where the boss's projectiles leave the playfield, because `BossSystem`'s spread
  and sweep angles are fixed ratios. At 175 both patterns cross the side edges inside the band the
  player flies in (spread at y≈41, sweep at y≈25). Raising it makes the fight progressively harmless
  rather than progressively easier. Treat it as a pattern parameter, not as a camera framing choice.
- The points sum to exactly 5000, this document's figure for the boss, counting `corePoints` twice —
  once for the core, once for `core-keel`.

### Heavy carrier's spawner — missing

`Spawner` needs four values per holder and this document had none. `enemies.json` gives
`enemy-carrier`: `enemyId enemy-basic`, `interval 4.0`, `offsetX 0`, `offsetY -24`. **Open, not
decided.** The reasoning behind each is recorded in `docs/plan/07-boss/status.md`; the one that is a
design choice rather than a tuning knob is `offsetX 0`, which puts a carrier's children in the same
column the player must occupy to damage it.

### Enemy health against the level's own pacing — the encounter does not last

Recorded here by the content lane, because it is a balance number that a pacing decision now depends
on. At the placeholder `weaponProjectileDamage` of 10 and `weaponFireCooldown` of 0.15, one stream of
player fire does about 67 damage per second. Against the placeholder health in `enemies.json` that
makes a heavy carrier (80 hp) die in about 1.2 s and a tank (40 hp) in under one.

`level-01.json` reserves a 32-second window for the strong encounter — two carriers whose whole reason
for existing is the sustained pressure their spawners produce — and a 21-second stretch built around a
tank surviving long enough to force a priority shift. Neither survives contact with these numbers:
the encounter would be over before its first pair of children spawns. **Open, not decided:** the
non-fragile archetypes' health has to be set against how long their stretch is meant to last, not
picked in isolation. As an order of magnitude at the current damage figures, a carrier that is meant
to take ~15 s of sustained fire needs roughly 1000 hit points, and a tank that is meant to be flown
around rather than deleted needs a few hundred. Those are not proposals — they are the shape of the
arithmetic, so that whoever tunes damage and health knows the pacing constraint they are tuning
against.

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

As built in `level-01.json` (phase 07, content lane): calm 0:00-0:08, body 0:08-3:22, strong encounter
3:28-4:00, rest 4:00-4:16, final escalation 4:16-4:57, boss entering at 5:02 and its entrance taking
5.4 s. Total between 5:45 and 6:00 depending on how fast the core dies. The per-stretch intention
behind those figures is in `docs/plan/07-boss/status.md`, and it is what any retuning should preserve.

## Guaranteed drops

So that the MVP feels designed and not random, level 1 guarantees:

- a weapon upgrade in the first third, so the player understands the system early;
- a shield before the strong encounter;
- the attachment on defeating the strong encounter;
- a bomb recharge before the boss.

The rest of the drops are placed in the wave design as suits the pacing.

Where they landed in `level-01.json`: the weapon upgrade at 0:21 on the centre of a three-wide wall of
basics, the shield at 3:21 on a lone basic in a deliberate quiet hole, the attachment on slot 0 of the
two-carrier encounter at 3:28, and the bomb recharge at 4:05 on the only enemy of the rest. Three more
are placed for pacing rather than guaranteed: weapon upgrades at 2:14 and 4:24 — the second is a power
spike aimed straight into the boss — and an extra life at 3:03, on the level's hardest wave before the
encounter.

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
