package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import java.util.ArrayList;
import java.util.List;

/**
 * Retry or menu, nothing else — {@code docs/planning/02-mvp-functional-spec.md}'s Defeat screen.
 * There are no checkpoints in the MVP, so there is nothing else to offer.
 *
 * <p>{@link PlayScreen} opens this screen when {@code WorldView.outcome()} reports {@code
 * LevelOutcome.DEFEATED} — {@code Player.lives} reached zero, exactly what
 * {@code 02-mvp-functional-spec.md} names for a loss, with no caveat needed the way {@link
 * VictoryScreen}'s outcome has one.
 */
public final class DefeatScreen extends BaseUiScreen {

    public DefeatScreen(LittleSpaceshipGame game, int score) {
        super(game, "GAME OVER");
        content.top().left();

        Label subtitle = new Label("THE SHIP WAS LOST OVER THE CITY.", skin, "body");
        content.add(subtitle).left().padBottom(20f).row();

        Label scoreLabel = new Label("SCORE   " + score, skin, "title");
        content.add(scoreLabel).left().padBottom(24f).row();

        List<KeyboardFocusable> focusables = new ArrayList<>();
        MenuEntries.add(content, skin, "RETRY", () -> game.setScreen(new ShipSelectScreen(game)), focusables);
        MenuEntries.add(content, skin, "QUIT TO MENU", () -> game.setScreen(new MenuScreen(game)), focusables);
        new MenuNavigator(stage, focusables);
    }
}
