# Post-MVP roadmap

Written on 25/08/2026, the day the MVP shipped, from the player's direction after playing the
deployed build.

This document says **what the phases after 09 are and why**. It does not plan them: each one gets
its own folder under `docs/plan/` with a `plan.md` and a `status.md` when it is picked up, the same
as phases 01–09. Nothing here is scheduled.

Read `docs/STATUS.md` first for where the project actually stands.

## Where this came from

The MVP is live and playable. Playing it produced three defects and a longer list of things that are
not defects but decisions the MVP deferred. Rather than fixing them one at a time, the player asked
for the remaining work to be split into four phases, in this order, with the reordering done **first**
so that everything after it is built on a base that holds.

That ordering is the point. It is cheaper to fix the process and the foundations now, with one level
in the repository, than after five.

## What playing the web build found

Three issues, all open, none blocking:

| Issue | What |
|---|---|
| [#41](https://github.com/LuchoC-Dev/little-spaceship/issues/41) | Losing pointer lock breaks mouse control until the page is refocused |
| [#42](https://github.com/LuchoC-Dev/little-spaceship/issues/42) | No in-game options: volume cannot be changed while playing |
| [#43](https://github.com/LuchoC-Dev/little-spaceship/issues/43) | The shield and the attachment are invisible — no sprite, no animation |

[#40](https://github.com/LuchoC-Dev/little-spaceship/issues/40), QUIT doing nothing on the web
target, was found the same day.

**#41 is worth singling out.** Phase 09's task 4 was "verify pointer capture", it was never actually
verified, and the defect it would have caught is the one the player hit. Everything else about that
phase was checked against reality; that one criterion was assumed, and assuming it was wrong.

Other than these, the player's verdict on the web build was that it works.

## Phase 10 — Reordering: process, architecture, documents

**Why first:** the MVP shipped, but the way it was built failed in ways that will get more expensive
with every level added. The evidence is already written down:

- **Cost.** `docs/planning/13-working-with-agents.md` records ~3,300 model calls, 665 million cached
  input tokens and **fourteen spend limits across nine contexts**, two thirds of it re-reading
  conversation history rather than doing new work. The regime written after that audit — one
  coordinator per phase, `reviewer` on Sonnet, one issue per worker — was followed in phase 09 and
  held. It should be checked against phase 09's actual numbers and tightened where it did not.
- **Documents going stale, repeatedly.** `docs/design/07-skin.md` still describes a reflective Skin
  integration the code does not use, and that stale document is what put a false warning into
  `docs/STATUS.md` for a future phase to trip over. Phase 09 caught two more of the same shape: a
  `status.md` claiming CI had never run when four real runs existed, and a licence claim corrected in
  the README but left false in the status file. The reviewer's memory now catalogues this as a
  recurring pattern, which means it is a process defect, not bad luck.
- **Art delivered to nowhere.** Three times in one day, art a phase called delivered existed only
  under `docs/design/` with nothing in `assets/`. The sprites and fonts were fixed; `module-satellite`,
  `ship-bank`, `ship-tilt`, `ship-hit`, `structure-tower` and the five `icon-*` glyphs are still in the
  atlas, referenced by nothing (see #43).
- **Architecture and code structure**, to be audited before more levels are built on top.

**Not decided:** the specific scope. What counts as "reordering" needs to be settled before the phase
opens, or it will absorb anything anyone dislikes.

## Phase 11 — Reordering 2: the game itself

Same spirit, aimed at the code, the MVP and level 1 rather than at process. The goal is a base the
`level-designer` agent can build levels on **easily**, which today it cannot.

### Level 1 is too long

Measured, not estimated: `assets/data/level-01.json` has **92 spawn events** and the boss enters at
**302 s — 5.03 minutes**.

The player's judgement is that five minutes is too long, and the figure discussed was somewhere around
2.5–3 minutes. **That number is deliberately not fixed here** — it is decided in the phase that does
the work, once the wave structure below exists and the pacing can be felt rather than arithmetic'd.
What is settled is the direction: shorter, with the boss arriving earlier. Whatever the number turns
out to be, this is close to halving the timeline rather than trimming it.

### The boss changes

Both because it arrives earlier and because it is still too easy. `docs/STATUS.md` already records
the player's own diagnosis, which is better than the fix that was tried: the spread always points
*outward* and the sweep *inward*, so a player parked in the centre is never threatened. The fight is a
positioning problem solved once, not a dodge. Tripling the fan did not move it. What was suggested:
five rays, aimed at the player rather than at fixed outward angles — weighed against keeping the tell
honest, which is what makes the fight fair.

### Stats need a real pass

Enemy, ship, projectile and drop values are still largely placeholder — a heavy carrier dies in about
1.2 s against the 32 s its stretch reserves. The player's verdict after playing the web build is that
the game is **too easy**. This is a redesign of the numbers, not a tweak.

The open tension `docs/STATUS.md` already records belongs here: the spec asks the basic enemy for
"low health and a slow shot", and those fight each other, because an enemy that dies to one hit rarely
lives long enough for its rate of fire to read.

**Decided, and it stays decided: balance is tuned by playing, not by arithmetic.**

### Levels should be built out of waves, not a flat list of timestamps

Today `level-01.json` is **92 spawn events**, each an absolute time, an archetype, a formation and an
x position. It is a transcript, not a design: there is nothing in it that says "this is the opening",
and no way to reuse a piece of it.

The player's proposal is to group spawns into **waves** as the unit of level design. Roughly, as
described:

> wave 1 is one, two, then three basic ships; wave 2 is when they start coming three at a time and
> the faster ones begin to appear; wave 3 is the next step up, and so on.

Two reasons, both real: **a level becomes readable as a design** rather than as 92 rows, and **waves
can be reused** — across a level, and potentially across levels.

This is the piece the per-level document and the movement work below both hang off, which is why it
is written first.

**Open, and each of these changes what a wave is:**

- **What ends a wave.** A fixed duration, or "the wave is cleared"? Clearing is the more interesting
  design — pacing follows the player rather than the clock — but it makes level length depend on how
  the player performs, which is exactly what "the boss arrives at 2.5 minutes" stops meaning. Note it
  does *not* break determinism: the core reads world state, not the clock, so a cleared-based trigger
  stays reproducible. The invariant survives; the schedule does not.
- **How a wave is placed.** Absolute time, or relative to the previous wave ending? Reuse pushes hard
  towards relative — an absolute time is not reusable anywhere.
- **Whether a wave takes parameters.** The same wave at a higher difficulty, mirrored, or entering
  from a different side, is the difference between reuse and copy-paste. It is also the difference
  between a simple format and a small language, and this project's sixth invariant is no abstraction
  without a real case in the MVP.
- **Where waves live.** `assets/data/` alongside formations, or a level-level construct. Formations
  already exist as a grouping below this one; waves sit above them, and the two should not blur.

None of this is decided. It is written down so the phase that picks it up starts from the questions
rather than rediscovering them.

### Movement needs to be a system, not a value

Today a spawn event names a formation and an x position. What the player wants is for an enemy's
*behaviour* to vary by where it appears in the level:

> a fast enemy might enter from one direction with one movement early in the level, from another
> direction with a different movement at the midpoint, and do something else again at the end.

Two pieces come out of that:

1. **Movement as a described thing** — a class, interface or data description covering shapes like a
   U-shaped attack run, a straight 30° diagonal, or a curve following something like an inverted
   logarithmic function. Today there are four trajectories in `assets/data/`.
2. **A per-level document** giving, for each enemy, projectile and appearance, its stats and what it
   actually does — so that changing something means reading that document rather than reading the
   code. The player's reason, in their words: so we do not forget, and do not have to look directly at
   the code every time we want to change something.

**Open, and it matters:** whether that per-level document is authored by hand and consumed by the
game, generated from the level data, or a view produced for reading only. Those are three different
things with three different failure modes — the first can drift from the code, the second cannot but
constrains the format, the third is safe and does the least. This needs deciding before anything is
built, and it interacts with `core`'s invariant that content is data read without reflection.

Also open from before: the intensity-curve tooling, which `docs/STATUS.md` has been carrying as a
non-blocking item since the level's length and climax were decided.

## Phase 12 — Levels 2 and 3, and the shape of the story

Build levels 2 and 3 on the base phase 11 produces, and plan the story across the first five levels.

The story planned here is **not final** — it covers five levels while only three exist, so it is a
working outline that phase 13 completes.

## Phase 13 — Through level 5, and the finished story

- Levels 4 and 5.
- **The final story**, complete rather than outlined.
- **Animations.**
- **Backgrounds per level.**
- **Progression**, and whatever else the game needs by then to be a whole rather than a sequence of
  levels.

## What this document is not

It is not a schedule, and it is not a plan. Each phase needs its own `plan.md` with acceptance
criteria before work starts, written the way phases 01–09 were. Several things above are marked open
on purpose: deciding them here, in a summary written the day the MVP shipped, would be exactly the
kind of guess `docs/planning/08-decisions-and-open-items.md` exists to prevent.
