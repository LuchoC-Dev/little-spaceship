---
name: agent-must-not-play-the-game-to-verify
description: correction from the project owner mid-task on phase 11f — do not drive the game to reach a specific state to verify a fix, even locally on desktop
metadata:
  type: feedback
---

Mid-way through verifying #43 (shield/attachment visibility), the coordinator relayed a correction
from the project owner: an agent may launch the desktop build only to confirm it starts and the menu
renders, then stop. **Do not** click through to gameplay, do not try to reach a specific wave or
beat, do not try to trigger a drop or survive to see a HUD state.

**Why**: judging the game by playing it is the project owner's role, deliberately — phase 11e was
structured around exactly this split (agents build a candidate, the owner plays it, a clean build
and a green check are explicitly not the deliverable). An agent driving input into the game via
simulated mouse/keyboard is neither a build check nor a real play session; it is slow (each
foreground+click+screenshot round trip on this shared Windows machine costs real time and is flaky —
focus gets stolen by other apps on the desktop, see `[[windows-desktop-screenshot-verification]]`)
and it produces a claim ("I saw the shield") that is far weaker evidence than it sounds, since it was
manufactured by the same agent making the claim.

**How to apply**: for any future task that asks to "verify the fix works" in a way that requires
progressing through gameplay (reaching a wave, a boss, a pickup, a specific `PlayerStatus` state):
compile, launch once to confirm the window opens and the first screen renders, then stop and write
"not checked" for everything past that — plus the exact steps (including which wave/wave-offset/file
to look at) the project owner should follow to see it themselves. `CLAUDE.md` already says "not
checked" is always acceptable and never a failure; this is that principle applied specifically to
"do not play the game to produce your own evidence of gameplay behaviour."

A **local content edit to skip ahead** (e.g. temporarily changing a wave's spawn time in
`assets/data/waves.json` to force an early drop, per `[[temp-content-edit-for-boss-verification]]`)
is a legitimate technique for *reaching* a state to look at code/config against — but reaching a
state and then *playing to interact with it* (steering the ship, timing a pickup collection) is the
part that crossed the line here. If a task's acceptance criterion genuinely needs to be exercised
through play, that verification belongs to the project owner's play session, not to this agent.
