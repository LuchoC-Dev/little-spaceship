# Phase 10b — Agents and the way sessions are run · status

**State:** in progress — tasks 7 and 8 landed on the phase branch; tasks 1–6 open
**Updated:** 26/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the `plan.md` next to it says what to do and does not change to reflect progress.

This is the first phase run under the branch regime it introduced: everything below lands on
`phase/10b-agents-and-sessions`, one sub-branch per issue, and the phase reaches `dev` as a single
pull request rather than nine merges into `main`.

## Done

**Task 8, the pre-PR check** (issue [#66](https://github.com/LuchoC-Dev/little-spaceship/issues/66),
PR [#67](https://github.com/LuchoC-Dev/little-spaceship/pull/67)).

- `tools/pre-pr-check` is one POSIX shell script, run as
  `tools/pre-pr-check --base <the phase branch>`. Nine checks: the branch is not `main`/`dev` and is
  named `type/description`; there are commits to open a pull request for; commit subjects are
  Conventional Commits under 72 characters with no `Co-Authored-By`; the tree is clean;
  `.claude/agent-memory/` was not written from a linked worktree; no `build/`, `.gradle/`, `.class`
  or `.log` in the diff; markdown links in changed documents resolve; scripts under `tools/` and
  `gradlew` carry the executable bit in the index; and `./gradlew build`, but only when the diff
  touches code.
- **It caught a real defect on its first run, on itself**: the script had been committed as mode
  `100644`. That is the same defect that made phase 09's first two CI runs die in fifteen seconds
  with `./gradlew: Permission denied`, and it is the acceptance criterion the issue asked for.
- Code references in documents are deliberately *not* checked here — that is `docs-refs`, issue
  [#56](https://github.com/LuchoC-Dev/little-spaceship/issues/56), handed to the 11 group.

**Task 7, the branch regime** (issue [#65](https://github.com/LuchoC-Dev/little-spaceship/issues/65)).

- `main` ← `dev` ← `phase/<phase>-<description>` ← `type/description`. Nothing is committed on `main`
  or `dev`; `main` receives only a pull request from `dev` and only the project owner merges it; a
  phase opens a pull request against `dev` instead of merging; every agent branches from the phase
  branch and **merges nothing**, the coordinator merges the sub-branches.
- Written into `CLAUDE.md`, `docs/plan/how-to-run-a-phase.md` (the cycle, the branch table, a "branch
  regime" section and the worktree command) and `docs/planning/13-working-with-agents.md`.
- All six agent definitions in `.claude/agents/` gained the same "Branches and the pull request"
  section: branch from the phase branch, run the check, open the pull request, merge nothing.
- `level-designer`'s stale "Work on a branch, never on `main`" line was removed rather than left to
  contradict the new table.

**`CLAUDE.md` edits so far**, each with its justification in
[`claude-md-changes.md`](claude-md-changes.md), which is the phase plan's condition for touching that
file: the branch regime, the pre-PR check, phase 10a's "name the file, or say Not built" convention,
and F32 — the claim that all six agents have a memory directory, when `test-engineer` has never
written one.

## In progress

Tasks 1–6, issues [#59](https://github.com/LuchoC-Dev/little-spaceship/issues/59)–[#64](https://github.com/LuchoC-Dev/little-spaceship/issues/64).
Task 1's measurement of phase 09 is done as analysis and not yet written down.

## Blocked

Nothing.

## Decisions taken while implementing

- **The git workflow came back into scope**, by the project owner on 26/08/2026, after the phase had
  started. `plan.md` listed it as out of scope; that bullet is now struck through and tasks 7 and 8
  were added. What remains out of scope is worktree *ergonomics* — creating and cleaning them up is
  still done by hand.
- **The check is a script, not a checklist.** Phase 09's evidence is that instructions produce false
  claims when an agent is tired and a command does not, which is also why task 4's countermeasure
  points at the same instrument.
- **The pre-PR check does not verify code references in documents.** That would duplicate `docs-refs`
  (#56), which the 11 group owns, and two checkers with one job is how one of them rots.

## Notes for whoever comes next

—
