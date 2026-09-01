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

This document fixes the **policy** for player movement (additive devices, clamped result, slow movement as a multiplier — see Controls below) but no concrete number for the ship's top speed or the slow-movement multiplier. Phase 02 needed both to implement the clamp and added them to `BalanceValues` as `playerSpeed()` and `playerSlowFactor()`, with placeholder values of 140 logical units/s and ×0.45. They live in `assets/data/balance.json`, read by `JsonBalanceValues`, since phase 05. **Open, not decided:** replace the placeholders with real numbers, and record the result here.

### Player starting position — missing

Nothing here fixes where the ship starts a run, only that it is inside the 208x270 playfield. Phase 04 needed a concrete point so a run never starts with an empty world, and added `BalanceValues.playerStartX()`/`.playerStartY()` with placeholder values of 104, 30 — bottom-centre. They are in `balance.json`. **Open, not decided:** replace with real numbers.

### Enemy health and weapon/bomb damage — missing

`12-architecture.md` names `Health` as a component ("health points, enemies and boss") and shows `{"points": 40}` as a tank's value in its JSON schema example, but that `40` is illustrative there, not a decided balance number — no enemy hit-point value appears anywhere else in this document. Phase 05 needed `Health` to exist for a weapon upgrade to mean anything beyond more projectiles and for the bomb to be able to damage a tank or a heavy carrier instead of leaving them untouched, so it built the component and added `BalanceValues.weaponProjectileDamage()` and `.bombDamage()`, with placeholder values of 10 and 50. They are in `balance.json`. **Open, not decided:**

- per-archetype `Health` points for the level 1 roster (basic, light, shooter, rush, tank, carrier) and the boss. **A candidate set was proposed on 01/09/2026 in phase 11e and is in `assets/data/enemies.json`: basic 30, light 20, shooter 40, tank 300, carrier 1000, rush none.** It replaces the earlier placeholders (tank 40, carrier 80, and no `"health"` at all on the four fragile archetypes). It is a candidate, not a decision — the reasoning behind each number, and what a play session has to confirm, is in "Enemy health against the level's own pacing" below;
- `weaponProjectileDamage` and `bombDamage`.

Per `01-vision-and-scope.md`'s "difficulty through pressure" principle, these are **not** meant to become difficulty dials — difficulty "must not depend only on raising health and damage." They are fixed per-shot/per-detonation constants, tuned once against real gameplay, not values that scale with a difficulty setting that does not exist yet in the MVP anyway.

### Weapon and pickup values — open

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
`entranceSpeed 25`, `combatY 175`, `patternCooldown 0.7`, `spreadProjectileSpeed 95`,
`sweepProjectileSpeed 140`. **Open, not decided**, with four things worth knowing before they are
retuned:

- `patternCooldown` was **1.3 and is now 0.7**, changed on 25/08/2026 after a play session, which
  takes the boss's attack cycle from 2.05 s to 1.45 s against its fixed 0.75 s tell. It is the only
  value on this page that has been through the "tuned by playing" loop, and it was not written back
  here — which is what the sentence at the top of this document exists to prevent. Recorded
  26/08/2026 by phase 10a.

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

### Enemy health against the level's own pacing — a candidate set, proposed 01/09/2026

Recorded here by the content lane, because it is a balance number that a pacing decision depends on.

**The problem, as it stood.** At `weaponProjectileDamage` 10 and `weaponFireCooldown` 0.15, one
stream of player fire does about 67 damage per second. Against the old placeholders that made a heavy
carrier (80 hp) die in about 1.2 s and a tank (40 hp) in under one, while `level-01.json` reserves
28 s and 37 s for the carrier stretches and 20 s for the tank's. The carrier's `spawner` fires every
4.0 s, so **at 80 hp the carrier died before producing a single child — its entire designed mechanism
never happened.** The four fragile archetypes carried no `Health` at all, so `enemy-basic`'s "low
health and a slow shot" could not be told from `enemy-shooter`'s fast one: neither lived long enough
to fire twice under fire.

**"Projectiles to kill" is not "trigger pulls to kill", and the column in `docs/levels/level-01.md`
counts projectiles.** `core/domain/system/WeaponSystem.java:96` fires 1, 2, 3 and 5 parallel
projectiles at shot levels 1 to 4, spaced 3 units apart, so one pull lands **all** of them on any
target wider than about 12 units — which is every archetype in the roster. A pull is therefore worth
10, 20, 30 or 50 damage, and the table below is written against a single projectile. Concretely, at
shot level 4 a basic, a light and a shooter all die to one pull whatever their `Health` says. Level 1
hands out `weapon-upgrade` at 21.0 s, 133.5 s and 256.0 s, so the fragile numbers below read as
written for the first ~130 s — the stretch where those archetypes are introduced — and collapse to
one pull afterwards. That is the weapon upgrade doing its job, not the numbers failing, but it is the
reason `enemy-basic`'s rate-of-fire contrast can only be judged early in the level.

**The candidate, in `assets/data/enemies.json`.** Derived columns for all of it are in the Roster
section of `docs/levels/level-01.md`, which is generated by `tools/build-level-docs.js`.

| archetype | health | projectiles to kill | reasoning |
|---|---|---|---|
| `enemy-rush` | none | 1 | Unchanged. Its threat is the ram and a 3.4 s screen time on `dive`, not resistance, and one of the play session's four watch-items is about its single shot per pass — moving its health mid-flight would change what that question measures. |
| `enemy-light` | 20 | 2 | The least durable thing that is not a one-hit kill. Its pressure is a 130 u/s projectile and a 6.9 s pass, so it must stay flimsy — but 2 makes a `vee-5` cost ten projectiles rather than five, which is the whole point of flying it in fives. |
| `enemy-basic` | 30 | 3 | The spec's "low health": three projectiles is 0.30 s of held fire at shot level 1, so killing one is an act rather than a reflex, and a `line-3` costs nine. Above 10, so it is not the no-op the plan's "2–3" would have produced read as points. |
| `enemy-shooter` | 40 | 4 | Not named by the task, changed for coherence. Leaving it at one hit while the basic took three would have **inverted** the contrast the spec asks for: the bigger, faster-firing archetype would have died first and the basic would have outlived it. |
| `enemy-tank` | 300 | 30 | "A few hundred", the order of magnitude this section already recorded. 4.5 s of held fire at shot level 1 against a 20 s stretch and a 31 s pass: enough to force the priority shift the beat is built on, not enough to be a wall. |
| `enemy-carrier` | 1000 | 100 | The ~1000 this section already derived for "roughly 15 s of sustained fire" at 67 dps. At the shot level the player realistically holds when carriers first appear it is around 5 s of ideal fire out of a 28 s stretch, so **the carrier now produces children while being shot at**, which is the pressure the beat was designed around. |

**This is a candidate and not a decision.** Balance here is settled by playing — decided twice, on
22/08 and 25/08 — so these numbers stand until the session that phase 11e's plan asks for either
confirms them or moves them.

**Still open, and touched by this change:**

- **`bombDamage` 50 against a carrier at 1000.** A bomb now removes 5% of a carrier, where it used to
  remove 62%. `02-mvp-functional-spec.md` asks the bomb for "heavy damage to resistant enemies" and 5%
  is not that. The fragile archetypes are unaffected — `core/domain/system/BombSystem.java:115`
  destroys a `fragile` enemy outright whatever `Health` says, so basic, light, shooter and rush still
  vanish in one detonation regardless of the numbers above. `bombDamage` was left alone here because
  it also lands on the boss's parts, and the boss is another task's. **Open, not decided.**
- **`weaponProjectileDamage` 10 itself**, still a placeholder. Every number in the table above is
  expressed against it and scales with it.

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
