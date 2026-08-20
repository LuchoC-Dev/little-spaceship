package dev.luchoc.littlespaceship.core.port;

/**
 * The numbers that decide how the game feels.
 *
 * <p>They live outside the code because their purpose is to change during balancing: the adapter
 * reads them from content and the simulation asks for them. The starting values are recorded in
 * {@code docs/planning/10-mvp-initial-values.md}, and none of them is definitive.
 *
 * <p>Only the values the simulation already consumes are declared. The rest arrive with the systems
 * that read them.
 */
public interface BalanceValues {

    /**
     * @return lives the player starts a run with
     */
    int initialLives();

    /**
     * @return lives the player can never exceed, so stacking them does not remove all tension
     */
    int maxLives();

    /**
     * @return bombs the player starts a run with
     */
    int initialBombs();

    /**
     * @return bombs the player can never exceed
     */
    int maxBombs();

    /**
     * @return number of shot levels, the base one included
     */
    int weaponLevels();

    /**
     * @return seconds of invulnerability after respawning
     */
    float respawnInvulnerability();

    /**
     * @return seconds of invulnerability after a hit absorbed by shield or attachment
     */
    float damageInvulnerability();

    /**
     * @return points awarded when a power-up is picked up at maximum, so the drop is never wasted
     */
    int maxedPickupBonus();

    /**
     * Top speed of the player's ship, in logical units per second. The movement vector is clamped to
     * this magnitude regardless of direction, which is what keeps diagonal movement no faster than
     * a single axis.
     *
     * <p>Not yet in {@code 10-mvp-initial-values.md}: the document fixes the movement policy —
     * additive devices, clamped result — but not a concrete speed. This is a placeholder pending
     * that number being added during balancing.
     *
     * @return the ship's top speed
     */
    float playerSpeed();

    /**
     * Multiplier applied to {@link #playerSpeed()} while the precision control is held.
     *
     * <p>Slow movement is a multiplier and not a separate mode: the same clamp is used with a
     * smaller cap. Also missing from {@code 10-mvp-initial-values.md}, for the same reason as
     * {@link #playerSpeed()}.
     *
     * @return a value in {@code (0, 1]}
     */
    float playerSlowFactor();
}
