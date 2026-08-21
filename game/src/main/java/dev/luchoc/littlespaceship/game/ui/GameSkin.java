package dev.luchoc.littlespaceship.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.graphics.g2d.NinePatch;

/**
 * The one {@link Skin} every screen shares, built in code instead of loaded from a {@code .json} +
 * atlas pair — {@code CLAUDE.md} asks for a Skin over a hand-rolled UI framework, not for a
 * particular loading mechanism, and there is nothing to load yet: {@code docs/design/03-typography.md}
 * fixes {@code font-mini}/{@code font-title} as hand-drawn PNG sheets, but drawing them is art
 * production, {@code docs/plan/06-presentation/plan.md} tasks 3 and 11, neither of which has run.
 *
 * <p><b>The text in every screen this Skin styles is a placeholder.</b> It uses libGDX's bundled
 * default font — a real bitmap font, not {@code FreeTypeFontGenerator}, so it costs nothing extra
 * under TeaVM. It reads at a glance but its glyph shapes, advance and letter-spacing do not match
 * {@code docs/design/03-typography.md}'s {@code font-mini}/{@code font-title}, and nothing here
 * enforces the fixed 6/8 px advance that document is built around. Swapping it for the real sheets
 * is a change confined to this one class: every screen asks the Skin for {@code "font-mini"}/{@code
 * "font-title"} by name and never touches a {@link BitmapFont} directly.
 *
 * <p><b>The scale is snapped to a whole number, never a fraction.</b> The default font's native
 * line height is well above the 10/13 px {@code 03-typography.md} specifies for {@code
 * font-mini}/{@code font-title}, so a first pass scaled it down with {@code getData().setScale(10f
 * / getLineHeight())} — a fractional factor, exactly the "no fractional scaling anywhere" {@code
 * CLAUDE.md} rules out for sprites, applied to glyphs by mistake. Combined with {@code
 * setUseIntegerPositions(false)} it put glyphs on half pixels, which reads as a rendering bug rather
 * than as an unfinished placeholder. {@link #wholeScale} rounds down to the nearest whole multiple —
 * 1 here, since the native font is already larger than the target — so the placeholder ends up
 * oversized rather than blurred. Wrong size reads as "not the final font yet"; blurred type reads as
 * broken, on a project whose own invariant is integer scaling with no exception.
 */
public final class GameSkin {

    private GameSkin() {
    }

    public static Skin build() {
        Skin skin = new Skin();

        Texture pixel = solidTexture(0xFFFFFFFF);
        skin.add("white", new TextureRegion(pixel));

        BitmapFont fontMini = new BitmapFont();
        fontMini.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        fontMini.getData().setScale(wholeScale(10f, fontMini.getLineHeight()));
        fontMini.setUseIntegerPositions(true);

        BitmapFont fontTitle = new BitmapFont();
        fontTitle.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        fontTitle.getData().setScale(wholeScale(13f, fontTitle.getLineHeight()));
        fontTitle.setUseIntegerPositions(true);

        skin.add("font-mini", fontMini, BitmapFont.class);
        skin.add("font-title", fontTitle, BitmapFont.class);

        // Stored explicitly under Drawable.class: Skin.add(name, resource) files a resource under
        // its own runtime class (NinePatchDrawable.class here), but Skin.getDrawable(name) looks it
        // up under the Drawable interface, so an implicit add is invisible to every getDrawable()
        // call and to every style field a .json skin would populate through it.
        skin.add("n2-panel", ninePatch(Palette.N2, Palette.N3), Drawable.class);
        skin.add("n1-panel", ninePatch(Palette.N1, Palette.N0), Drawable.class);

        Label.LabelStyle title = new Label.LabelStyle(fontTitle, Palette.N7);
        skin.add("title", title);

        Label.LabelStyle hudLabel = new Label.LabelStyle(fontMini, Palette.N4);
        skin.add("hud-label", hudLabel);

        Label.LabelStyle body = new Label.LabelStyle(fontMini, Palette.N4);
        skin.add("body", body);

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = fontMini;
        button.fontColor = Palette.N7;
        button.overFontColor = Palette.W4;
        button.downFontColor = Palette.W4;
        button.checkedFontColor = Palette.W4;
        button.disabledFontColor = Palette.N3;
        button.up = skin.getDrawable("n2-panel");
        button.down = skin.getDrawable("n2-panel");
        skin.add("default", button);

        Slider.SliderStyle slider = new Slider.SliderStyle();
        slider.background = new NinePatchDrawable(
            new NinePatch(new TextureRegion(pixel), 0, 0, 0, 0));
        slider.background.setMinHeight(3f);
        slider.knob = skin.newDrawable("white", Palette.N6);
        slider.knob.setMinWidth(5f);
        slider.knob.setMinHeight(9f);
        slider.knobBefore = skin.newDrawable("white", Palette.W4);
        skin.add("default-horizontal", slider);

        com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle checkBox =
            new com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle();
        checkBox.font = fontMini;
        checkBox.fontColor = Palette.N7;
        checkBox.checkboxOff = skin.newDrawable("white", Palette.N2);
        checkBox.checkboxOff.setMinWidth(9f);
        checkBox.checkboxOff.setMinHeight(9f);
        checkBox.checkboxOn = skin.newDrawable("white", Palette.W4);
        checkBox.checkboxOn.setMinWidth(9f);
        checkBox.checkboxOn.setMinHeight(9f);
        skin.add("default", checkBox);

        List.ListStyle list = new List.ListStyle();
        list.font = fontMini;
        list.fontColorUnselected = Palette.N4;
        list.fontColorSelected = Palette.W4;
        list.selection = skin.newDrawable("white", Palette.N2);
        skin.add("default", list);

        return skin;
    }

    /**
     * The nearest whole multiple of the native line height to the target, never below 1 — a
     * fractional scale is what blurs a nearest-neighbour glyph, per the class javadoc.
     */
    private static float wholeScale(float targetHeight, float nativeLineHeight) {
        return Math.max(1f, Math.round(targetHeight / nativeLineHeight));
    }

    private static NinePatchDrawable ninePatch(Color fill, Color border) {
        Pixmap pm = new Pixmap(3, 3, Pixmap.Format.RGBA8888);
        pm.setColor(border);
        pm.fill();
        pm.setColor(fill);
        pm.fillRectangle(1, 1, 1, 1);
        Texture texture = new Texture(pm);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return new NinePatchDrawable(new NinePatch(texture, 1, 1, 1, 1));
    }

    private static Texture solidTexture(int rgba8888) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color((int) ((rgba8888 >>> 24) & 0xFF) / 255f,
            ((rgba8888 >>> 16) & 0xFF) / 255f, ((rgba8888 >>> 8) & 0xFF) / 255f,
            (rgba8888 & 0xFF) / 255f));
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

}
