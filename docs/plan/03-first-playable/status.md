# Phase 03 — First playable · status

**State:** draft — waiting on phase 04 before it can be accepted
**Updated:** 20/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Acceptance criteria against `plan.md` — read this first

Three different reasons keep this phase from being a plain accept, and they are not the same kind of gap. Sorted so the blocking one is not missed:

| Criterion | Status | Why |
|---|---|---|
| `./gradlew :desktop:run` opens the game and the ship responds to keyboard and mouse | **not met — waiting on phase 04** | Opens cleanly, no exception. **No ship exists yet.** `core`'s `Simulation` never spawns a player entity and `game` has no way to create one without manipulating the ECS directly, which is out of bounds. Phase 04 (content pipeline) owns creating entities from definitions and will close this. See "Waiting on phase 04" below. |
| Moving mouse right and pressing left at once leaves the ship still | **not met — waiting on phase 04** | The summing logic is implemented in `InputAdapter` and is ready to be exercised, but there is no ship on screen to observe cancelling with. Cannot be verified until phase 04 lands. |
| The window scales at x2, x3 and x4 with no blurring and no fractional scaling | **implemented, unverifiable without a display** | `PixelPerfectViewport` computes an integer-only scale; `resize()` is wired and was exercised once at the launcher's default 3x window size. No human has watched a window being resized through x2/x3/x4 to confirm no blur — this session has no visible display. |
| A checkerboard test texture shows no distortion at any window size | **implemented, unverifiable without a display** | Same constraint as above: the probe exists and draws, nobody has looked at it. |
| Nothing in `game` reads or writes ECS components directly | **met** | `grep -rn "core\.domain\." game/src desktop/src web/src` returns zero matches. One violation (importing `MotionSystem` for a constant) was found and fixed during this phase; see "Decisions taken while implementing." |
| The render loop allocates nothing per frame | **met by code inspection, not measured** | `WorldRenderer` implements `SpriteVisitor` on itself instead of a lambda; textures are built once in constructors, never in `render()`. No profiler was run — this is a static read of the code, not a measurement. |

**Plain statement for whoever reviews this:** two criteria are blocked on `core-domain` work that does not exist yet, two are implemented but this session could not look at a screen to confirm them, and two are actually met. Pull request [#14](https://github.com/LuchoC-Dev/little-spaceship/pull/14) stays **draft** because of the first two, on explicit direction from the project owner — not a judgement call made here.

## Done

Everything the plan's task list asks for that lives inside `game` and `desktop`, on branch `feat/first-playable`.

- **Desktop launcher.** `desktop/.../DesktopLauncher.java`, LWJGL3, window opened at 3x the logical resolution (1440x810), vsync on, 60 fps cap. `desktop/build.gradle.kts` gained a `run` task with `workingDir = assets/`, mirroring the spike, so `Gdx.files.internal` resolves correctly.
- **Input adapter.** `game/adapter/input/InputAdapter.java`. Keyboard (arrow keys) and mouse (relative, via `Gdx.input.getDeltaX/Y`, pointer captured after the first click) each contribute a vector in the logical units per second `InputFrame` documents, and the two are **summed in this class** — this is the piece phase 02 deferred, and `MotionSystem` only clamps whatever magnitude it is handed.
- **Renderer.** `game/adapter/render/WorldRenderer.java` implements `SpriteVisitor` on the class itself instead of a lambda, so `WorldView.forEachSprite` allocates nothing per call — no per-entity object crosses the boundary. It never imports a `core.domain` class, only `core.port` types.
- **Viewport.** `game/adapter/render/PixelPerfectViewport.java`, a hand-written `Viewport` that floors the scale to a whole number and letterboxes the remainder, unlike `FitViewport` which scales fractionally.
- **Placeholder art.** `game/adapter/render/PlaceholderAtlas.java` generates the player ship (15x17, per `docs/design/02-sprite-sizes.md`) into a single `Texture` at four colours from the closed `ls32` palette (N0 outline, N5/N6 hull, C1/C2 engine). `game/adapter/render/CheckerboardBackground.java` is the distortion probe from `spikes/web-viability`, kept out of the atlas on purpose because it needs `Repeat` wrapping that would bleed into the ship's region if it shared the texture.
- **`assets/startup-logo.png`.** Copied from the spike, at the project root (sibling to `core`/`game`/`desktop`/`web`, matching `docs/planning/12-architecture.md`'s module list). Stays in place regardless of the web target's status, precisely so phase 09 does not rediscover the crash it prevents — that is what the plan's task 6 asks for.
- **Composition root.** `game/LittleSpaceshipGame.java`, an `ApplicationAdapter` ready to be shared by a future web launcher unchanged. Assembles a `Simulation` from a new `game/adapter/content/PlaceholderContentSource` + `PlaceholderBalanceValues` (hard-coded numbers mirroring `core`'s own `TestBalance` defaults, since phase 04's content pipeline does not exist yet), drives it through `core`'s own `GameLoop` for the fixed-step/accumulator behaviour, and draws through `WorldView` only.

## Not in this phase: the web target

A web launcher was built, verified (`./gradlew :web:gdx_teavm_web_js_build` succeeded, `assets/startup-logo.png` confirmed copied into the output) and then **removed** on explicit direction from the project owner. The plan's Goal section says "Desktop only" and that is what governs; a stale comment in `web/build.gradle.kts` claiming phase 03 owned the web target is what led to building it in the first place, and that comment has been corrected so it does not mislead again.

Phase 09 (`docs/plan/09-web-ci-release/plan.md`) owns the web target and brings it back deliberately. What survives from this detour:

- `assets/startup-logo.png` — required regardless of when the web target lands, task 6 exists precisely so it is not forgotten later.
- What was learned building it once, recorded in `.claude/agent-memory/game-presentation/` rather than left to be rediscovered: the `plugins {}` block's import-ordering requirement in the Kotlin script, and that the `gdxTeaVM` build and asset-copy step both worked cleanly against this project's actual module layout, not just the spike's.

## Waiting on phase 04

**`core`'s public API gives `game` no way to get a player entity into the world, so nothing is visible on screen yet.** Confirmed, not assumed, by checking:

- `Simulation`'s only public constructor (`Simulation(ContentSource, GameEventSink, int)`) always builds `mvpPipeline()`, which registers `MotionSystem`, `CollisionSystem`, `DamageSystem`, `CleanupSystem` — no `SPAWN` stage, confirmed in `core/src/main/java/.../application/Simulation.java`.
- The 4-arg constructor that accepts a custom `SystemPipeline` (which a bootstrap "spawn the player" system could ride on) is package-private to `core.application`, unreachable from `game`.
- `Simulation.world()` is package-private too, "so nothing outside the core can reach it" per its own javadoc.
- `World`'s constructor is technically public (`core.domain.World`), so `game` could construct one directly and call `createEntity()` — but that is exactly "manipulating the ECS," invariant 4 in `CLAUDE.md`. **Not done, on purpose**, confirmed correct by the project owner rather than a judgement call made here.
- `core-domain`'s own memory (`project_core-deferred-surface.md`) already names this: "`WorldView.player()` and `boss()` — need a player and a boss to report on. Phases 03 and 07."

**Resolution: phase 04, the content pipeline, owns creating entities from definitions and will close this gap there.** Nothing further for `game-presentation` to do about it until then. `WorldRenderer`, `InputAdapter` and `PixelPerfectViewport` need no change to pick it up once it lands — they were written and tested against the contract, not against today's empty world.

## Decisions taken while implementing

- **`PLAYFIELD_WIDTH` (208f) is duplicated in `LittleSpaceshipGame`, not imported from `MotionSystem.PLAYFIELD_WIDTH`.** That constant lives in `core.domain.system`, not `core.port`; importing it would be importing a concrete domain class across the boundary for a read-only number. Both copies trace to the same source (`10-mvp-initial-values.md` / `11-technical-prototype-results.md`), so drift is the risk being accepted, not a value being guessed.
- **`PlaceholderBalanceValues` hard-codes the same placeholder numbers `core`'s own `TestBalance` test fixture uses** (140 units/s top speed, x0.45 slow factor) rather than inventing different ones, since both are equally placeholders pending real balancing and using the same numbers avoids a spurious second "true" value existing in the repo.
- **No `GameEventSink` implementation yet** — `LittleSpaceshipGame` passes a no-op lambda. HUD and audio, the only planned consumers, do not exist until later phases; nothing is lost by not building a sink with nothing to notify.
- **Web launcher built, then removed** — see "Not in this phase: the web target" above.

## Verification performed

| Check | How | Result |
|---|---|---|
| `./gradlew :core:test` | ran | 129 tests pass, unaffected by this phase |
| `./gradlew :game:compileJava :desktop:compileJava :web:compileJava` | ran | all compile clean, `web` has no sources again |
| `./gradlew :desktop:run` | ran, 15s, killed by timeout | window opens, no exception, no crash; could not visually confirm the ship, because none exists yet |
| Integer scaling at x2/x3/x4, no blur | not observed | no display available in this session; see the acceptance table above |
| Allocation-free render loop | code inspection, not a profiler | see the acceptance table above |
| No `core.domain` import in `game`/`desktop`/`web` | `grep -rn "core\.domain\." game/src desktop/src web/src` | zero matches |

## Notes for whoever comes next

- **`game`, `desktop` are ready to render as soon as `core` produces a player entity in phase 04.** No renderer, viewport or input change should be needed — only wiring a HUD/audio consumer onto the currently-unused `GameEventSink` lambda, and eventually enemy sprites for `PlaceholderAtlas` to grow into.
- **`InputAdapter` has no unit test yet.** It is `game` module code exercised so far only by running the desktop build; `test-engineer` may want one once there is something to assert against on screen, or a way to fake `Gdx.input` for a headless test.
- **The web target is fully reverted**, not partially: no `web/src` sources, `gdxTeaVM` block commented out again in `web/build.gradle.kts`, with a corrected comment. `assets/startup-logo.png` is the only thing from that detour that stays.
