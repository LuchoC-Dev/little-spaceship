package dev.luchoc.littlespaceship.core.testsupport;

import dev.luchoc.littlespaceship.core.port.AttachmentDefinition;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.BossDefinition;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.EnemyDefinition;
import dev.luchoc.littlespaceship.core.port.FormationDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleAttachmentDefinition;
import dev.luchoc.littlespaceship.core.port.TrajectoryDefinition;
import dev.luchoc.littlespaceship.core.port.WaveDefinition;
import dev.luchoc.littlespaceship.core.port.WaveTimeline;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ContentSource} wrapping a {@link TestBalance}, for tests that need a world but not a
 * real content pipeline.
 *
 * <p>Enemies, trajectories, formations and timelines are registered with {@code with*}, which
 * mirrors what {@code SimpleEnemyDefinition} and friends are for in production code: content built
 * by hand, never read from a file.
 */
public final class TestContent implements ContentSource {

    public final TestBalance balance;

    private final Map<String, EnemyDefinition> enemies = new HashMap<>();
    private final Map<String, TrajectoryDefinition> trajectories = new HashMap<>();
    private final Map<String, FormationDefinition> formations = new HashMap<>();
    private final Map<String, WaveTimeline> timelines = new HashMap<>();
    private final Map<String, AttachmentDefinition> attachments = new HashMap<>();
    private final Map<String, BossDefinition> bosses = new HashMap<>();
    private final Map<String, WaveDefinition> waves = new HashMap<>();

    public TestContent() {
        this(new TestBalance());
    }

    public TestContent(TestBalance balance) {
        this.balance = balance;
    }

    public TestContent withEnemy(EnemyDefinition definition) {
        enemies.put(definition.id(), definition);
        return this;
    }

    public TestContent withTrajectory(TrajectoryDefinition definition) {
        trajectories.put(definition.id(), definition);
        return this;
    }

    public TestContent withFormation(FormationDefinition definition) {
        formations.put(definition.id(), definition);
        return this;
    }

    public TestContent withTimeline(String levelId, WaveTimeline timeline) {
        timelines.put(levelId, timeline);
        return this;
    }

    public TestContent withAttachment(AttachmentDefinition definition) {
        attachments.put(definition.id(), definition);
        return this;
    }

    /** Convenience for the common case: registers {@code "attachment"} with the given durability. */
    public TestContent withAttachment(int durability) {
        return withAttachment(new SimpleAttachmentDefinition("attachment", durability));
    }

    public TestContent withWave(WaveDefinition definition) {
        waves.put(definition.id(), definition);
        return this;
    }

    @Override
    public WaveDefinition wave(String id) {
        return require(waves, id, "wave");
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

    public TestContent withBoss(String levelId, BossDefinition definition) {
        bosses.put(levelId, definition);
        return this;
    }

    @Override
    public boolean hasBoss(String levelId) {
        return bosses.containsKey(levelId);
    }

    @Override
    public BossDefinition boss(String levelId) {
        return require(bosses, levelId, "boss");
    }

    private static <T> T require(Map<String, T> registry, String id, String kind) {
        T value = registry.get(id);
        if (value == null) {
            throw new IllegalArgumentException("unknown " + kind + " id '" + id + "'");
        }
        return value;
    }
}
