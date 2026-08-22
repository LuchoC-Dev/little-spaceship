# Level 1 content mechanics — what the format actually does

Written while replacing the `level-01.json` test fixture with the real level (phase 07, feat/boss).
Everything here is behaviour of the existing pipeline that a level's pacing depends on and that is
not stated in `docs/planning/`.

## Where a formation actually lands

`SpawnSystem.spawnWave` places the whole wave off-screen by measuring only the slot with the
**lowest** `offsetY` — that slot sits tangent to the top edge at `270 + its own collider radius`, and
every other slot ends up higher by the difference. Consequences worth designing around:

- A slot's `offsetY` is not a delay in seconds, it is a head start in pixels. The gap in seconds
  between two slots of a column is `offsetY difference / trajectory vy`. A `column-3` with 22 px
  spacing arrives 1.2 s apart on `slow-descent` (-18) and 0.27 s apart on `dive` (-80). The same
  formation is a stream or a burst depending purely on which archetype uses it.
- `offsetY` **positive** means "enters later", because positive is upward.
- Horizontal footprint is `atX * 208 + offsetX ± radius`, with no clamping anywhere. Nothing stops a
  wave being written half off-screen; it just spawns there. Check every `atX` against the widest slot
  of its formation and the archetype's own radius.

## Motion is archetype data, not event data

`enemies.json` binds one trajectory per archetype. "A tank on the rush's trajectory is a data change"
is true only in the sense that it means editing `enemies.json` — there is no per-event trajectory
override on `SpawnEvent`. With four trajectories and six archetypes, the levers a level actually has
are formation, `atX`, timing and simultaneity. Plan the curve around those.

## Nothing despawns except projectiles

`LifetimeSystem` only expires the two projectile layers. An enemy that reaches the bottom keeps
existing and keeps moving down forever. Harmless for correctness, but it means a slow archetype's
real screen time is `(270 + radius) / |vy|` — a `crawl` enemy (-9) occupies the playfield for about
30 s. That is a pacing lever: one tank at `crawl` is still present three waves later, which is what
makes "priority shift" work without any new behaviour.

## The carrier's spawner

`Spawner.timer` starts at `interval`, so the first child appears one full interval after the carrier
does, never on spawn. Children are placed at the carrier's *current* position plus the offset, so
they can appear off-screen if the carrier is still entering — with `crawl` and a 4 s interval the
first child is comfortably visible.

Two carriers spawned by the same `SpawnEvent` share a tick, so their timers stay in lockstep for the
whole encounter: children arrive in synchronised pairs. To stagger them, they have to be two separate
events, which costs the two-slot formation and the `dropSlot` guarantee.

## The boss's geometry is decided by `combatY`

`BossSystem` bakes the spread and sweep angles as fixed ratios (spread 0.45/-0.90 outward from the
pods, sweep 0.75/-0.65 inward from the arms). Both are shallower than 45°, so every projectile leaves
through a **side** edge, never the bottom. How low it leaves is a pure function of `combatY`: a
spread shot exits the side 134 units below the core, a sweep shot 128 units below it. Set `combatY`
so those numbers land in the band the player flies in, or the boss cannot hit anything.

The core spawns at y 310 and descends to `combatY` at `entranceSpeed`, so the entrance duration is
`(310 - combatY) / entranceSpeed` — budget it inside the level's length.

The fight ends when the core dies, and `core-keel` carries the core's own health independently, so
the practical health of the "kill target" is `2 * coreHealth`, while the health bar shows the sum
across all six parts. Killing pods and arms shortens the bar without shortening the fight.

## `dropSlot` and the boss block are written but not yet parsed

As of this pass, `JsonContentSource.loadLevel` reads five fields and uses `SpawnEvent`'s
five-argument constructor, so `dropSlot` silently defaults to 0, and no code reads a `"boss"` object
at all. Unknown JSON keys are ignored, not rejected — content can therefore look correct, load clean
and do something else. Re-check both before trusting a drop placement or a boss's numbers.
