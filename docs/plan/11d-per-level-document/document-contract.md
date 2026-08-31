# What a generated per-level document contains

**Decides:** the sections of the generated per-level document, and refuses the rest — task 1 of
[phase 11d](plan.md), [#180](https://github.com/LuchoC-Dev/little-spaceship/issues/180).
**Owner:** `level-designer`. **Written:** 31/08/2026.

**Nothing in this document is built.** The generator is task 2 and it does not exist yet — no file in
`tools/` reads `assets/data/`, checked with `ls tools/` (`agent-memory-path`, `commit-subject-ok`,
`hooks`, `install-hooks`, `pre-pr-check`, `status-fragments`). Everything below is a decision the
generator implements, not a description of behaviour that runs today. Where a passage describes
behaviour that *is* built, it names the file.

The decision this rests on is closed and is not reopened here: **the JSON is the source, the document
is generated from it, and CI fails when they disagree** ([plan.md](plan.md), "What was decided, and by
whom"). So the sections below are not a wish list — whatever the generator emits *is* the document,
and this is the list it emits.

**Output path:** `docs/levels/<levelId>.md`, one file per level, so `docs/levels/level-01.md` for the
level that exists today. **Not built** — `ls docs/` returns `STATUS.md`, `design`, `plan`, `planning`,
`sources`, and no `levels`.

---

## The bar, stated as a test

The roadmap sets it, and it is higher than a reference for someone who already knows the game:

> **The per-level document is the interface.** It is what the agent reads. It must be complete and
> readable enough to design from, which is a higher bar than being a reference for a human who
> already knows the game.
> — [`post-mvp-roadmap.md`](../post-mvp-roadmap.md), "How later levels get built"

**The test.** Give an agent `docs/levels/level-01.md`, `docs/planning/04-campaign-and-levels.md` and
`docs/plan/11c-movement-shapes/shape-catalogue.md`, and nothing else — no `core/`, no `game/`, no
`assets/`. It must be able to write `assets/data/level-02.json` and the waves it needs, and have that
content **load without an error and play roughly as intended on the first run**.

"Roughly as intended" is the honest half of the bar. The document cannot make a level *good*; only
playing decides that, and `post-mvp-roadmap.md` says so — *"balance is tuned by playing, not by
arithmetic"*. What it must remove is the class of mistake that costs a whole iteration: a formation
half off screen, a drop on a slot that does not exist, a spawn scheduled after its own wave ends, a
`cleared` wave that cannot clear, an archetype whose rate of fire means it never fires.

The answer to the test is in [What the document alone is enough for](#what-the-document-alone-is-enough-for),
after the sections, because it can only be judged once they are named.

---

## Vocabulary, so the sections below are unambiguous

A **level** is an ordered list of **placements** (`level-01.json` → `"waves"`), each naming a wave id
and an `offset` measured from the end of the placement before it
(`core/port/WavePlacement.java`). A **wave** is a reusable unit with its own spawn list and one end
condition (`assets/data/waves.json`, `core/port/WaveDefinition.java`,
`core/port/WaveEndCondition.java`). A **spawn** places one **formation** of one **archetype**
(`core/port/SpawnEvent.java`). `core/domain/system/SpawnSystem.java` reads all of it;
`game/adapter/content/JsonContentSource.java` parses it.

**Absolute level time is a derived quantity, not a field.** No absolute timestamp survives anywhere in
`assets/data/` — that is what phase 11b removed. The document is where absolute time comes back, as
output.

---

## The sections

Each section names the JSON it is derived from, at field level, and states the beat it answers for
someone designing the next level. Three of them cannot be fully derived from `assets/data/` and are
marked **GAP**; they are the phase's most valuable output and are collected again in
[What `assets/data/` cannot give the document](#what-assetsdata-cannot-give-the-document).

### 1. Header and provenance

Level id, the exact list of files it was generated from, and one line saying the file is generated and
must not be edited by hand.

**Derived from:** the generator's own inputs — `assets/data/level-01.json`, `waves.json`,
`enemies.json`, `formations.json`, `trajectories.json`, `attachments.json`, `balance.json`.

**Why:** it is the first thing a reader has to know, and it is what makes the CI check comprehensible
when it fires. It is also the phase's acceptance criterion — *"a generated document exists for level
1, in `docs/`, and it names the file it was generated from"*.

**No generation date, no git hash, no tool version in the output.** A timestamp makes
regenerate-and-diff fail on every run, which turns the phase's whole mechanism into noise the first
person to see it disables. This is a constraint on the generator, not a preference; it is repeated in
[Mechanical requirements](#mechanical-requirements-on-the-generator).

### 2. At a glance

Total level length in seconds, the boss's entry time, the number of placements, the number of distinct
waves, total entities spawned, and whether the length is **exact** or **player-dependent**.

**Derived from:** `level-01.json` → `waves[].offset`; `waves.json` → `end.type`, `end.seconds`;
`formations.json` → `slots` (for the entity count, which is one entity per slot, per
`SpawnSystem.spawnWave`).

**Why:** length is the single number the roadmap says level 1 has wrong — *"the boss currently enters
at 302 s (5.03 minutes). Five minutes is too long"*. An agent designing level 2 needs to see, in one
line, what a level's budget looks like.

**The exact/player-dependent distinction is load-bearing.** Every `end` in `waves.json` today is
`fixedDuration`, so level 1's length is arithmetic — the generator computes 298.0 s for the wave
sequence against a boss at 302.0 s. The moment one wave uses `{"type": "cleared"}`
(`core/port/WaveEndCondition.java`), every absolute time after it becomes a lower bound and the
document must say so rather than print a number that looks exact. This is the one place a generated
document can lie by rounding a decision away.

### 3. The pacing table — the intensity curve as rows

One row per **placement**, in order: index, wave id, `offset`, start, end, end condition, entity
count, entities per second, the archetypes present, and the drops delivered.

**Derived from:** `level-01.json` → `waves[].wave`, `waves[].offset`; `waves.json` → `end`, `spawns[].spawn`,
`spawns[].drop`; `formations.json` → `slots`.

**Why:** this *is* the intensity curve `docs/planning/03-game-systems.md` asks every level to be
designed against — *"introduction; escalation; peaks; rests; recombination of threats; climax/boss"* —
and it is the thing 92 flat rows destroyed. It is also the answer to that document's standing wish for
*"a tool or graphical representation for designing that curve"*: a table of density over time is that
representation, in text, at no cost.

Run by hand against today's content, it comes out as:

```
    8.0 ->    35.5  l1-basic-intro         dur  27.5  entities  15  density  0.55/s
   35.5 ->    59.5  l1-light-intro         dur  24.0  entities  21  density  0.88/s
   59.5 ->    86.0  l1-basic-light-mix     dur  26.5  entities  41  density  1.55/s
   86.0 ->    87.0  l1-tank-solo           dur   1.0  entities   1  density  1.00/s
   92.0 ->   112.0  l1-tank-intro-b        dur  20.0  entities  18  density  0.90/s
  112.0 ->   126.5  l1-rush-intro-a        dur  14.5  entities  11  density  0.76/s
  126.5 ->   127.5  l1-tank-solo           dur   1.0  entities   1  density  1.00/s
  129.0 ->   138.0  l1-rush-intro-b        dur   9.0  entities   5  density  0.56/s
  138.0 ->   166.0  l1-carrier-intro       dur  28.0  entities  10  density  0.36/s
  166.0 ->   183.0  l1-shooter-intro       dur  17.0  entities  11  density  0.65/s
  183.0 ->   208.0  l1-veteran-mix         dur  25.0  entities  34  density  1.36/s
  208.0 ->   245.0  l1-carrier-pair        dur  37.0  entities  14  density  0.38/s
  245.0 ->   256.0  l1-rest-basic          dur  11.0  entities   1  density  0.09/s
  256.0 ->   297.0  l1-finale-a            dur  41.0  entities  77  density  1.88/s
  297.0 ->   298.0  l1-tank-solo           dur   1.0  entities   1  density  1.00/s
```

That is the whole argument for this section: the rest at 245 s and the climb into the finale are
visible in one glance, and no other artefact in the repository shows them.

**Density is entities per second of the placement's own duration, and the document says that in
words.** It is not a difficulty score. Its blindness is deliberate and stated: `l1-carrier-pair` reads
as 0.38/s and is the level's hardest encounter, because a carrier keeps producing children
(`enemies.json` → `spawner.interval`) and because 80 hit points against a 10-damage projectile is
where its pressure lives.

### 4. The curve, as a bar

The same numbers as section 3, drawn as a fixed-width ASCII bar of entities per second, one line per
placement.

**Derived from:** section 3, so from the same fields; no new input.

**Why:** the shape is what a designer reads, and a column of numbers is not a shape. It costs the
generator ten lines and it is deterministic text, so it diffs cleanly. An image would not — see the
refusals.

### 5. Wave by wave

For each wave the level places, once, regardless of how many times it is placed:

- id, end condition, duration, its own spawn list with local `at`, archetype, formation, `atX`,
  the resolved trajectory, drop and drop slot;
- **where it is placed** — every absolute start time this level places it at;
- per spawn, derived: entity count, the horizontal footprint in playfield units, and the seconds
  between the arrival of a staggered formation's slots.

**Derived from:** `waves.json` → `id`, `end`, `spawns[].at`, `.spawn`, `.formation`, `.atX`,
`.drop`, `.dropSlot`, `.trajectory`; `formations.json` → `slots[].offsetX`, `.offsetY`;
`enemies.json` → `collider.radius`, `motion.trajectory`; `trajectories.json` → `vy`;
`level-01.json` → `waves[]` for the placement list.

**Why the reuse map is not optional.** A wave is reusable across a level and between levels — that is
the point of 11b's split, and `l1-tank-solo` is placed three times in level 1 at 86.0 s, 126.5 s and
297.0 s. An agent editing that wave for one beat is editing all three, and nothing in `waves.json`
tells it so. This is the single highest-value derived fact in the document.

**Why the trajectory is resolved rather than printed.** `SpawnEvent`'s `"trajectory"` is optional and
overrides the archetype's own default (`SpawnSystem.spawnWave`, `SpawnEvent.hasTrajectoryOverride`).
Printing "(blank)" would leave the reader to go and look it up in `enemies.json`; the document prints
the shape the entity actually flies, and marks it `(override)` when the spawn chose it. **Today no
wave in `waves.json` carries a `"trajectory"` key** — `grep -c trajectory assets/data/waves.json`
returns 0 — while `assets/data/trajectories.json` holds seven shapes since phase 11c. The generator
must handle the override on its first day even though level 1 does not use it, because
[11e](../11e-level-one-redesigned/plan.md) is what starts using it.

**Why the footprint.** `SpawnSystem.spawnWave` computes `anchorX = atX * 208` and adds the slot's own
`offsetX` with **no clamping anywhere**: a `line-5` at `atX 0.9` spawns two slots off the right edge
and nobody is told. The document prints `min x` and `max x` including the collider radius and flags
anything outside `0..208`. Checked against today's content: no spawn in `waves.json` is out of range.

**Why the stagger in seconds.** A slot's `offsetY` is a head start in pixels, not a delay in seconds
(`SpawnSystem.positionSpawned`), so `column-3`'s 22-unit spacing is 1.2 s apart on `slow-descent`
(`vy -18`) and 0.27 s apart on `dive` (`vy -80`). The same formation is a stream or a burst depending
purely on which archetype uses it, and that is invisible in the JSON. For an `arc` trajectory
(`core/port/ArcTrajectoryDefinition.java`) the gap is not constant and the document says "varies" with
the spawn-instant value, rather than printing a number that is only true at t=0.

### 6. Roster — the archetypes this level uses

For each archetype the level spawns: sprite id, collider radius, `fragile`, health, score, weapon
(pattern, rate, projectile speed, first shot delay), spawner, and default trajectory. Plus, derived:

- **shots to kill**, `ceil(health / weaponProjectileDamage)`, and for an archetype with no `health`
  component, the sentence *"no health component — dies to one player projectile"*;
- **screen time on its default trajectory**, `(270 + radius) / |vy|` for a `constant` shape;
- **maximum shots per pass**, from screen time, `firstShotDelay` and `rate`;
- **children per pass** for a `spawner` archetype, from screen time and `interval`.

**Derived from:** `enemies.json` → `components.sprite.id`, `collider.radius`, `collider.fragile`,
`health.points`, `scoreValue.points`, `weapon.pattern`, `.rate`, `.speed`, `.firstShotDelay`,
`spawner.enemyId`, `.interval`, `.offsetX`, `.offsetY`, `motion.trajectory`; `balance.json` →
`weaponProjectileDamage`; `trajectories.json` → `vy`.

**Why the derived numbers and not just the fields.** This is the roadmap's own wording for what the
document is for — *"for each enemy, projectile and appearance: its stats and what it actually does"* —
and the four numbers above are the difference between the two. `enemy-rush` has `rate 4.0` and about
3.4 s of screen time on `dive`, which means **one shot per pass, always**: that is a property of the
number and it is not visible by reading `4.0`. `enemy-tank` has `health 40` against
`weaponProjectileDamage 10`, so four hits; a designer who writes `health: 8` for a "slightly tougher"
enemy has written a no-op, because anything at or under 10 dies to the first projectile exactly like
an archetype with no health at all (`core/domain/system/DamageSystem.java`).

**`Lifetime` is named even though nothing uses it.** `core/domain/system/LifetimeSystem.java` reads an
optional per-archetype `Lifetime`, and none of the six archetypes in `enemies.json` carries one. The
roster prints `lifetime: none` rather than omitting the row, so an agent knows the lever exists.

### 7. Movement shapes this level uses

For each trajectory referenced by an archetype the level spawns, or by a spawn override: id, kind,
parameters, and for an `arc`, its turn time `-vy / ay` and its apex depth `vy² / (2·ay)` below the
spawn point.

**Derived from:** `trajectories.json` → `id`, `type`, `vx`, `vy`, `ay`.

**Why:** the apex is where the shape either threatens the player or falls past them. The player flies
at `playerStartY 30` in a 270-tall playfield (`balance.json`), and the useful band is roughly y 20–70;
a shape bottoming out at y 150 is scenery. The formula is in
[`../11c-movement-shapes/shape-catalogue.md`](../11c-movement-shapes/shape-catalogue.md), and the
document computes it so nobody has to.

**Only the shapes this level uses.** The full catalogue lives in one place and is linked, not copied —
see the refusals.

### 8. Formations this level uses

Slot offsets, slot count, total width, and per archetype used with it, the width including collider
radius.

**Derived from:** `formations.json` → `slots[].offsetX`, `.offsetY`; `enemies.json` →
`collider.radius`.

**Why:** the playfield is 208 units wide (`MotionSystem.PLAYFIELD_WIDTH:57`). `line-5` is 80 units of
offsets and, on `enemy-basic`'s radius 5.5, 91 units of occupied space — 44% of the width, which is
what decides whether an `atX` is legal. This is the arithmetic every new spawn needs and nobody wants
to redo.

### 9. Projectiles

The player's projectile speed and damage, and per archetype, its weapon's projectile speed, radius and
what its `pattern` string actually produces.

**Derived from:** `balance.json` → `weaponProjectileSpeed`, `weaponProjectileDamage`,
`weaponFireCooldown`, `weaponLevels`; `enemies.json` → `weapon.speed`, `weapon.pattern`.

**GAP — the projectile's radius and the meaning of `pattern` are not in `assets/data/`.**
`PROJECTILE_RADIUS` is `2.0f` in `core/domain/system/EnemyWeaponSystem.java:35`, and
`"straight-single"` is the only pattern that system builds (`EnemyWeaponSystem.java:37,86`) — any other
string is content that names a shape nothing draws. The generator carries a small glossary keyed to
those constants and **names the file each entry comes from**, so a reader can tell a fact from
`assets/data/` from a fact from `core/`. Handled the same way as the drop glossary in section 10; the
cost of getting it wrong is the same.

### 10. Drops and rewards

Every drop in the level, in placement order, with its absolute time, kind, the archetype and formation
slot that carries it, and one line saying what the kind does.

**Derived from:** `waves.json` → `spawns[].drop`, `.dropSlot`, `.spawn`, `.formation`, `.at`;
`level-01.json` → `waves[].offset` for the absolute time; `attachments.json` → `durability` for
`"attachment"`.

**GAP — what a drop kind does is code, not content.** The six recognised kinds are `public static
final String` constants in `core/domain/system/PickupSystem.java:39-71` —
`weapon-upgrade`, `shield`, `extra-life`, `bomb-recharge`, `invulnerability`, `attachment` — and
`SpawnSystem.requireRecognisedDrop` rejects anything else at spawn time. There is no `drops.json`. The
generator therefore carries the glossary and names `PickupSystem.java` as its source, and the document
prints **the recognised list**, so an agent designing level 2 knows the closed set it may draw from
without opening `core/`. Only `"attachment"` is content-driven: its durability comes from
`attachments.json`.

**Why absolute time matters here specifically.** Level 1 delivers the attachment inside
`l1-carrier-pair` at 208.0 s, which is beat 11 of fourteen — *"a difficult encounter. Defeating it
delivers the level's attachment"* (`docs/planning/04-campaign-and-levels.md`). Reward placement is
pacing, and it is unreadable from `waves.json` alone.

**One rule the document states once, from code:** a drop is delivered only if the player destroys the
carrier. `LifetimeSystem` strips `ScoreValue`, `Drop` and `Collider` from an enemy that escapes off
screen, by the project owner's decision of 28/08/2026 recorded in that file's javadoc. A drop placed
on a fast, fragile archetype can therefore be lost entirely, and the document says so beside the
table.

### 11. The boss

Every field of the level's `"boss"` block, plus derived: entrance duration, the effective health of
the kill target, the total health the bar shows, and how far below the core a spread and a sweep shot
leave the playfield.

**Derived from:** `level-01.json` → `boss.id`, `entersAt`, `coreHealth`, `podHealth`, `armHealth`,
`corePoints`, `podPoints`, `armPoints`, `entranceSpeed`, `combatY`, `patternCooldown`,
`spreadProjectileSpeed`, `sweepProjectileSpeed`.

**Why the derived half.** `combatY` alone decides whether the boss can hit anything: the spread and
sweep angles are fixed ratios in `core/domain/system/BossSystem.java:140-143`, all shallower than 45°,
so every projectile leaves through a side edge and how low it leaves is a pure function of `combatY`.
Set it wrong and the fight is unlosable with no error anywhere. Likewise the kill target's practical
health is `2 × coreHealth`, because `core-keel` carries the core's health independently, while the bar
shows the sum across six parts — so killing pods shortens the bar without shortening the fight.

**GAP — the constants those formulas need are in `BossSystem.java`, not in content.** `CORE_SPAWN_Y`
is computed at `BossSystem.java:150`, the projectile ratios at `:140-143`, and the six part offsets in
the same file. Same treatment as sections 9 and 10: the generator computes the derived figures and
names `BossSystem.java` as their source in the document, so the reader knows which half of the section
would go stale if that file changed.

**`entersAt` is absolute level time, and the document says so loudly.** `BossSystem` compares it
against its own `levelTime` (`BossSystem.java:235`), which is independent of the wave chain. With
every wave on `fixedDuration` the document can print "the waves end at 298.0 s, the boss enters at
302.0 s, a 4.0 s gap". The instant one wave is `cleared`, that gap becomes unknowable and the boss can
enter over a wave still running. This is the most dangerous interaction in the whole format and it is
invisible in the JSON.

### 12. Designing against the player

The constants a level is designed against: playfield 208 × 270, player start position, speed, slow
factor, fire cooldown, weapon levels, lives, bombs, pickup radius, invulnerability durations.

**Derived from:** `balance.json` → all of it; `MotionSystem.PLAYFIELD_WIDTH:57` and
`SpawnSystem.PLAYFIELD_HEIGHT:92` for the two dimensions, which are fixed properties of the logical
resolution rather than balance values.

**Why:** an `atX` is meaningless without 208, and an apex is meaningless without `playerStartY 30`.
These are the same for every level, and repeating them per level costs nothing and removes the one
lookup an agent would otherwise have to make outside the document.

### 13. Checks — what the generator found

A list of derived warnings, or the sentence "no issues found". At minimum:

| Check | Why it is here |
|---|---|
| A spawn whose `at` exceeds its wave's `end.seconds` | It never fires. `SpawnSystem.spawnDue` only advances the cursor while the wave is active, and `hasEnded` removes it at `>= seconds`. A spawn exactly at the duration does fire; one past it is silently dropped |
| A formation whose footprint leaves `0..208` at its `atX` | Nothing clamps it (`SpawnSystem.spawnWave`); it just spawns off screen |
| A `dropSlot` beyond its formation's slot count | Already fatal at spawn time (`SpawnSystem.requireSlotInRange`), so the document catches it before a run does |
| A drop kind outside the six | Already fatal (`SpawnSystem.requireRecognisedDrop`); same reason |
| A `cleared` wave containing an archetype that could survive indefinitely | A `cleared` wave ends only when every entity it spawned is gone (`SpawnSystem.noEntityCarries`), and `LifetimeSystem` removes an enemy only once it is off screen. Today no wave is `cleared` and every shape leaves in finite time, so this check is a guard for 11e, not a report on level 1 |
| A negative `offset`, with the two waves it overlaps and the overlap in seconds | Overlap is deliberate and it is the one thing in the format that produces pressure nothing else can. It is also the thing a reader misreads first |
| `entersAt` earlier than the end of the last placement | The boss entering over a running wave. Legal, occasionally intended, never accidental |

**Derived from:** every field already named above; this section introduces no new input.

**Why this section is the one that earns the document its cost.** Every check listed is a failure that
is either silent at runtime or fatal only on the tick it happens, and each one costs an iteration of
`./gradlew :desktop:run` to find by hand. This is the part of the document a generator can do and a
human reliably will not.

### 14. The beat map — **GAP, and the largest one**

Which of the fourteen beats of `docs/planning/04-campaign-and-levels.md` each placement serves, and
why the placement exists.

**Cannot be derived from `assets/data/` at all.** There is no field for it. The mapping exists, written
by hand, in [`../11c-movement-shapes/shape-catalogue.md`](../11c-movement-shapes/shape-catalogue.md)
under "What points at what", and the only thing tying `l1-rest-basic` to *"brief rest"* is its id.

This is the structural consequence of the decision the phase rests on, and area G of
[`../10c-architecture-review/assessment.md`](../10c-architecture-review/assessment.md) predicted it in
as many words: JSON admits no comments, so *"the 'document' half would have nowhere to put the
sentence explaining why a beat exists"*. Generating the document from the JSON buys zero drift and
pays for it in intent.

**Decided: the generated document does not carry design intent, and it says so in one line, pointing
at where intent lives.** The alternatives and why not:

- **Infer the beat from the wave id.** Guessing prose from a string. It would be right for
  `l1-rest-basic` and wrong for `l1-tank-solo`, which serves three different beats at three different
  times, and a document that is confidently wrong about intent is worse than one that is silent.
- **Add an optional `"note"` string to a wave.** The cheapest real fix, and it is genuinely out of
  scope twice over: [#180](https://github.com/LuchoC-Dev/little-spaceship/issues/180) forbids changing
  any format under `assets/data/`, and it is not a content-only change —
  `JsonContentSource.requireOnlyKeys:431` now rejects every key its schema does not name, so a `"note"`
  key would fail the load until `core-domain` and `game-presentation` allow it. **Recommended to
  phase 12**, with the wave-parameters question that phase already reopens; naming it here so the next
  person does not re-derive it.
- **A hand-written companion file per level.** Exactly the two-hand-maintained-artefacts arrangement
  the plan's acceptance criteria refuse — *"if the phase ends with two artefacts a human edits, the
  phase failed"*.

---

## What `assets/data/` cannot give the document

Four things, in descending order of how much they cost.

1. **Design intent — why a beat exists.** Section 14. No field, and adding one is a parser change.
   The document points at `04-campaign-and-levels.md` and stops.
2. **What a drop kind does.** Section 10. Six constants in `PickupSystem.java:39-71`. The generator
   carries a glossary naming that file.
3. **The boss's geometry constants.** Section 11. `BossSystem.java:140-150`. Same treatment.
4. **The enemy projectile's radius and the set of real `pattern` strings.** Section 9.
   `EnemyWeaponSystem.java:35,37`. Same treatment.

**2, 3 and 4 are the same defect wearing three hats:** a value the level designer must design against
lives in `core/` as a constant, so a generated document can only quote it, and the quote can go stale
without the CI check noticing — regenerating produces the same text either way. The mitigation is the
convention this repository already has: **each glossary entry names its file in backticks**, so #56's
`docs-refs` check would fail on it if the file moved. That is a mitigation, not a fix; the fix is
moving those values into content, and it belongs to whoever next opens `core/`.

**One interaction worth handing to task 5 (#56) explicitly.** The generated document is dense with
backticked strings that look like repository references and are not: `enemy-basic`, `l1-veteran-mix`,
`slow-descent`, `weapon-upgrade`, `straight-single`. A `docs-refs` check that resolves backticked
spans against the repository will fire on every one of them. It needs either an exemption for
`docs/levels/`, or — better, and it is nearly free once the generator exists — a resolver that treats a
content id as valid when `assets/data/` defines it. That second form turns `docs-refs` into a real
check on the generated document rather than a check it has to be excused from.

---

## What the document alone is enough for

Answering the test stated at the top, honestly. **Not checked by experiment** — no generator exists, so
this is reasoning over the section list, not an observation of an agent using one.

**Enough to do, from the document alone:**

- write a syntactically valid `level-02.json` and its waves, because every field of every file is
  named with its type and its units, and the closed sets — six archetypes, eight formations, seven
  trajectories, six drop kinds, two end conditions — are all printed;
- choose an `atX` that puts a formation on screen, because the footprint arithmetic is done;
- choose a `dropSlot` that exists;
- pick an archetype for a beat and know what it does in play: how many hits it takes, how many shots
  it gets off, how long it occupies the screen, what it spawns;
- build a curve deliberately — set a rest, an escalation and a climax at chosen densities, against
  level 1's own numbers as the reference;
- reuse a wave and know how many other beats that edit lands on;
- place a boss and know whether it can reach the player.

**Still needs the code, or a person:**

- **whether the level is any good.** Density is not difficulty, and the project's own rule is that
  balance is tuned by playing. The document cannot close this and should not pretend to;
- **why level 1 is shaped the way it is.** Section 14's gap;
- **any behaviour outside the format** — a new movement kind, a homing enemy, a destructible obstacle,
  a second weapon pattern. The document lists what exists, and an agent that needs something absent
  must say so and stop, exactly as `.claude/agents/level-designer.md` already requires. That is the
  right answer, not a shortfall;
- **the boss's attack patterns as felt.** The document gives numbers and geometry;
  `docs/design/06-boss-presentation.md` gives the reading. Both are needed and neither is generated;
- **the exact frame-level consequences of a `cleared` wave**, which resolves one tick after the clear
  because `CLEANUP` runs after `SPAWN` (`SpawnSystem`'s class javadoc, `core/domain/system/SystemOrder.java`).
  The document states the rule; anyone designing against single ticks is designing wrong.

The honest summary: **the document removes the whole class of mistakes that make content fail to load
or silently do nothing, and it does not remove the need to play.** That is what phase 12 will measure —
*"the first honest measurement is someone building level 2 with the new one"*.

---

## What is refused

A contract that lists only what is in it says nothing about where its edge is. Each of these was
considered for a section and is not one.

| Refused | Why |
|---|---|
| **The JSON itself, embedded or field-dumped** | It is the plan's own named failure mode — *"a dump of every field in the JSON satisfies the CI check and fails the actual purpose"*. Anyone who wants the JSON opens the JSON; the document exists for what the JSON does not say. |
| **A flat timeline of every spawn event at absolute times** | 92 rows, one per spawn event, which expand to 261 entities. That is `level-01.json` as it stood before phase 11b — a transcript, not a design — and reintroducing it as "the document" would undo the phase that removed it. Section 3 aggregates per placement; section 5 gives local times per wave. Both are readable; 92 flat rows are not. |
| **A single difficulty number or rating per wave** | An invented metric nobody validated, and `docs/planning/01-vision-and-scope.md` is explicit that difficulty rests on eight axes at once — quantity, projectile density, speed, resistance, formation, obstacles, space, simultaneity. A number that collapses them would be quoted in design conversations as if it meant something. Density is printed instead, labelled as one axis. |
| **A rendered chart, PNG or SVG** | A binary or near-binary artefact regenerated on every content change, which makes the CI diff unreadable and puts churn in git for a shape ASCII already conveys. Also a rendering dependency in `tools/` for no gain. |
| **Tuning advice — "this wave is too easy", "raise this to 60"** | The generator would be making judgements, and a generated judgement is unaccountable: nobody signed it and nobody can argue with it. Facts are generated, judgement is the designer's, and the project's rule is that judgement comes from playing. |
| **The full content catalogue in every level document** | Every trajectory, formation and archetype in `assets/data/`, whether the level uses it or not. It duplicates `shape-catalogue.md` into N files that all change when one entry changes, and it buries the eight formations this level actually uses among the ones it does not. The document lists what the level uses and links the catalogue for the rest. |
| **Maximum achievable score, or a score budget** | Not derivable: a carrier's children depend on how long it survives, escaped enemies score nothing (`LifetimeSystem`), and `maxedPickupBonus` depends on the player's state. A number that looks exact and is not is worse than no number. Per-archetype `scoreValue` is printed; the total is refused. |
| **Time-to-kill in seconds** | Shots-to-kill is arithmetic — `ceil(health / weaponProjectileDamage)` — and is printed. Turning it into seconds needs the player's shot level, fire cooldown *and* whether they are hitting, which is play, not content. Half of that is derivable and the half that is not is the half that matters. |
| **A player-facing walkthrough or strategy notes** | "Stay left through the finale" is a claim about play that only playing can make, and it would be generated from geometry as if it had been tested. |
| **Cross-level comparison — a table of every level side by side** | It belongs to a campaign-level document, and putting it in each level's file means every level's document changes when any level changes, so the CI check fires on files nobody touched. One artefact per level, and the comparison, if it is ever wanted, is its own generated file. |
| **Sprite appearance, colour and animation** | `docs/design/02-sprite-sizes.md` and `docs/design/00-visual-direction.md` own it, they were audited in 10a, and duplicating them here creates exactly the second copy this phase exists to prevent. The document prints the sprite **id**, which is the join. |
| **A generation timestamp, git hash or tool version** | It would make regenerate-and-diff fail on every run and destroy the mechanism the phase is built on. Named as a refusal rather than a footnote because it is the single easiest way to sink task 3. |

---

## Mechanical requirements on the generator

Constraints task 2 inherits, all of them consequences of task 3 — *"regenerate in CI and fail if the
working tree changes"*. **Not built.**

- **No timestamp, no hash, no version line, no absolute path** in the output.
- **Deterministic ordering everywhere.** Placements in level order; waves in first-placement order;
  archetypes, trajectories and formations in the order they first appear in the level, not in hash
  order. Iterating a map without sorting is how this check starts failing at random.
- **Fixed float formatting**, one decimal for seconds and two for densities, so `27.5` never becomes
  `27.50` between runs of different Node versions.
- **Fail loudly on unresolved content** — a wave id, archetype id, formation id or trajectory id that
  does not resolve. The generator is the second reader of this content after
  `JsonContentSource`, and a document that silently prints a blank for a broken id is a worse liar
  than a crash.
- **Every derived number states its formula in the document**, once, where it is used. A reader who
  disagrees with a number must be able to see how it was reached without reading `tools/`.
- **Every quoted constant from `core/` names its file in backticks.** The three gaps above depend on
  this and so does #56.

---

## For task 4, which reads the generated document back

Task 4 is *"generate the document for level 1 and read it as if you were designing level 2 from it"*,
and it is `level-designer`'s, deliberately not the generator's author's. The test at the top of this
document is what it is measured against, and the specific things to check are:

1. Could you write `level-02.json` without opening `assets/data/`? Try it; do not reason about it.
2. Does section 13 catch a deliberate mistake — a `dropSlot` of 5 on a `single`, an `atX` of 1.0 on a
   `line-5`, a spawn at 30 s in a 20 s wave? Break level 1 in a scratch copy and regenerate.
3. Is section 14's absence felt? If reading the document leaves you unable to say what a placement is
   *for*, that is the finding, and it goes back into this contract rather than into a gaps list.
