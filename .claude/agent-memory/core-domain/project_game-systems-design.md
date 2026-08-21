---
name: game-systems-design
description: How phase 05's weapon, bomb, pickup and score systems were shaped, and the gaps left deliberately open by the missing Health component
metadata:
  type: project
---

Built in phase 05 (`docs/plan/05-game-systems/`). The shape decided here is load-bearing for
whoever eventually adds a `Health` component (phase 07's boss, most likely) or a second attachment
type.

**No `Health` component exists, and every "how much damage" question in this phase resolved to "one
hit destroys it" as a result.** `core-deferred-surface.md` already flagged that no enemy hit-point
value exists in `docs/planning/`; phase 05 is the first phase where that gap actually bites, because
it is the first phase with something that deals damage (`WeaponSystem`, `BombSystem`). Both
`DamageSystem.resolveEnemyHit` (a player projectile reaching an enemy) and `BombSystem.detonate`
(the bomb) reuse `Collider.fragile` — the same boolean that already decided "does this enemy survive
ramming the player" — as their only notion of enemy toughness. A tank or heavy carrier survives a
bomb completely untouched, not damaged-but-alive, because there is nothing to store partial damage
in. This is a known, recorded gap (`05-game-systems/status.md`), not a bug to go hunting for. **The
day a `Health` component is added, both of those call sites need revisiting together** — they are
the only two places that currently treat "not fragile" as "the bomb/weapon cannot touch it at all."

**`ScoreValue` removal on award is what makes double-marking-for-destruction safe.**
`World.markForDestruction` has no dedupe — nothing stops two systems from marking the same entity
in the same tick (a fragile enemy both rammed and shot, say). `ScoreSystem` calls
`world.scoreValues().remove(entity)` the instant it awards the points, so a second appearance of the
same entity in `pendingDestruction` finds nothing to award. Whoever adds a third destruction source
should rely on this rather than trying to dedupe `pendingDestruction` itself — that list is shared
and cleared by `CleanupSystem`, not owned by whoever marks it.

**`CleanupSystem` is the one place `Drop` becomes an actual `Pickup` entity**, deliberately not
`DamageSystem` or `BombSystem` even though both are sources of enemy death. `Drop`'s own javadoc
(written in phase 04) already said the resolution happens "when the holder is destroyed" — `Cleanup`
is the literal single place every destruction path converges, so putting it there instead of
duplicating "check for a Drop" in every system that can kill something was the only design that
does not grow linearly with the number of death sources. Read `Transform` and `Drop` *before*
calling `World.destroyEntity`, in that order — the entity's components are still valid until that
call.

**A `Weapon` component (cooldown timer) is separate from `Player.shotLevel`** on purpose: shot level
survives a death like every other persistent power-up (it lives on `Player`, per phase 02's
precedent), while the cooldown is per-tick machinery nothing needs to persist. Putting both on
`Player` would have made a field on the "persistent stats" component that is not actually
persistent, which is exactly the kind of drift `core-boundary-decisions.md` warns about elsewhere.

**`LifetimeSystem` expires by position, not by a timer**, and no `Lifetime` component was built even
though `core-deferred-surface.md` named one as a future possibility. Every MVP projectile is a
straight line, so "has this left the playfield" is a cheap position check against bounds
`SpawnSystem`/`MotionSystem` already define — a timer would have been an invented number
(`docs/planning/` has none) solving a problem position-checking already solves for free. Same
"build what the existing need asks for" precedent `content-pipeline-design.md` already recorded for
trajectories vs. patterns.

**Six pickup kinds are fixed string constants on `PickupSystem`, and `"attachment"` is not one of
the fixed five** — it is resolved through `ContentSource.attachment(String)` using the pickup's own
kind string as the content id. This is what makes attachment durability genuinely data-driven
(`AttachmentDefinition.durability()`) without a seventh special case: the MVP's one attachment type
happens to reuse `"attachment"` as both its `Pickup.kind` and its content id, and a second attachment
type would only need a different content id, never a branch in `PickupSystem`.

See [[core-deferred-surface]] for what is still unbuilt (`Health` chief among them after this
phase), [[defensive-chain-and-collision-design]] for the `SystemOrder`-ordinal discipline `BOMB`'s
insertion had to respect, and `docs/plan/05-game-systems/status.md` for the acceptance-criteria
table and the exact `game`-module compile breakage this phase leaves behind for
`game-presentation`.
