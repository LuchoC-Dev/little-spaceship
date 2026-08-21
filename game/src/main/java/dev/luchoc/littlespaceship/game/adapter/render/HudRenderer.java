package dev.luchoc.littlespaceship.game.adapter.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.InvulnerabilitySource;
import dev.luchoc.littlespaceship.core.port.PlayerStatus;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import dev.luchoc.littlespaceship.game.ui.Palette;

/**
 * Draws the two side plates of {@code docs/design/04-hud-layout.md} — everything the player reads
 * about their own ship — from the live {@link PlayerStatus} {@code WorldView.player()} returns.
 *
 * <p>Coordinates in this class are exactly the ones the design document tables give, in its own
 * y-down convention; {@link #yGdx} is the one place the flip to libGDX's y-up space happens, per the
 * document's own warning that doing it anywhere else is how a HUD ends up half-flipped.
 *
 * <p>The boss bar is deliberately not drawn: it needs a boss health signal {@code core.port} does
 * not expose yet (a {@code BossStatus}, out of this phase's scope — phase 07's concern) and the
 * document itself says it only ever appears during that fight, so its absence here is the same
 * "only during the fight" rule applied to a fight that cannot happen yet.
 *
 * <p>{@code maxLives}, {@code maxBombs} and {@code maxWeaponLevel} come from {@link BalanceValues}
 * at construction, not per frame: they are run-wide caps, and {@link PlayerStatus} only ever reports
 * the current value, per {@code 04-hud-layout.md}'s "five life slots and three bomb slots are always
 * drawn". The same source supplies the three invulnerability durations, needed to turn {@link
 * PlayerStatus#invulnerabilityRemaining()} — a raw second count — into the fraction the timer bar
 * shrinks by.
 */
public final class HudRenderer {

    private static final int LEFT_COLUMN_X = 12;
    private static final int RIGHT_COLUMN_X = 362;
    private static final int RIGHT_COLUMN_RIGHT_EDGE = 467;

    private final BitmapFont fontMini;
    private final BitmapFont fontTitle;
    private final Texture pixel;
    private final GlyphLayout layout = new GlyphLayout();

    private final int maxLives;
    private final int maxBombs;
    private final int maxWeaponLevel;
    private final float respawnDuration;
    private final float damageDuration;
    private final float powerupDuration;

    public HudRenderer(Skin skin, BalanceValues balance) {
        this.fontMini = skin.getFont("font-mini");
        this.fontTitle = skin.getFont("font-title");
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        this.pixel = new Texture(pm);
        pm.dispose();

        this.maxLives = balance.maxLives();
        this.maxBombs = balance.maxBombs();
        this.maxWeaponLevel = balance.weaponLevels();
        this.respawnDuration = balance.respawnInvulnerability();
        this.damageDuration = balance.damageInvulnerability();
        this.powerupDuration = balance.invulnerabilityPickupDuration();
    }

    /**
     * Draws both plates. The batch must already be between {@code begin()} and {@code end()}, and
     * projected onto the logical 480x270 space {@link LittleSpaceshipGame} uses.
     */
    public void draw(SpriteBatch batch, PlayerStatus status) {
        drawFrame(batch);
        drawLeftPlate(batch, status);
        drawRightPlate(batch, status);
    }

    /** The plates' own fill and the two playfield rules, per the region table in {@code 04-hud-layout.md}. */
    private void drawFrame(SpriteBatch batch) {
        rect(batch, 0, 0, 135, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N2);
        rect(batch, 135, 0, 1, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N3);
        rect(batch, 344, 0, 1, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N3);
        rect(batch, 345, 0, 135, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N2);
    }

    private void drawLeftPlate(SpriteBatch batch, PlayerStatus status) {
        int x = LEFT_COLUMN_X;

        label(batch, "LIVES", x, 14);
        for (int i = 0; i < maxLives; i++) {
            boolean filled = i < status.lives();
            rect(batch, x + i * 12, 24, 9, 9, filled ? Palette.N6 : Palette.N2);
            outline(batch, x + i * 12, 24, 9, 9, filled ? Palette.N0 : Palette.N3);
        }

        label(batch, "BOMBS", x, 44);
        for (int i = 0; i < maxBombs; i++) {
            boolean filled = i < status.bombs();
            rect(batch, x + i * 12, 54, 9, 9, filled ? Palette.N6 : Palette.N2);
            outline(batch, x + i * 12, 54, 9, 9, filled ? Palette.W4 : Palette.N3);
        }

        label(batch, "POWER", x, 74);
        for (int i = 0; i < maxWeaponLevel; i++) {
            boolean filled = i < status.weaponLevel();
            rect(batch, x + i * 15, 84, 13, 7, filled ? Palette.C1 : Palette.N2);
            if (filled) {
                rect(batch, x + i * 15, 84, 13, 2, Palette.C2);
            } else {
                outline(batch, x + i * 15, 84, 13, 7, Palette.N3);
            }
        }

        label(batch, "STATE", x, 104);
        if (status.shieldActive()) {
            rect(batch, x, 114, 13, 13, Palette.C1);
            outline(batch, x, 114, 13, 13, Palette.N6);
        }
        InvulnerabilitySource source = status.invulnerabilitySource();
        if (source != InvulnerabilitySource.NONE) {
            Color iconColor = switch (source) {
                case POWERUP -> Palette.C1;
                case DAMAGE -> Palette.N7;
                case RESPAWN -> Palette.N4;
                case NONE -> Palette.N2;
            };
            rect(batch, x + 16, 114, 13, 13, iconColor);
            outline(batch, x + 16, 114, 13, 13, Palette.W4);
            float total = totalDuration(source);
            float fraction = total > 0f ? status.invulnerabilityRemaining() / total : 0f;
            float remainingWidth = 13f * Math.max(0f, Math.min(1f, fraction));
            rect(batch, x + 16, 128, 13, 1, Palette.N2);
            rect(batch, x + 16, 128, remainingWidth, 1, Palette.F1);
        }

        if (!status.attachmentId().isEmpty()) {
            label(batch, "MODULE", x, 146);
            rect(batch, x, 156, 17, 17, Palette.G2);
            outline(batch, x, 156, 17, 17, Palette.G3);
            value(batch, attachmentLabel(status.attachmentId()), x + 22, 161, Palette.N7);
        }
    }

    /**
     * A display label from a content id such as {@code "attachment-missiles"}: no {@code
     * AttachmentDefinition} field carries a human name yet, so this uppercases the id and drops any
     * {@code "attachment-"} prefix, truncated to the 13-character column {@code 04-hud-layout.md}
     * fixes. A real display name, if content ever needs one distinct from its id, replaces this
     * method's body only.
     */
    private static String attachmentLabel(String attachmentId) {
        String label = attachmentId.replace("attachment-", "").replace('-', ' ').toUpperCase();
        return label.length() > 13 ? label.substring(0, 13) : label;
    }

    private float totalDuration(InvulnerabilitySource source) {
        return switch (source) {
            case RESPAWN -> respawnDuration;
            case DAMAGE -> damageDuration;
            case POWERUP -> powerupDuration;
            case NONE -> 0f;
        };
    }

    private void drawRightPlate(SpriteBatch batch, PlayerStatus status) {
        label(batch, "SCORE", RIGHT_COLUMN_X, 14);
        String score = zeroPadded(status.score(), 7);
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
