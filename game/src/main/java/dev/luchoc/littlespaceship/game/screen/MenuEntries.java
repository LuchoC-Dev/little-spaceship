package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import dev.luchoc.littlespaceship.game.adapter.audio.Sfx;
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
 *
 * <p><b>This is also where every screen's audio gets unlocked.</b> {@link
 * dev.luchoc.littlespaceship.game.adapter.audio.AudioSystem#unlock()} is called here, once per
 * activation but idempotent, because a click or an Enter key press reaching this method is by
 * construction a real user gesture — the exact thing a browser waits for before it allows audio to
 * play at all. Every menu entry in the game goes through {@link #add}, so this is the one place
 * that needs to know the rule exists.
 */
final class MenuEntries {

    private MenuEntries() {
    }

    /**
     * Adds a button and registers it with {@code focusables} so a {@link MenuNavigator} built from
     * that list afterwards can reach it with the keyboard.
     */
    static TextButton add(Table table, LittleSpaceshipGame game, Skin skin, String label,
            Runnable action, List<KeyboardFocusable> focusables) {
        TextButton button = new TextButton("  " + label, skin);
        button.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
        // The 2-character lead ("  " / "> ") is the marker's own 12 px gutter from
        // 04-hud-layout.md, not padding — without a matching pad on every other edge the text sat
        // flush against the right and bottom of the plate while the left looked padded by accident.
        button.pad(3f, 0f, 3f, 12f);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activate(game, action);
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
                MenuEntries.activate(game, action);
            }
        });
        return button;
    }

    private static void activate(LittleSpaceshipGame game, Runnable action) {
        game.audio().unlock();
        game.audio().playSfx(Sfx.UI_SELECT);
        action.run();
    }
}
