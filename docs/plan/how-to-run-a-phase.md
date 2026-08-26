# How to run a phase

Read this once. Every phase follows the same cycle.

To launch an agent by hand, see [writing prompts for agents](agent-prompts.md).

## Before writing anything

1. Read the phase's `plan.md` in full, including its **Before you start** section.
2. Read what that section tells you to read. It is short and specific on purpose.
3. Check your agent memory in `.claude/agent-memory/<your-name>/`.
4. If something in the plan contradicts `CLAUDE.md` or the planning documents, **stop and ask**. Do not resolve the contradiction on your own — it usually means a decision was recorded somewhere you have not seen.

## The cycle

```
issue  →  branch  →  work  →  PR  →  reviewer  →  merge  →  status
```

**Issue.** One per task in the plan. Title from the task, body with the relevant acceptance criteria.

**Branch.** `type/description`, lowercase, only `a-z 0-9 . _ -`. Never work on `main`.

**Work.** Stay inside your module. If the task pushes you outside it, that is a sign the task belongs to another agent — say so instead of crossing the boundary.

**Commits.** Through the `/git-commit` skill, never a bare `git commit`. One logical change per commit.

**PR.** Opened against `main`, closing its issue. Describe what changed and which acceptance criteria it satisfies.

**Review.** `reviewer` audits against the acceptance criteria in the plan and the invariants in `CLAUDE.md`, and accepts or rejects. A rejection is normal: it comes back with what failed and why.

**Status.** Update the phase's `status.md` **on the branch, before the PR is reviewed**. It is part of the phase's work, it travels with the code, and it lets the reviewer check whether the status tells the truth. Record what was completed, what is open, and anything the next person needs to know.

**Merge** once accepted.

**Afterwards**, two writes, and both of them or neither:

1. the phase's `status.md` **`State:` line**, to what the phase actually is now, naming the PR;
2. the phase table in `docs/STATUS.md`, which describes what is on `main` rather than what a branch claims.

Then read back over the `status.md` you just closed and strike out anything in it written in the
future tense — "remains", "whoever merges should", "not yet" — that the merge has answered. A status
file is a dated record and stays one; a *forward-looking* sentence in it is read as current by the
next person, and that is how phase 09's file ended up telling its reader the play link was a 404
three weeks after it went live.

**The first of those two writes did not exist before 26/08/2026**, and four of the nine phase status
files had drifted from the table in `docs/STATUS.md` as a result — phase 09's still said "in
progress" with the MVP shipped. See `docs/plan/10a-honest-documentation/audit.md`, F28 and F29.

Then record in your agent memory what you learned that is **not** in `docs/`: a tool limitation, an operation that behaves differently under TeaVM, where something turned out to live. Not what the phase achieved — `status.md` already says that, and two copies of the same fact end with one of them stale.

## Writing anything into `docs/`

One convention, decided in phase 10a and justified in
[`10a-honest-documentation/mechanism.md`](10a-honest-documentation/mechanism.md):

> **A passage that describes behaviour either names, in backticks, the file that implements it — or
> says "Not built".**

It costs a backtick. It buys three things: a reader can tell a rule that is enforced from one that is
only written down, which nine of phase 10a's findings turned on; a named file breaks loudly when the
code moves, where prose rots silently; and the `docs-refs` check
([#56](https://github.com/LuchoC-Dev/little-spaceship/issues/56)) can see the reference and fail the
build on it.

`docs/design/04-hud-layout.md` and `HudRenderer` are what it looks like done properly — the most
accurate document/code pair in the repository, and the one where each side quotes the other by name.

## Two failures and you stop

If the same thing fails twice, stop and report. Do not try a third variation.

Two failed attempts almost always mean the problem is not where it looks: a wrong assumption, a decision recorded somewhere you have not read, or a plan that asks for something impossible. A third attempt usually buries the evidence instead of solving it.

Report what you tried, what happened each time, and what you think the real obstacle is.

## When you are blocked

Say so and stop. Do not guess at a game rule — `docs/planning/08-decisions-and-open-items.md` separates what is decided from what is still open, and inventing an answer to an open item creates work that has to be undone.

Three things are worth interrupting for, always:

- an invariant in `CLAUDE.md` seems to get in the way of the task;
- the plan asks for something that contradicts a decided rule;
- you need to touch a module that is not yours.

## Parallel work

If another session is working at the same time, use a worktree:

```bash
git worktree add ../little-spaceship-<task> -b <type>/<description>
```

The art lane and the code lane always run in parallel, so this is the normal case rather than the exception.
