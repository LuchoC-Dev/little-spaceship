package dev.luchoc.littlespaceship.game.adapter.content;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import dev.luchoc.littlespaceship.core.port.ArcTrajectoryDefinition;
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
    private final Map<String, List<WavePlacement>> placements = new HashMap<>();

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
    public List<WavePlacement> placements(String levelId) {
        return require(placements, levelId, "level placements");
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
                TrajectoryDefinition trajectory = parseTrajectory(entry);
                trajectories.put(trajectory.id(), trajectory);
            }
            return null;
        });
    }

    /**
     * Parses one {@code trajectories.json} entry into the {@link TrajectoryDefinition} kind its
     * {@code "type"} names — {@code "constant"} (the default, so the four entries that shipped
     * before {@code "type"} existed still load unchanged) or {@code "arc"}, the two kinds
     * {@code docs/plan/11c-movement-shapes/shape-catalogue.md} decides and {@link
     * TrajectoryDefinition} is sealed to. Any other value fails loudly naming both the trajectory id
     * and the bad type — deliberately not defaulted to {@code "constant"}, the same reasoning {@link
     * #parseEndCondition} already applies to a wave's end condition: a typo in {@code "type"} silently
     * loading as a different shape is a wrong game, not a crash, and this loader used to be exactly
     * that permissive by reading only {@code id}, {@code vx} and {@code vy} and ignoring everything
     * else.
     */
    private static TrajectoryDefinition parseTrajectory(JsonValue entry) {
        String id = entry.getString("id");
        String type = entry.getString("type", "constant");
        if ("constant".equals(type)) {
            requireOnlyKeys(entry, "trajectory '" + id + "'", "id", "type", "vx", "vy");
            return new SimpleTrajectoryDefinition(id, entry.getFloat("vx"), entry.getFloat("vy"));
        }
        if ("arc".equals(type)) {
            requireOnlyKeys(entry, "trajectory '" + id + "'", "id", "type", "vx", "vy", "ay");
            return new ArcTrajectoryDefinition(
                id, entry.getFloat("vx"), entry.getFloat("vy"), entry.getFloat("ay"));
        }
        throw new IllegalArgumentException(
            "trajectory '" + id + "' has an unknown type '" + type + "'");
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
     * {@code "events"} (the legacy flat list, still {@code level-01.json}'s own shape until issue
     * #114 migrates it) or {@code "waves"} (an ordered list of {@link WavePlacement}s). Unlike the
     * flattening this class did before issue #112 merged, a {@code "waves"} block is stored as-is —
     * each placement's wave id resolved against {@link #waves} to fail loudly on a typo, but its
     * offset and its wave's own end condition left untouched — because {@code SpawnSystem} now reads
     * {@link ContentSource#wave(String)} and {@link ContentSource#placements(String)} directly and
     * does its own scheduling, including resolving a {@link WaveEndCondition.Cleared} wave at
     * runtime. Flattening here would have quietly reimplemented that scheduling a second time and
     * silently dropped {@code Cleared} support, which is exactly the mistake this rewrite undoes.
     * {@code hasBoss}/{@code boss} answer false/throw for a level whose file carries no {@code "boss"}
     * key at all — a legitimate case per {@link ContentSource}'s own contract — but any key on either
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
            if (eventsValue != null) {
                List<SpawnEvent> events = new ArrayList<>();
                for (JsonValue entry : eventsValue) {
                    events.add(parseSpawnEvent(entry, "spawn event"));
                }
                timelines.put(levelId, new SimpleWaveTimeline(events));
            } else if (placementsValue != null) {
                placements.put(levelId, parsePlacements(placementsValue));
            } else {
                throw new IllegalArgumentException("level file needs either 'events' or 'waves'");
            }
            return null;
        });
    }

    /**
     * Parses a level's {@code "waves"} block into the ordered {@link WavePlacement} list {@code
     * SpawnSystem} now reads through {@link ContentSource#placements(String)}. Each placement's
     * {@code "wave"} id is resolved against {@link #waves} here, at load time, so a typo fails
     * loudly naming the level and the id — the same "malformed content fails at startup" guarantee
     * every other lookup in this class already gives — rather than surfacing later as {@code
     * SpawnSystem} reaching {@link ContentSource#wave(String)}'s own failure with no file name
     * attached. Offsets and end conditions are left exactly as declared: this class no longer
     * schedules anything, it only hands {@code SpawnSystem} the declarations to schedule itself.
     */
    private List<WavePlacement> parsePlacements(JsonValue placementsValue) {
        List<WavePlacement> resolved = new ArrayList<>();
        for (JsonValue entry : placementsValue) {
            requireOnlyKeys(entry, "wave placement", "wave", "offset");
            WavePlacement placement = new WavePlacement(entry.getString("wave"), entry.getFloat("offset"));
            require(waves, placement.waveId(), "wave");
            resolved.add(placement);
        }
        return resolved;
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
