package dev.luchoc.littlespaceship.core.domain.component;

/**
 * Velocity of an entity, in logical units per second.
 *
 * <p>Per second and not per tick: the step is fixed, so the conversion happens once inside the
 * motion system and the balance values stay readable.
 *
 * <p>Trajectories —the curves some enemies follow— are not here yet. They arrive with the system
 * that needs them, which is not this phase.
 */
public final class Motion {

    /** Horizontal velocity. */
    public float vx;

    /** Vertical velocity. */
    public float vy;

    /**
     * Creates a motion component with the given velocity.
     *
     * @param vx horizontal velocity
     * @param vy vertical velocity
     */
    public Motion(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;
    }
}
