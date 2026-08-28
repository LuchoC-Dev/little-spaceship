package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.ComponentSpec;
import dev.luchoc.littlespaceship.core.port.FormationSlot;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.LevelOutcome;
import dev.luchoc.littlespaceship.core.port.MapComponentSpec;
import dev.luchoc.littlespaceship.core.port.SimpleEnemyDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleFormationDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleTrajectoryDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleWaveDefinition;
import dev.luchoc.littlespaceship.core.port.SpawnEvent;
import dev.luchoc.littlespaceship.core.port.WaveEndCondition;
import dev.luchoc.littlespaceship.core.port.WavePlacement;
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
    private static final String WAVE = "level-01-wave";

    @Test
    @DisplayName("a wave due this tick spawns, positioned from the anchor and the formation slot")
    void spawnsAWaveWhenItIsDue() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null)));
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
            .withSingleWave(LEVEL, List.of(new SpawnEvent(10f, "enemy-basic", "single", 0.5f, null)));
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
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null)));
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
            .withSingleWave(LEVEL, List.of(
                new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null),
                new SpawnEvent(2f, "enemy-basic", "single", 0.5f, null)));
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
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "line-3", 0.5f, null)));
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
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "diagonal", 0.5f, null)));
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
    @DisplayName("a wave instance marked with a drop attaches it to its single-slot formation")
    void designedDropAttachesToSpawnedEntities() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, "shield")));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(1, world.drops().size());
        assertEquals("shield", world.drops().valueAt(0).pickupId);
    }

    @Test
    @DisplayName("a drop on a multi-slot formation attaches to exactly the slot named, not every slot")
    void designedDropAttachesToExactlyOneSlot() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("pair",
                List.of(new FormationSlot(-20f, 0f), new FormationSlot(20f, 0f))))
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "pair", 0.5f, "attachment", 1)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(2, world.colliders().size());
        assertEquals(1, world.drops().size());
        int dropped = world.drops().entityAt(0);
        assertEquals(20f + 0.5f * MotionSystem.PLAYFIELD_WIDTH, world.transforms().get(dropped).x);
    }

    @Test
    @DisplayName("a drop naming a slot outside its formation fails at spawn time")
    void dropSlotOutsideFormationFails() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, "shield", 1)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> system.update(world, 1f, InputFrame.IDLE));
        assertTrue(e.getMessage().contains("single"));
    }

    @Test
    @DisplayName("an unrecognised drop id fails the moment the wave spawns, naming the enemy and id")
    void unrecognisedDropIdFailsAtSpawnTime() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, "typo-shiled")));
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
                .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, kind)));
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
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null)));
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
            .withWave(new SimpleWaveDefinition(WAVE, List.of(
                new SpawnEvent(1f, "enemy-rush", "single", 0.5f, null),
                new SpawnEvent(2f, "enemy-tank", "single", 0.5f, null)),
                new WaveEndCondition.FixedDuration(20f)))
            .withSingleWavePlacement(LEVEL, WAVE);
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
    @DisplayName("an unknown enemy id in the wave fails naming that id")
    void unknownEnemyIdFailsWithMessage() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-ghost", "single", 0.5f, null)));
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
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> system.update(world, 1f, InputFrame.IDLE));
        assertTrue(e.getMessage().contains("enemy-basic"));
        assertTrue(e.getMessage().contains("radius"));
    }

    @Test
    @DisplayName("once every placement is exhausted and the spawned enemy is destroyed, the run completes")
    void completesOnceTheTimelineIsExhaustedAndNothingIsAlive() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withWave(new SimpleWaveDefinition(WAVE,
                List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null)),
                new WaveEndCondition.FixedDuration(1.5f)))
            .withSingleWavePlacement(LEVEL, WAVE);
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);
        int enemy = world.colliders().entityAt(0);
        world.destroyEntity(enemy);
        // The FixedDuration wave itself must end before the level counts as exhausted, not just the
        // spawn of its last event — otherwise this test would pass even if SpawnSystem forgot to
        // resolve end conditions at all.
        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(LevelOutcome.COMPLETED, world.view().outcome());
    }

    @Test
    @DisplayName("the run is not complete while an enemy the timeline spawned is still alive")
    void notCompleteWhileASpawnedEnemySurvives() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "single", 0.5f, null)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(LevelOutcome.IN_PROGRESS, world.view().outcome());
    }

    @Test
    @DisplayName("every entity a wave spawns carries a WaveOrigin naming that wave's content id")
    void spawnedEntitiesCarryAWaveOrigin() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("pair",
                List.of(new FormationSlot(-10f, 0f), new FormationSlot(10f, 0f))))
            .withSingleWave(LEVEL, List.of(new SpawnEvent(1f, "enemy-basic", "pair", 0.5f, null)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(2, world.waveOrigins().size());
        String firstWaveId = world.waveOrigins().valueAt(0).waveId;
        assertEquals(WAVE, firstWaveId);
        assertEquals(firstWaveId, world.waveOrigins().valueAt(1).waveId);
    }

    @Test
    @DisplayName("two different placements tag their entities with different wave ids")
    void differentWavesGetDifferentIds() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withWave(new SimpleWaveDefinition("wave-a",
                List.of(new SpawnEvent(0f, "enemy-basic", "single", 0.5f, null)),
                new WaveEndCondition.FixedDuration(1f)))
            .withWave(new SimpleWaveDefinition("wave-b",
                List.of(new SpawnEvent(0f, "enemy-basic", "single", 0.5f, null)),
                new WaveEndCondition.FixedDuration(1f)))
            .withPlacements(LEVEL, List.of(
                new WavePlacement("wave-a", 0f),
                new WavePlacement("wave-b", 0f)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        // wave-a starts at t=0 and spawns immediately; wave-b is only scheduled once wave-a's own
        // FixedDuration(1s) has elapsed. Two ticks give both waves ample room to have run.
        system.update(world, 1f, InputFrame.IDLE);
        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(2, world.waveOrigins().size());
        String first = world.waveOrigins().valueAt(0).waveId;
        String second = world.waveOrigins().valueAt(1).waveId;
        assertTrue(!first.equals(second));
    }

    @Test
    @DisplayName("a FixedDuration wave schedules the next placement exactly at its own end, not before or after")
    void fixedDurationEndsExactlyAtItsOwnDuration() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withWave(new SimpleWaveDefinition("wave-a",
                List.of(new SpawnEvent(0f, "enemy-basic", "single", 0.5f, null)),
                new WaveEndCondition.FixedDuration(2f)))
            .withWave(new SimpleWaveDefinition("wave-b",
                List.of(new SpawnEvent(0f, "enemy-basic", "single", 0.5f, null)),
                new WaveEndCondition.FixedDuration(2f)))
            .withPlacements(LEVEL, List.of(
                new WavePlacement("wave-a", 0f),
                new WavePlacement("wave-b", 0f)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        // wave-a spawns at t=0 (levelTime 1 after this tick); its FixedDuration(2s) has not yet
        // elapsed, so wave-b must not have started.
        system.update(world, 1f, InputFrame.IDLE);
        assertEquals(1, world.entityCount());

        // levelTime reaches 2s exactly here: wave-a ends and wave-b is scheduled and spawns the
        // very same tick, since its own offset is zero.
        system.update(world, 1f, InputFrame.IDLE);
        assertEquals(2, world.entityCount());
    }

    @Test
    @DisplayName("a Cleared wave does not end while an entity it spawned is still alive")
    void clearedWaveWaitsForEveryEntityToBeGone() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withWave(new SimpleWaveDefinition("wave-a",
                List.of(new SpawnEvent(0f, "enemy-basic", "single", 0.5f, null)),
                new WaveEndCondition.Cleared()))
            .withWave(new SimpleWaveDefinition("wave-b",
                List.of(new SpawnEvent(0f, "enemy-basic", "single", 0.5f, null)),
                new WaveEndCondition.FixedDuration(1f)))
            .withPlacements(LEVEL, List.of(
                new WavePlacement("wave-a", 0f),
                new WavePlacement("wave-b", 0f)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);
        assertEquals(1, world.entityCount());

        // wave-a's single entity is still alive: many more ticks must not schedule wave-b.
        for (int i = 0; i < 20; i++) {
            system.update(world, 1f, InputFrame.IDLE);
        }
        assertEquals(1, world.entityCount());

        // Destroying the last entity of wave-a lets it be resolved as cleared on the *next* tick,
        // per SystemOrder.SPAWN's own one-tick-late documentation, and only then is wave-b scheduled.
        int survivor = world.colliders().entityAt(0);
        world.destroyEntity(survivor);
        system.update(world, 1f, InputFrame.IDLE);

        assertEquals(1, world.entityCount());
        assertEquals("wave-b", world.waveOrigins().valueAt(0).waveId);
    }

    @Test
    @DisplayName("a negative offset genuinely overlaps two waves in the same tick")
    void negativeOffsetOverlapsTwoWaves() {
        TestContent content = baseContent()
            .withFormation(new SimpleFormationDefinition("pair",
                List.of(new FormationSlot(-10f, 0f), new FormationSlot(10f, 0f))))
            .withWave(new SimpleWaveDefinition("wave-a",
                List.of(new SpawnEvent(0f, "enemy-basic", "pair", 0.5f, null)),
                new WaveEndCondition.FixedDuration(1f)))
            .withWave(new SimpleWaveDefinition("wave-b",
                List.of(new SpawnEvent(0f, "enemy-basic", "pair", 0.5f, null)),
                new WaveEndCondition.FixedDuration(1f)))
            .withPlacements(LEVEL, List.of(
                new WavePlacement("wave-a", 0f),
                // wave-a ends at levelTime 1s; a raw -1s offset would compute a start of 0s, before
                // wave-a's own end is even detected, so scheduleNext clamps it forward to the
                // detecting tick's levelTime (1s) instead of spawning into the past. Both waves'
                // entities still land in this very same tick, genuinely overlapping rather than
                // merely abutting the way a zero or positive offset would.
                new WavePlacement("wave-b", -1f)));
        World world = worldOf(content);
        SpawnSystem system = new SpawnSystem(LEVEL);

        system.update(world, 1f, InputFrame.IDLE);

        // Both waves' entities exist after the single tick that resolves wave-a's end: wave-a's own
        // pair, plus wave-b's pair spawned immediately since its clamped start already reached now.
        assertEquals(4, world.entityCount());
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < world.waveOrigins().size(); i++) {
            ids.add(world.waveOrigins().valueAt(i).waveId);
        }
        assertEquals(java.util.Set.of("wave-a", "wave-b"), ids);
    }

    @Test
    @DisplayName("moving a placement earlier in the level changes no other placement's own offset")
    void movingAPlacementEarlierChangesNoOtherOffset() {
        WavePlacement untouched = new WavePlacement("wave-b", 3f);
        List<WavePlacement> originalOrder = List.of(new WavePlacement("wave-a", 0f), untouched);
        List<WavePlacement> earlierFirst = List.of(new WavePlacement("wave-a", -5f), untouched);

        // The second placement's own declaration — its wave id and its offset from whichever comes
        // before it — is identical in both lists; only the first placement changed.
        assertEquals(untouched, originalOrder.get(1));
        assertEquals(untouched, earlierFirst.get(1));
        assertEquals(originalOrder.get(1).waveId(), earlierFirst.get(1).waveId());
        assertEquals(originalOrder.get(1).offsetSeconds(), earlierFirst.get(1).offsetSeconds());
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
