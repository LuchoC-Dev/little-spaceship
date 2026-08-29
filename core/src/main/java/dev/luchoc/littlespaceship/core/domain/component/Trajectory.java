package dev.luchoc.littlespaceship.core.domain.component;

/**
 * How long this entity has existed, accumulated from the fixed step.
 *
 * <p>{@code docs/plan/11c-movement-shapes/shape-catalogue.md} settles what a movement shape is
 * allowed to read: "a function from the entity's own elapsed time to its velocity. Nothing else goes
 * in." {@link #elapsed} is that one input. Neither kind the catalogue defines — {@code constant} or
 * {@code arc} — reads a position, so this component carries none: an origin field would be an
 * abstraction with no case, which is exactly what invariant 6 refuses.
 *
 * <p>This component only holds state; it resolves nothing. Evaluating {@link #elapsed} into a
 * velocity is a named shape's job, which is content this phase does not yet define — see
 * {@code docs/plan/11c-movement-shapes/plan.md}, task 3. An entity with no {@link Trajectory} simply
 * keeps whatever constant {@code Motion} its trajectory gave it at spawn, exactly as before this
 * component existed.
 */
public final class Trajectory {

    /** Seconds elapsed since this entity was placed, accumulated from the fixed step. */
    public float elapsed;

    public Trajectory() {
        this.elapsed = 0f;
    }
}
