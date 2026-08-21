# Boss presentation — art structure and the tell

Phase 07 decided on 21/08/2026 that the level 1 boss has **one phase, two alternating attack
patterns and a clear tell before each** (`../planning/08-decisions-and-open-items.md`). The tell is
a visual problem before it is a design one: it has to read in the fraction of a second before the
attack, at 480x270, over whatever the background is doing, while the player is dodging. This page
settles how the boss is drawn and how the tell is built. Its footprint and colliders are frozen in
[`02-sprite-sizes.md`](02-sprite-sizes.md) and nothing here moves them.

## The boss is five sprites, not one image

`02-sprite-sizes.md` already says the boss collides as five entities because one circle over 119 px
would swallow the gaps the player flies through. The art follows the same split, and this is a
decision rather than a convenience:

| Part | Sprite | Centre offset | Radius |
|---|---|---|---|
| Core | `boss-core` 47x87 | 0, 0 | 18.0 |
| Pod, left / right | `boss-pod` 25x25 | -34, +6 / +34, +6 | 12.0 |
| Arm, left / right | `boss-arm` 31x45 | -44, -18 / +44, -18 | 14.0 |

Offsets are as in `Transform`: x grows right, y grows up. The arms at ±44 with a 31 px sprite reach
x = ±59, which is what makes the boss 119 px wide; the core at 87 px tall is what makes it 87 tall.
Nothing else touches either extreme.

Four reasons the split is the right one, in order of weight:

1. **The hit flash has to be per part.** The player is told which of five colliders he damaged. A
   single 119x87 image can only flash as a whole, which reads as "the boss took a hit" when the
   information the player needs is "*that* pod took a hit".
2. **Parts die.** Pods and arms are destroyed before the core. A monolithic image would need a
   frame for every surviving combination — five parts is 2^4 = 16 variants of one 119x87 sheet.
3. **The tell is a part animation** (below), and a part that animates independently must be an
   independent region.
4. **A hand-authored 119x87 grid is a transcription error waiting to happen.** 47x87 and 31x45 are
   still large but they are mirrorable and countable; the mock's own source already says this about
   the carrier and the boss, which is why both were drawn from primitives.

`SPRITES` in the mock therefore gains `boss-core`, `boss-pod` and `boss-arm`, and `drawBoss` in
`src/03-scenes.js` becomes five `blit` calls at the offsets above instead of the primitive stack it
holds today. **The right-hand parts are the left-hand sprites mirrored**, drawn by the renderer, not
two entries in the atlas.

## What the boss is made of

The alien ramp from [`01-palette.md`](01-palette.md) — N0, V4, N5, N6 — with one addition that is
the boss's own: the **core is the only place F1 appears on an enemy at rest**. Every other enemy
keeps F1 for its explosion. That is what makes the boss's centre the thing the eye returns to.

| Region | Colours | Why |
|---|---|---|
| Hull mass | V4 | the campaign's alien colour, unchanged at this size |
| Ribs, plate edges | N5 | cold metal; the alien ramp has no lighter violet |
| Top-lit plate edges | N6 | 1 px, on upward-facing edges only |
| Interior shadow, gaps | N0 | the gaps are negative space, not shading — see below |
| Core iris | W3 / W4 / F1 | the one warm accent, and the aim point |

**The one-violet constraint holds on the boss, but only because of the gaps.** At 119x87 the missing
mid-dark violet would normally show as flat, blotchy black wherever a hull needs occlusion. It does
not here because the boss's dark regions are *structural*: the channels between core, pods and arms
are holes the player flies through, so N0 there reads as background showing past the boss rather
than as a shadow that failed. A boss without those channels would need the second violet. Do not
close the channels to make the silhouette read better — closing them is what would force the
palette to widen.

## The tell

**One tell shape, two colours, three quarters of a second.** The player learns one thing and reads
it twice.

The tell is a **charge**: the part that is about to fire brightens from the inside out, in three
steps, holding the third for the last beat before the shot.

| Beat | Duration | What is drawn |
|---|---|---|
| 1 | 0.25 s | the firing part's iris fills W4, its rim ring lights N6 |
| 2 | 0.25 s | the iris fills F1, the rim ring flashes N7 for one frame then holds N6 |
| 3 | 0.25 s | a 1 px N7 outline traces the whole firing part, held steady |
| fire | — | the outline drops in the same frame the first bullet leaves |

Three properties make it survive the background, and each answers a specific failure:

- **It grows.** A colour change alone is lost in a busy frame; a change in *lit area* is not. The
  charge roughly doubles the part's bright pixel count across the three beats.
- **It ends on N7 at 100 `L*`, on the outline.** The background ceiling is 44.9, so the last beat is
  55 points of lightness above anything the scenery can produce, and it is on the silhouette edge
  where nothing overlaps it.
- **It never enters hues 320-350.** The tell is warm-white, the bullets that follow are magenta.
  The player is never asked to tell a warning apart from the thing it warns about.

**The two patterns are told apart by which parts charge, not by the tell's own appearance.**

| Pattern | Charges | Reads as |
|---|---|---|
| Spread | both **pods**, upper inner | pressure from above the centre — move outward |
| Sweep | both **arms**, lower outer | pressure from the flanks — move to the middle |

That mapping is deliberate: the part that lights is on the side the danger comes from, so the tell
carries the dodge direction and not only its timing. A player who never consciously learns the two
patterns still moves the right way.

The **core never charges**. It is the aim point and it stays constant, which is what keeps the two
tells from being confused with "the boss is doing something".

### What the tell must not be

- **Not a screen shake or a flash of the whole boss.** At 119 px that covers 57 % of the playfield;
  a full-boss flash momentarily raises the brightest thing on screen everywhere, which is exactly
  the condition under which an enemy bullet stops being findable.
- **Not a colour-only change.** It fails for a dichromat player and it fails against an explosion.
- **Not shorter than 0.5 s.** Below that the tell and the attack are one event and the player is
  reacting to the bullets, which is the boss the decision was taken to avoid.

## The boss bar and the tell are different channels

The vertical bar in the right margin (`04-hud-layout.md`) reports damage. It must not react to the
tell: a bar that also pulses turns the margin into a second thing to watch during the only moment
the player cannot afford to look away from the playfield.

## Open, for whoever draws it

The three part sprites are specified here and **not yet drawn**. When they are, the checks that
matter are `check.js` for the palette split and a look at the boss at 1x with the tell on its last
beat while the escalation scene's enemy fire is on screen — the tell is only correct if the magenta
is still the easiest thing to find.
