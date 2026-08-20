# Sprite sizes and hitboxes

**This is synchronisation point 1.** Phase 03 writes hitboxes from this table and no sprite is drawn
before it. Everything here is in pixels at the logical resolution of 480x270. There are no
proportions in this document: at this size a proportion is unusable.

## The five rules behind the table

**1. Every dimension is odd.** A sprite is drawn centred on its `Transform`, so an odd width puts
the axis of symmetry on a single pixel column instead of straddling two. Even dimensions produce a
ship that visibly wobbles when it crosses a half-pixel.

**2. The collider is a circle, centred on the same point.** `core.domain.component.Collider` is a
radius and a layer — there is no offset field and no rectangle. So the hitbox is always concentric
with the sprite, and the art has to be drawn around that: the visual mass belongs at the centre.

**3. Collidable mass sits inside the circle.** At least 85% of a colliding sprite's opaque pixels
fall within its radius. Anything outside must be at most 3 px thin and read as secondary —
wingtips, antennae, exhaust. A player who shoots a wing and sees nothing happen is looking at a
bug, whether or not the code has one.

**4. Generosity always favours the player.** It is applied by ratio, in one direction only:

| | Hitbox against sprite |
|---|---|
| Enemy fire | **smaller** — it kills you at less than it looks |
| Enemies and structures | **as drawn** — you hit what you aim at |
| Pickups | **larger** — they feel magnetic |
| The player ship | **far smaller** — 6 px across a 15 px ship |

**5. Radii may be fractional.** `Collider.radius` is a float and the comparison is a squared
distance, so 4.5 costs exactly what 4 costs.

## Player

| What | Sprite | Radius | Proposed id | Notes |
|---|---|---|---|---|
| Ship | 15x17 | **3.0** | `ship-basic` | the 6 px hitbox is the fuselage, not the wings |
| Ship explosion | 31x31 | — | `fx-explosion-player` | |
| Attachment satellite | 7x9 | — | `module-*` | one per side, flanking; no collider of its own |

The ship's hitbox is 40% of its width. That is the whole "smaller than the sprite, but not a single
point" rule from `../planning/02-mvp-functional-spec.md` turned into a number: forgiving enough for
an intermediate-density shoot 'em up, large enough that dodging is about position rather than about
finding an invisible pixel. It is never drawn on screen.

The three wing pixels on each side sit outside the circle by design, and every silhouette of the
ship must keep them thin so the hitbox stays believable.

## Projectiles

Player fire is **cyan and elongated**. Enemy fire is **magenta and compact**. Shape carries the
distinction as well as colour, so it survives a colour-blind player and a bright explosion alike.

| What | Sprite | Radius | Proposed id |
|---|---|---|---|
| Player shot, level 1 | 3x9 | 1.5 | `shot-p1` |
| Player shot, level 2 | 3x9, two of them | 1.5 each | `shot-p1` |
| Player shot, level 3 | 5x11 centre plus two 3x9 | 2.0 / 1.5 | `shot-p2`, `shot-p1` |
| Player shot, level 4 | 5x11 centre plus four 3x9 | 2.0 / 1.5 | `shot-p2`, `shot-p1` |
| Enemy bullet, small | 5x5 | 2.0 | `shot-e-small` |
| Enemy bullet, heavy | 7x7 | 3.0 | `shot-e-heavy` |
| Enemy bolt, aimed | 5x11 | 2.0 | `shot-e-bolt` |

The four weapon levels are told apart by **count and shape**, never by colour: 1, 2, 3 and 5
projectiles, with the wider `shot-p2` appearing from level 3. That satisfies the spec's requirement
that the level be recognisable without a numeric indicator, and it keeps working for a player who
cannot separate the hues.

Enemy bullets are never smaller than 5x5. Below that a projectile stops being a shape and becomes a
speck.

## Enemy archetypes

The six from `../planning/02-mvp-functional-spec.md`, in the order the level introduces them.

| Archetype | Sprite | Radius | Coverage | Proposed id |
|---|---|---|---|---|
| Basic | 13x13 | 5.5 | 85% | `enemy-basic` |
| Fast light | 11x13 | 4.5 | 82% | `enemy-light` |
| Evolved basic / shooter | 15x15 | 6.5 | 87% | `enemy-shooter` |
| Super-fast | 9x15 | 4.0 | 89% | `enemy-rush` |
| Tank | 23x23 | 10.5 | 91% | `enemy-tank` |
| Heavy carrier | 39x31 | 15.0 | 97% of its height | `enemy-carrier` |

Coverage is the collider's diameter against the sprite's **smaller** dimension.

The carrier is the one exception worth explaining. It is 39 px wide and its circle is 30 across, so
its outer 4 px each side are wing and must be drawn as wing: thin, dark, obviously not hull. Its
mass and its damage-taking silhouette are the central 31x31.

Sizes are staged deliberately. The basic enemy is 13 px against the player's 15, so the player never
looks outgunned by a single unit; the tank at 23 and the carrier at 39 read as a different class of
problem the moment they enter, before they have done anything.

## Boss

| What | Sprite | Proposed id |
|---|---|---|
| Boss | 119x87 | `boss-l1` |
| Boss explosion, chained | 95x95 plus medium and large | `fx-explosion-boss` |

At 119 px the boss occupies 57% of the 208 px playfield: dominant, with dodging room left on both
sides.

**A boss that wide cannot be one circle.** A single collider large enough to cover it would swallow
the gaps the player is meant to fly through. It is built from **five entities** on the `ENEMY`
layer, moved together:

Offsets are in pixels from the boss centre, x growing right and y growing up, as in `Transform`.

| Part | Radius | Offset from the boss centre |
|---|---|---|
| Core | 18.0 | 0, 0 |
| Pod, left and right | 12.0 | -34, +6 and +34, +6 |
| Arm, left and right | 14.0 | -44, -18 and +44, -18 |

Phase 07 owns the boss's behaviour, phases and look; this fixes only its footprint and how it takes
hits. If phase 07 needs different parts it should change them here first, because the art is drawn
against this map.

## Pickups and structures

| What | Sprite | Radius | Proposed id |
|---|---|---|---|
| Power-up capsule | 11x11 | 6.0 | `pickup-weapon`, `pickup-shield`, `pickup-life`, `pickup-bomb`, `pickup-invuln` |
| Attachment capsule | 13x13 | 7.0 | `pickup-module` |
| Destructible structure | 31x39 | 15.0 | `structure-*` |

Pickup radii exceed the sprite on purpose — 12 px of collision on an 11 px capsule. Missing a
power-up you flew through is the kind of unfairness a player remembers, and nothing about the game
depends on it being precise.

The five power-ups share one capsule silhouette and are told apart by the **icon inside it**, which
is the same icon the HUD uses. Learn it once, read it everywhere.

## Explosions

Sized against what they replace, always larger so the destruction covers its cause.

| What | Sprite | Covers |
|---|---|---|
| Small | 21x21 | basic, light, shooter, rush, projectiles |
| Medium | 31x31 | tank, structures, the player |
| Large | 47x47 | carrier, boss parts |
| Boss | 95x95 | the final chain |

## Frame budget

Not a specification — a budget, so the atlas can be planned and so a sprite is not drawn with
sixteen frames when the game shows four. Art production may adjust it with a reason.

| Animation | Frames |
|---|---|
| Ship idle | 2 |
| Ship tilt, each side | 2 held, 1 transition |
| Ship thrust | 2, looping with idle |
| Ship hit | 1, a full N7 silhouette |
| Enemy idle | 2 |
| Explosion, small | 5 |
| Explosion, medium | 6 |
| Explosion, large | 8 |
| Pickup float | 4 |

## What the code lane needs from this page

Only two columns: **sprite** and **radius**. Everything else is for whoever draws. The sprite ids
are a proposal for synchronisation point 2 in phase 04, not a decision this document gets to make.
