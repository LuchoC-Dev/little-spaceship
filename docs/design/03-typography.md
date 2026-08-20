# Typography

Two bitmap fonts, both drawn by hand as PNG sheets. No TrueType, no `FreeTypeFontGenerator`: at
480x270 a scalable font produces blurred, half-pixel glyphs, and the generator is one more thing to
verify under TeaVM.

## `font-mini` — everything the player reads

The working font. HUD labels, menu entries, options, credits, ship characteristics.

| Property | Value |
|---|---|
| Glyph box | 5 x 7 px |
| Cell in the sheet | 6 x 10 px |
| Cap height | 7 px, rows 0-6 of the cell |
| x-height | 5 px |
| Descender | 2 px, rows 7-8, for `g j p q y` |
| Baseline | row 7 of the cell |
| Advance | 6 px, fixed |
| Line height | 10 px |
| Coverage | ASCII 32-126, 95 glyphs |
| Sheet | 16 columns x 6 rows = 96 cells, **96 x 60 px** |

The advance is **fixed**, which makes every number tabular for free: a score does not jitter as its
digits change, and the width of a string is `characters * 6` without measuring anything. At this
size proportional spacing buys a few pixels and costs a moving HUD.

The 96th cell is left empty; it is spare, not a glyph.

## `font-title` — screen titles and the score

Used in exactly two places: the title on each screen, and the score value in the HUD. Everywhere
else uses `font-mini`.

| Property | Value |
|---|---|
| Glyph box | 7 x 11 px |
| Cell in the sheet | 8 x 13 px |
| Baseline | row 11 of the cell |
| Descender | none |
| Advance | 8 px, fixed |
| Line height | 13 px |
| Coverage | `A-Z`, `0-9`, space, `. , : - ! ?` |
| Sheet | 10 columns x 4 rows = 40 cells, **80 x 52 px** |

Uppercase only, and there is no lowercase to fall back to. A string with lowercase in it is a
mistake in the caller, not in the font — the loader uppercases before drawing rather than showing a
gap.

It is a **hand-drawn** font and not `font-mini` scaled x2. Scaling would be exact, since only
integer factors are allowed, but a 10x14 letter made of 2x2 blocks reads as a placeholder.

## How both are drawn

**White on transparent.** One sheet per font, every glyph in `N7`. Colour comes from the batch tint
at draw time, so a label in `N4` and a value in `N7` share one texture region and one draw call. No
colour is baked into the glyphs.

**Colours in use:**

| Role | Colour |
|---|---|
| HUD label | `N4` |
| HUD value, menu entry | `N7` |
| Menu entry, disabled | `N3` |
| Menu entry, selected | `W4` |
| Warning, destructive confirmation | `W3` |

**Text over the playfield carries a shadow.** One pixel of `N0` at offset `+1, +1`, drawn as a
second pass before the glyph. Over the HUD plates it does not: the plate is already `N2` and the
shadow only muddies the letter. The rule is about what is behind the text, not about which font it
is.

**No kerning, no ligatures, no letter-spacing tricks.** Position is `x + index * advance`, an
integer, always.

## Layout consequences

Because both advances are fixed, every string's width is known before it is drawn:

| Font | Width of `n` characters |
|---|---|
| `font-mini` | `n * 6` px |
| `font-title` | `n * 8` px |

The widest usable line inside a HUD column is 106 px, which is **17 characters** of `font-mini` or
**13** of `font-title`. Any label that does not fit gets shortened at design time; there is no
ellipsis and no wrapping in the HUD. Menus, which have the whole 480 px width, do wrap — at word
boundaries, on 10 px lines.

The score uses `font-title`, seven digits, zero-padded and right-aligned: `0012500`. Padding is the
arcade convention and it also means the field never changes width, so nothing next to it moves.

## Licensing

Both fonts are drawn for this project, so they are covered by the repository's own licence and
nothing has to be recorded in the external-asset register of
`../planning/07-references-and-asset-constraints.md`. That register exists for the case where an
external font is used, and this decision is what keeps that case from arising.
