# Writing prompts for agents

For launching an agent by hand. The main session normally does this on its own, but the prompt has to be built the same way either way.

## The principle

**The prompt points at the plan; it does not repeat it.**

Every phase has a `plan.md` written to be self-sufficient, with a "Before you start" section naming exactly what to read. If the prompt restates it, two things go wrong: the two copies drift, and the plan stops being tested. When an agent has to come back asking for something the plan should have said, that is a defect worth fixing in the plan — and a fat prompt hides it.

A good prompt is short. It says which work, where the boundaries are, how to deliver, and what to be careful about. Nothing else.

## Template

```
<One line: what to do and where the instructions are.>

Working directory: <absolute path>

**Your instructions are in `<path to plan.md>`.** Read it in full, follow its
"Before you start" section, and work from its task list and acceptance criteria.
It is written to be self-sufficient — if you find it is not, say so in your
report, because that is a defect worth fixing.

This closes GitHub issue #<n>.

## Scope
<Only if the task pushes past the agent's normal boundary. State the exception
and its limit explicitly, or omit this section.>

## Working method
- Branch from `main`: `<type>/<description>`. Never commit to `main`.
- Commit through the `/git-commit` skill. Several small commits beat one large one.
- Update the phase's `status.md` on the branch, before review.
- Push and open a **draft** pull request against `main` closing the issue.
- Record in your agent memory what you learned that is not already in `docs/` —
  gotchas and constraints, not phase progress.

## Watch out for
<One or two real risks. Not a checklist — the things that would actually go
wrong, and why they are hard to notice.>

## Stop rule
If the same thing fails twice, stop and report instead of trying a third
variation. Two failures usually mean the problem is not where it looks.

## Report back
Keep it short: what you built, anything in the plan that was ambiguous or
missing, decisions you had to make that it did not cover, and which acceptance
criteria pass and which do not.
```

## What makes the difference

**"Watch out for" is where the value is.** Everything else is form. Use it for the failure that is invisible until much later — the `Rng` diverging between runtimes, an allocation per frame, a rule that was corrected mid-planning and whose old version is still the intuitive one. Naming two real risks beats listing ten generic ones.

**Ask for the plan's defects.** Agents will paper over a gap rather than report it unless told otherwise. That feedback is the only way the remaining plans improve.

**Draft pull requests, not ready ones.** The review happens before the PR is marked ready, so nothing merges on the author's own say-so.

**Say what not to do when the temptation is real.** "Do not over-build the ECS" worked because the pull towards archetypes and queries is genuine. Prohibitions nobody would have violated are noise.

## Choosing the agent

| Work | Agent |
|---|---|
| Game rules, ECS, systems | `core-domain` |
| Rendering, HUD, screens, audio, input | `game-presentation` |
| Palette, sprite sizes, HUD layout, legibility | `visual-designer` |
| Unit tests and replays | `test-engineer` |
| Auditing finished work | `reviewer` |

Boundaries come from the module architecture. If a task spans two agents, split it rather than granting an exception — the exception in phase 01 was for build scaffolding, which belongs to nobody, and it was worth stating its limit precisely.

## Reviews are prompts too

A review prompt has one extra job: **say what has already been checked**. Re-running the greps and the build wastes the audit on the cheap half.

Point it at what a grep cannot see — leaked mutable references, iteration order, whether the tests assert real rules or just that a setter sets, over-engineering the plan warned against. Ask it to argue with the author's recorded decisions rather than merely flag them. And tell it that finding nothing is a valid result, or it will manufacture something.
