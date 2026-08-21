package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import java.util.ArrayList;
import java.util.List;

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

        Label subtitle = new Label(
            "AN EXPERIMENTAL SHIP AGAINST THE FIRST WAVE", skin, "body");
        content.add(subtitle).left().padBottom(20f).row();

        List<KeyboardFocusable> focusables = new ArrayList<>();
        MenuEntries.add(content, skin, "PLAY", () -> game.setScreen(new ShipSelectScreen(game)), focusables);
        MenuEntries.add(content, skin, "OPTIONS",
            () -> game.setScreen(new OptionsScreen(game, () -> new MenuScreen(game))), focusables);
        MenuEntries.add(content, skin, "QUIT", Gdx.app::exit, focusables);
        new MenuNavigator(stage, focusables);

        Table footerRow = new Table();
        footerRow.add(new Label("MVP BUILD", skin, "body")).left();
        content.add(footerRow).left().expandY().bottom().padBottom(0f).row();
    }
}
