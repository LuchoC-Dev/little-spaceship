package dev.luchoc.littlespaceship.core.port;

/**
 * A reusable velocity, referenced by id from an archetype's {@code "motion"} component.
 *
 * <p>The MVP's trajectories are constant velocities, not curves: {@code MotionSystem} already
 * integrates whatever {@code Motion} an entity holds, so a trajectory only has to supply that vector.
 * A curved or waypoint-following trajectory is a real future need, but nothing in the level 1 design
 * asks for one yet, and guessing that shape now risks getting it wrong before a concrete case exists
 * to check it against.
 *
 * <p>Kept separate from {@link EnemyDefinition} precisely so it can be shared: the same id attached
 * to two different archetypes' {@code "motion"} spec is what makes "a tank on the super-fast's
 * trajectory" a one-line data change.
 */
public interface TrajectoryDefinition {

    /**
     * @return the content id
     */
    String id();

    /**
     * @return horizontal velocity, in logical units per second
     */
    float vx();

    /**
     * @return vertical velocity, in logical units per second, positive upwards like {@code Motion}
     */
    float vy();
}
