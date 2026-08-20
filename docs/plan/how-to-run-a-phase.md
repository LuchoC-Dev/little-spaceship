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

**Afterwards**, update the phase table in `docs/STATUS.md`, which describes what is on `main` rather than what a branch claims. Then record in your agent memory what you learned that is not written in `docs/`.

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
