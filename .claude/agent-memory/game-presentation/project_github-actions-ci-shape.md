---
name: github-actions-ci-shape
description: What could and could not be verified before opening the CI PR, and why the workflow needs two separate run steps
metadata:
  type: project
---

`gdx_teavm_web_js_build` (registered by the gdx-teavm plugin) is **not** wired into the standard
`build` lifecycle task. `./gradlew build` green does not build the web target — it only compiles
and tests the JVM sources of every module, including `web`'s own `src/main` if it had one. A CI
workflow that wants to prove the web target builds needs an explicit second step
(`./gradlew gdx_teavm_web_js_build`), not just `build`. Confirmed by reading `web/build.gradle.kts`
and `./gradlew tasks --all | grep teavm`: the teavm tasks live under their own task group, not
under `assemble`/`check`.

`gradle/actions/setup-gradle@v4` is worth using over hand-rolled `actions/cache`: it caches both
the wrapper distribution and the dependency/build cache with zero extra config, and it is the
action Gradle itself documents as current (superseding the older `gradle-build-action`).

**What was and wasn't verified before the PR existed:** the exact command sequence
(`./gradlew build` then `./gradlew gdx_teavm_web_js_build`) was run locally and passed, but under
JDK 21, not the JDK 17 the workflow pins — this machine has no JDK 17 installed, only 21 and 25.
The reasoning for 17 (matches `sourceCompatibility`, avoids relying on javac's downcompile path)
is sound but untested locally; the workflow itself is untested until GitHub Actions actually runs
it on the opened PR. Say this plainly in a report rather than calling the workflow "verified" —
"the commands it runs pass on this machine under a different JDK, and the YAML parses" is the
honest version of that claim.
