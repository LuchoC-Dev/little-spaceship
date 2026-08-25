---
name: libgdx-jsonvalue-key-iteration
description: How to walk a com.badlogic.gdx.utils.JsonValue object's own keys to reject unrecognised ones, and how to get a runnable classpath for a throwaway verification program without a test source set
metadata:
  type: project
---

**`JsonValue` exposes its children as public fields, not just through the `getX(name)` accessors.**
`child` (the first child, or null) and each child's own `next`/`name` let you walk an object's actual
keys without knowing them in advance — `for (JsonValue c = value.child; c != null; c = c.next)`,
checking `c.name` against an allow-list. This is what closed phase 07's "unknown key loads clean and
silently does nothing" gap in `JsonContentSource`: `getString`/`getInt`/etc. only ever check for a
key's *presence*, never flag one that exists but isn't expected.

**Getting a real classpath to run a throwaway verification program against `game`'s compiled
classes, when `game` has no test source set configured (`build.gradle.kts` only declares
`implementation`, no `testImplementation`/JUnit):** append a temporary Gradle task to `game/build.gradle.kts`
that prints `sourceSets["main"].runtimeClasspath.files.joinToString(";") { it.absolutePath }`, run it
once, copy the printed paths, then `git checkout -- game/build.gradle.kts` to discard the temp task.
`javac -cp "<paths>" -d . Scratch.java && java -cp ".;<paths>" Scratch` from a scratch directory runs
against the real `core.jar`/`game` classes plus the real `gdx-1.14.2.jar`, no display needed — the
same "content loading needs no `Gdx.app`" fact `[[headless-libgdx-verification]]` already recorded,
just with a working classpath this time instead of guessing jar locations under `~/.gradle`.

See `[[headless-libgdx-verification]]` for the underlying fact this technique exercises, and
`[[windows-desktop-screenshot-verification]]` for the complementary real-window technique used to
confirm the tell/health bar actually render, not just that the loader parses.
