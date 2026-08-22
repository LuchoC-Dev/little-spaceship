---
name: boss-fight-design
description: How phase 07's five-part boss avoided a new component/store, and the record-constructor trick used to fix issue #23 without breaking existing call sites
metadata:
  type: project
---

Two reusable techniques from building `BossSystem`, plus one verification habit worth repeating.

**A single, unique multi-entity thing does not need a component or a `ComponentStore`.** The boss is
five entities (core, two pods, two arms) that must move and be tracked together, but nothing else in
the game needs to query "which entities belong to a boss" generically — there is exactly one boss,
owned by exactly one `BossSystem`. So the five entity ids are plain instance fields on `BossSystem`
itself (the same pattern `SpawnSystem` already uses for its cursor and elapsed time), not a new
`BossPart` component with a fifth `ComponentStore` in `World`. This avoided touching `World`'s store
list, `destroyEntity`'s removal list, and any new iteration cost, for a case with exactly one
consumer. Reach for a component/store only when something *else* — a different system, or
presentation through `WorldView` — needs to look the relationship up independently.

**A record can grow a field without breaking existing callers, by keeping the old constructor as a
delegating overload.** `SpawnEvent` needed a `dropSlot` field for issue #23 (a designed drop must tie
to one formation slot, not apply to every slot), but dozens of call sites across `core` tests and, more
importantly, `game`'s content loader (outside this agent's boundary to fix) already used the original
five-argument constructor. Adding `dropSlot` as a sixth canonical component and then adding back a
five-argument constructor that delegates to the six-argument one (defaulting `dropSlot` to `0`) kept
every old call site compiling, with old behaviour reinterpreted as "the drop goes to slot 0" — a
behaviour change, but not a compile break, and the correct default besides. Worth remembering whenever
a decided fix needs a new field on a widely-constructed record: check whether the field can default
sensibly before assuming every call site (including ones outside this agent's own module) must change.

**Adding a method to a `core.port` interface that `game` already implements is expected to break
`game`'s compile, and that is fine to leave for the other agent — but verify it, don't guess.**
`ContentSource` gained `hasBoss`/`boss` this phase; running `./gradlew :game:compileJava` from the
worktree (read-only — this agent never edits `game/`) confirmed the exact, single compile error named
in the status file, rather than asserting "this will need updating" without checking what the actual
error says. The same check is worth repeating any time a `core.port` interface implemented outside
`core` grows a method — `BalanceValues`, by contrast, was deliberately *not* touched this phase
precisely because it has more implementers across the boundary and none of the new boss numbers were
generic enough to belong there; they went on the new `BossDefinition` instead.

Related: [[core-deferred-surface]], [[content-pipeline-design]], [[rng-teavm-constraints]].
