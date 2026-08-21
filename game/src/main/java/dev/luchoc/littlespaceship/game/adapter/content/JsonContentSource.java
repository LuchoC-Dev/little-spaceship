package dev.luchoc.littlespaceship.game.adapter.content;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import dev.luchoc.littlespaceship.core.port.AttachmentDefinition;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.EnemyDefinition;
import dev.luchoc.littlespaceship.core.port.FormationDefinition;
import dev.luchoc.littlespaceship.core.port.FormationSlot;
import dev.luchoc.littlespaceship.core.port.SimpleAttachmentDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleEnemyDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleFormationDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleTrajectoryDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleWaveTimeline;
import dev.luchoc.littlespaceship.core.port.SpawnEvent;
import dev.luchoc.littlespaceship.core.port.TrajectoryDefinition;
import dev.luchoc.littlespaceship.core.port.WaveTimeline;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * The one level this phase ships. A second level would need this hardcoded id turned into a
     * parameter — not done now, per the plan's own warning against over-generalising the schema
     * ahead of a second concrete case.
     */
    public static final String LEVEL_ID = "level-01";

    private final BalanceValues balance;
    private final Map<String, EnemyDefinition> enemies = new HashMap<>();
    private final Map<String, TrajectoryDefinition> trajectories = new HashMap<>();
    private final Map<String, FormationDefinition> formations = new HashMap<>();
    private final Map<String, WaveTimeline> timelines = new HashMap<>();
    private final Map<String, AttachmentDefinition> attachments = new HashMap<>();

    /**
     * Loads every content file under {@code dataDir}.
     *
     * @param dataDir the directory holding {@code balance.json}, {@code trajectories.json},
     *     {@code formations.json}, {@code enemies.json}, {@code attachments.json} and
     *     {@code level-01.json}
     * @throws IllegalArgumentException if any file is missing or malformed, naming the file and
     *     whatever it could not resolve
     */
    public JsonContentSource(FileHandle dataDir) {
        if (dataDir == null) {
            throw new IllegalArgumentException("the content source needs a data directory");
        }
        JsonReader reader = new JsonReader();
        this.balance = loadBalance(reader, dataDir.child("balance.json"));
        loadTrajectories(reader, dataDir.child("trajectories.json"));
        loadFormations(reader, dataDir.child("formations.json"));
        loadEnemies(reader, dataDir.child("enemies.json"));
        loadAttachments(reader, dataDir.child("attachments.json"));
        loadLevel(reader, dataDir.child("level-01.json"), LEVEL_ID);
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

    private void loadLevel(JsonReader reader, FileHandle file, String levelId) {
        inFile(file, () -> {
            List<SpawnEvent> events = new ArrayList<>();
            for (JsonValue entry : reader.parse(file).get("events")) {
                events.add(new SpawnEvent(
                    entry.getFloat("at"),
                    entry.getString("spawn"),
                    entry.getString("formation"),
                    entry.getFloat("atX"),
                    entry.getString("drop", null)));
            }
            timelines.put(levelId, new SimpleWaveTimeline(events));
            return null;
        });
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
