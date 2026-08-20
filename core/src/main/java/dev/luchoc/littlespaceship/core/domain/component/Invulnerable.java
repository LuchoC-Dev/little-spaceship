package dev.luchoc.littlespaceship.core.domain.component;

/**
 * Remaining grace time during which the holder ignores damage entirely.
 *
 * <p>Granted and decayed only by {@code DamageSystem}, the single place the defensive chain lives.
 * Two durations feed it, both read from {@code BalanceValues}: a longer one after a life is lost, a
 * shorter one after a hit absorbed by the shield or the attachment. The second is a confirmed, late
 * addition to the spec — invulnerability follows any damage, not only death — recorded in
 * {@code 08-decisions-and-open-items.md}.
 */
public final class Invulnerable {

    /** Seconds of grace left. The component is removed once this reaches zero. */
    public float remaining;

    /**
     * Creates the grace period.
     *
     * @param remaining seconds of grace, strictly positive
     */
    public Invulnerable(float remaining) {
        this.remaining = remaining;
    }
}
