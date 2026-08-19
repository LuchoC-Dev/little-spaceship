package dev.luchoc.littlespaceship.core.domain.system;

/**
 * The order in which the systems run, declared once and in one place.
 *
 * <p>This is a game rule, not an implementation detail. Collision before damage is what makes the
 * defensive priority resolvable in a single place; cleanup last is what lets a system mark an
 * entity for destruction without anything reading a half-destroyed world afterwards. Reordering
 * these constants changes how the game behaves.
 *
 * <p>The pipeline runs the stages in declaration order and every system says which stage it belongs
 * to, so the order cannot be changed by accident when registering one. Stages with no system yet
 * are simply skipped: they arrive with the phase that implements them.
 */
public enum SystemOrder {

    /** Translates the input frame into the player's intent. */
    INPUT,

    /** Applies velocities and trajectories. */
    MOTION,

    /** Resolves rates of fire and creates projectiles. */
    WEAPON,

    /** Advances the level timeline and spawns waves. */
    SPAWN,

    /** Expires projectiles and effects. */
    LIFETIME,

    /** Detects impacts between layer pairs. */
    COLLISION,

    /** Applies the defensive priority: invulnerability, shield, attachment, life. */
    DAMAGE,

    /** Resolves power-ups and attachments. */
    PICKUP,

    /** Accumulates score. */
    SCORE,

    /** Destroys what was marked and frees identifiers. */
    CLEANUP
}
