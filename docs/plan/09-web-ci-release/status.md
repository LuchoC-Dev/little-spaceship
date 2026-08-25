# Phase 09 — Web, CI and release · status

**State:** in progress
**Updated:** 25/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

## Done

Tasks 1-3 of the plan (issue #32):

- **Task 1, web launcher.** `web/build.gradle.kts`'s `gdxTeaVM {}` block is uncommented and finished:
  `js {}` (ships) and `wasm {}` (kept available, per the decision already made — not re-decided).
  `dev.luchoc.littlespaceship.web.WebLauncher` mirrors `DesktopLauncher`: same
  `LittleSpaceshipGame`, no platform branch.
- **Task 2, canvas size and resize policy.** `WebApplicationConfiguration.width/height` are set to
  `LOGICAL_WIDTH * 2` / `LOGICAL_HEIGHT * 2` (960x540), never 0. The resize policy is: the canvas
  itself stays fixed-size, and window/browser resizing is absorbed by `PixelPerfectViewport`
  (already implemented for desktop, `BaseUiScreen`/`PlayScreen#resize`), which does the
  integer-scale letterbox math the same way on both targets. No web-specific resize code was
  needed.
- **Task 3, `startup-logo.png`.** Confirmed present in the built webapp by reading the
  `generateJavaScript` task's own log, not by assumption:
  `Copied [Classpath resource] startup-logo.png (12214 bytes)` and
  `Copied [Internal] .../assets/startup-logo.png (12214 bytes)` — it lands twice, once as the
  backend's own classpath default and once as the project's real file from `assets/`, both present
  in `web/build/dist/js/webapp/assets/`.
- Two blockers `docs/STATUS.md` handed to this phase, both resolved:
  - `tools.audio` (`GenerateAudio`, `Synth`, `Wav`) moved out of `game`'s `main` source set into a
    new `tools` source set (`game/src/tools/java`). `web` depends on `:game`, which only exposes
    `main`, so TeaVM never compiles `java.nio.file` again. `generateAudio`'s classpath now points at
    `sourceSets["tools"].runtimeClasspath`. Verified: `game.jar` (the `main` jar `web` consumes)
    contains no `tools` classes, and `:game:compileToolsJava` / `:game:generateAudio` still work.
  - `GameSkin` was checked and does **not** use `Skin(FileHandle, TextureAtlas)` or `skin.load(...)`
    — the whole skin is built in code from `Pixmap`/`NinePatch`/style objects, with only
    `new BitmapFont(FileHandle)` reading a file, which is not reflective. So the reflective `Skin`
    concern the issue flagged does not apply to this codebase; nothing needed changing here.

Verified, measured:

- `./gradlew gdx_teavm_web_js_build` — **succeeds**. Dist at `web/build/dist/js/webapp`.
- `./gradlew gdx_teavm_web_js_build -Prelease` — **succeeds**. After a clean build:
  - `app.js`: 1,026,955 bytes uncompressed, ~298 KB gzip-compressed (measured with `gzip -c | wc -c`,
    not a browser network trace, so treat as an upper bound).
  - Full `webapp/` directory (JS + assets + `index.html`): 2.5 MB, of which `assets/` alone is
    1.3 MB — mostly the two music WAVs (`level.wav` 592 KB, `boss.wav` 395 KB) and the SFX. This is
    the number `11-technical-prototype-results.md` said would change once real assets replaced the
    spike's code-generated textures: 2.5 MB vs. the spike's 192 KB compressed.
  - No load-time or in-browser framerate measurement was taken — that needs a real GPU and is task 6
    of this phase, explicitly out of scope for issue #32 (tasks 1-3 only).
- `./gradlew gdx_teavm_web_wasm_build` — **succeeds** too (kept available at no cost, per the plan).
- `./gradlew build` — **green**, including `core:test` (re-run with `--rerun` to rule out a cached
  pass).
- Did **not** attempt headless-Chrome verification — the plan and `CLAUDE.md` both say it fails
  under SwiftShader even when the real thing works, so it would have produced nothing useful.

## In progress

Nothing — tasks 1-3 are the whole scope of issue #32.

## Blocked

Nothing.

## Decisions taken while implementing

- **Canvas stays fixed-size on web** (960x540, 2x logical resolution) instead of an auto-sizing
  canvas that tracks the browser window. `WebApplicationConfiguration.isAutoSizeApplication()`
  (`width == 0 && height == 0`) is exactly the documented pitfall's trigger, and the container it
  would auto-size to is not something this project controls without also writing responsive CSS for
  the generated `index.html` — not worth doing since `PixelPerfectViewport` already gives correct
  integer-scale letterboxing for whatever size the canvas actually is. This is a presentation
  choice, not a game rule, so it does not touch `docs/planning/08-decisions-and-open-items.md`.
- **`tools` source set**, not a separate Gradle module, for `tools.audio`. It has zero dependency on
  the rest of `game` (confirmed by reading its imports), so a source set inside the same module was
  enough to keep it out of `web`'s classpath without the overhead of a new subproject.

## Notes for whoever comes next

- Tasks 4-9 remain: pointer capture verification, the browser matrix (Chrome/Firefox/Edge/Safari),
  real-asset load-time and framerate measurement, CI on GitHub Actions, static-site deploy, and the
  repository README.
- A human needs to open `web/build/dist/js/webapp/index.html` through a real local server (not
  `file://`, module loading needs HTTP) in a real browser to confirm the game actually runs, the
  pointer-lock relative mouse works, and audio unlocks on the first click. `./gradlew
  gdx_teavm_web_js_run` serves it on `http://localhost:8181`.
- The measured 2.5 MB / ~298 KB gzip `app.js` numbers are from this machine's build only; re-measure
  once task 6 is actually picked up, ideally with a real browser's network panel rather than
  `gzip -c | wc -c`, since HTTP compression and caching headers change what a visitor actually
  downloads.
