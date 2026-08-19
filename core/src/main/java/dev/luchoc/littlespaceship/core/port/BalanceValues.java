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
}
