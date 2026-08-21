package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import dev.luchoc.littlespaceship.game.GameSettings;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;

/**
 * Master, music and effects volume, and the mouse-control switch — exactly
 * {@code docs/planning/02-mvp-functional-spec.md}'s Options list. No key remapping and no
 * difficulty selector: both are explicit MVP exclusions, not omissions.
 */
public final class OptionsScreen extends BaseUiScreen {

    public OptionsScreen(LittleSpaceshipGame game, Screen previous) {
        super(game, "OPTIONS");
        GameSettings settings = game.settings();
        content.top().left();

        addSlider(content, "MASTER VOLUME", settings.masterVolume(), settings::masterVolume);
        addSlider(content, "MUSIC VOLUME", settings.musicVolume(), settings::musicVolume);
        addSlider(content, "EFFECTS VOLUME", settings.effectsVolume(), settings::effectsVolume);

        CheckBox mouseToggle = new CheckBox(" MOUSE CONTROL", skin);
        mouseToggle.setChecked(settings.mouseEnabled());
        mouseToggle.addListener(event -> {
            settings.mouseEnabled(mouseToggle.isChecked());
            return false;
        });
        content.add(mouseToggle).left().padTop(8f).padBottom(16f).row();

        MenuEntries.add(content, skin, "BACK", () -> game.setScreen(previous));
    }

    private void addSlider(Table table, String labelText, float initial, java.util.function.Consumer<Float> onChange) {
        Table row = new Table();
        Label label = new Label(labelText, skin, "hud-label");
        Slider slider = new Slider(0f, 1f, 0.01f, false, skin);
        slider.setValue(initial);
        Label percent = new Label(Integer.toString(Math.round(initial * 100)), skin, "body");
        slider.addListener(event -> {
            onChange.accept(slider.getValue());
            percent.setText(Integer.toString(Math.round(slider.getValue() * 100)));
            return false;
        });
        row.add(label).width(150f).left();
        row.add(slider).width(120f).padLeft(16f);
        row.add(percent).width(40f).padLeft(16f);
        table.add(row).left().padBottom(12f).row();
    }
}
