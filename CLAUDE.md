# little-spaceship

A vertical shoot 'em up built from scratch in Java: pixel-art, level-based, single-player and local. It is a portfolio piece, so architecture, tests, CI, performance, documentation, art and deployment all count as part of the deliverable — not just a game that runs.

Built on libGDX, shipped to the browser through TeaVM, with a desktop target sharing the same core. It is not a remake, and no code or assets from earlier projects are reused.

Planning lives in `docs/planning/`. Before inventing a game rule, look there: most of it is already decided, and `08-decisions-and-open-items.md` separates what is settled from what is still open.

## Invariants

Decided and measured. These are not preferences — breaking one invalidates earlier work.

1. **`core` does not depend on libGDX**, not even on its math utilities. Needing to import `com.badlogic.gdx` inside `core` means the design is wrong.
2. **Determinism.** The core never reads the clock, never reads input directly and never calls `Math.random()`. It receives a fixed step (1/60), an immutable `InputFrame` and a seeded `Rng`. Replays depend on this and break silently when it is violated.
3. **Single-threaded.** The web target offers no real parallelism, and `ExecutorService`, `CompletableFuture` and `ReentrantLock` do not exist in TeaVM — they break the build. It is also unnecessary: logic costs fractions of a millisecond against ~10 ms of drawing.
4. **Contracts at the boundaries.** No module exposes concrete classes to another. Whatever crosses is immutable or read-only. `game` never manipulates the ECS; it reads through `WorldView`.
5. **Fixed system order.** Execution order is part of the game rules, not an implementation detail.
6. **No abstraction without a real case in the MVP.**

## Web target pitfalls

Each of these cost hours during the spike.

- **`assets/startup-logo.png` is mandatory.** Without it the app crashes when preloading finishes, with an error that never mentions the logo.
- **The canvas needs an explicit size.** With `config.width = 0` it inherits a 0×0 container and the preloader ends up without a stage.
- **Headless Chrome cannot validate the web runtime.** It fails under SwiftShader even when a real browser works, so CI can only verify that the build compiles.
- **Read JSON with `JsonReader`/`JsonValue`**, never with the `Json` serialisation class: it relies on reflection, which TeaVM would require declaring class by class.
- Every new dependency must be checked for TeaVM compatibility before being added.

## Conventions

- Java 17, root package `dev.luchoc.littlespaceship`, Gradle wrapper, JUnit 5.
- **Everything written in the repository is in English**: code, comments, logs, JSON keys, content ids, agent definitions and documentation. The one exception is `docs/sources/`, a verbatim transcript kept in Spanish because translating evidence would falsify it.
- Conversation with the user is always in Spanish.
- Composition over inheritance. Components are plain data with no logic.
- Logical resolution 480×270, playfield 208 px wide, integer scaling, nearest-neighbour.
- Drawing is the cost, not simulation. Optimise batching and atlases first, spatial structures for collision only if it ever becomes necessary, concurrency never.

## Commits

Every commit goes through the `/git-commit` skill — never a bare `git commit`. This applies to one-file and docs-only commits, and it applies to agents as well as to the main session.

Conventional Commits: `type(scope): description`, present tense, imperative mood, under 72 characters. Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.

Branches follow `type/description`, lowercase, only `a-z 0-9 . _ -`.

Before committing: one logical change per commit, diff matches the stated scope, no secrets, no binaries or local artifacts, no `Co-Authored-By` trailers, relevant tests passing.

Never update git config, never force-push, never skip hooks with `--no-verify`, and never use destructive commands unless explicitly asked. If a hook rejects a commit, fix the problem and make a new commit rather than amending.

## Agents

Defined in `.claude/agents/`, each with persistent memory under `.claude/agent-memory/`. Boundaries come from the module architecture, so two agents cannot collide.

| Agent | Owns |
|---|---|
| `core-domain` | `core/` — ECS, systems, game rules |
| `game-presentation` | `game/`, `desktop/`, `web/` — rendering, HUD, audio, input |
| `visual-designer` | visual direction; produces documents, not code |
| `test-engineer` | unit tests and deterministic replays |
| `reviewer` | reads and reports only |

An agent never writes outside its module. Agent memory records only what that agent learned and is not already written in `docs/`.
