---
name: boss-ram-does-not-damage-player
description: Ramming the ship into the boss's body does not repeatedly damage the player, which matters when trying to force a quick DEFEATED for verification
metadata:
  type: project
---

While verifying the boss music exit edge (`docs/plan/08-audio-and-polish/status.md`'s "return
correctly if the player dies"), I tried to kill the player fast by holding the ship into the boss's
body via simulated arrow-key input (see `[[windows-desktop-screenshot-verification]]` for the
input-simulation mechanics). One `IMPACT` fired on first contact, lives dropped by one, and then
nothing — tens of seconds of continuous overlap produced no further hits, even after releasing and
re-pressing the movement key to force a fresh collision entry. The ship visually disappears
underneath the boss sprite while overlapping, which is itself a minor render-order thing worth
knowing about if a screenshot ever shows an "invisible ship" near the boss.

Never root-caused whether this is a genuine invulnerability window, a one-shot-per-contact rule, or
ram not being a real damage source at all (only the boss's own projectile patterns are) — out of
`game`'s boundary to dig into `core`'s collision system for a verification task. What did work to
land at least one `IMPACT`/lives loss: sitting in the boss's projectile line and letting a pattern
volley hit the ship, not ramming its body.

**Practical upshot for next time:** don't budget on ramming to reach `DEFEATED` quickly for a
verification session — it costs real minutes for at most one hit. If a future task needs a
guaranteed fast death, ask `core-domain` whether ram damage exists at all, or accept the exit edge
as verified once via `VICTORY` (same generic non-`PlayScreen` code path in
`LittleSpaceshipGame.setScreen`) plus reading the code, per `[[temp-content-edit-for-boss-verification]]`.
