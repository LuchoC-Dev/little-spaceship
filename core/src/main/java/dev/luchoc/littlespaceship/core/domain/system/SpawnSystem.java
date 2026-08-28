package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Drop;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.component.WaveOrigin;
import dev.luchoc.littlespaceship.core.domain.content.ComponentFactoryRegistry;
import dev.luchoc.littlespaceship.core.port.ComponentSpec;
import dev.luchoc.littlespaceship.core.port.EnemyDefinition;
import dev.luchoc.littlespaceship.core.port.FormationDefinition;
import dev.luchoc.littlespaceship.core.port.FormationSlot;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.SpawnEvent;
import dev.luchoc.littlespaceship.core.port.WaveDefinition;
import dev.luchoc.littlespaceship.core.port.WaveEndCondition;
import dev.luchoc.littlespaceship.core.port.WavePlacement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Advances a level's ordered {@link WavePlacement} sequence and spawns what is due — the intensity
 * curve turned into entities.
 *
 * <p>Unlike most other systems, this one is not stateless: it holds the level id it was built for,
 * how much level time has elapsed, and every {@link ActiveWave} currently spawning or waiting on its
 * end condition. That is a deliberate exception to the pattern the other systems keep, not an
 * oversight — a fresh {@code Simulation} always builds a fresh {@code SpawnSystem}, so the state is
 * exactly as reproducible as everything else the composition root creates once per run.
 *
 * <p><b>How a level's placements resolve into running waves.</b> {@link WavePlacement#offsetSeconds()}
 * is relative to the end of the placement before it, so the next placement can only be scheduled once
 * the current one's end time is known. For a {@link WaveEndCondition.FixedDuration} wave that end
 * time is deterministic — {@code startTime + seconds} — but it is still only <em>acted on</em> the
 * first tick this system observes {@code levelTime} has reached it, exactly the same one-tick
 * granularity every fixed-step system already has. For a {@link WaveEndCondition.Cleared} wave the end
 * time cannot be known until it actually happens: {@code CLEANUP}, the stage that removes a spawned
 * entity, runs after {@code SPAWN} in the very same tick, so a wave clearing on this tick is only
 * visible to this system on the <em>next</em> one — {@code SPAWN} stays fifth in {@link SystemOrder},
 * and a {@code cleared} wave resolves one tick late as a direct, deterministic consequence of that
 * order, not a bug. Both cases feed the same mechanism: whichever tick this system first observes a
 * wave has ended, that tick's {@code levelTime} becomes the "end of the placement before it" the next
 * one in the sequence is offset from. A negative offset can therefore place the next wave's start
 * before that detection tick; {@link #scheduleNext} clamps it forward to {@code levelTime} rather than
 * retroactively spawning anything in the past.
 *
 * <p>Several waves can be active at once — that is what a negative offset is for — so every tick
 * first lets every active wave spawn whatever of its own {@link WaveDefinition#spawns()} is now due,
 * then checks every active wave's end condition and schedules the placement after any that just
 * ended. Because scheduling a new wave can itself make it immediately due this same tick (a
 * clamped-to-now start with a spawn at its own time zero), that two-phase step repeats until nothing
 * changes, bounded by the level's own placement count.
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
 *
 * <p>Every entity a wave spawns also carries a {@link WaveOrigin}, tagged with the {@link
 * WaveDefinition#id()} of the wave that spawned it — the string content id, not a synthetic per-call
 * counter, which is what lets {@link WaveEndCondition.Cleared} ask "does any entity still carry this
 * wave's id" and get a real answer even when the same wave id is placed twice in a level.
 * {@code SpawnerSystem} copies this tag onto the children a carrier spawns, per the carrier-children
 * rule in {@code docs/planning/08-decisions-and-open-items.md}.
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

    private final List<ActiveWave> activeWaves = new ArrayList<>();
    private List<WavePlacement> placements;
    private int nextPlacementIndex;
    private float levelTime;
    private boolean started;

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
        if (!started) {
            // Scheduled at levelTime 0, before this tick's own step is added — the level's very
            // first placement starts at "the beginning", the same instant the old flat-cursor system
            // began at, not one step late. Scheduling it after the increment below would clamp its
            // start forward to this tick's levelTime and push every one of its spawns back by one
            // step, which is not what "clamp forward on late detection" in scheduleNext is for.
            started = true;
            placements = world.content().placements(levelId);
            ActiveWave first = scheduleNext(world, 0f);
            if (first != null) {
                activeWaves.add(first);
            }
        }
        levelTime += step;

        boolean progressed;
        do {
            for (ActiveWave wave : activeWaves) {
                spawnDue(world, wave);
            }
            progressed = resolveEnded(world);
        } while (progressed);

        if (nextPlacementIndex >= placements.size() && activeWaves.isEmpty()) {
            // Marks every tick from here on, not only the one where the level's last wave first
            // ended — cheaper than a one-shot guard and just as correct, since World.outcome() only
            // ever reads this as a level, not an edge.
            world.markWaveTimelineExhausted();
        }
    }

    /**
     * Removes every active wave whose end condition is true this tick and schedules the placement
     * after each, returning whether at least one did. Collects newly scheduled waves separately and
     * adds them only once the removal pass is over, since {@link #activeWaves} cannot be both
     * iterated and appended to at the same time.
     */
    private boolean resolveEnded(World world) {
        List<ActiveWave> scheduled = null;
        boolean anyEnded = false;
        Iterator<ActiveWave> it = activeWaves.iterator();
        while (it.hasNext()) {
            ActiveWave wave = it.next();
            if (!hasEnded(world, wave)) {
                continue;
            }
            it.remove();
            anyEnded = true;
            ActiveWave next = scheduleNext(world, levelTime);
            if (next != null) {
                if (scheduled == null) {
                    scheduled = new ArrayList<>();
                }
                scheduled.add(next);
            }
        }
        if (scheduled != null) {
            activeWaves.addAll(scheduled);
        }
        return anyEnded;
    }

    /**
     * Resolves the next placement of the level, if any remain, into a running {@link ActiveWave}
     * started at {@code previousEndTime + offsetSeconds}, clamped forward to {@link #levelTime} so a
     * negative offset following a {@link WaveEndCondition.Cleared} wave — whose true end time is only
     * ever discovered after the fact — never schedules a start in the past.
     *
     * @param previousEndTime the level time the placement before this one ended, or {@code 0f} for
     *     the level's very first placement
     * @return the new active wave, or {@code null} when the level has no more placements
     */
    private ActiveWave scheduleNext(World world, float previousEndTime) {
        if (nextPlacementIndex >= placements.size()) {
            return null;
        }
        WavePlacement placement = placements.get(nextPlacementIndex);
        nextPlacementIndex++;
        float start = Math.max(previousEndTime + placement.offsetSeconds(), levelTime);
        WaveDefinition definition = world.content().wave(placement.waveId());
        return new ActiveWave(definition, start);
    }

    private void spawnDue(World world, ActiveWave wave) {
        List<SpawnEvent> events = wave.definition.spawns();
        float localTime = levelTime - wave.startTime;
        while (wave.cursor < events.size() && events.get(wave.cursor).at() <= localTime) {
            spawnWave(world, events.get(wave.cursor), wave.definition.id());
            wave.cursor++;
        }
    }

    /**
     * @return true once {@code wave}'s own end condition is satisfied at the current {@link
     *     #levelTime}
     */
    private boolean hasEnded(World world, ActiveWave wave) {
        WaveEndCondition condition = wave.definition.endCondition();
        if (condition instanceof WaveEndCondition.FixedDuration duration) {
            return levelTime - wave.startTime >= duration.seconds();
        }
        if (condition instanceof WaveEndCondition.Cleared) {
            return wave.cursor >= wave.definition.spawns().size()
                && noEntityCarries(world, wave.definition.id());
        }
        // WaveEndCondition is sealed to exactly these two, per its own class javadoc.
        throw new IllegalStateException("unknown wave end condition: " + condition);
    }

    /**
     * @return true when no live entity's {@link WaveOrigin} names {@code waveId} — a wave's own
     *     spawns and, per the carrier-children rule, every child a carrier of theirs produced
     */
    private static boolean noEntityCarries(World world, String waveId) {
        ComponentStore<WaveOrigin> origins = world.waveOrigins();
        for (int i = 0; i < origins.size(); i++) {
            if (origins.valueAt(i).waveId.equals(waveId)) {
                return false;
            }
        }
        return true;
    }

    private static void spawnWave(World world, SpawnEvent event, String waveId) {
        EnemyDefinition enemy = world.content().enemy(event.enemyId());
        FormationDefinition formation = world.content().formation(event.formationId());
        if (event.hasDrop()) {
            requireRecognisedDrop(enemy, event);
            requireSlotInRange(formation, event);
        }
        float anchorX = event.atX() * MotionSystem.PLAYFIELD_WIDTH;
        float lowestOffsetY = lowestOffsetY(formation.slots());

        List<FormationSlot> slots = formation.slots();
        for (int i = 0; i < slots.size(); i++) {
            FormationSlot slot = slots.get(i);
            int entity = world.createEntity();
            attachComponents(world, enemy, entity);
            positionSpawned(world, entity, anchorX, lowestOffsetY, slot);
            world.waveOrigins().set(entity, new WaveOrigin(waveId));
            if (event.hasDrop() && i == event.dropSlot()) {
                world.drops().set(entity, new Drop(event.dropId()));
            }
        }
    }

    /**
     * Fails the moment a wave's drop names a slot its own formation does not have, instead of the
     * drop silently landing on no entity at all — the same reasoning as {@link
     * #requireRecognisedDrop}, applied to issue #23's fix: a designed drop is tied to one specific
     * slot, and a typo in which one should not go unnoticed until someone wonders why an encounter
     * gave up nothing.
     */
    private static void requireSlotInRange(FormationDefinition formation, SpawnEvent event) {
        if (event.dropSlot() >= formation.slots().size()) {
            throw new IllegalArgumentException(
                "spawn event at " + event.at() + "s drops into slot " + event.dropSlot()
                    + " of formation '" + formation.id() + "', which only has "
                    + formation.slots().size() + " slot(s)");
        }
    }

    /**
     * Fails the moment a wave carrying an unrecognised drop id spawns, instead of only when a
     * player reaches the pickup that {@code Drop} eventually produces. {@code PickupSystem} is the
     * single place that decides which kinds are real; this only asks it, rather than keeping a
     * second list of the same six strings that could drift from the one {@code PickupSystem}
     * resolves against.
     */
    private static void requireRecognisedDrop(EnemyDefinition enemy, SpawnEvent event) {
        if (!PickupSystem.isRecognisedKind(event.dropId())) {
            throw new IllegalArgumentException(
                "enemy '" + enemy.id() + "' at " + event.at() + "s drops an unrecognised kind '"
                    + event.dropId() + "'");
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

    /**
     * One placement's wave, currently running: spawning what is due, or already past its last spawn
     * and only waiting on {@link WaveDefinition#endCondition()} to become true.
     */
    private static final class ActiveWave {

        final WaveDefinition definition;
        final float startTime;
        int cursor;

        ActiveWave(WaveDefinition definition, float startTime) {
            this.definition = definition;
            this.startTime = startTime;
        }
    }
}
