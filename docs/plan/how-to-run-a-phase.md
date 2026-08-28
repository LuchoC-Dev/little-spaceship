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
issue  →  branch  →  work  →  pre-pr-check  →  PR  →  reviewer  →  merge  →  status
```

**Issue.** One per task in the plan. Title from the task, body with the relevant acceptance criteria.

**Branch.** Four levels, and each one only ever receives a pull request from the level below.
Decided by the project owner on 26/08/2026; see [the branch regime](#the-branch-regime) below for
what each level is for.

| Branch | Who commits on it | How work leaves it |
|---|---|---|
| `main` | nobody | a pull request from `dev`, **merged by the project owner and by nobody else — an authorisation does not transfer this** |
| `dev` | nobody | a pull request from a phase branch, merged by a coordinator **only with the project owner's direct approval** |
| `phase/<phase>-<description>` | the coordinator, by merging sub-branches | a pull request against `dev` |
| `type/description` | the agent doing one task | a pull request against the phase branch |

**Work.** Stay inside your module. If the task pushes you outside it, that is a sign the task belongs to another agent — say so instead of crossing the boundary.

**Commits.** Through the `/git-commit` skill, never a bare `git commit`. One logical change per commit.

**pre-pr-check.** Run `tools/pre-pr-check --base <the phase branch>` before you open anything, and
paste its output into the pull request. It is a script, so it costs no tokens and it does not depend
on your judgement: branch name, commit hygiene, a clean tree, no build output, resolvable markdown
links, the executable bit on scripts, agent memory in the right checkout, and `./gradlew build` when
the diff touches code. **A red check means no pull request.** It is not a formality — the first time
it ran it caught the missing executable bit that killed phase 09's first two CI runs.

**PR.** Opened against **the phase branch**, closing its issue. Describe what changed and which acceptance criteria it satisfies. An agent opens the pull request and stops there: **an agent never merges its own branch**, the coordinator does.

**Review.** `reviewer` audits against the acceptance criteria in the plan and the invariants in `CLAUDE.md`, and accepts or rejects. A rejection is normal: it comes back with what failed and why.

A rejection goes back to the worker only while that worker is still open and the fix is inside what it just did. Once it is closed it stays closed: the coordinator takes prose fixes of one or two files, and anything larger becomes a new issue against the state already in Git. `docs/planning/13-working-with-agents.md` has the rule and what phase 09 measured behind it.

**Status.** Write your task's own file, `docs/plan/<phase>/status/<issue>-<slug>.md`, **on the branch, before the PR is reviewed**. It is part of the phase's work, it travels with the code, and it lets the reviewer check whether the status tells the truth. Record what was completed, what was decided that the plan did not specify, what is open, and anything the next person needs to know.

**One file per task, never a shared one.** Two tasks running at the same time never write the same path, so they cannot conflict — not "rarely", not "if the insertions land far enough apart". Phase 11b learned this the expensive way: every parallel agent edited one `docs/plan/11b-wave-system/status.md`, an agent hit a conflict there and force-pushed to escape it, and `reviewer` found that two other branches had auto-merged cleanly only because their two paragraphs happened to land at different offsets. The same shared file also produced the opposite failure — two merged pull requests touched it not at all, so a real defect and its fix were missing from the record until the coordinator noticed at close.

**The phase's own `status.md` is the coordinator's**, and holds only the `State:` line, the date and the phase narrative. It is written twice: when the phase opens and when it closes, assembled from the fragments. Do not edit it for per-task progress — that is what your fragment is for.

**Merge** once accepted — the coordinator merges the sub-branch into the phase branch. When every
task of the phase is in, the phase branch does **not** merge either: it opens a pull request against
`dev` and waits. A coordinator may merge that one, **but only with the project owner's direct
approval on that pull request** — ask, and never treat an earlier approval as covering this one.
`dev` reaches `main` through a pull request **the project owner merges themselves** — that one is
never a coordinator's, and being told to do it is a reason to stop and confirm, not to do it. It
happened once, on 26/08/2026: the owner said "you can merge to main", the coordinator did, and the
rule had been written that same day. `main` now requires an approving review on GitHub, so the normal
path refuses and only a deliberate admin override gets through.

**Afterwards**, two writes, and both of them or neither:

1. the phase's `status.md` **`State:` line**, to what the phase actually is now, naming the PR;
2. the phase table in `docs/STATUS.md`, which describes what is on `dev` rather than what a branch claims.

Then read back over the `status.md` you just closed and strike out anything in it written in the
future tense — "remains", "whoever merges should", "not yet" — that the merge has answered. A status
file is a dated record and stays one; a *forward-looking* sentence in it is read as current by the
next person, and that is how phase 09's file ended up telling its reader the play link was a 404
three weeks after it went live.

**The first of those two writes did not exist before 26/08/2026**, and four of the nine phase status
files had drifted from the table in `docs/STATUS.md` as a result — phase 09's still said "in
progress" with the MVP shipped. See `docs/plan/10a-honest-documentation/audit.md`, F28 and F29.

Then record in your agent memory what you learned that is **not** in `docs/`: a tool limitation, an operation that behaves differently under TeaVM, where something turned out to live. Not what the phase achieved — `status.md` already says that, and two copies of the same fact end with one of them stale.

## The branch regime

Decided by the project owner on 26/08/2026, and it replaces "work on a branch, merge back into
`main`". Phases 01–09 all merged straight into `main`, so `main` was simultaneously the trunk, the
integration branch and the thing the deploy workflow publishes — which is why a half-finished phase
was one merge away from the live site.

- **`main` is release.** Nothing is committed on it and nothing is merged into it except a pull
  request from `dev`. **The project owner merges that one**; no coordinator and no agent does.
- **`dev` is the trunk.** Nothing is committed on it either. Branching from `dev` happens for one
  reason only: to open a phase.
- **One phase branch per phase**, `phase/<phase>-<description>` — a super-branch if several phases
  genuinely run together. When the phase is done it is not merged: it opens a pull request against
  `dev`, so a whole phase is reviewable as one thing. A coordinator may merge that pull request once
  the project owner approves it directly; without that approval it stays open.
- **Every agent branches from the phase branch**, never from `dev`, with the usual
  `type/description`. It opens a pull request against the phase branch and stops. **A subagent
  merges nothing.** The coordinator merges the sub-branches.

The point of the two extra levels is control, not ceremony: the phase branch makes a phase reviewable
as a unit, and `dev` means nothing reaches the published site without a deliberate act by the person
who owns the project.

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

If another session is working at the same time, use a worktree, branched from the phase branch:

```bash
git worktree add ../little-spaceship-<task> -b <type>/<description> phase/<phase>-<description>
```

The art lane and the code lane always run in parallel, so this is the normal case rather than the exception.
