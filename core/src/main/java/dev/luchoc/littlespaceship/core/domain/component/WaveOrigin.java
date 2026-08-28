package dev.luchoc.littlespaceship.core.domain.component;

/**
 * Which wave spawned this entity, needed to answer a "cleared" wave's end condition: a wave is not
 * cleared until every entity carrying its id has been destroyed or has left the playfield.
 *
 * <p>Attached by {@code SpawnSystem} to every entity a wave's own {@code spawnWave} call creates, and
 * copied — never recomputed — onto every child {@code SpawnerSystem} spawns from a carrier that
 * itself carries one. That copy is the carrier-children rule the project owner decided on 28/08/2026,
 * recorded in {@code docs/planning/08-decisions-and-open-items.md}: a child inherits its parent's
 * wave, because what the player reads on screen is "the wave's encounter is still there", not
 * "the entity that originally carried it is still there". A carrier with no {@link WaveOrigin} of
 * its own — one built outside a wave, such as by a test — simply produces children with none either.
 *
 * <p>{@link #waveId} is an integer, one per call to {@code SpawnSystem.spawnWave}, not yet the string
 * id a named wave in {@code waves.json} will carry — that content contract is a later task of this
 * same phase. Nothing reads this component yet; the "cleared" end condition is a different task.
 */
public final class WaveOrigin {

    /** Identifies the wave instance that spawned this entity, directly or through a carrier. */
    public final int waveId;

    /**
     * @param waveId identifier of the spawning wave instance
     */
    public WaveOrigin(int waveId) {
        this.waveId = waveId;
    }
}
