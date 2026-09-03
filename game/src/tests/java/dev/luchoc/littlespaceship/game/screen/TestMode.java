package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import java.util.List;

/**
 * The {@code -Ptests} build flavour's own variant of the hook {@link MenuScreen} always calls: it
 * adds the TESTS entry, opening {@link TestMenuScreen}. Compiled only when {@code
 * game/build.gradle.kts}'s {@code tests} Gradle property is present — see this class's stub
 * counterpart under {@code src/teststub/java} for what a build without that property contains
 * instead, which is nothing of this package.
 */
final class TestMode {

    private TestMode() {
    }

    static void addMenuEntry(Table content, LittleSpaceshipGame game, Skin skin,
            List<KeyboardFocusable> focusables) {
        MenuEntries.add(content, game, skin, "TESTS",
            () -> game.setScreen(new TestMenuScreen(game)), focusables);
    }
}
