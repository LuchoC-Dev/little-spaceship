package dev.luchoc.littlespaceship.game.screen;

import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import java.util.ArrayList;
import java.util.List;

/**
 * The TESTS submenu: one entry per {@link TestScenarios.Scenario}, each starting the game directly
 * in that scenario. Present only in the {@code -Ptests} build flavour — see {@code TestMode} and
 * {@code game/build.gradle.kts}. That flavour's {@code LittleSpaceshipGame#create()} opens this
 * screen directly at startup (issue #250: the build exists for exactly one purpose, so the main
 * menu is a step with no reason to be there); {@link MenuScreen}'s TESTS entry reaches it again
 * from anywhere else in the flow.
 *
 * <p>BACK leads to {@link MenuScreen}, not back to whatever screen preceded this one — deliberately:
 * this screen has no "preceding screen" to return to when it is also the one the run started on,
 * and {@link MenuScreen} still carries the TESTS entry in this flavour, so BACK never strands the
 * player. The main menu is skipped only at startup, not on every path back to it.
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
