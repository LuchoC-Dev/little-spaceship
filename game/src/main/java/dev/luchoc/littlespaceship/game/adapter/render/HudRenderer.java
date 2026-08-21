package dev.luchoc.littlespaceship.game.adapter.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import dev.luchoc.littlespaceship.game.adapter.hud.InvulnerabilitySource;
import dev.luchoc.littlespaceship.game.adapter.hud.PlayerHudState;
import dev.luchoc.littlespaceship.game.ui.Palette;

/**
 * Draws the two side plates of {@code docs/design/04-hud-layout.md} — everything the player reads
 * about their own ship — from a {@link PlayerHudState}.
 *
 * <p>Coordinates in this class are exactly the ones the design document tables give, in its own
 * y-down convention; {@link #yGdx} is the one place the flip to libGDX's y-up space happens, per the
 * document's own warning that doing it anywhere else is how a HUD ends up half-flipped.
 *
 * <p>The boss bar is deliberately not drawn: it needs a boss health signal {@code core.port} does
 * not expose yet (a {@code BossStatus}, out of this phase's scope per the task brief — phase 07's
 * concern) and the document itself says it only ever appears during that fight, so its absence here
 * is the same "only during the fight" rule applied to a fight that cannot happen yet.
 */
public final class HudRenderer {

    private static final int LEFT_COLUMN_X = 12;
    private static final int RIGHT_COLUMN_X = 362;
    private static final int RIGHT_COLUMN_RIGHT_EDGE = 467;

    private final BitmapFont fontMini;
    private final BitmapFont fontTitle;
    private final Texture pixel;
    private final GlyphLayout layout = new GlyphLayout();

    public HudRenderer(Skin skin) {
        this.fontMini = skin.getFont("font-mini");
        this.fontTitle = skin.getFont("font-title");
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        this.pixel = new Texture(pm);
        pm.dispose();
    }

    /**
     * Draws both plates. The batch must already be between {@code begin()} and {@code end()}, and
     * projected onto the logical 480x270 space {@link LittleSpaceshipGame} uses.
     */
    public void draw(SpriteBatch batch, PlayerHudState state) {
        drawFrame(batch);
        drawLeftPlate(batch, state);
        drawRightPlate(batch, state);
    }

    /** The plates' own fill and the two playfield rules, per the region table in {@code 04-hud-layout.md}. */
    private void drawFrame(SpriteBatch batch) {
        rect(batch, 0, 0, 135, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N2);
        rect(batch, 135, 0, 1, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N3);
        rect(batch, 344, 0, 1, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N3);
        rect(batch, 345, 0, 135, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N2);
    }

    private void drawLeftPlate(SpriteBatch batch, PlayerHudState state) {
        int x = LEFT_COLUMN_X;

        label(batch, "LIVES", x, 14);
        for (int i = 0; i < state.maxLives(); i++) {
            boolean filled = i < state.lives();
            rect(batch, x + i * 12, 24, 9, 9, filled ? Palette.N6 : Palette.N2);
            outline(batch, x + i * 12, 24, 9, 9, filled ? Palette.N0 : Palette.N3);
        }

        label(batch, "BOMBS", x, 44);
        for (int i = 0; i < state.maxBombs(); i++) {
            boolean filled = i < state.bombs();
            rect(batch, x + i * 12, 54, 9, 9, filled ? Palette.N6 : Palette.N2);
            outline(batch, x + i * 12, 54, 9, 9, filled ? Palette.W4 : Palette.N3);
        }

        label(batch, "POWER", x, 74);
        for (int i = 0; i < state.maxWeaponLevel(); i++) {
            boolean filled = i < state.weaponLevel();
            rect(batch, x + i * 15, 84, 13, 7, filled ? Palette.C1 : Palette.N2);
            if (filled) {
                rect(batch, x + i * 15, 84, 13, 2, Palette.C2);
            } else {
                outline(batch, x + i * 15, 84, 13, 7, Palette.N3);
            }
        }

        label(batch, "STATE", x, 104);
        if (state.shieldActive()) {
            rect(batch, x, 114, 13, 13, Palette.C1);
            outline(batch, x, 114, 13, 13, Palette.N6);
        }
        InvulnerabilitySource source = state.invulnerability();
        if (source != InvulnerabilitySource.NONE) {
            Color iconColor = switch (source) {
                case POWERUP -> Palette.C1;
                case DAMAGE -> Palette.N7;
                case RESPAWN -> Palette.N4;
                case NONE -> Palette.N2;
            };
            rect(batch, x + 16, 114, 13, 13, iconColor);
            outline(batch, x + 16, 114, 13, 13, Palette.W4);
            float remainingWidth = 13f * Math.max(0f, Math.min(1f, state.invulnerabilityFraction()));
            rect(batch, x + 16, 128, 13, 1, Palette.N2);
            rect(batch, x + 16, 128, remainingWidth, 1, Palette.F1);
        }

        if (state.attachmentId() != null) {
            label(batch, "MODULE", x, 146);
            rect(batch, x, 156, 17, 17, Palette.G2);
            outline(batch, x, 156, 17, 17, Palette.G3);
            if (state.attachmentName() != null) {
                value(batch, state.attachmentName(), x + 22, 161, Palette.N7);
            }
        }
    }

    private void drawRightPlate(SpriteBatch batch, PlayerHudState state) {
        label(batch, "SCORE", RIGHT_COLUMN_X, 14);
        String score = zeroPadded(state.score(), 7);
        layout.setText(fontTitle, score);
        title(batch, score, RIGHT_COLUMN_RIGHT_EDGE - layout.width, 24, Palette.N7);
    }

    private void label(SpriteBatch batch, String text, int x, int yDown) {
        fontMini.setColor(Palette.N4);
        fontMini.draw(batch, text, x, yGdx(yDown, fontMini.getCapHeight()));
    }

    private void value(SpriteBatch batch, String text, int x, int yDown, Color color) {
        fontMini.setColor(color);
        fontMini.draw(batch, text, x, yGdx(yDown, fontMini.getCapHeight()));
    }

    private void title(SpriteBatch batch, String text, float x, int yDown, Color color) {
        fontTitle.setColor(color);
        fontTitle.draw(batch, text, x, yGdx(yDown, fontTitle.getCapHeight()));
    }

    private void rect(SpriteBatch batch, float xDown, float yDown, float w, float h, Color color) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        batch.setColor(color);
        batch.draw(pixel, xDown, yGdx(yDown, h), w, h);
        batch.setColor(Color.WHITE);
    }

    /** A one-pixel-thick rectangle outline, drawn as four filled rects to avoid a second texture. */
    private void outline(SpriteBatch batch, float xDown, float yDown, float w, float h, Color color) {
        rect(batch, xDown, yDown, w, 1, color);
        rect(batch, xDown, yDown + h - 1, w, 1, color);
        rect(batch, xDown, yDown, 1, h, color);
        rect(batch, xDown + w - 1, yDown, 1, h, color);
    }

    /**
     * Converts the design document's y-down coordinate, measured from an element's top edge, to
     * libGDX's y-up space — {@code y_gdx = 270 - y_down - height}, exactly as
     * {@code docs/design/04-hud-layout.md} specifies.
     */
    private static float yGdx(float yDown, float height) {
        return LittleSpaceshipGame.LOGICAL_HEIGHT - yDown - height;
    }

    private static String zeroPadded(int value, int digits) {
        String raw = Integer.toString(Math.max(0, value));
        StringBuilder sb = new StringBuilder();
        for (int i = raw.length(); i < digits; i++) {
            sb.append('0');
        }
        sb.append(raw);
        return sb.toString();
    }

    public void dispose() {
        pixel.dispose();
    }
}
