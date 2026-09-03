package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import java.util.List;

/**
 * The ordinary build's variant of the {@code -Ptests} flavour's hook: a no-op, compiled whenever
 * {@code game/build.gradle.kts}'s {@code tests} Gradle property is absent, which is every build
 * that reaches a player, including {@code :web}'s.
 *
 * <p>{@link MenuScreen} calls {@link #addMenuEntry} unconditionally, so it needs no flavour-aware
 * code of its own — the difference between "no TESTS entry" and "a TESTS entry that opens a
 * submenu of scenarios" lives entirely in which of this class's two mutually exclusive source
 * files Gradle compiles, never in a runtime check. The real implementation, along with
 * {@code TestMenuScreen} and {@code TestScenarios}, lives under {@code src/tests/java} and is
 * added to the {@code main} source set only when {@code -Ptests} is passed — so this stub is the
 * only trace of the flavour that ever reaches a shipped build.
 */
final class TestMode {

    private TestMode() {
    }

    static void addMenuEntry(Table content, LittleSpaceshipGame game, Skin skin,
            List<KeyboardFocusable> focusables) {
        // Intentionally empty.
    }
}
