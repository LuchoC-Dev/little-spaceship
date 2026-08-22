package dev.luchoc.littlespaceship.game.adapter.audio;

/**
 * One entry per one-shot sound {@link AudioSystem} can play, matching {@code
 * docs/planning/02-mvp-functional-spec.md}'s list — shots, impacts, explosions, power-ups, the
 * bomb and UI — one category each, not one sound per pickup kind or per enemy archetype. A player
 * hearing six distinct power-up chimes gains nothing a single one does not already give.
 *
 * <p>{@link #EXPLOSION} has no caller yet. {@code core} destroys an enemy without reporting when
 * or where — see {@code docs/plan/08-audio-and-polish/status.md}'s account of the missing seam —
 * so nothing in {@code game} can currently tell "an enemy died" from "nothing happened". The asset
 * and the playback path both exist and wait for a real trigger once {@code core} provides one.
 */
public enum Sfx {

    /** The player's own shot leaving the ship. */
    SHOOT("shoot.wav"),

    /** The player absorbing a hit — by the shield, an attachment, or a life, per {@link
     * dev.luchoc.littlespaceship.core.port.InvulnerabilitySource}'s DAMAGE/RESPAWN transition. */
    IMPACT("impact.wav"),

    /** An enemy destroyed. See the class javadoc: wired but not yet triggered. */
    EXPLOSION("explosion.wav"),

    /** Any of the six pickup kinds {@code PickupSystem} resolves, collected. */
    POWERUP("powerup.wav"),

    /** The bomb triggered. */
    BOMB("bomb.wav"),

    /** A menu entry activated, by mouse click or keyboard. */
    UI_SELECT("ui-select.wav");

    private final String fileName;

    Sfx(String fileName) {
        this.fileName = fileName;
    }

    /** @return the file name under {@code assets/audio/sfx/}, matching {@code GenerateAudio}'s output */
    public String fileName() {
        return fileName;
    }
}
