---
name: review-tooling-and-memory-placement
description: gh CLI scope failures on this token, and why reviewer memory written on a feature branch diverges from main
metadata:
  type: project
---

Operational traps around delivering a review on this repository. Neither is visible from the code.

**Why:** the verdict has to land as a comment on the pull request so it lives in the repository, and the reviewer's own memory has to land in the worktree being audited. Both steps have failed here for reasons that look like bugs and are not.

**How to apply:**

- **`gh pr view --comments` and `gh pr edit` fail with GraphQL scope errors on the token in use.** They are not usable for reading a prior review or amending a PR body. The REST endpoints work: `gh api repos/:owner/:repo/issues/<n>/comments --jq '.[] | .body'` to read the review thread, `gh api repos/:owner/:repo/pulls/<n> --jq '.body'` to read the description, and `-X PATCH` to change one. `gh pr comment <n> --body-file <path>` works normally — that is the one to post the verdict with, writing the body into the scratchpad first.
- **`gh pr view --json <fields> --jq` works** for plain metadata (title, headRefName, baseRefName); it is only the comment/review fields that trip the scope error.
- **Heredocs with backticks can fail through the Bash tool even when quoted (`<<'EOF'`).** Writing a long markdown body failed with "unexpected EOF while looking for matching quote". Use the Write tool for the file, then `--body-file`. Faster than debugging the quoting.
- ~~**Reviewer memory on `main` is usually ahead of the copy on a feature branch.** … memory belongs in the worktree being audited … write the *superset*.~~ **Reversed on 26/08/2026 by phase 10b, issue #61.** This advice was the memory-path trap written down as guidance, and following it is what produced three hand corrections in phase 09 and a divergent copy on every phase branch. Memory now has exactly one home: run `tools/agent-memory-path reviewer`, which prints the main checkout's directory from any worktree, and write there. The `pre-commit` hook refuses a commit that stages `.claude/agent-memory/` from a linked worktree, so the old habit now fails loudly instead of quietly. The superset dance is no longer needed: there is only one revision.
- **Leave the memory files uncommitted and say so in the report.** The reviewer does not commit; the parent decides whether the memory rides along with the phase branch or goes in separately.

Related: [[defect-patterns]], [[audit-techniques]].
