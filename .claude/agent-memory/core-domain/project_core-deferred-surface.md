---
name: core-deferred-surface
description: What the core deliberately leaves unimplemented, and which phase owns each gap
metadata:
  type: project
---

The core grew deliberately incomplete: each phase adds only what its own systems need. Knowing what was left out **on purpose** stops it being re-decided as if it were an oversight.

**How to apply:** before adding a type to `core.port` or a component to the domain, check whether it was deferred here with a reason.

- `WorldView.player()` and `boss()` — need a player and a boss to report on. Phases 03 and 07.
- `ContentSource.enemy()` and `timeline()` — content pipeline, phase 04.
- Concrete `GameEvent` implementations — the interface exists, no event does. Each arrives with the system that emits it.
- Components beyond `Transform`, `Motion`, `Collider` and `Sprite`.
- Every system: `SystemOrder` declares the ten stages, `Simulation.mvpPipeline()` is empty.

See [[core-boundary-decisions]] for the boundary shape those additions have to respect, and [[rng-teavm-constraints]] for what stays reproducible across runtimes.
