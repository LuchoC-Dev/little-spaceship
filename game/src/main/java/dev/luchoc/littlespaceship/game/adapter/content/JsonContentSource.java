package dev.luchoc.littlespaceship.game.adapter.content;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import dev.luchoc.littlespaceship.core.port.AttachmentDefinition;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.BossDefinition;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.EnemyDefinition;
import dev.luchoc.littlespaceship.core.port.FormationDefinition;
import dev.luchoc.littlespaceship.core.port.FormationSlot;
import dev.luchoc.littlespaceship.core.port.SimpleAttachmentDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleBossDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleEnemyDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleFormationDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleTrajectoryDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleWaveDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleWaveTimeline;
import dev.luchoc.littlespaceship.core.port.SpawnEvent;
import dev.luchoc.littlespaceship.core.port.TrajectoryDefinition;
import dev.luchoc.littlespaceship.core.port.WaveDefinition;
import dev.luchoc.littlespaceship.core.port.WaveEndCondition;
import dev.luchoc.littlespaceship.core.port.WavePlacement;
import dev.luchoc.littlespaceship.core.port.WaveTimeline;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link ContentSource} the plan's phase 04 left to {@code game}: reads
 * {@code assets/data/*.json} with {@link JsonReader}/{@link JsonValue} — never the reflection-based
 * {@code Json} class, which would break the web build far from where the mistake was made — and
 * exposes the result as id-keyed lookups, built once at load time.
 *
 * <p>Every file is parsed eagerly in the constructor, so a malformed file fails at startup, not on
 * whichever tick first asks for the broken id. {@code core}'s own contracts already name the id or
 * the component field that is wrong ({@link ComponentSpec} accessors, {@link SimpleEnemyDefinition}
 * and friends); this class is the only place that also knows the file name, so every failure caught
 * here is rethrown with that name prefixed, which is what the acceptance criterion "names the file
 * and the offending id" actually needs end to end.
 */
public final class JsonContentSource implements ContentSource {

    private final BalanceValues balance;
    private final Map<String, EnemyDefinition> enemies = new HashMap<>();
    private final Map<String, TrajectoryDefinition> trajectories = new HashMap<>();
    private final Map<String, FormationDefinition> formations = new HashMap<>();
    private final Map<String, WaveTimeline> timelines = new HashMap<>();
    private final Map<String, AttachmentDefinition> attachments = new HashMap<>();
    private final Map<String, BossDefinition> bosses = new HashMap<>();
    private final Map<String, WaveDefinition> waves = new HashMap<>();

    /**
     * Loads every content file under {@code dataDir}, plus the level named by {@code levelId} from
     * {@code <levelId>.json} in that same directory.
     *
     * <p>The id is a constructor parameter rather than a hardcoded constant, per this class's own
     * former javadoc naming exactly this as the thing a second level would need — {@code
     * game-presentation}'s handover of issue #87. Loading stays single-level and eager: nothing
     * here lists the directory to discover every {@code level-*.json} on disk, because {@link
     * FileHandle#list()} has no answer for the web target's asset packaging, and there is still only
     * one concrete level to load. Whoever calls this decides which one.
     *
     * @param dataDir the directory holding {@code balance.json}, {@code trajectories.json},
     *     {@code formations.json}, {@code enemies.json}, {@code attachments.json}, the optional
     *     {@code waves.json} and {@code <levelId>.json}
     * @param levelId the content id of the level to load; also the level file's name without the
     *     {@code .json} extension
     * @throws IllegalArgumentException if any file is missing or malformed, naming the file and
     *     whatever it could not resolve
     */
    public JsonContentSource(FileHandle dataDir, String levelId) {
        if (dataDir == null) {
            throw new IllegalArgumentException("the content source needs a data directory");
        }
        if (levelId == null || levelId.isEmpty()) {
            throw new IllegalArgumentException("the content source needs a level id");
        }
        JsonReader reader = new JsonReader();
        this.balance = loadBalance(reader, dataDir.child("balance.json"));
        loadTrajectories(reader, dataDir.child("trajectories.json"));
        loadFormations(reader, dataDir.child("formations.json"));
        loadEnemies(reader, dataDir.child("enemies.json"));
        loadAttachments(reader, dataDir.child("attachments.json"));
        loadWaves(reader, dataDir.child("waves.json"));
        loadLevel(reader, dataDir.child(levelId + ".json"), levelId);
    }

    @Override
    public BalanceValues balance() {
        return balance;
    }

    @Override
    public EnemyDefinition enemy(String id) {
        return require(enemies, id, "enemy");
    }

    @Override
    public TrajectoryDefinition trajectory(String id) {
        return require(trajectories, id, "trajectory");
    }

    @Override
    public FormationDefinition formation(String id) {
        return require(formations, id, "formation");
    }

    @Override
    public WaveTimeline timeline(String levelId) {
        return require(timelines, levelId, "level timeline");
    }

    @Override
    public AttachmentDefinition attachment(String id) {
        return require(attachments, id, "attachment");
    }

    @Override
    public WaveDefinition wave(String id) {
        return require(waves, id, "wave");
    }

    @Override
    public boolean hasBoss(String levelId) {
        return bosses.containsKey(levelId);
    }

    @Override
    public BossDefinition boss(String levelId) {
        return require(bosses, levelId, "level boss");
    }

    private static <T> T require(Map<String, T> registry, String id, String kind) {
        T value = registry.get(id);
        if (value == null) {
            throw new IllegalArgumentException("unknown " + kind + " id '" + id + "'");
        }
        return value;
    }

    private static BalanceValues loadBalance(JsonReader reader, FileHandle file) {
        return inFile(file, () -> JsonBalanceValues.from(reader.parse(file)));
    }

    private void loadTrajectories(JsonReader reader, FileHandle file) {
        inFile(file, () -> {
            for (JsonValue entry : reader.parse(file).get("trajectories")) {
                TrajectoryDefinition trajectory = new SimpleTrajectoryDefinition(
                    entry.getString("id"), entry.getFloat("vx"), entry.getFloat("vy"));
                trajectories.put(trajectory.id(), trajectory);
            }
            return null;
        });
    }

    private void loadFormations(JsonReader reader, FileHandle file) {
        inFile(file, () -> {
            for (JsonValue entry : reader.parse(file).get("formations")) {
                List<FormationSlot> slots = new ArrayList<>();
                for (JsonValue slot : entry.get("slots")) {
                    slots.add(new FormationSlot(slot.getFloat("offsetX"), slot.getFloat("offsetY")));
                }
                FormationDefinition formation = new SimpleFormationDefinition(
                    entry.getString("id"), slots);
                formations.put(formation.id(), formation);
            }
            return null;
        });
    }

    private void loadEnemies(JsonReader reader, FileHandle file) {
        inFile(file, () -> {
            for (JsonValue entry : reader.parse(file).get("enemies")) {
                String id = entry.getString("id");
                JsonValue componentsValue = entry.get("components");
                if (componentsValue == null) {
                    throw new IllegalArgumentException("enemy '" + id + "' has no components");
                }
                EnemyDefinition enemy = new SimpleEnemyDefinition(
                    id, JsonComponentSpecs.parse(componentsValue));
                enemies.put(enemy.id(), enemy);
            }
            return null;
        });
    }

    private void loadAttachments(JsonReader reader, FileHandle file) {
        inFile(file, () -> {
            for (JsonValue entry : reader.parse(file).get("attachments")) {
                AttachmentDefinition attachment = new SimpleAttachmentDefinition(
                    entry.getString("id"), entry.getInt("durability"));
                attachments.put(attachment.id(), attachment);
            }
            return null;
        });
    }

    /**
     * Reads {@code waves.json}, the named-content file a level's {@code "waves"} block references by
     * id — beside {@code formations.json} and {@code trajectories.json}, per {@code
     * docs/planning/08-decisions-and-open-items.md}, "The 11 group, 27/08/2026". Optional: no level
     * shipped by this phase is required to use the wave format yet ({@code level-01.json} keeps its
     * flat {@code "events"} list until issue #114 migrates it), so a data directory without this file
     * loads with an empty wave registry rather than failing — the same reasoning {@link #loadLevel}
     * already applies to a level's optional {@code "boss"} block.
     */
    private void loadWaves(JsonReader reader, FileHandle file) {
        if (!file.exists()) {
            return;
        }
        inFile(file, () -> {
            JsonValue root = reader.parse(file);
            requireOnlyKeys(root, "wave file", "waves");
            JsonValue wavesValue = root.get("waves");
            if (wavesValue == null) {
                throw new IllegalArgumentException("wave file has no 'waves' array");
            }
            for (JsonValue entry : wavesValue) {
                requireOnlyKeys(entry, "wave", "id", "end", "spawns");
                String id = entry.getString("id");
                JsonValue spawnsValue = entry.get("spawns");
                if (spawnsValue == null) {
                    throw new IllegalArgumentException("wave '" + id + "' has no spawns");
                }
                List<SpawnEvent> spawns = new ArrayList<>();
                for (JsonValue spawnEntry : spawnsValue) {
                    spawns.add(parseSpawnEvent(spawnEntry, "wave '" + id + "' spawn"));
                }
                JsonValue endValue = entry.get("end");
                if (endValue == null) {
                    throw new IllegalArgumentException("wave '" + id + "' needs an end condition");
                }
                WaveEndCondition end = parseEndCondition(endValue, id);
                waves.put(id, new SimpleWaveDefinition(id, spawns, end));
            }
            return null;
        });
    }

    /**
     * Parses the {@code "end"} object of one {@code waves.json} entry into a {@link
     * WaveEndCondition}. {@code "type"} discriminates between the two sealed cases — {@code
     * "fixedDuration"}, which also needs {@code "seconds"}, and {@code "cleared"}, which needs
     * nothing else — and any other value is rejected rather than defaulted to {@link
     * WaveEndCondition.FixedDuration}: a wave that quietly became a fixed duration because of a typo
     * in {@code "type"} is exactly the silent pacing bug {@code docs/plan/11b-wave-system/plan.md}'s
     * "Watch out for" section warns against.
     */
    private static WaveEndCondition parseEndCondition(JsonValue endValue, String waveId) {
        String type = endValue.getString("type");
        if ("fixedDuration".equals(type)) {
            requireOnlyKeys(endValue, "wave '" + waveId + "' end condition", "type", "seconds");
            return new WaveEndCondition.FixedDuration(endValue.getFloat("seconds"));
        }
        if ("cleared".equals(type)) {
            requireOnlyKeys(endValue, "wave '" + waveId + "' end condition", "type");
            return new WaveEndCondition.Cleared();
        }
        throw new IllegalArgumentException(
            "wave '" + waveId + "' has an unknown end condition type '" + type + "'");
    }

    /**
     * @param entry one spawn's JSON object, shared by a level's legacy {@code "events"} list and a
     *     {@code waves.json} wave's {@code "spawns"} list — the two places {@link SpawnEvent} is
     *     read from, per its own javadoc on having no reference frame of its own
     * @param context named in any error this entry raises, so it reads "wave 'x' spawn" or
     *     "spawn event" depending on which list called it
     */
    private static SpawnEvent parseSpawnEvent(JsonValue entry, String context) {
        requireOnlyKeys(entry, context, "at", "spawn", "formation", "atX", "drop", "dropSlot");
        return new SpawnEvent(
            entry.getFloat("at"),
            entry.getString("spawn"),
            entry.getString("formation"),
            entry.getFloat("atX"),
            entry.getString("drop", null),
            entry.getInt("dropSlot", 0));
    }

    /**
     * Reads the level file's top-level blocks: the optional {@code "boss"} object and exactly one of
     * {@code "events"} (the legacy flat list) or {@code "waves"} (an ordered list of {@link
     * WavePlacement}s, each resolved against {@link #waves} and flattened into the same absolute,
     * level-relative {@link SpawnEvent} list {@code "events"} would have produced by hand — {@code
     * SpawnSystem} still walks a flat {@link WaveTimeline} with a single cursor until issue #112
     * migrates it onto {@link WaveDefinition} and {@link WavePlacement} directly). {@code
     * hasBoss}/{@code boss} answer false/throw for a level whose file carries no {@code "boss"} key
     * at all — a legitimate case per {@link ContentSource}'s own contract — but any key on either
     * object this schema does not name fails loudly through {@link #requireOnlyKeys}, closing the gap
     * {@code level-designer} found: an unrecognised key used to load clean and silently leave the
     * level with no boss, which is worse than a parse error.
     */
    private void loadLevel(JsonReader reader, FileHandle file, String levelId) {
        inFile(file, () -> {
            JsonValue root = reader.parse(file);
            requireOnlyKeys(root, "level file", "boss", "events", "waves");
            JsonValue bossValue = root.get("boss");
            if (bossValue != null) {
                bosses.put(levelId, parseBoss(bossValue));
            }
            JsonValue eventsValue = root.get("events");
            JsonValue placementsValue = root.get("waves");
            if (eventsValue != null && placementsValue != null) {
                throw new IllegalArgumentException(
                    "level file has both 'events' and 'waves' — use exactly one");
            }
            List<SpawnEvent> events;
            if (eventsValue != null) {
                events = new ArrayList<>();
                for (JsonValue entry : eventsValue) {
                    events.add(parseSpawnEvent(entry, "spawn event"));
                }
            } else if (placementsValue != null) {
                events = flattenPlacements(placementsValue, levelId);
            } else {
                throw new IllegalArgumentException("level file needs either 'events' or 'waves'");
            }
            timelines.put(levelId, new SimpleWaveTimeline(events));
            return null;
        });
    }

    /**
     * Resolves a level's {@code "waves"} block — an ordered list of {@code {"wave", "offset"}}
     * placements — into the flat, absolute-time {@link SpawnEvent} list {@link #loadLevel} needs
     * today, since {@code SpawnSystem} has not yet migrated onto {@link WaveDefinition} and {@link
     * WavePlacement} directly (issue #112). Each placement starts {@code offsetSeconds} after the
     * previous one ends — the first starts at level time zero, per {@link WavePlacement}'s own
     * contract — and a wave's own {@link SpawnEvent#at()} values, which are relative to its own
     * start, are shifted by that start time to become level-relative.
     *
     * <p>Only {@link WaveEndCondition.FixedDuration} can be flattened this way, because only it says
     * in the data, ahead of time, when the next placement may begin; a {@link
     * WaveEndCondition.Cleared} wave's end depends on what the simulation does at runtime, which this
     * loader has no access to. A level placing one is rejected rather than guessed — the risk {@code
     * docs/plan/11b-wave-system/plan.md} names directly: "Do not write a cleared-based wave into any
     * level before both are merged."
     *
     * <p>Placements may overlap (a negative {@code offset}), so the per-wave lists are not
     * individually concatenated in placement order — they are merged and sorted by absolute {@code
     * at} at the end, which is all {@link SimpleWaveTimeline} itself requires.
     */
    private List<SpawnEvent> flattenPlacements(JsonValue placementsValue, String levelId) {
        List<SpawnEvent> flattened = new ArrayList<>();
        float previousEnd = 0f;
        for (JsonValue entry : placementsValue) {
            requireOnlyKeys(entry, "wave placement", "wave", "offset");
            WavePlacement placement = new WavePlacement(entry.getString("wave"), entry.getFloat("offset"));
            WaveDefinition wave = require(waves, placement.waveId(), "wave");
            float start = previousEnd + placement.offsetSeconds();
            if (!(wave.endCondition() instanceof WaveEndCondition.FixedDuration fixedDuration)) {
                throw new IllegalArgumentException(
                    "level '" + levelId + "': wave '" + wave.id() + "' has a 'cleared' end condition, "
                        + "which cannot be resolved into a level timeline until SpawnSystem migrates "
                        + "onto waves directly (issue #112)");
            }
            for (SpawnEvent spawn : wave.spawns()) {
                flattened.add(new SpawnEvent(
                    start + spawn.at(), spawn.enemyId(), spawn.formationId(), spawn.atX(),
                    spawn.dropId(), spawn.dropSlot()));
            }
            previousEnd = start + fixedDuration.seconds();
        }
        flattened.sort(Comparator.comparing(SpawnEvent::at));
        return flattened;
    }

    /**
     * Parses the {@code "boss"} block, keys named exactly after {@link BossDefinition}'s accessors —
     * see {@code docs/plan/07-boss/status.md}'s "Notes for whoever comes next" for the field list.
     */
    private static BossDefinition parseBoss(JsonValue value) {
        requireOnlyKeys(value, "boss block",
            "id", "entersAt", "coreHealth", "podHealth", "armHealth",
            "corePoints", "podPoints", "armPoints",
            "entranceSpeed", "combatY", "patternCooldown",
            "spreadProjectileSpeed", "sweepProjectileSpeed");
        return new SimpleBossDefinition(
            value.getString("id"),
            value.getFloat("entersAt"),
            value.getInt("coreHealth"),
            value.getInt("podHealth"),
            value.getInt("armHealth"),
            value.getInt("corePoints"),
            value.getInt("podPoints"),
            value.getInt("armPoints"),
            value.getFloat("entranceSpeed"),
            value.getFloat("combatY"),
            value.getFloat("patternCooldown"),
            value.getFloat("spreadProjectileSpeed"),
            value.getFloat("sweepProjectileSpeed"));
    }

    /**
     * Rejects a key {@code value} carries that {@code allowedKeys} does not name. This is the other
     * half of "malformed content fails naming the file and the offending id": {@code core}'s own
     * constructors already reject a missing or invalid field, but nothing before this rejected an
     * unrecognised one — a typo'd or stale key used to load clean and silently do nothing, which for
     * a top-level block like {@code "boss"} means the level loads with no boss and can never be
     * completed, the worst kind of failure because nothing reports it.
     */
    private static void requireOnlyKeys(JsonValue value, String context, String... allowedKeys) {
        Set<String> allowed = new HashSet<>(Arrays.asList(allowedKeys));
        for (JsonValue child = value.child; child != null; child = child.next) {
            if (!allowed.contains(child.name)) {
                throw new IllegalArgumentException(
                    context + " has an unrecognised key '" + child.name + "'");
            }
        }
    }

    /**
     * Runs {@code loader} and, if it throws, rethrows with the file's path prefixed. {@code core}'s
     * own exceptions already name the id or field at fault — see this class's javadoc — so this is
     * the other half the acceptance criterion needs, and the only half {@code game} can supply.
     */
    private static <T> T inFile(FileHandle file, Loader<T> loader) {
        try {
            return loader.load();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(file.path() + ": " + e.getMessage(), e);
        }
    }

    /**
     * A throwing supplier scoped to this class, so {@link #inFile} does not need
     * {@code java.util.concurrent.Callable} for what is not concurrent code — this project treats
     * that package as TeaVM-hostile territory and avoids it even where a single method would be
     * harmless, per this agent's own memory on the subject.
     */
    @FunctionalInterface
    private interface Loader<T> {
        T load();
    }
}
