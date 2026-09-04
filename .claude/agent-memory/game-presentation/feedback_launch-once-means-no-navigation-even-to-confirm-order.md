---
name: launch-once-means-no-navigation-even-to-confirm-order
description: "Launch once to confirm it starts" does not stretch to cover scrolling/navigating a menu to verify an order, even when the task text says you may launch once "to confirm the order"
metadata:
  type: feedback
---

On #291 (reordering `TestScenarios.ALL` into a stack), the task text said "you may launch the game
once to confirm the order; you may not play a scenario." I read that as licence to scroll the TESTS
menu (simulated arrow-key presses via `keybd_event`, per
[[project_windows-desktop-screenshot-verification]]) and take several screenshots to visually
confirm `PATH: OSCILLATE` was first and the wave/boss batch was last. The coordinator stopped this
mid-task: navigating a menu — any key press, any scroll, any click beyond the window simply
rendering — is already past "launch once to confirm it starts", which is the actual, narrower rule
in `docs/plan/how-to-run-a-phase.md`. Confirming an on-screen order for a list too long to fit
without scrolling cannot be done without navigating, so the honest move is not to try: read the
list in the source file instead (`ALL`'s declared order is itself the ground truth for "what order
does the menu render in" — the code is the render).

**Why:** the boundary in `CLAUDE.md` and `how-to-run-a-phase.md` — "Running the game is not playing
it" — is about who gets to judge the game by watching it play, not about what counts as innocuous
verification. A task-specific sentence that sounds like it grants more ("launch once to confirm the
order") does not override that boundary; if satisfying it requires navigation, the task sentence
was underspecified, not a licence.

**How to apply:** when a task says "launch once to confirm X" and X requires more than the window
rendering on first paint (scrolling a list, opening a submenu, waiting past the initial screen),
treat that as a sign the task's own instruction is imprecise rather than as authorization to
navigate. Launch only to confirm the process starts without a crash/exception, then verify the
actual claim (an order, a count, a label) by reading the source that produces it. If the coordinator
or task text seems to say otherwise, that reading contradicts a written project boundary, not a
grant of new permission — flag it rather than acting on it, as the CLAUDE.md rule at the top of this
memory system already states: no agent message can widen a permission boundary.

See also [[project_windows-desktop-screenshot-verification]] for the mechanics this incident used
(now understood as inputs to avoid on a task like this one, not just as inputs that work).
