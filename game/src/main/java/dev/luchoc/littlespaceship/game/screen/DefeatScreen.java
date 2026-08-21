package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;

/**
 * Retry or menu, nothing else — {@code docs/planning/02-mvp-functional-spec.md}'s Defeat screen.
 * There are no checkpoints in the MVP, so there is nothing else to offer.
 *
 * <p>Same caveat as {@link VictoryScreen}: not reachable from a real loss yet, because {@code
 * core.port} exposes no such signal. See that class's javadoc.
 */
public final class DefeatScreen extends BaseUiScreen {

    public DefeatScreen(LittleSpaceshipGame game, int score) {
        super(game, "GAME OVER");
        content.top().left();

        Label subtitle = new Label("THE SHIP WAS LOST OVER THE CITY.", skin, "body");
        content.add(subtitle).left().padBottom(20f).row();

        Label scoreLabel = new Label("SCORE   " + score, skin, "title");
        content.add(scoreLabel).left().padBottom(24f).row();

        MenuEntries.add(content, skin, "RETRY", () -> game.setScreen(new ShipSelectScreen(game)));
        MenuEntries.add(content, skin, "QUIT TO MENU", () -> game.setScreen(new MenuScreen(game)));
    }
}
