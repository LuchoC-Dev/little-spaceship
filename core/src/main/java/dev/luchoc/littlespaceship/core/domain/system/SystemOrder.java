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
     * Detonates the bomb when requested: clears on-screen fragile enemies and enemy projectiles,
     * damages on-screen resistant ones. Runs before {@code SPAWN} so a bomb used the instant a wave
     * would appear never reaches a wave that has not spawned yet, and — this is the part that
     * matters for correctness, not just tidiness — before {@code COLLISION}. {@code BombSystem} only
     * marks entities for destruction; it does not remove their collider. Running before {@code
     * COLLISION} is what lets {@code CollisionSystem}'s own rule (skip anything already marked for
     * destruction this tick, stated on its class) turn that marking into actual protection: an enemy
     * or an enemy projectile the bomb just cleared never produces a {@code CollisionHit} against the
     * player in the same tick, so {@code DamageSystem} never consumes a layer or a life for it. Move
     * {@code BOMB} to run after {@code COLLISION} and this protection is lost regardless of what
     * {@code CollisionSystem} does — the hits would already be in the list.
     */
    BOMB,

    /** Advances the level timeline and spawns waves. */
    SPAWN,

    /** Expires projectiles and effects. */
    LIFETIME,

    /**
     * Detects impacts between layer pairs. Also the one place that enforces: an entity already
     * marked for destruction this tick — by {@code BombSystem}, by {@code LifetimeSystem}, by
     * anything that runs earlier in the pipeline and calls {@code World.markForDestruction} — never
     * produces a {@code CollisionHit}, regardless of what marked it. Marking alone leaves the
     * collider in place, so without this rule an entity destroyed earlier in the same tick could
     * still register a hit against the player before {@code CleanupSystem} ever runs.
     */
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
