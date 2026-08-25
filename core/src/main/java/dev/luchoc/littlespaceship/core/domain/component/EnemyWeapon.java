package dev.luchoc.littlespaceship.core.domain.component;

/**
 * An enemy archetype's own firing pattern: how often it fires, what shape and how fast the shot
 * leaves. Data-driven per archetype, the same treatment {@link Spawner} already gets — {@code
 * enemy-shooter} is content, not a class, exactly like every other archetype component {@code
 * ComponentFactoryRegistry} attaches.
 *
 * <p>Distinct from the player's {@link Weapon}: that one carries only a timer, because rate and
 * pattern are either global ({@code BalanceValues.weaponFireCooldown()}) or read from {@code
 * Player.shotLevel} by {@code WeaponSystem} itself. An enemy has neither a global rate nor a shot
 * level — its rate and pattern genuinely vary per archetype — so both travel on the component
 * instead, the same shape {@code 12-architecture.md}'s own {@code "weapon": { "rate": 2.2, "pattern":
 * "straight-single" }} example already implies.
 *
 * <p>{@code pattern} names a shot shape {@code EnemyWeaponSystem} knows how to build — {@code
 * "straight-single"} is the only one the MVP needs, per {@code 02-mvp-functional-spec.md}'s
 * "evolved basic or shooter: similar to the basic one with a higher rate of fire". A second shape
 * would be a second case in that system, not a new content contract: nothing here justifies a
 * general {@code PatternDefinition} the way {@code content-pipeline-design} already rejected one for
 * lack of a second real case.
 */
public final class EnemyWeapon {

    /** A shot shape {@code EnemyWeaponSystem} knows how to build, such as {@code "straight-single"}. */
    public final String pattern;

    /** Seconds between two shots. Confusingly named {@code "rate"} in content, per the architecture
     * document's own example — a lower number fires more often, exactly like {@code Weapon}'s own
     * cooldown. */
    public final float cooldown;

    /** Speed of the fired projectile, in logical units per second, always positive: the pattern
     * itself decides direction. */
    public final float projectileSpeed;

    /** Seconds left before the next shot. Starts at {@code cooldown}, not zero — the same choice
     * {@link Spawner#timer} already makes — so an enemy does not fire the instant it spawns. */
    public float cooldownRemaining;

    /**
     * @param pattern the shot shape, never null or empty
     * @param cooldown seconds between two shots, strictly positive
     * @param projectileSpeed the fired projectile's speed, strictly positive
     */
    public EnemyWeapon(String pattern, float cooldown, float projectileSpeed) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("an enemy weapon needs a pattern");
        }
        if (cooldown <= 0f) {
            throw new IllegalArgumentException("an enemy weapon needs a strictly positive cooldown, was " + cooldown);
        }
        if (projectileSpeed <= 0f) {
            throw new IllegalArgumentException(
                "an enemy weapon needs a strictly positive projectile speed, was " + projectileSpeed);
        }
        this.pattern = pattern;
        this.cooldown = cooldown;
        this.projectileSpeed = projectileSpeed;
        this.cooldownRemaining = cooldown;
    }
}
