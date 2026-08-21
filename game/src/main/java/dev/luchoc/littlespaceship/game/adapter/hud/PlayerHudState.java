package dev.luchoc.littlespaceship.game.adapter.hud;

/**
 * Everything {@link dev.luchoc.littlespaceship.game.adapter.render.HudRenderer} needs to draw the
 * left and right plates of {@code docs/design/04-hud-layout.md}, gathered into one immutable value
 * so the renderer never has to ask several sources and reconcile them mid-frame.
 *
 * <p><b>This is a {@code game}-side value, not a core contract.</b> {@code core.port.WorldView} does
 * not expose a player status yet — {@code docs/plan/06-presentation/plan.md}'s task 14 names this as
 * the one thing this phase needs from {@code core-domain} and defers it. {@link
 * dev.luchoc.littlespaceship.game.LittleSpaceshipGame} currently builds a fixed placeholder instance
 * instead of reading the simulation, which is why the HUD renders but does not yet react to a life
 * lost, a pickup or a shot fired. The proposed core contract this record exists to be filled from is
 * recorded in the phase 06 status file and in the game-presentation agent's report.
 *
 * @param lives lives remaining, 0 to {@code maxLives}
 * @param maxLives the cap from {@code BalanceValues.maxLives()}, always 5 slots drawn
 * @param bombs bomb charges available, 0 to {@code maxBombs}
 * @param maxBombs the cap from {@code BalanceValues.maxBombs()}, always 3 slots drawn
 * @param weaponLevel current shot level, 1-based
 * @param maxWeaponLevel the cap from {@code BalanceValues.weaponLevels()}
 * @param shieldActive whether the shield layer is currently held
 * @param invulnerability which source of grace frames is active, if any
 * @param invulnerabilityFraction remaining grace time as a fraction of its total duration, in
 *     {@code [0, 1]}; only meaningful when {@code invulnerability != NONE}
 * @param attachmentId content id of the equipped attachment, or null when there is none — the
 *     {@code MODULE} block is hidden entirely in that case, per {@code 04-hud-layout.md}
 * @param attachmentName the label drawn next to the attachment icon, already shortened to 13
 *     characters of {@code font-mini}
 * @param score points accumulated this run, never negative
 */
public record PlayerHudState(
    int lives,
    int maxLives,
    int bombs,
    int maxBombs,
    int weaponLevel,
    int maxWeaponLevel,
    boolean shieldActive,
    InvulnerabilitySource invulnerability,
    float invulnerabilityFraction,
    String attachmentId,
    String attachmentName,
    int score) {
}
