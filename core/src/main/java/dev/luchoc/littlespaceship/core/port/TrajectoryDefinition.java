package dev.luchoc.littlespaceship.core.port;

/**
 * A movement shape, referenced by id from an archetype's {@code "motion"} component or from a
 * {@code SpawnEvent}'s override — a function from an entity's own elapsed time since spawn to a
 * velocity, per {@code docs/plan/11c-movement-shapes/shape-catalogue.md}.
 *
 * <p>Sealed to the two kinds that catalogue decides and refuses the rest, the same reason {@link
 * WaveEndCondition} is sealed: the system evaluating one (issue #164) can switch on which it is
 * without a third, unnamed case ever compiling. {@link SimpleTrajectoryDefinition} is a fixed
 * velocity; {@link ArcTrajectoryDefinition} is a fixed velocity with constant vertical acceleration.
 * Horizontal velocity never varies with time in either kind — the catalogue names no case for
 * horizontal acceleration — so {@link #vx()} alone answers it; only the vertical component is a
 * function of elapsed time, through {@link #verticalVelocityAt(float)}.
 *
 * <p>Kept separate from {@link EnemyDefinition} precisely so it can be shared: the same id attached
 * to two different archetypes' {@code "motion"} spec is what makes "a tank on the super-fast's
 * trajectory" a one-line data change.
 */
public sealed interface TrajectoryDefinition permits SimpleTrajectoryDefinition, ArcTrajectoryDefinition {

    /**
     * @return the content id
     */
    String id();

    /**
     * @return horizontal velocity, in logical units per second, constant for the whole life of an
     *     entity following this shape
     */
    float vx();

    /**
     * @return vertical velocity at elapsed time zero, in logical units per second, positive upwards
     *     like {@code Motion} — equivalent to {@code verticalVelocityAt(0f)}
     */
    float vy();

    /**
     * Evaluates this shape's vertical velocity at a given point in an entity's life. A pure function
     * of {@code elapsedSeconds} and this shape's own parameters — reads nothing else, per the
     * catalogue's first rule, and carries no randomness, per its second.
     *
     * @param elapsedSeconds seconds since the entity carrying this shape spawned, accumulated from
     *     the fixed step so a replay reproduces it exactly; never the system clock
     * @return vertical velocity at that instant, in logical units per second, positive upwards
     */
    float verticalVelocityAt(float elapsedSeconds);
}
