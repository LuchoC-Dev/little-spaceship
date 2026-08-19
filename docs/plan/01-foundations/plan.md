# Phase 01 — Foundations

**Lane:** code · **Owner:** `core-domain` · **Depends on:** nothing · **Target:** day 1

## Goal

The Gradle skeleton and the beating heart of the simulation: entities, systems, a fixed-step loop, seeded randomness and an immutable input frame — all testable without libGDX.

Nothing is visible on screen at the end of this phase. That is expected: what it buys is that everything after it can be tested in milliseconds.

## Preconditions

None. This is the first code in the repository.

## Tasks

1. **Gradle skeleton.** Wrapper 9.7.0, modules `core`, `game`, `desktop`, `web`, Java 17, root package `dev.luchoc.littlespaceship`. `core` declares **no** libGDX dependency — that is what makes the invariant mechanical rather than aspirational.
2. **Entity registry.** An entity is an `int` with a generation, so a reference to a destroyed entity can be detected instead of silently pointing at a recycled slot.
3. **Component storage.** Plain data, no logic. Start with `Transform`, `Motion` and `Collider`; the rest arrive with the phase that needs them.
4. **System pipeline.** Fixed execution order, declared in one place. Order is a game rule, not a detail.
5. **`GameLoop`.** Accumulator with a 1/60 step and a clamp on the real delta to avoid the spiral of death. The simulation never sees a variable delta.
6. **`Rng`.** Own implementation, explicit seed, reproducible. Never `Math.random()`.
7. **`InputFrame`.** Immutable record: movement vector, fire, slow, bomb. Built outside the core and handed in per tick.
8. **`WorldView` and the ports.** The read-only contracts `game` will consume, plus `ContentSource` and `GameEventSink` as the core declares them.

## Acceptance criteria

Verifiable, not opinions:

- `./gradlew :core:test` passes and `core` compiles **without libGDX on its classpath**.
- Grep finds zero occurrences of `com.badlogic.gdx`, `System.currentTimeMillis`, `System.nanoTime` and `Math.random` inside `core/src/main`.
- Feeding the same seed and the same `InputFrame` sequence twice produces an identical final state.
- The loop runs a fixed number of ticks for a given elapsed time, independent of frame rate.
- No public type in `core` exposes an implementation class.
- An architecture test asserts the dependency rule between layers.

## Risks

**The determinism invariant is easy to break and silent when broken.** A single `System.nanoTime()` slipped in for "just logging" invalidates every replay without any test failing. The greps above exist precisely for that.

**Over-engineering the ECS.** It is tempting to build archetypes, queries and bitsets now. The MVP has a few hundred entities: build the smallest thing the systems actually need.

## Notes for whoever implements it

The spike in `spikes/web-viability/` already proved plain Java behaves identically on the JVM and in the browser — `collisionbench` runs on both unchanged. Reuse the idea, not the code: the spike is throwaway and its shortcuts are not the architecture.
