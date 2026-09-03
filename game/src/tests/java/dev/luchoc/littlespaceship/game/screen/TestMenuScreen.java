package dev.luchoc.littlespaceship.game.screen;

import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import java.util.ArrayList;
import java.util.List;

/**
 * The TESTS submenu: one entry per {@link TestScenarios.Scenario}, each starting the game directly
 * in that scenario. Reached only from {@link MenuScreen}'s TESTS entry, itself present only in the
 * {@code -Ptests} build flavour — see {@code TestMode} and {@code game/build.gradle.kts}.
 *
 * <p>Goes straight to {@link PlayScreen}, skipping {@link ShipSelectScreen}: a scenario's starting
 * state — weapon level, lives, bombs — is the level file's own decision per
 * {@code docs/plan/11h-test-mode/plan.md}, not a choice this screen offers.
 */
final class TestMenuScreen extends BaseUiScreen {

    TestMenuScreen(LittleSpaceshipGame game) {
        super(game, "TESTS");
        content.top().left();

        List<KeyboardFocusable> focusables = new ArrayList<>();
        for (TestScenarios.Scenario scenario : TestScenarios.ALL) {
            MenuEntries.add(content, game, skin, scenario.label(), () -> {
                game.overrideLevelId(scenario.levelId());
                game.setScreen(new PlayScreen(game));
            }, focusables);
        }
        MenuEntries.add(content, game, skin, "BACK",
            () -> game.setScreen(new MenuScreen(game)), focusables);
        new MenuNavigator(stage, focusables);
    }
}
