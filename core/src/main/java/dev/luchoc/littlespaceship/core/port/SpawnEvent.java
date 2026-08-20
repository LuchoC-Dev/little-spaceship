package dev.luchoc.littlespaceship.core.port;

/**
 * One timestamped entry of a {@link WaveTimeline} — the intensity curve in executable form.
 *
 * @param at seconds since the level started, when this wave spawns
 * @param enemyId the {@link EnemyDefinition} to spawn
 * @param formationId the {@link FormationDefinition} that lays it out
 * @param atX the anchor's horizontal position, a fraction of the playfield width in {@code [0, 1]},
 *     0 at the left edge and 1 at the right
 * @param dropId the pickup this specific wave instance delivers, or {@code null}/empty for none —
 *     a designed drop is a property of the event, never of the archetype, per
 *     {@code 03-game-systems.md}
 */
public record SpawnEvent(float at, String enemyId, String formationId, float atX, String dropId) {

    /**
     * Rejects a malformed event: a negative timestamp, a missing archetype or formation, or an
     * anchor outside the playfield.
     */
    public SpawnEvent {
        if (at < 0f || Float.isNaN(at) || Float.isInfinite(at)) {
            throw new IllegalArgumentException("a spawn event needs a finite, non-negative timestamp");
        }
        if (enemyId == null || enemyId.isEmpty()) {
            throw new IllegalArgumentException("a spawn event at " + at + "s needs an enemy id");
        }
        if (formationId == null || formationId.isEmpty()) {
            throw new IllegalArgumentException("a spawn event at " + at + "s needs a formation id");
        }
        if (atX < 0f || atX > 1f || Float.isNaN(atX)) {
            throw new IllegalArgumentException(
                "spawn event at " + at + "s has atX " + atX + ", outside [0, 1]");
        }
    }

    /**
     * @return true when this instance is marked to drop something on defeat
     */
    public boolean hasDrop() {
        return dropId != null && !dropId.isEmpty();
    }
}
