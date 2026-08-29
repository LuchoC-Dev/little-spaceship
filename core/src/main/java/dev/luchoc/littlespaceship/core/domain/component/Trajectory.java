package dev.luchoc.littlespaceship.core.domain.component;

/**
 * Per-entity state a movement shape resolves against: how long this entity has existed, and where it
 * started.
 *
 * <p>A shape — a U-shaped attack run, a diagonal, a curve — is a function of an entity's own elapsed
 * time, not of the clock and not of any other entity. {@link #elapsed} is that input, accumulated
 * from the fixed step by {@code MotionSystem} every {@code MOTION} tick, never read from the system
 * clock. {@link #originX} and {@link #originY} are the {@code Transform} this entity was placed at,
 * captured once and never touched again: a shape shaped like "loop back towards where you came from"
 * needs a fixed point to measure against, and the entity's current {@code Transform} cannot be that
 * point once the shape has started moving it.
 *
 * <p>This component only holds state; it resolves nothing. Evaluating {@link #elapsed} and the origin
 * into a velocity is a named shape's job, which is content this phase does not yet define — see
 * {@code docs/plan/11c-movement-shapes/plan.md}, tasks 2 and 3. An entity with no {@link Trajectory}
 * simply keeps whatever constant {@code Motion} its trajectory gave it at spawn, exactly as before
 * this component existed.
 */
public final class Trajectory {

    /** Horizontal position this entity's {@code Transform} was set to when it was placed. */
    public final float originX;

    /** Vertical position this entity's {@code Transform} was set to when it was placed. */
    public final float originY;

    /** Seconds elapsed since this entity was placed, accumulated from the fixed step. */
    public float elapsed;

    /**
     * @param originX horizontal spawn position
     * @param originY vertical spawn position
     */
    public Trajectory(float originX, float originY) {
        this.originX = originX;
        this.originY = originY;
        this.elapsed = 0f;
    }
}
