package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.Gdx;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;

/**
 * The main menu: Play, Options, Quit — {@code docs/planning/02-mvp-functional-spec.md}'s exact
 * list, with no locked mode and no "coming soon" entry, per the spec's own exclusion and per
 * {@code docs/design/mockups/screens.html}'s note that drawing either would promise something the
 * build does not have.
 */
public final class MenuScreen extends BaseUiScreen {

    public MenuScreen(LittleSpaceshipGame game) {
        super(game, "LITTLE SPACESHIP");
        content.top().left();
        MenuEntries.add(content, skin, "PLAY", () -> game.setScreen(new ShipSelectScreen(game)));
        MenuEntries.add(content, skin, "OPTIONS", () -> game.setScreen(new OptionsScreen(game, this)));
        MenuEntries.add(content, skin, "QUIT", Gdx.app::exit);
    }
}
