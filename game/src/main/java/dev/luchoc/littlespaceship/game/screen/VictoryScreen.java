package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;

/**
 * The end-of-level bonus, per {@code docs/planning/10-mvp-initial-values.md}'s per-life and
 * per-bomb completion bonus. Retrying is optional per
 * {@code docs/planning/02-mvp-functional-spec.md}, so the only exit is back to the menu.
 *
 * <p><b>Not reachable from real play yet.</b> {@code core.port} exposes no level-complete signal —
 * {@code WorldView} has no notion of "the boss is dead and the run is won" for this screen to be
 * shown from, and inventing one is a core-domain decision, not a rendering one. {@link PlayScreen}
 * reaches this screen through a fixed debug key for now, documented there and in the phase status
 * file, so the screen itself can be reviewed against the mock while that signal does not exist.
 */
public final class VictoryScreen extends BaseUiScreen {

    public VictoryScreen(LittleSpaceshipGame game, int score, int livesBonus, int bombsBonus) {
        super(game, "VICTORY");
        content.top().left();

        Label subtitle = new Label(
            "THE ATTACK ZONE IS CLEAR. THE INVASION IS NOT.", skin, "body");
        content.add(subtitle).left().padBottom(20f).row();

        addRow("SCORE", score);
        addRow("LIVES BONUS", livesBonus);
        addRow("BOMBS BONUS", bombsBonus);

        Label total = new Label("TOTAL   " + zeroPadded(score + livesBonus + bombsBonus), skin, "title");
        content.add(total).left().padTop(12f).padBottom(20f).row();

        MenuEntries.add(content, skin, "CONTINUE", () -> game.setScreen(new MenuScreen(game)));
    }

    private void addRow(String label, int value) {
        Label row = new Label(label + "   " + zeroPadded(value), skin, "body");
        content.add(row).left().padBottom(6f).row();
    }

    private static String zeroPadded(int value) {
        String raw = Integer.toString(Math.max(0, value));
        StringBuilder sb = new StringBuilder();
        for (int i = raw.length(); i < 7; i++) {
            sb.append('0');
        }
        return sb.append(raw).toString();
    }
}
