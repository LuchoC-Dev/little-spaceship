---
name: defect-patterns
description: The recurring shapes of defect found when auditing this repo — where to look first in a phase review
metadata:
  type: project
---

Defect patterns observed auditing phase 01 (`feat/core-foundations`, PR #2). None of them are visible to a grep, and none of them failed a test.

**Why:** this project defends its invariants with self-written architecture tests and with a status/PR narrative. Both can be *narrower or staler than they claim* while everything stays green, so the audit has to check the claim against the check, not just run the check.

**How to apply:** on any phase review, walk these five before reading the implementation line by line.

1. **An architecture test whose scope is narrower than the criterion it is cited for.** Read the test's package/class filter, not its javadoc. Phase 01's boundary check inspected only two of the three core layers while the plan's criterion and the PR table said "no public type in core". The test was still useful; the *claim* was overstated.
2. **Public accessors with zero call sites that hand out a mutable internal.** Grep the accessor name across `src/main` and `src/test`; a getter used by nobody is usually a boundary hole that a later phase will walk into. Cross-check it against the "no abstraction without a real case in the MVP" invariant, which catches it on two grounds at once.
3. **Two ways to destroy/mutate the same state, only one of which is safe.** Whenever a class documents "the caller always does X", check whether the API also allows not-X and what happens then. Ghost state that survives destruction is the shape this takes here.
4. **Agent memory under `.claude/agent-memory/` contradicting `docs/plan/*/status.md`.** Memory gets committed mid-phase and then the phase moves on; the memory file is not updated. Diff the two whenever both changed on the same branch.
5. **Docs conventions.** The English-only rule is absolute outside `docs/sources/`, and `spikes/` is already fully Spanish, so new files added there inherit the violation. Check whether an offending line is pre-existing on `main` before treating it as introduced by the PR — `git diff main...HEAD -- <file>` settles it and changes whether it can block.

Also worth checking every time: whether the branch edited its own `plan.md` acceptance criteria. Phase 01 did, and the edit *raised* the bar rather than lowering it, which is fine — but it has to be read, not assumed.

See [[audit-techniques]] for how to confirm these cheaply without touching the repo.
