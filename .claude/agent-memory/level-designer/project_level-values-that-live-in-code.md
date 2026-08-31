---
name: level-values-that-live-in-code
description: Values a level is designed against that are constants in core/ rather than fields in assets/data/ — where each one is, and why that matters when writing or generating anything that describes a level
metadata:
  type: project
---

Found writing the per-level document contract (phase 11d, #180), by asking of every section "which
JSON field is this derived from" and finding four that had no answer.

**Four things a level designer designs against are `public static final` constants in `core/`, not
content.** Grep before assuming a number is tunable from `assets/data/`:

- **the six recognised drop kinds** — `PickupSystem.java:39-71`. There is no `drops.json`. Only
  `"attachment"` is content-driven, through `attachments.json`'s `durability`;
- **the boss's geometry** — `BossSystem.java:140-150`: the spread/sweep velocity ratios and
  `CORE_SPAWN_Y`. The level's `boss` block gives `combatY` and speeds; the angles that decide whether
  the boss can reach the player at all are code;
- **the enemy projectile radius (`2.0f`) and the one real `pattern` string** —
  `EnemyWeaponSystem.java:35,37`. `"straight-single"` is the only shape that system builds, so a
  `pattern` value is content that names a code branch;
- **the playfield dimensions** — `MotionSystem.PLAYFIELD_WIDTH:57` (208) and
  `SpawnSystem.PLAYFIELD_HEIGHT:92` (270). Deliberately not balance values.

**Why this is worth remembering rather than re-deriving.** Any document *generated* from
`assets/data/` can only quote these, and a stale quote survives regeneration untouched — the
regenerate-and-diff check cannot see it. So the moment you write a passage about a level that
mentions one of them, name the file in backticks: it is the only thing that makes the staleness
findable later.

**The gap that has no workaround at all is design intent.** JSON admits no comments, and
`JsonContentSource.requireOnlyKeys` now rejects unrecognised keys, so there is nowhere in
`assets/data/` to say *why* a wave exists — not even a `"note"` string, which would need a parser
change and therefore `core-domain` and `game-presentation`. Anything asking "what beat is this wave
for" is answered by `docs/planning/04-campaign-and-levels.md` and by hand-written mapping tables like
the one in `docs/plan/11c-movement-shapes/shape-catalogue.md`, never by the content.

See [[wave-migration-mechanics]] and [[level-one-content-mechanics]] for the format's behaviour
itself.
