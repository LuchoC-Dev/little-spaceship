package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;

/**
 * Ship selection/building, per {@code docs/planning/02-mvp-functional-spec.md}: one selectable
 * ship, its characteristics, and a screen conceptually ready for more without offering unlocking,
 * purchase or deep customisation — none of which the MVP has.
 *
 * <p>The empty second slot from {@code docs/design/mockups/screens.html} is not drawn here: the
 * spec calls it optional, and the design note for it — "it says the system grows without promising
 * anything" — is exactly the kind of promise this screen has no data to back yet. It can be added
 * with no layout change once a second ship exists to slot in.
 */
public final class ShipSelectScreen extends BaseUiScreen {

    public ShipSelectScreen(LittleSpaceshipGame game) {
        super(game, "SELECT SHIP");
        content.top().left();

        Label name = new Label("PROTOTYPE X-1", skin, "hud-label");
        Label stats = new Label(
            "SUSTAINED SHOT, BOMB, SLOW MODE\nSPEED   FIRE   BOMBS   LIVES", skin, "body");
        content.add(name).left().padBottom(6f).row();
        content.add(stats).left().padBottom(24f).row();

        MenuEntries.add(content, skin, "LAUNCH", () -> game.setScreen(new PlayScreen(game)));
        MenuEntries.add(content, skin, "BACK", () -> game.setScreen(new MenuScreen(game)));
    }
}
