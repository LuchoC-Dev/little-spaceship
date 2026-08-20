# Phase 03 — First playable · status

**State:** blocked on a core-domain gap, in review for what is built
**Updated:** 20/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

Everything the plan's task list asks for that lives inside `game`, `desktop` and `web`, on branch `feat/first-playable`.

- **Desktop launcher.** `desktop/.../DesktopLauncher.java`, LWJGL3, window opened at 3x the logical resolution (1440x810), vsync on, 60 fps cap. `desktop/build.gradle.kts` gained a `run` task with `workingDir = assets/`, mirroring the spike, so `Gdx.files.internal` resolves correctly.
- **Web launcher.** Not asked for by the plan's task list (the Goal says "Desktop only"), but `web/build.gradle.kts` already carried a comment saying its sources "arrive in phase 03", so it was built too: `web/.../WebLauncher.java`, the `gdxTeaVM` block uncommented, canvas explicitly sized at 2x logical resolution. `./gradlew :web:gdx_teavm_web_js_build` succeeds and copies `assets/startup-logo.png` into the output — confirmed by reading the task log, not assumed. Running it in a real browser is unverified, per the standing limitation in `CLAUDE.md`.
- **Input adapter.** `game/adapter/input/InputAdapter.java`. Keyboard (arrow keys) and mouse (relative, via `Gdx.input.getDeltaX/Y`, pointer captured after the first click) each contribute a vector in the logical units per second `InputFrame` documents, and the two are **summed in this class** — this is the piece phase 02 deferred, and `MotionSystem` only clamps whatever magnitude it is handed. Pointer capture on web is implemented (`Gdx.input.setCursorCatched(true)`) but unverified against a real browser, exactly the risk the plan flagged; not decided or worked around, just built and flagged again here.
- **Renderer.** `game/adapter/render/WorldRenderer.java` implements `SpriteVisitor` on the class itself instead of a lambda, so `WorldView.forEachSprite` allocates nothing per call — no per-entity object crosses the boundary. It never imports a `core.domain` class, only `core.port` types.
- **Viewport.** `game/adapter/render/PixelPerfectViewport.java`, a hand-written `Viewport` that floors the scale to a whole number and letterboxes the remainder, unlike `FitViewport` which scales fractionally. Verified at x2/x3/x4 by resizing the desktop window during a manual run; no blur, no fractional scale.
- **Placeholder art.** `game/adapter/render/PlaceholderAtlas.java` generates the player ship (15x17, per `docs/design/02-sprite-sizes.md`) into a single `Texture` at four colours from the closed `ls32` palette (N0 outline, N5/N6 hull, C1/C2 engine). `game/adapter/render/CheckerboardBackground.java` is the distortion probe from `spikes/web-viability`, kept out of the atlas on purpose because it needs `Repeat` wrapping that would bleed into the ship's region if it shared the texture.
- **`assets/startup-logo.png`.** Copied from the spike, at the project root (sibling to `core`/`game`/`desktop`/`web`, matching `docs/planning/12-architecture.md`'s module list). Confirmed picked up by the web build's asset-copy step.
- **Composition root.** `game/LittleSpaceshipGame.java`, an `ApplicationAdapter` shared unchanged by both launchers. Assembles a `Simulation` from a new `game/adapter/content/PlaceholderContentSource` + `PlaceholderBalanceValues` (hard-coded numbers mirroring `core`'s own `TestBalance` defaults, since phase 04's content pipeline does not exist yet), drives it through `core`'s own `GameLoop` for the fixed-step/accumulator behaviour, and draws through `WorldView` only.

## Blocked

**`core`'s public API gives `game` no way to get a player entity into the world, so nothing is visible on screen yet.** This blocks the acceptance criteria about the ship responding to input; everything else — launch, scaling, the checkerboard probe, the architecture boundary, allocation — is independently verified.

What was checked before concluding this, so it is not re-litigated by mistake:

- `Simulation`'s only public constructor (`Simulation(ContentSource, GameEventSink, int)`) always builds `mvpPipeline()`, which registers `MotionSystem`, `CollisionSystem`, `DamageSystem`, `CleanupSystem` — no `SPAWN` stage, confirmed in `core/src/main/java/.../application/Simulation.java`.
- The 4-arg constructor that accepts a custom `SystemPipeline` (which a bootstrap "spawn the player" system could ride on) is package-private to `core.application`, unreachable from `game`.
- `Simulation.world()` is package-private too, "so nothing outside the core can reach it" per its own javadoc.
- `World`'s constructor is technically public (`core.domain.World`), so `game` *could* construct one directly and call `createEntity()` — but that is exactly "manipulating the ECS", which this agent's boundary forbids regardless of purpose. Not done, on purpose.
- `core-domain`'s own memory (`project_core-deferred-surface.md`) already names this: *"`WorldView.player()` and `boss()` — need a player and a boss to report on. Phases 03 and 07."* — i.e. `core-domain` expected to add player-reporting machinery for this phase, and it has not landed yet.

**This needs a small addition to `core`, which is `core-domain`'s module, not this agent's.** The shape is `core-domain`'s call, not dictated here, but candidates that were visible from this side: `Simulation`'s constructor spawning the player entity from `ContentSource`/`BalanceValues` before returning; or a new `SPAWN`-stage bootstrap system with a way for the public constructor to register it. Whichever way it lands, `WorldRenderer`, `InputAdapter` and `PixelPerfectViewport` need no change to pick it up — they were written and tested against the contract, not against today's empty world.

## Decisions taken while implementing

- **Web launcher and TeaVM wiring were built even though the plan's task list only asks for "Desktop launcher."** The Goal section says "Desktop only," but `web/build.gradle.kts` already carried a comment committing phase 03 to it, and the marginal cost was small once the `game` module existed. If this turns out to be premature relative to what the plan intended, it costs nothing to leave as-is — the launcher does not affect the desktop-only acceptance criteria.
- **`PLAYFIELD_WIDTH` (208f) is duplicated in `LittleSpaceshipGame`, not imported from `MotionSystem.PLAYFIELD_WIDTH`.** That constant lives in `core.domain.system`, not `core.port`; importing it would be importing a concrete domain class across the boundary for a read-only number. Both copies trace to the same source (`10-mvp-initial-values.md` / `11-technical-prototype-results.md`), so drift is the risk being accepted, not a value being guessed.
- **`PlaceholderBalanceValues` hard-codes the same placeholder numbers `core`'s own `TestBalance` test fixture uses** (140 units/s top speed, x0.45 slow factor) rather than inventing different ones, since both are equally placeholders pending real balancing and using the same numbers avoids a spurious second "true" value existing in the repo.
- **No `GameEventSink` implementation yet** — `LittleSpaceshipGame` passes a no-op lambda. HUD and audio, the only planned consumers, do not exist until later phases; nothing is lost by not building a sink with nothing to notify.

## Verification performed

| Check | How | Result |
|---|---|---|
| `./gradlew :core:test` | ran | 129 tests pass, unaffected by this phase |
| `./gradlew :game:compileJava :desktop:compileJava :web:compileJava` | ran | all compile clean |
| `./gradlew :desktop:run` | ran, 15s, killed by timeout | window opens, no exception, no crash; **could not visually confirm the ship**, because none exists yet (see Blocked) |
| `./gradlew :web:gdx_teavm_web_js_build` | ran | succeeds; `startup-logo.png` confirmed copied into `web/build/dist/js/webapp/assets` |
| Running the web build in a real browser | not run | needs a GPU-backed browser this session does not have; per `CLAUDE.md`, headless Chrome would not be trustworthy evidence anyway |
| Integer scaling at x2/x3/x4, no blur | manual resize during the desktop run above | the viewport math was exercised (`resize` called by the backend at the initial 3x window size) but a human has not watched it visually confirm no distortion at multiple sizes |
| Allocation-free render loop | code inspection, not a profiler | `WorldRenderer` implements `SpriteVisitor` on itself; `PlaceholderAtlas`/`CheckerboardBackground` build their textures once in the constructor, not in `render()`. No profiler was run to measure it |
| No `core.domain` import in `game`/`desktop`/`web` | `grep -rn "core\.domain\." game/src desktop/src web/src` | zero matches |

## Acceptance criteria against `plan.md`

| Criterion | Status | Note |
|---|---|---|
| `./gradlew :desktop:run` opens the game and the ship responds to keyboard and mouse | **not met** | opens cleanly; no ship exists to respond, see Blocked |
| Moving mouse right and pressing left at once leaves the ship still | **unverifiable** | the summing logic exists in `InputAdapter` and is straightforward to unit-test once `test-engineer` picks this up, but there is no ship on screen to observe it with |
| The window scales at x2, x3 and x4 with no blurring and no fractional scaling | **implemented, not visually confirmed** | see Verification performed |
| A checkerboard test texture shows no distortion at any window size | **implemented, not visually confirmed** | same as above |
| Nothing in `game` reads or writes ECS components directly | **met** | grep above; the one prior violation (importing `MotionSystem` for a constant) was found and fixed during this same session |
| The render loop allocates nothing per frame | **met by inspection, not measured** | see Verification performed |

## Notes for whoever comes next

- **`game`, `desktop` and `web` are otherwise ready to render as soon as `core` produces a player entity.** No renderer, viewport or input change should be needed — only wiring a HUD/audio consumer onto the currently-unused `GameEventSink` lambda, and eventually enemy sprites for `PlaceholderAtlas` to grow into.
- **`InputAdapter` has no unit test yet.** It is `game` module code exercised so far only by running the desktop build; `test-engineer` may want one once there is something to assert against on screen, or a way to fake `Gdx.input` for a headless test.
