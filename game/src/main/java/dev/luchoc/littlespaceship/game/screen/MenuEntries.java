package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import java.util.List;

/**
 * A vertical stack of {@code font-mini} entries, per {@code docs/design/04-hud-layout.md}'s "16 px
 * between entries".
 *
 * <p>Selection state has two independent sources now: libGDX's own hover/press styling for the
 * mouse ({@code overFontColor}/{@code downFontColor}, {@code W4} per the design doc), and keyboard
 * focus for {@link MenuNavigator}, which this class also registers each button under —
 * {@code button.setChecked(true)} drives {@code checkedFontColor}, the same {@code W4}, and a
 * "> " prefix on the label so the focused entry differs in shape as well as colour, per
 * {@code 05-legibility-rules.md} R4. Both mechanisms coexist without conflict: a mouse hover and a
 * keyboard focus simply agree on which entry looks selected when they happen to be the same one.
 */
final class MenuEntries {

    private MenuEntries() {
    }

    /**
     * Adds a button and registers it with {@code focusables} so a {@link MenuNavigator} built from
     * that list afterwards can reach it with the keyboard.
     */
    static TextButton add(
            Table table, Skin skin, String label, Runnable action, List<KeyboardFocusable> focusables) {
        TextButton button = new TextButton("  " + label, skin);
        button.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        table.add(button).left().padBottom(16f).row();

        focusables.add(new KeyboardFocusable() {
            @Override
            public void setFocused(boolean focused) {
                button.setChecked(focused);
                button.setText((focused ? "> " : "  ") + label);
            }

            @Override
            public void activate() {
                action.run();
            }
        });
        return button;
    }
}
