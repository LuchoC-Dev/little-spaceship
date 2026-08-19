---
name: phase-01-foundations
description: What phase 01 left in place for the core, and what phases 02-09 are expected to add on top of it
metadata:
  type: project
---

Phase 01 landed the core skeleton on branch `feat/core-foundations`, draft PR #2, closing issue #1. Later phases extend it rather than reshape it.

**Why:** the plan asks each phase to add only what its systems need, so the core grew deliberately incomplete. Knowing what was left out on purpose avoids re-deciding it as if it were an oversight.

**How to apply:** before adding a type to `core.port`, check whether it was deferred here with a reason.

Deferred on purpose, with the phase that owns it:

- `WorldView.player()` and `boss()` — need a player and a boss to report on. Phases 03 and 07.
- `ContentSource.enemy()` and `timeline()` — content pipeline, phase 04.
- Concrete `GameEvent` implementations — the interface exists, no event does. Each arrives with the system that emits it.
- Components beyond `Transform`, `Motion`, `Collider`, `Sprite`.
- Every system: `SystemOrder` declares the ten stages, `Simulation.mvpPipeline()` is empty.

Still unverified: that the `Rng` stream is identical under TeaVM. The algorithm avoids everything known to diverge, but nobody has run it in a browser. Phase 09 confirms it; if a replay ever diverges between desktop and web, look there first.

See [[core-boundary-decisions]] for the boundary shape those additions have to respect.
