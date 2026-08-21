package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * A vertical stack of {@code font-mini} entries, per {@code docs/design/04-hud-layout.md}'s "16 px
 * between entries". Selection state is whatever libGDX's own focus/hover already tracks through the
 * button style's {@code overFontColor}/{@code downFontColor} — {@code W4} per the design doc — so
 * this class only lays entries out and wires their action, nothing about how a selected entry looks.
 */
final class MenuEntries {

    private MenuEntries() {
    }

    static TextButton add(Table table, Skin skin, String label, Runnable action) {
        TextButton button = new TextButton(label, skin);
        button.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        table.add(button).left().padBottom(16f).row();
        return button;
    }
}
