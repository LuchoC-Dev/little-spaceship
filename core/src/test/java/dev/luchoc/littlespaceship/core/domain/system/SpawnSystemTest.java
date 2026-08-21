package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.Drop;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.ScoreValue;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.ComponentSpec;
import dev.luchoc.littlespaceship.core.port.FormationSlot;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.MapComponentSpec;
import dev.luchoc.littlespaceship.core.port.SimpleEnemyDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleFormationDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleTrajectoryDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleWaveTimeline;
import dev.luchoc.littlespaceship.core.port.SpawnEvent;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code SpawnSystem} against content built inline, never read from a file, per the acceptance
 * criterion that core tests do not need a real content pipeline to prove the system works.
 */
class SpawnSystemTest {

    private static final String LEVEL = "level-01";

    @Test
    @DisplayName("a wave due this tick spawns, positioned from the anchor and the formation slot")
    void spawnsAWaveWhenItIsDue() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(1, world.colliders().size());
        int entity = world.colliders().entityAt(0);
        Transform transform = world.transforms().get(entity);
        Collider collider = world.colliders().get(entity);
        assertEquals(0.5f * MotionSystem.PLAYFIELD_WIDTH, transform.x);
        assertEquals(SpawnSystem.PLAYFIELD_HEIGHT + collider.radius, transform.y);
    }

    @Test
    @DisplayName("a wave not due yet does not spawn")
    void doesNotSpawnBeforeItsTime() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(10f, "enemy-basic", "single", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(0, world.entityCount());
    }

    @Test
    @DisplayName("the cursor only advances, so a wave never spawns twice")
    void neverSpawnsTheSameWaveTwice() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        for (int i = 0; i < 10; i++) {
            system.update(world, 1f, InputFrame.IDLE);
        }

        assertEquals(1, world.entityCount());
    }

    @Test
    @DisplayName("a stalled tick can fire more than one due wave in the same update")
    void oneTickCanFireSeveralWaves() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(List.of(
                new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null),
                new SpawnEvent(2f, "enemy-basic", "single", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 5f, InputFrame.IDLE);

        assertEquals(2, world.entityCount());
    }

    @Test
    @DisplayName("a formation with several slots spawns one entity per slot, all from the same wave")
    void formationSpawnsOneEntityPerSlot() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("line-3", List.of(
                new FormationSlot(-10f, 0f), new FormationSlot(0f, 0f), new FormationSlot(10f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-basic", "line-3", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(3, world.entityCount());
        float anchorX = 0.5f * MotionSystem.PLAYFIELD_WIDTH;
        List<Float> xs = new java.util.ArrayList<>();
        for (int i = 0; i < world.transforms().size(); i++) {
            xs.add(world.transforms().valueAt(i).x);
        }
        assertTrue(xs.contains(anchorX - 10f));
        assertTrue(xs.contains(anchorX));
        assertTrue(xs.contains(anchorX + 10f));
    }

    @Test
    @DisplayName("a diagonal formation still enters fully off-screen, every slot, not just offsetY 0")
    void diagonalFormationEntersFullyOffScreen() {
        List<FormationSlot> diagonal = List.of(
            new FormationSlot(-15f, 0f), new FormationSlot(0f, -15f), new FormationSlot(15f, -30f));
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("diagonal", diagonal))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-basic", "diagonal", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(3, world.entityCount());
        float radius = world.colliders().valueAt(0).radius;
        for (int i = 0; i < world.transforms().size(); i++) {
            float y = world.transforms().valueAt(i).y;
            float bottomEdge = y - radius;
            assertTrue(bottomEdge >= SpawnSystem.PLAYFIELD_HEIGHT,
                "entity " + i + " has its bottom edge at " + bottomEdge
                    + ", inside the visible playfield (below " + SpawnSystem.PLAYFIELD_HEIGHT + ")");
        }
        // The slot with the lowest offsetY (-30) is the one tangent to the edge; every other slot
        // must be strictly further above it, or the guarantee would only hold for one slot.
        float lowestY = Float.POSITIVE_INFINITY;
        for (int i = 0; i < world.transforms().size(); i++) {
            lowestY = Math.min(lowestY, world.transforms().valueAt(i).y);
        }
        assertEquals(SpawnSystem.PLAYFIELD_HEIGHT + radius, lowestY);
    }

    @Test
    @DisplayName("a wave instance marked with a drop attaches it to every entity it spawns")
    void designedDropAttachesToSpawnedEntities() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, "shield"))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(1, world.drops().size());
        assertEquals("shield", world.drops().valueAt(0).pickupId);
    }

    @Test
    @DisplayName("an unrecognised drop id fails the moment the wave spawns, naming the enemy and id")
    void unrecognisedDropIdFailsAtSpawnTime() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, "typo-shiled"))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> system.update(world, 1f, InputFrame.IDLE));

        assertTrue(e.getMessage().contains("enemy-basic"));
        assertTrue(e.getMessage().contains("typo-shiled"));
        // Failing before any entity of the wave is created is what keeps this a load-time-shaped
        // failure rather than a half-spawned wave left behind for something else to trip over.
        assertEquals(0, world.entityCount());
    }

    @Test
    @DisplayName("every one of PickupSystem's six recognised kinds spawns a wave without failing")
    void everyRecognisedKindIsAccepted() {
        for (String kind : List.of(PickupSystem.KIND_WEAPON_UPGRADE, PickupSystem.KIND_SHIELD,
            PickupSystem.KIND_EXTRA_LIFE, PickupSystem.KIND_BOMB_RECHARGE,
            PickupSystem.KIND_INVULNERABILITY, PickupSystem.KIND_ATTACHMENT)) {
            TestContent content = baseContent()
                .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
                .withTimeline(LEVEL, new SimpleWaveTimeline(
                    List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, kind))));
            World world = worldOf(content);
            SpawnSystem system = new SpawnSystem(LEVEL);

            system.update(world, 1f, InputFrame.IDLE);

            assertEquals(kind, world.drops().valueAt(0).pickupId);
        }
    }

    @Test
    @DisplayName("a wave instance with no drop leaves the spawned entity without one")
    void noDropMeansNoDropComponent() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(0, world.drops().size());
    }

    @Test
    @DisplayName("the same trajectory attaches to two different archetypes from data alone")
    void sameTrajectoryReusedAcrossArchetypes() {
        ComponentSpec sharedMotion = new MapComponentSpec("motion", Map.of("trajectory", "dive"));
        TestContent content = new TestContent()
            .withTrajectory(new SimpleTrajectoryDefinition("dive", 0f, -80f))
            .withEnemy(new SimpleEnemyDefinition("enemy-rush", List.of(
                sharedMotion, spriteSpec("enemy-rush"), colliderSpec(4.0f, true))))
            .withEnemy(new SimpleEnemyDefinition("enemy-tank", List.of(
                sharedMotion, spriteSpec("enemy-tank"), colliderSpec(10.5f, false))))
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(List.of(
                new SpawnEvent(1f, "enemy-rush", "single", 0.5f, null),
                new SpawnEvent(2f, "enemy-tank", "single", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 2f, InputFrame.IDLE);

        assertEquals(2, world.motions().size());
        for (int i = 0; i < world.motions().size(); i++) {
            Motion motion = world.motions().valueAt(i);
            assertEquals(0f, motion.vx);
            assertEquals(-80f, motion.vy);
        }
    }

    @Test
    @DisplayName("an unknown enemy id in the timeline fails naming that id")
    void unknownEnemyIdFailsWithMessage() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-ghost", "single", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> system.update(world, 1f, InputFrame.IDLE));
        assertTrue(e.getMessage().contains("enemy-ghost"));
    }

    @Test
    @DisplayName("a malformed component in an archetype fails naming the archetype and the field")
    void malformedComponentNamesArchetypeAndField() {
        TestContent content = new TestContent()
            .withEnemy(new SimpleEnemyDefinition("enemy-basic",
                List.of(new MapComponentSpec("collider", Map.of()))))
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null))));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> system.update(world, 1f, InputFrame.IDLE));
        assertTrue(e.getMessage().contains("enemy-basic"));
        assertTrue(e.getMessage().contains("radius"));
    }

    @Test
    @DisplayName("rejects being built without a level id")
    void rejectsMissingLevelId() {
        assertThrows(IllegalArgumentException.class, () -> new SpawnSystem(null));
        assertThrows(IllegalArgumentException.class, () -> new SpawnSystem(""));
    }

    private static TestContent baseContent() {
        return new TestContent()
            .withEnemy(new SimpleEnemyDefinition("enemy-basic", List.of(
                spriteSpec("enemy-basic"), colliderSpec(5.5f, true), scoreSpec(100f))));
    }

    private static ComponentSpec spriteSpec(String id) {
        return new MapComponentSpec("sprite", Map.of("id", id));
    }

    private static ComponentSpec colliderSpec(float radius, boolean fragile) {
        return new MapComponentSpec("collider", Map.of("radius", radius, "fragile", fragile));
    }

    private static ComponentSpec scoreSpec(float points) {
        return new MapComponentSpec("scoreValue", Map.of("points", points));
    }

    private static World worldOf(TestContent content) {
        return new World(content, new Rng(1), new GameEventQueue());
    }
}
