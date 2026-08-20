package dev.luchoc.littlespaceship.game.adapter.content;

import dev.luchoc.littlespaceship.core.port.BalanceValues;

/**
 * The MVP's starting balance numbers, hard-coded until phase 04 reads them from JSON.
 *
 * <p>Values mirror the placeholders documented on {@link BalanceValues} and in
 * {@code docs/planning/10-mvp-initial-values.md}: everything the document fixes is copied verbatim,
 * and {@code playerSpeed}/{@code playerSlowFactor} use the same placeholder numbers the core's own
 * test fixture does, since the document does not fix a concrete value for either yet.
 */
public final class PlaceholderBalanceValues implements BalanceValues {

    @Override
    public int initialLives() {
        return 3;
    }

    @Override
    public int maxLives() {
        return 5;
    }

    @Override
    public int initialBombs() {
        return 2;
    }

    @Override
    public int maxBombs() {
        return 3;
    }

    @Override
    public int weaponLevels() {
        return 4;
    }

    @Override
    public float respawnInvulnerability() {
        return 2f;
    }

    @Override
    public float damageInvulnerability() {
        return 1f;
    }

    @Override
    public int maxedPickupBonus() {
        return 500;
    }

    @Override
    public float playerSpeed() {
        return 140f;
    }

    @Override
    public float playerSlowFactor() {
        return 0.45f;
    }
}
