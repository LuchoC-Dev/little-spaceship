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

    /**
     * Detonates the bomb when requested: clears fragile enemies and enemy projectiles on screen.
     * Runs before {@code SPAWN} so a bomb used the instant a wave would appear never reaches a wave
     * that has not spawned yet — there is nothing on screen for it to touch either way, but placing
     * it here keeps every stage that reacts to on-screen entities grouped together, after input and
     * weapon resolution and before anything new enters the world this tick.
     */
    BOMB,

    /** Advances the level timeline and spawns waves. */
    SPAWN,

    /** Expires projectiles and effects. */
    LIFETIME,

    /** Detects impacts between layer pairs. */
    COLLISION,

    /**
     * Applies the defensive priority against the player — invulnerability, shield, attachment,
     * life — and resolves what a player projectile did to the enemy it reached. Both are damage
     * resolution against a collision hit reported the same tick, which is why one system owns both
     * instead of splitting the second half into a stage of its own.
     */
    DAMAGE,

    /** Resolves power-ups and attachments. */
    PICKUP,

    /** Accumulates score. */
    SCORE,

    /** Destroys what was marked and frees identifiers. */
    CLEANUP
}
