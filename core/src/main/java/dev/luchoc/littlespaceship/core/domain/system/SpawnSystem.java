package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.Drop;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.content.ComponentFactoryRegistry;
import dev.luchoc.littlespaceship.core.port.ComponentSpec;
import dev.luchoc.littlespaceship.core.port.EnemyDefinition;
import dev.luchoc.littlespaceship.core.port.FormationDefinition;
import dev.luchoc.littlespaceship.core.port.FormationSlot;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.SpawnEvent;
import dev.luchoc.littlespaceship.core.port.WaveTimeline;
import java.util.List;

/**
 * Advances a level's {@link WaveTimeline} and spawns what is due — the intensity curve turned into
 * entities.
 *
 * <p>Unlike every other system shipped so far, this one is not stateless: it holds the level id it
 * was built for, how much level time has elapsed, and a cursor into the timeline. That is a
 * deliberate first exception to the pattern the other systems keep, not an oversight — a fresh
 * {@code Simulation} always builds a fresh {@code SpawnSystem}, so the state is exactly as
 * reproducible as everything else the composition root creates once per run. Tracking it on
 * {@code World} instead would mean inventing a general "level time" concept that nothing but this
 * system needs yet.
 *
 * <p>The timeline is walked with a single advancing cursor, never re-scanned from the start: {@link
 * WaveTimeline#events()} is guaranteed sorted by {@link SpawnEvent#at()}, so once an event has fired
 * every earlier one has too. A tick may fire more than one event — a stalled frame or a very short
 * step between two closely timed waves both produce that legitimately.
 *
 * <p>Each entity of a spawned wave is built the same way: an entity is created, every {@link
 * ComponentSpec} of the {@link EnemyDefinition} is handed to the {@link ComponentFactoryRegistry},
 * and only then is its {@code Transform} set from the wave's anchor plus the {@link FormationSlot}'s
 * offset — position is spawn-event data, never archetype data, so it is not something a component
 * factory could know. Every entity of a wave enters fully off-screen, regardless of its own size and
 * of the formation's shape: the anchor is pushed up by the formation's lowest {@code offsetY}
 * ({@link #lowestOffsetY}), so the slot closest to being visible is the one measured against the
 * playfield edge, and every other slot ends up further above it. A single-slot formation is the
 * special case of this where the lowest offset is its own, usually zero.
 */
public final class SpawnSystem implements GameSystem {

    /**
     * Height of the playfield, in logical units. {@code docs/design/04-hud-layout.md} fixes the
     * playfield at 208x270 inside the 480x270 logical resolution — this is that second figure, the
     * counterpart to {@link MotionSystem#PLAYFIELD_WIDTH}. A fixed dimension of the logical
     * resolution, not a balance value, for the same reason that constant is not one either.
     */
    public static final float PLAYFIELD_HEIGHT = 270f;

    private static final ComponentFactoryRegistry FACTORIES = ComponentFactoryRegistry.withDefaults();

    private final String levelId;

    private float levelTime;
    private int cursor;

    /**
     * @param levelId the content id of the level this system advances
     */
    public SpawnSystem(String levelId) {
        if (levelId == null || levelId.isEmpty()) {
            throw new IllegalArgumentException("a spawn system needs a level id");
        }
        this.levelId = levelId;
    }

    @Override
    public SystemOrder order() {
        return SystemOrder.SPAWN;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        levelTime += step;
        List<SpawnEvent> events = world.content().timeline(levelId).events();
        while (cursor < events.size() && events.get(cursor).at() <= levelTime) {
            spawnWave(world, events.get(cursor));
            cursor++;
        }
    }

    private static void spawnWave(World world, SpawnEvent event) {
        EnemyDefinition enemy = world.content().enemy(event.enemyId());
        FormationDefinition formation = world.content().formation(event.formationId());
        float anchorX = event.atX() * MotionSystem.PLAYFIELD_WIDTH;
        float lowestOffsetY = lowestOffsetY(formation.slots());

        for (FormationSlot slot : formation.slots()) {
            int entity = world.createEntity();
            attachComponents(world, enemy, entity);
            positionSpawned(world, entity, anchorX, lowestOffsetY, slot);
            if (event.hasDrop()) {
                world.drops().set(entity, new Drop(event.dropId()));
            }
        }
    }

    /**
     * The offset closest to the playfield — the smallest {@code offsetY}, since {@code Transform.y}
     * grows upwards and a lower value means lower on screen. Measuring the spawn height against this
     * one slot, rather than against each slot's own offset, is what keeps every other slot of the
     * same formation above it and therefore also off-screen.
     */
    private static float lowestOffsetY(List<FormationSlot> slots) {
        float lowest = Float.POSITIVE_INFINITY;
        for (FormationSlot slot : slots) {
            lowest = Math.min(lowest, slot.offsetY());
        }
        return lowest;
    }

    private static void attachComponents(World world, EnemyDefinition enemy, int entity) {
        for (ComponentSpec spec : enemy.components()) {
            try {
                FACTORIES.attach(world, entity, spec);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "enemy '" + enemy.id() + "': " + e.getMessage(), e);
            }
        }
    }

    private static void positionSpawned(
        World world, int entity, float anchorX, float lowestOffsetY, FormationSlot slot) {
        Collider collider = world.colliders().get(entity);
        float radius = collider == null ? 0f : collider.radius;
        float x = anchorX + slot.offsetX();
        // The slot at lowestOffsetY lands exactly at PLAYFIELD_HEIGHT + radius — its own radius
        // above the edge, tangent to it. Every other slot is further above by however much its
        // offsetY exceeds lowestOffsetY, so the whole formation clears the edge together.
        float y = PLAYFIELD_HEIGHT + radius + (slot.offsetY() - lowestOffsetY);
        world.transforms().set(entity, new Transform(x, y));
    }
}
