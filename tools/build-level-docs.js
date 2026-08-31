#!/usr/bin/env node
/**
 * Generate one markdown document per level from the content in `assets/data/`.
 *
 *     node tools/build-level-docs.js            # write docs/levels/<levelId>.md
 *     node tools/build-level-docs.js --check    # write nothing; exit 1 if anything would change
 *
 * The JSON is the source and the document is generated from it — phase 11d's decision, taken
 * because a document describing a level and a JSON defining it are two copies of one truth and one
 * of them always rots. `docs/plan/11d-per-level-document/document-contract.md` decides what the
 * document contains, section by section, and refuses the rest; this file implements exactly that
 * list. A section here the contract does not name is a bug in one of the two.
 *
 * No third-party dependency, per the precedent `docs/design/atlas/build-atlas.js` set: Node
 * built-ins only, so CI needs nothing installed.
 *
 * THE OUTPUT MUST BE A PURE FUNCTION OF THE CONTENT. `.github/workflows/ci.yml` regenerates and
 * fails if the working tree changes, which is the whole mechanism of the phase, and it dies the
 * moment the output carries anything the content does not determine. So: no timestamp, no git
 * hash, no version line, no absolute path, and every iteration over a collection is over an
 * explicitly ordered one. That is the constraint the contract's "Mechanical requirements" section
 * lists first, and the reason it lists it first.
 *
 * This is the second reader of the content after `game/adapter/content/JsonContentSource.java`, and
 * it deliberately does not reimplement that parser's validation — it resolves ids and dies on one it
 * cannot resolve. A document that printed a blank for a broken id would be a worse liar than a crash.
 */

'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const DATA = path.join(ROOT, 'assets', 'data');
const OUT_DIR = path.join(ROOT, 'docs', 'levels');

/**
 * Constants that live in `core/` rather than in `assets/data/`, each carrying the file it came from.
 * The document prints that file beside the value, because these are the one thing regenerating
 * cannot keep honest: the text is identical whether or not the Java still says this. Sections 9, 10,
 * 11 and 12 of the contract are where a reader has to be able to tell a fact from content from a
 * fact from code, and this is how.
 */
const CODE = {
  playfieldWidth: { value: 208, from: '`core/domain/system/MotionSystem.java:57`' },
  playfieldHeight: { value: 270, from: '`core/domain/system/SpawnSystem.java:92`' },
  enemyProjectileRadius: { value: 2.0, from: '`core/domain/system/EnemyWeaponSystem.java:35`' },
  patterns: { value: ['straight-single'], from: '`core/domain/system/EnemyWeaponSystem.java:37,86`' },
  boss: {
    from: '`core/domain/system/BossSystem.java:74-89,140-151`',
    spreadVxRatios: [0.25, 0.45, 0.70],
    spreadVyRatio: -0.90,
    sweepVxRatios: [0.55, 0.75, 0.95],
    sweepVyRatio: -0.65,
    coreKeelRadius: 13.0,
    coreKeelOffsetY: -27.0,
    parts: 6,
  },
};

/**
 * The six drop kinds `PickupSystem` recognises, and what each does. There is no `drops.json`: these
 * are `public static final String` constants, and `SpawnSystem.requireRecognisedDrop` rejects
 * anything else the moment a wave carrying it spawns.
 */
const DROP_KINDS = [
  ['weapon-upgrade', "raises the player's shot level by one, up to `weaponLevels`"],
  ['shield', 'grants a shield, if the player does not already have one'],
  ['extra-life', 'one more life, up to `maxLives`'],
  ['bomb-recharge', 'one more bomb, up to `maxBombs`'],
  ['invulnerability', 'grace time set — not added — to `invulnerabilityPickupDuration`'],
  ['attachment', 'equips an attachment; the only kind that is content-driven, through `assets/data/attachments.json`'],
];
const DROP_KINDS_FROM = '`core/domain/system/PickupSystem.java:39-71`';

// ------------------------------------------------------------------------------------------------
// Reading

function die(message) {
  process.stderr.write(`build-level-docs: ${message}\n`);
  process.exit(2);
}

function readJson(name) {
  const file = path.join(DATA, name);
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch (e) {
    return die(`cannot read ${path.relative(ROOT, file)}: ${e.message}`);
  }
}

function index(rows, kind) {
  const byId = new Map();
  for (const row of rows) {
    if (byId.has(row.id)) die(`two ${kind} share the id '${row.id}'`);
    byId.set(row.id, row);
  }
  return byId;
}

/** Resolving, not looking up: an id that does not resolve stops the generator. See the file header. */
function resolve(byId, id, kind, context) {
  const row = byId.get(id);
  if (row === undefined) {
    die(`${context || 'somewhere'}: no ${kind} with the id '${id}' exists in assets/data/`);
  }
  return row;
}

// ------------------------------------------------------------------------------------------------
// Formatting. Fixed everywhere, so two runs on two Node versions produce the same bytes.

const s1 = (n) => n.toFixed(1);
const s2 = (n) => n.toFixed(2);
const padL = (text, width) => String(text).padStart(width);
const padR = (text, width) => String(text).padEnd(width);

function table(headers, rows) {
  const out = [`| ${headers.join(' | ')} |`, `|${headers.map(() => '---').join('|')}|`];
  for (const row of rows) out.push(`| ${row.join(' | ')} |`);
  return out.join('\n');
}

// ------------------------------------------------------------------------------------------------
// Derivation

/** One entity per formation slot, per `SpawnSystem.spawnWave`. */
function entitiesOf(spawn, formations) {
  return resolve(formations, spawn.formation, 'formation', `spawn '${spawn.spawn}'`).slots.length;
}

/**
 * The shape an entity actually flies: the spawn's own `trajectory` when it has one, otherwise the
 * archetype's `motion.trajectory`. The key is optional and overrides the default
 * (`core/port/SpawnEvent.java`, `core/domain/system/SpawnSystem.java`).
 */
function trajectoryOf(spawn, enemy) {
  if (spawn.trajectory) return { id: spawn.trajectory, override: true };
  const motion = enemy.components && enemy.components.motion;
  if (!motion || !motion.trajectory) {
    die(`archetype '${enemy.id}' has no motion.trajectory and no spawn overrides it`);
  }
  return { id: motion.trajectory, override: false };
}

/**
 * Absolute start and end of every placement, in level order. A `fixedDuration` wave's end is
 * arithmetic; a `cleared` wave's is not, and from the first one onwards every later time is a lower
 * bound rather than a value. The document must say which — this is the one place a generated
 * document could lie by rounding a decision away.
 */
function timeline(level, waves) {
  const rows = [];
  let cursor = 0;
  let exact = true;
  level.waves.forEach((placement, i) => {
    const wave = resolve(waves, placement.wave, 'wave', `placement ${i + 1} of the level`);
    const start = cursor + placement.offset;
    const fixed = wave.end.type === 'fixedDuration';
    const duration = fixed ? wave.end.seconds : null;
    const end = fixed ? start + duration : null;
    rows.push({ i, placement, wave, start, end, duration, exact });
    if (!fixed) exact = false;
    cursor = fixed ? end : start;
  });
  return { rows, exact };
}

/** Horizontal extent a spawn occupies, collider radius included. Nothing clamps this at runtime. */
function footprint(spawn, enemy, formations) {
  const formation = resolve(formations, spawn.formation, 'formation', `spawn '${spawn.spawn}'`);
  const radius = enemy.components.collider ? enemy.components.collider.radius : 0;
  const anchor = spawn.atX * CODE.playfieldWidth.value;
  let min = Infinity;
  let max = -Infinity;
  for (const slot of formation.slots) {
    min = Math.min(min, anchor + slot.offsetX - radius);
    max = Math.max(max, anchor + slot.offsetX + radius);
  }
  return { min, max, offScreen: min < 0 || max > CODE.playfieldWidth.value };
}

/** Screen time on a constant shape: the whole playfield plus the radius it spawns above the edge. */
function screenTime(trajectory, radius) {
  if (trajectory.type === 'arc') return null;
  const vy = Math.abs(trajectory.vy);
  if (vy === 0) return null;
  return (CODE.playfieldHeight.value + radius) / vy;
}

// ------------------------------------------------------------------------------------------------
// The document. Fourteen sections, in the order the contract names them.

function buildLevel(levelFile, content) {
  const { level, waves, enemies, formations, trajectories, attachments, balance } = content;
  const levelId = path.basename(levelFile, '.json');
  const { rows, exact } = timeline(level, waves);
  const out = [];
  const w = (line = '') => out.push(line);

  // 1. Header and provenance ---------------------------------------------------------------------
  w(`# ${levelId}`);
  w();
  w('**This file is generated. Do not edit it by hand — CI will fail before your edit reaches anyone.**');
  w('`tools/build-level-docs.js` writes it from the content listed below, and `.github/workflows/ci.yml`');
  w('regenerates it on every push and fails if the result differs.');
  w();
  w('Generated from:');
  w();
  for (const name of [levelFile, 'waves.json', 'enemies.json', 'formations.json', 'trajectories.json',
    'attachments.json', 'balance.json']) {
    w(`- \`assets/data/${name}\``);
  }
  w();
  w('It carries no generation date, git hash or tool version on purpose: any of those would make the');
  w('check above fail on every run, which is how a mechanism becomes noise somebody switches off.');
  w();
  w('**What it does not carry is why any of this is the way it is.** JSON admits no comments, so');
  w('design intent has nowhere to live in the source and cannot be generated from it. The intent for');
  w('level 1 is in `docs/planning/04-campaign-and-levels.md`, and which wave serves which beat is in');
  w('`docs/plan/11c-movement-shapes/shape-catalogue.md` under "What points at what". The reasoning');
  w('behind the gap is section 14 of `docs/plan/11d-per-level-document/document-contract.md`.');
  w();

  // 2. At a glance -------------------------------------------------------------------------------
  const last = rows[rows.length - 1];
  const wavesEnd = last.end;
  const spawnEvents = rows.reduce((n, r) => n + r.wave.spawns.length, 0);
  let entities = 0;
  for (const row of rows) for (const sp of row.wave.spawns) entities += entitiesOf(sp, formations);
  const distinct = new Set(level.waves.map((p) => p.wave));

  w('## At a glance');
  w();
  const glance = [
    ['placements', String(level.waves.length)],
    ['distinct waves', String(distinct.size)],
    ['spawn events', String(spawnEvents)],
    ['entities spawned directly', String(entities)],
    ['the waves end at', exact ? `${s1(wavesEnd)} s` : 'unknowable — see below'],
  ];
  if (level.boss) {
    glance.push(['the boss enters at', `${s1(level.boss.entersAt)} s (${s1(level.boss.entersAt / 60)} min)`]);
    if (exact) glance.push(['gap between them', `${s1(level.boss.entersAt - wavesEnd)} s`]);
  }
  w(table(['', ''], glance));
  w();
  if (exact) {
    w('**Every wave ends on `fixedDuration`, so every time below is exact arithmetic.** The moment one');
    w('wave uses `{"type": "cleared"}` (`core/port/WaveEndCondition.java`), every absolute time after');
    w('it becomes a lower bound, because a cleared wave ends when the player finishes it. This');
    w('document would then say so here rather than print numbers that look exact.');
  } else {
    w('**At least one wave ends on `cleared`, so the times below are lower bounds, not values.** A');
    w('cleared wave ends when the last entity it spawned is gone (`SpawnSystem.noEntityCarries`),');
    w('which is when the player finishes it. Every placement after the first `cleared` one starts no');
    w('earlier than the time printed and may start much later. Affected rows are marked `>=`.');
  }
  w();

  // 3. The pacing table --------------------------------------------------------------------------
  w('## The pacing table');
  w();
  w("One row per placement, in level order. **Density is entities per second of that placement's own");
  w('duration** — `entities / duration` — and it is one axis of difficulty, not a difficulty score.');
  w('`docs/planning/01-vision-and-scope.md` names eight axes at once; this is one. A slow carrier that');
  w('keeps producing children reads low here and plays hard.');
  w();
  const density = [];
  const pacing = [];
  for (const row of rows) {
    const ents = row.wave.spawns.reduce((n, sp) => n + entitiesOf(sp, formations), 0);
    const archetypes = [];
    const drops = [];
    for (const sp of row.wave.spawns) {
      if (!archetypes.includes(sp.spawn)) archetypes.push(sp.spawn);
      if (sp.drop) drops.push(sp.drop);
    }
    const d = row.duration === null ? null : ents / row.duration;
    density.push(d);
    const mark = row.exact ? '' : '>= ';
    pacing.push([
      String(row.i + 1),
      `\`${row.placement.wave}\``,
      s1(row.placement.offset),
      mark + s1(row.start),
      row.duration === null ? 'when cleared' : mark + s1(row.end),
      row.duration === null ? '`cleared`' : `${s1(row.duration)} s`,
      String(ents),
      d === null ? '—' : `${s2(d)}/s`,
      archetypes.map((a) => `\`${a}\``).join(' '),
      drops.length ? drops.map((x) => `\`${x}\``).join(' ') : '—',
    ]);
  }
  w(table(['#', 'wave', 'offset', 'start', 'end', 'lasts', 'entities', 'density', 'archetypes', 'drops'],
    pacing));
  w();

  // 4. The curve, as a bar -----------------------------------------------------------------------
  w('## The curve');
  w();
  w('The same numbers as a shape, because a column of numbers is not one. The bar is scaled to the');
  w("densest placement in this level, so it compares beats within a level and not between levels.");
  w();
  const peak = Math.max(...density.map((d) => d || 0), 0.0001);
  w('```');
  rows.forEach((row, i) => {
    const d = density[i];
    const bar = d === null ? '?' : '#'.repeat(Math.round((d / peak) * 40));
    w(`${padL(s1(row.start), 7)}  ${padR(row.placement.wave, 22)} ${padL(d === null ? '—' : s2(d), 5)}/s  ${bar}`);
  });
  w('```');
  w();

  // 5. Wave by wave ------------------------------------------------------------------------------
  w('## Wave by wave');
  w();
  w('Each wave this level places, once, in the order it first appears. **Placed at** lists every');
  w('absolute time the level starts it: a wave is reusable, so editing one for one beat edits every');
  w('placement of it, and nothing in `assets/data/waves.json` says so.');
  w();
  const seen = [];
  for (const row of rows) if (!seen.includes(row.placement.wave)) seen.push(row.placement.wave);
  for (const waveId of seen) {
    const wave = resolve(waves, waveId, 'wave', 'the wave-by-wave section');
    const placedAt = rows.filter((r) => r.placement.wave === waveId);
    w(`### \`${waveId}\``);
    w();
    w('**Ends:** ' + (wave.end.type === 'fixedDuration'
      ? `\`fixedDuration\`, ${s1(wave.end.seconds)} s`
      : `\`${wave.end.type}\` — when every entity it spawned is gone (\`SpawnSystem.noEntityCarries\`)`));
    w();
    w(`**Placed ${placedAt.length} time${placedAt.length === 1 ? '' : 's'}:** `
      + placedAt.map((r) => `#${r.i + 1} at ${r.exact ? '' : '>= '}${s1(r.start)} s`).join(', ')
      + (placedAt.length > 1 ? ' — **reused: an edit here lands on all of them.**' : ''));
    w();
    const spawnRows = [];
    for (const sp of wave.spawns) {
      const enemy = resolve(enemies, sp.spawn, 'archetype', `wave '${waveId}'`);
      const traj = trajectoryOf(sp, enemy);
      resolve(trajectories, traj.id, 'trajectory', `archetype '${enemy.id}' or a spawn in '${waveId}'`);
      const fp = footprint(sp, enemy, formations);
      const formation = resolve(formations, sp.formation, 'formation', `wave '${waveId}'`);
      spawnRows.push([
        s1(sp.at),
        `\`${sp.spawn}\``,
        `\`${sp.formation}\` (${formation.slots.length})`,
        s2(sp.atX),
        `\`${traj.id}\`${traj.override ? ' *(override)*' : ''}`,
        `${s1(fp.min)} .. ${s1(fp.max)}${fp.offScreen ? ' **off screen**' : ''}`,
        sp.drop ? `\`${sp.drop}\` slot ${sp.dropSlot === undefined ? 0 : sp.dropSlot}` : '—',
      ]);
    }
    w(table(['at', 'archetype', 'formation', 'atX', 'shape', 'x extent', 'drop'], spawnRows));
    w();
  }
  w("**`x extent`** is `atX * 208 + slot.offsetX`, plus and minus the archetype's collider radius");
  w('(`SpawnSystem.spawnWave`, `SpawnSystem.positionSpawned`). **Nothing clamps it** — a formation');
  w('whose extent leaves `0 .. 208` spawns partly off screen and nobody is told at runtime.');
  w();
  w("**`shape` is resolved, not copied.** A spawn's own `trajectory` key overrides the archetype's");
  w('`motion.trajectory` and is marked *(override)*; every other row is the archetype default.');
  w();

  // 6. Roster ------------------------------------------------------------------------------------
  const used = [];
  for (const row of rows) for (const sp of row.wave.spawns) if (!used.includes(sp.spawn)) used.push(sp.spawn);
  // A spawner archetype's children are in the level too, though no wave names them.
  for (const id of [...used]) {
    const spawner = resolve(enemies, id, 'archetype', 'the roster').components.spawner;
    if (spawner && !used.includes(spawner.enemyId)) used.push(spawner.enemyId);
  }

  w('## Roster');
  w();
  w('Every archetype this level spawns, plus any an archetype spawns itself. The derived columns are');
  w('the point: `rate 4.0` is a number, "one shot per pass" is what it means.');
  w();
  const roster = [];
  for (const id of used) {
    const e = resolve(enemies, id, 'archetype', 'the roster');
    const c = e.components;
    const radius = c.collider ? c.collider.radius : 0;
    const health = c.health ? c.health.points : null;
    const traj = resolve(trajectories, c.motion.trajectory, 'trajectory', `archetype '${id}'`);
    const time = screenTime(traj, radius);
    let shotsPerPass = '—';
    if (c.weapon) {
      if (time === null) shotsPerPass = 'varies (arc)';
      else {
        const firing = time - c.weapon.firstShotDelay;
        shotsPerPass = firing < 0 ? '0' : String(1 + Math.floor(firing / c.weapon.rate));
      }
    }
    let children = '—';
    if (c.spawner) children = time === null ? 'varies (arc)' : String(Math.floor(time / c.spawner.interval));
    roster.push([
      `\`${id}\``,
      `\`${c.sprite.id}\``,
      s1(radius),
      c.collider && c.collider.fragile ? 'yes' : 'no',
      health === null ? 'none' : String(health),
      String(health === null ? 1 : Math.ceil(health / balance.weaponProjectileDamage)),
      c.scoreValue ? String(c.scoreValue.points) : '—',
      `\`${traj.id}\``,
      time === null ? 'varies (arc)' : `${s1(time)} s`,
      c.weapon
        ? `\`${c.weapon.pattern}\`, every ${s1(c.weapon.rate)} s from ${s1(c.weapon.firstShotDelay)} s, speed ${s1(c.weapon.speed)}`
        : 'none',
      shotsPerPass,
      c.spawner ? `\`${c.spawner.enemyId}\` every ${s1(c.spawner.interval)} s` : 'none',
      children,
      c.lifetime ? `${s1(c.lifetime.seconds)} s` : 'none',
    ]);
  }
  w(table(['archetype', 'sprite', 'radius', 'fragile', 'health', 'shots to kill', 'score',
    'default shape', 'screen time', 'weapon', 'shots per pass', 'spawner', 'children per pass',
    'lifetime'], roster));
  w();
  w(`**shots to kill** is \`ceil(health / weaponProjectileDamage)\` against \`weaponProjectileDamage ${balance.weaponProjectileDamage}\``);
  w('from `assets/data/balance.json`. An archetype with no `health` dies to one projectile — and so');
  w(`does one with \`health\` at or below ${balance.weaponProjectileDamage}, which is how a "slightly tougher" enemy becomes a`);
  w('no-op (`core/domain/system/DamageSystem.java`).');
  w();
  w(`**screen time** is \`(${CODE.playfieldHeight.value} + radius) / |vy|\`: the playfield height (${CODE.playfieldHeight.from}) plus the`);
  w('radius the entity spawns above the edge (`SpawnSystem.positionSpawned`). It is `varies` on an');
  w('`arc`, whose speed changes as it flies.');
  w();
  w('**shots per pass** is `1 + floor((screen time - firstShotDelay) / rate)`, and it is the number');
  w('that matters: an archetype whose rate exceeds its screen time fires once whatever the rate says.');
  w('**children per pass** is `floor(screen time / interval)`.');
  w();
  w('**`lifetime` is printed even though nothing carries one.** `core/domain/system/LifetimeSystem.java`');
  w('reads an optional per-archetype `Lifetime`; the column says `none` rather than being omitted, so');
  w('the lever stays visible.');
  w();

  // 7. Movement shapes ---------------------------------------------------------------------------
  const shapeIds = [];
  for (const id of used) {
    const t = resolve(enemies, id, 'archetype', 'the shapes section').components.motion.trajectory;
    if (!shapeIds.includes(t)) shapeIds.push(t);
  }
  for (const row of rows) {
    for (const sp of row.wave.spawns) {
      if (sp.trajectory && !shapeIds.includes(sp.trajectory)) shapeIds.push(sp.trajectory);
    }
  }
  w('## Movement shapes this level uses');
  w();
  w('Only the shapes this level reaches. The full catalogue, including the eight shapes that were');
  w('refused and why, is `docs/plan/11c-movement-shapes/shape-catalogue.md`.');
  w();
  const shapeRows = [];
  for (const id of shapeIds) {
    const t = resolve(trajectories, id, 'trajectory', 'the shapes section');
    const arc = t.type === 'arc';
    shapeRows.push([
      `\`${id}\``,
      arc ? '`arc`' : '`constant`',
      s1(t.vx),
      s1(t.vy),
      arc ? s1(t.ay) : '—',
      arc ? `${s1(-t.vy / t.ay)} s` : '—',
      arc ? `${s1((t.vy * t.vy) / (2 * t.ay))} below spawn` : '—',
    ]);
  }
  w(table(['shape', 'kind', 'vx', 'vy', 'ay', 'turns after', 'apex depth'], shapeRows));
  w();
  w('An `arc` turns at `-vy / ay` and bottoms out `vy² / (2·ay)` below where it spawned, evaluated in');
  w("closed form from the entity's own elapsed time (`core/port/ArcTrajectoryDefinition.java`).");
  w(`The player flies at \`playerStartY ${s1(balance.playerStartY)}\` in a ${CODE.playfieldHeight.value}-tall playfield, so a shape whose apex sits`);
  w('far above that band is scenery.');
  w();
  w('**The veers spawn on the side they veer away from** — `veer-left` at `atX >= 0.75`, `veer-right`');
  w("at `atX <= 0.25` — or the shape happens off screen. That constraint is the catalogue's.");
  w();

  // 8. Formations --------------------------------------------------------------------------------
  const formIds = [];
  const formArchetypes = new Map();
  for (const row of rows) {
    for (const sp of row.wave.spawns) {
      if (!formIds.includes(sp.formation)) formIds.push(sp.formation);
      if (!formArchetypes.has(sp.formation)) formArchetypes.set(sp.formation, []);
      const list = formArchetypes.get(sp.formation);
      if (!list.includes(sp.spawn)) list.push(sp.spawn);
    }
  }
  w('## Formations this level uses');
  w();
  const formRows = [];
  for (const id of formIds) {
    const f = resolve(formations, id, 'formation', 'the formations section');
    const xs = f.slots.map((s) => s.offsetX);
    const span = Math.max(...xs) - Math.min(...xs);
    formRows.push([
      `\`${id}\``,
      String(f.slots.length),
      f.slots.map((s) => `(${s.offsetX}, ${s.offsetY})`).join(' '),
      s1(span),
      formArchetypes.get(id).map((a) => {
        const r = resolve(enemies, a, 'archetype', 'the formations section').components.collider.radius;
        return `\`${a}\` ${s1(span + 2 * r)}`;
      }).join(', '),
    ]);
  }
  w(table(['formation', 'slots', 'offsets (x, y)', 'span', 'occupied width, per archetype'], formRows));
  w();
  w(`The playfield is ${CODE.playfieldWidth.value} units wide (${CODE.playfieldWidth.from}). Occupied width is what decides whether`);
  w('an `atX` is legal, and it is the arithmetic every new spawn needs.');
  w();
  w("**A slot's `offsetY` is a head start in pixels, not a delay in seconds** — the whole formation");
  w('clears the bottom edge together and the slot furthest back arrives later only because it has');
  w('further to travel (`SpawnSystem.positionSpawned`). So one formation is a stream on a slow shape');
  w('and a burst on a fast one:');
  w();
  const stagger = [];
  for (const id of formIds) {
    const f = resolve(formations, id, 'formation', 'the stagger table');
    const ys = f.slots.map((s) => s.offsetY);
    const spread = Math.max(...ys) - Math.min(...ys);
    if (spread === 0) continue;
    for (const a of formArchetypes.get(id)) {
      const t = resolve(trajectories,
        resolve(enemies, a, 'archetype', 'the stagger table').components.motion.trajectory,
        'trajectory', 'the stagger table');
      stagger.push([
        `\`${id}\``,
        `\`${a}\``,
        `\`${t.id}\``,
        String(spread),
        t.type === 'arc' ? 'varies (arc)' : `${s2(spread / Math.abs(t.vy))} s`,
      ]);
    }
  }
  if (stagger.length) {
    w(table(['formation', 'archetype', 'shape', 'y spread', 'first to last'], stagger));
  } else {
    w('No formation this level uses spreads its slots vertically, so nothing is staggered.');
  }
  w();

  // 9. Projectiles -------------------------------------------------------------------------------
  w('## Projectiles');
  w();
  const projectiles = [['the player', s1(balance.weaponProjectileSpeed),
    String(balance.weaponProjectileDamage), 'not in content']];
  for (const id of used) {
    const wp = resolve(enemies, id, 'archetype', 'the projectiles section').components.weapon;
    if (wp) projectiles.push([`\`${id}\``, s1(wp.speed), 'contact', s1(CODE.enemyProjectileRadius.value)]);
  }
  w(table(['fired by', 'speed', 'damage', 'radius'], projectiles));
  w();
  w(`The player fires every \`weaponFireCooldown ${s2(balance.weaponFireCooldown)}\` s across \`weaponLevels ${balance.weaponLevels}\` shot levels`);
  w('(`assets/data/balance.json`).');
  w();
  w(`**Not in \`assets/data/\`:** the enemy projectile's radius is \`${s1(CODE.enemyProjectileRadius.value)}\` in ${CODE.enemyProjectileRadius.from},`);
  w(`and \`${CODE.patterns.value.join('`, `')}\` is the **only** \`pattern\` string that system builds`);
  w(`(${CODE.patterns.from}). Any other value is content naming a shape nothing draws, and it throws the`);
  w('moment that enemy first fires. Those two sentences are quoted from code, and **regenerating this');
  w('document cannot keep them honest** — if those lines move, this text does not change.');
  w();

  // 10. Drops and rewards ------------------------------------------------------------------------
  w('## Drops and rewards');
  w();
  const dropRows = [];
  for (const row of rows) {
    for (const sp of row.wave.spawns) {
      if (!sp.drop) continue;
      dropRows.push([
        `${row.exact ? '' : '>= '}${s1(row.start + sp.at)}`,
        `\`${sp.drop}\``,
        `\`${row.placement.wave}\` (#${row.i + 1})`,
        `\`${sp.spawn}\` in \`${sp.formation}\`, slot ${sp.dropSlot === undefined ? 0 : sp.dropSlot}`,
      ]);
    }
  }
  if (dropRows.length) w(table(['at', 'kind', 'wave', 'carried by'], dropRows));
  else w('This level delivers no drops.');
  w();
  w('**A drop is delivered only if the player destroys the carrier.** `core/domain/system/LifetimeSystem.java`');
  w('strips `ScoreValue`, `Drop` and `Collider` from an enemy that leaves the screen, so a drop placed');
  w('on a fast, fragile archetype can be lost entirely.');
  w();
  w(`**The six kinds are code, not content** (${DROP_KINDS_FROM}). There is no \`drops.json\`, and`);
  w('`SpawnSystem.requireRecognisedDrop` rejects anything outside this closed set the moment a wave');
  w('carrying it spawns:');
  w();
  w(table(['kind', 'what it does'], DROP_KINDS.map(([k, d]) => [`\`${k}\``, d])));
  w();
  if (attachments.size) {
    w('Attachment durability, the one content-driven part: '
      + [...attachments.keys()].map((k) => `\`${k}\` ${attachments.get(k).durability}`).join(', ')
      + ' (`assets/data/attachments.json`).');
    w();
  }

  // 11. The boss ---------------------------------------------------------------------------------
  if (level.boss) {
    const b = level.boss;
    w('## The boss');
    w();
    w(table(['field', 'value'],
      Object.keys(b).map((k) => [`\`${k}\``, typeof b[k] === 'number' ? s1(b[k]) : String(b[k])])));
    w();
    const coreSpawnY = CODE.playfieldHeight.value + (CODE.boss.coreKeelRadius - CODE.boss.coreKeelOffsetY);
    w(table(['derived', 'value', 'how'], [
      ['entrance duration', `${s1((coreSpawnY - b.combatY) / b.entranceSpeed)} s`,
        `\`(${s1(coreSpawnY)} - combatY) / entranceSpeed\`, the core spawning at \`CORE_SPAWN_Y\``],
      ['health of the kill target', String(2 * b.coreHealth),
        "`2 x coreHealth` — `core-keel` carries the core's health independently"],
      ['health the bar shows', String(2 * b.coreHealth + 2 * b.podHealth + 2 * b.armHealth),
        `the sum across all ${CODE.boss.parts} parts, so killing pods shortens the bar without shortening the fight`],
    ]));
    w();
    w('**How low a shot leaves the playfield**, from `combatY` and the fixed velocity ratios. Every');
    w('ratio is shallower than 45 degrees, so every projectile exits through a side edge and how far');
    w('down it gets is a pure function of `combatY`:');
    w();
    const shots = [];
    const half = CODE.playfieldWidth.value / 2;
    for (const [name, ratios, vyRatio, speed] of [
      ['spread', CODE.boss.spreadVxRatios, CODE.boss.spreadVyRatio, b.spreadProjectileSpeed],
      ['sweep', CODE.boss.sweepVxRatios, CODE.boss.sweepVyRatio, b.sweepProjectileSpeed],
    ]) {
      for (const r of ratios) {
        const vx = r * speed;
        const vy = vyRatio * speed;
        shots.push([name, s2(r), s1(vx), s1(vy), s1(b.combatY - (half / vx) * Math.abs(vy))]);
      }
    }
    w(table(['pattern', 'vx ratio', 'vx', 'vy', 'y at the side edge'], shots));
    w();
    w(`The ratios and \`CORE_SPAWN_Y\` are in ${CODE.boss.from}, not in content. **A \`y at the side`);
    w(`edge\` above the player's band around \`playerStartY ${s1(balance.playerStartY)}\` means that ray never reaches them**,`);
    w('which is how a boss becomes unlosable with no error anywhere.');
    w();
    w("**`entersAt` is absolute level time**, compared against `BossSystem`'s own clock, which is");
    w('independent of the wave chain. ' + (exact
      ? `The waves end at ${s1(wavesEnd)} s and the boss enters at ${s1(b.entersAt)} s: a ${s1(b.entersAt - wavesEnd)} s gap.`
      : 'Because at least one wave ends on `cleared`, that gap is unknowable and the boss can enter over a wave still running.'));
    w();
  }

  // 12. Designing against the player --------------------------------------------------------------
  w('## Designing against the player');
  w();
  w('The same for every level, repeated here so no lookup leaves this document.');
  w();
  const balRows = [
    ['playfield width', `${CODE.playfieldWidth.value} (${CODE.playfieldWidth.from})`],
    ['playfield height', `${CODE.playfieldHeight.value} (${CODE.playfieldHeight.from})`],
  ];
  for (const k of Object.keys(balance).sort()) balRows.push([`\`${k}\``, String(balance[k])]);
  w(table(['', 'value'], balRows));
  w();
  w('The two dimensions are fixed properties of the logical resolution rather than balance values,');
  w('which is why they live in code and everything under them lives in `assets/data/balance.json`.');
  w();

  // 13. Checks -------------------------------------------------------------------------------------
  w('## Checks');
  w();
  w('Every check below is a failure that is either silent at runtime or fatal only on the tick it');
  w('happens, and each one costs a run of `./gradlew :desktop:run` to find by hand. That is the part of');
  w('this document a generator can do and a human reliably will not.');
  w();
  const findings = [];
  for (const row of rows) {
    const wave = row.wave;
    for (const sp of wave.spawns) {
      if (wave.end.type === 'fixedDuration' && sp.at > wave.end.seconds) {
        findings.push(`\`${wave.id}\`: a spawn at ${s1(sp.at)} s never fires — the wave ends at ${s1(wave.end.seconds)} s. \`SpawnSystem.spawnDue\` only advances the cursor while the wave is active.`);
      }
      const enemy = resolve(enemies, sp.spawn, 'archetype', 'the checks section');
      const fp = footprint(sp, enemy, formations);
      if (fp.offScreen) {
        findings.push(`\`${wave.id}\`: \`${sp.spawn}\` in \`${sp.formation}\` at \`atX ${s2(sp.atX)}\` occupies ${s1(fp.min)} .. ${s1(fp.max)}, outside 0 .. ${CODE.playfieldWidth.value}. Nothing clamps it.`);
      }
      const slots = resolve(formations, sp.formation, 'formation', 'the checks section').slots.length;
      if (sp.dropSlot !== undefined && sp.dropSlot >= slots) {
        findings.push(`\`${wave.id}\`: \`dropSlot ${sp.dropSlot}\` on \`${sp.formation}\`, which has ${slots} slot(s). Fatal at spawn time (\`SpawnSystem.requireSlotInRange\`).`);
      }
      if (sp.drop && !DROP_KINDS.some(([k]) => k === sp.drop)) {
        findings.push(`\`${wave.id}\`: drop kind \`${sp.drop}\` is outside the six ${DROP_KINDS_FROM} recognises. Fatal at spawn time.`);
      }
      if (wave.end.type === 'cleared') {
        const t = resolve(trajectories, trajectoryOf(sp, enemy).id, 'trajectory', 'the checks section');
        if (t.type !== 'arc' && t.vy >= 0) {
          findings.push(`\`${wave.id}\` is \`cleared\` and spawns \`${sp.spawn}\` on \`${t.id}\`, which never leaves the playfield unattended, so the wave can never end.`);
        }
      }
    }
    if (row.placement.offset < 0) {
      const previous = rows[row.i - 1];
      findings.push(`placement #${row.i + 1} \`${row.placement.wave}\` has \`offset ${s1(row.placement.offset)}\`, overlapping \`${previous ? previous.placement.wave : '(nothing)'}\` by ${s1(-row.placement.offset)} s. Overlap is the one thing in this format that produces pressure nothing else can, and it is the thing a reader misreads first.`);
    }
  }
  if (level.boss && exact && level.boss.entersAt < wavesEnd) {
    findings.push(`\`boss.entersAt ${s1(level.boss.entersAt)}\` is earlier than the last placement's end at ${s1(wavesEnd)} s, so the boss enters over a running wave. Legal, occasionally intended, never accidental.`);
  }
  if (findings.length === 0) w('**No issues found.**');
  else for (const f of findings) w(`- ${f}`);
  w();

  // 14. The beat map -------------------------------------------------------------------------------
  w('## The beat map');
  w();
  w('**Not generated, and it cannot be.** Which of the fourteen beats of');
  w('`docs/planning/04-campaign-and-levels.md` a placement serves, and why it exists, is design intent,');
  w('and there is no field for it in `assets/data/`. JSON admits no comments; area G of');
  w('`docs/plan/10c-architecture-review/assessment.md` predicted this exactly as the price of');
  w('generating the document from the JSON, and section 14 of');
  w('`docs/plan/11d-per-level-document/document-contract.md` decided to pay it rather than guess.');
  w();
  w('The mapping exists, written by hand, in `docs/plan/11c-movement-shapes/shape-catalogue.md` under');
  w('"What points at what".');
  w();

  return { path: path.join(OUT_DIR, `${levelId}.md`), text: out.join('\n').replace(/\n+$/, '\n') };
}

// ------------------------------------------------------------------------------------------------

function main() {
  const check = process.argv.includes('--check');
  const content = {
    waves: index(readJson('waves.json').waves, 'waves'),
    enemies: index(readJson('enemies.json').enemies, 'archetypes'),
    formations: index(readJson('formations.json').formations, 'formations'),
    trajectories: index(readJson('trajectories.json').trajectories, 'trajectories'),
    attachments: index(readJson('attachments.json').attachments, 'attachments'),
    balance: readJson('balance.json'),
  };

  const levelFiles = fs.readdirSync(DATA).filter((f) => /^level-\d+\.json$/.test(f)).sort();
  if (levelFiles.length === 0) die('no level-NN.json in assets/data/');

  fs.mkdirSync(OUT_DIR, { recursive: true });
  let stale = 0;
  for (const file of levelFiles) {
    const built = buildLevel(file, Object.assign({}, content, { level: readJson(file) }));
    const relative = path.relative(ROOT, built.path).split(path.sep).join('/');
    const existing = fs.existsSync(built.path) ? fs.readFileSync(built.path, 'utf8') : null;
    if (existing === built.text) {
      process.stdout.write(`unchanged  ${relative}\n`);
      continue;
    }
    if (check) {
      stale += 1;
      process.stdout.write(`STALE      ${relative}\n`);
      continue;
    }
    fs.writeFileSync(built.path, built.text);
    process.stdout.write(`${existing === null ? 'written' : 'updated'}    ${relative}\n`);
  }
  if (check && stale > 0) {
    process.stderr.write(
      `\n${stale} level document(s) do not match assets/data/.\n\n`
      + 'The JSON is the source and the document is generated from it, so this means either the\n'
      + 'content changed without the document being regenerated, or the document was edited by\n'
      + 'hand. Either way, run:\n\n'
      + '    node tools/build-level-docs.js\n\n'
      + 'and commit the result.\n');
    process.exit(1);
  }
}

main();
