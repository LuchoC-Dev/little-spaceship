package dev.luchoc.littlespaceship.core.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The simple content contracts fail loudly on malformed data instead of letting a null or an empty
 * collection surface as a {@code NullPointerException} somewhere else later.
 */
class ContentDefinitionsTest {

    @Test
    @DisplayName("an enemy definition needs an id and at least one component")
    void enemyDefinitionValidates() {
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleEnemyDefinition("", List.of(basicSprite())));
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleEnemyDefinition("enemy-basic", List.of()));
    }

    @Test
    @DisplayName("a trajectory needs an id")
    void trajectoryValidates() {
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleTrajectoryDefinition("", 0f, -20f));
    }

    @Test
    @DisplayName("a formation needs an id and at least one slot")
    void formationValidates() {
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleFormationDefinition("", List.of(new FormationSlot(0f, 0f))));
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleFormationDefinition("single", List.of()));
    }

    @Test
    @DisplayName("a spawn event rejects a negative timestamp, a missing id and an anchor outside [0,1]")
    void spawnEventValidates() {
        assertThrows(IllegalArgumentException.class,
            () -> new SpawnEvent(-1f, "enemy-basic", "single", 0.5f, null));
        assertThrows(IllegalArgumentException.class,
            () -> new SpawnEvent(1f, "", "single", 0.5f, null));
        assertThrows(IllegalArgumentException.class,
            () -> new SpawnEvent(1f, "enemy-basic", "", 0.5f, null));
        assertThrows(IllegalArgumentException.class,
            () -> new SpawnEvent(1f, "enemy-basic", "single", 1.5f, null));
    }

    @Test
    @DisplayName("hasDrop is false for a null or empty drop id, true otherwise")
    void hasDropReadsTheDropId() {
        assertTrue(new SpawnEvent(1f, "enemy-basic", "single", 0f, "shield").hasDrop());
        assertEqualsFalse(new SpawnEvent(1f, "enemy-basic", "single", 0f, null).hasDrop());
        assertEqualsFalse(new SpawnEvent(1f, "enemy-basic", "single", 0f, "").hasDrop());
    }

    @Test
    @DisplayName("a wave timeline rejects an empty list and one out of order")
    void waveTimelineValidates() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleWaveTimeline(List.of()));

        List<SpawnEvent> outOfOrder = List.of(
            new SpawnEvent(10f, "enemy-basic", "single", 0.5f, null),
            new SpawnEvent(5f, "enemy-light", "single", 0.5f, null));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new SimpleWaveTimeline(outOfOrder));
        assertTrue(e.getMessage().contains("1"), "should name the offending index");
    }

    @Test
    @DisplayName("an attachment needs an id and a strictly positive durability")
    void attachmentDefinitionValidates() {
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleAttachmentDefinition("", 1));
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleAttachmentDefinition("attachment", 0));
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleAttachmentDefinition("attachment", -1));
    }

    @Test
    @DisplayName("a sorted timeline is accepted as-is")
    void sortedTimelineIsAccepted() {
        List<SpawnEvent> sorted = List.of(
            new SpawnEvent(5f, "enemy-basic", "single", 0.5f, null),
            new SpawnEvent(10f, "enemy-light", "single", 0.5f, null));

        WaveTimeline timeline = new SimpleWaveTimeline(sorted);

        assertEquals(2, timeline.events().size());
    }

    @Test
    @DisplayName("a fixed wave duration rejects zero, negative, NaN and infinite durations")
    void fixedDurationValidates() {
        assertThrows(IllegalArgumentException.class, () -> new WaveEndCondition.FixedDuration(0f));
        assertThrows(IllegalArgumentException.class, () -> new WaveEndCondition.FixedDuration(-1f));
        assertThrows(IllegalArgumentException.class,
            () -> new WaveEndCondition.FixedDuration(Float.NaN));
        assertThrows(IllegalArgumentException.class,
            () -> new WaveEndCondition.FixedDuration(Float.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("a wave definition needs an id, at least one spawn, spawns in order and an end condition")
    void waveDefinitionValidates() {
        List<SpawnEvent> oneSpawn = List.of(new SpawnEvent(0f, "enemy-basic", "single", 0.5f, null));
        WaveEndCondition fixed = new WaveEndCondition.FixedDuration(3f);

        assertThrows(IllegalArgumentException.class,
            () -> new SimpleWaveDefinition("", oneSpawn, fixed, 0f));
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleWaveDefinition("wave-1", List.of(), fixed, 0f));
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleWaveDefinition("wave-1", oneSpawn, null, 0f));
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleWaveDefinition("wave-1", oneSpawn, fixed, Float.NaN));

        List<SpawnEvent> outOfOrder = List.of(
            new SpawnEvent(2f, "enemy-basic", "single", 0.5f, null),
            new SpawnEvent(1f, "enemy-light", "single", 0.5f, null));
        assertThrows(IllegalArgumentException.class,
            () -> new SimpleWaveDefinition("wave-1", outOfOrder, fixed, 0f));
    }

    @Test
    @DisplayName("a wave accepts a negative offset, to overlap the wave before it")
    void waveDefinitionAcceptsNegativeOffset() {
        List<SpawnEvent> oneSpawn = List.of(new SpawnEvent(0f, "enemy-basic", "single", 0.5f, null));
        WaveDefinition wave =
            new SimpleWaveDefinition("wave-1", oneSpawn, new WaveEndCondition.Cleared(), -2f);

        assertEquals(-2f, wave.offsetSeconds());
        assertEquals(1, wave.spawns().size());
    }

    private static void assertEqualsFalse(boolean value) {
        assertEquals(false, value);
    }

    private static ComponentSpec basicSprite() {
        return new MapComponentSpec("sprite", java.util.Map.of("id", "enemy-basic"));
    }
}
