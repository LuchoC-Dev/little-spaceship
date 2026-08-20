# Phase 06 — Presentation · status

**State:** visual direction done, art production not started
**Updated:** 20/08/2026

Update this file when the phase moves. It is the only place phase progress is recorded — the
`plan.md` next to it says what to do and does not change to reflect progress.

## Done

**Visual direction, tasks 1 to 5** — issue #6. It lives in [`docs/design/`](../../design/):

| Task | Where |
|---|---|
| 1. Palette | [`01-palette.md`](../../design/01-palette.md) — 32 colours, two disjoint sets, a reserved hue band for enemy fire |
| 2. Sprite sizes | [`02-sprite-sizes.md`](../../design/02-sprite-sizes.md) — pixels and hitbox radii per archetype |
| 3. Bitmap typography | [`03-typography.md`](../../design/03-typography.md) — `font-mini` 5x7 and `font-title` 7x11 |
| 4. HUD layout | [`04-hud-layout.md`](../../design/04-hud-layout.md) — every widget at fixed coordinates |
| 5. Legibility rules | [`05-legibility-rules.md`](../../design/05-legibility-rules.md) — R1 to R17, with the procedures that check them |

**Synchronisation point 1 is closed.** Phase 03 can write hitboxes: sizes and radii are in task 2's
table, and the art lane can start drawing.

Two scripts came with it, because "checked on the real thing" needed to be a command rather than an
intention: `palette/check.py` verifies the palette's own rules, and `palette/lint-art.py` verifies a
drawn PNG against them.

## In progress

Nothing. Art production, tasks 6 to 11, has not started.

## Blocked

Integration, tasks 12 to 14, waits on phase 03 as planned.

## Decisions taken while implementing

The plan named the deliverables but not their content. These were decided here, and the reasoning
is in the document each one points to.

- **The palette is split into two disjoint sets** — background-legal and gameplay-only — with a
  measured gap of 3.2 points of lightness between them, rather than a legibility rule people are
  asked to remember. Enemy bullets get a reserved hue band on top of that.
- **The boss health bar is vertical, in the right margin.** A horizontal bar over the playfield
  would cover 208 px of play space at peak density.
- **Invulnerability is shown on the ship**, not as a HUD widget, and its three sources look
  different from each other.
- **The `MODULE` block is hidden when there is no attachment**, since the spec asks for the
  attachment *if any* and the acceptance criterion is "nothing more".
- **The boss collides as five circles, not one.** `Collider` is a circle with no offset, and one
  circle over a 119 px boss would swallow the gaps the player flies through. The part map is in
  task 2; **phase 07 should confirm or change it before art is drawn against it.**
- **Every sprite dimension is odd**, so a centred sprite has its axis of symmetry on one pixel
  column.
- **Sprite ids are proposed, not decided.** They are a proposal for synchronisation point 2 in
  phase 04.

None of these changes a game rule, so nothing was added to
`docs/planning/08-decisions-and-open-items.md`.

## Notes for whoever comes next

**For the code lane, phase 03.** Only two columns of task 2's table matter: sprite size and hitbox
radius. The player ship is 15x17 with a radius of 3.0 — 40% of its width — which is the spec's
"smaller than the sprite, but not a single point" turned into a number.

**For the art lane, tasks 6 to 11.** Run `lint-art.py` on each asset as it is finished rather than
at the end. The failure it catches — a background using a gameplay colour — is invisible until the
level runs.

**Where the plan was thin.** It asked for sprite sizes "for hitboxes" without saying that
`core.domain.component.Collider` is a circle with no offset. That shapes the art: visual mass has to
sit at the centre of the sprite, and anything outside the circle has to read as secondary. It is
recorded in task 2 as a rule rather than left to be discovered when the first wide enemy is drawn.
