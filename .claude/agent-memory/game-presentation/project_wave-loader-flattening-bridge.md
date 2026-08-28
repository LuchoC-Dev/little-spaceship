---
name: wave-loader-flattening-bridge
description: How JsonContentSource loads waves.json and a level's wave references (#113) while SpawnSystem still only consumes the legacy flat WaveTimeline
metadata:
  type: project
---

**The core contract for waves (`WaveDefinition`, `WaveEndCondition`, `WavePlacement`,
`SimpleWaveDefinition`, `ContentSource.wave(String)`) landed before `SpawnSystem` was migrated to
consume it.** #111/#112/#113 were three separate issues and only #111 and #113 were done when I
worked #113 — #112 (`SpawnSystem` advancing on `WaveDefinition`/`WavePlacement` directly, cleared
conditions resolved at runtime) had not merged. `ContentSource.timeline(String)` — the legacy,
flat, absolute-time `SpawnEvent` list — is still the only thing `SpawnSystem` reads.

**Consequence for the loader:** a level's new `"waves"` block (a list of `{"wave", "offset"}`
placements) has nowhere to go except flattened, at load time, into that same legacy `WaveTimeline`.
Each placement starts `offsetSeconds` after the previous one *ends*; a wave's own `SpawnEvent.at`
values (relative to the wave's own start) get shifted by that start to become level-relative; the
merged result across all placements must be **sorted by absolute `at`** before
`SimpleWaveTimeline` will accept it, because a negative offset (an intentional overlap) can
interleave one placement's spawns with the next's — simple per-placement concatenation is not
sorted in that case even though each placement's own list is.

**Only `FixedDuration` waves can be flattened this way.** A `Cleared` wave's end is a fact about
what the simulation does at runtime (every spawned entity destroyed or off-screen), which a
load-time loader has no access to — there is no "duration" to hand to the next placement's start
computation. A placement naming a `Cleared` wave has to be rejected loudly (naming the level and
wave id) rather than guessed at; this is exactly the risk `docs/plan/11b-wave-system/plan.md`
names: "Do not write a cleared-based wave into any level before both are merged." **Whoever does
#112 will presumably delete this flattening bridge** (or narrow it) once `SpawnSystem` reads
`WaveDefinition`/`WavePlacement` directly and can resolve `Cleared` at runtime — check `git grep
flattenPlacements` before assuming this method still exists.

**`waves.json` had to be made optional**, not required, because `level-01.json` (the only shipped
level when I worked this) still uses the legacy `"events"` block and `assets/data/waves.json` did
not exist in the repo yet — an unconditional `loadWaves` would have broken the real game at
startup. Guarded with `FileHandle.exists()` before parsing, same pattern already used by
`AudioSystem` and `PackedSpriteAtlas` for optional assets, so it is a known-safe check under TeaVM
too.

See `[[libgdx-jsonvalue-key-iteration]]` for the scratch-classpath technique reused to verify all
of #113's error paths (missing wave id, malformed offset, unknown end-condition type, both/neither
of `"events"`/`"waves"` present) against a real `core.jar` without a `game` test suite.
