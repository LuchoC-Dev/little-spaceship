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
 *
 * <p><b>Feedback for hits and lost upgrades</b> comes from a frame-over-frame diff of {@link
 * PlayerStatus} against the value {@link #draw} was last called with, kept in {@link
 * #previousStatus} — there is no event stream to read this from, and {@code core} does not need one
 * for it, since every case in {@code 04-hud-layout.md}'s "Feedback" table is a change already visible
 * in the snapshot ({@code lives} going down, {@code shieldActive} going false, and so on). One case
 * from that table is not built: "pickup collected at maximum" cannot be told apart from an ordinary
 * kill by diffing {@link PlayerStatus} alone — {@code enemy-tank}'s kill score and {@code
 * maxedPickupBonus} are both 500 points in {@code assets/data}, so a same-value {@code score} jump is
 * ambiguous without a signal from {@code core} naming the cause. Flashing on every {@code score}
 * increase would fire on every kill too, which is not what the document asks for, so this renderer
 * leaves that one case out rather than guess it.
 */
public final class HudRenderer {

    private static final int LEFT_COLUMN_X = 12;
    private static final int RIGHT_COLUMN_X = 362;
    private static final int RIGHT_COLUMN_RIGHT_EDGE = 467;

    /** Tick counts from {@code 04-hud-layout.md}'s "Feedback" table. */
    private static final int LIFE_LOST_FLASH_TICKS = 6;
    private static final int BOMB_USED_FLASH_TICKS = 2;
    private static final int SHIELD_LOST_FLASH_TICKS = 3;
    private static final int ATTACHMENT_LOST_FLASH_TICKS = 3;
    private static final int WEAPON_GAINED_FLASH_TICKS = 4;
    private static final int RULE_FLASH_N7_TICKS = 2;
    private static final int RULE_FLASH_W3_TICKS = 4;
    private static final int RULE_FLASH_TOTAL_TICKS = RULE_FLASH_N7_TICKS + RULE_FLASH_W3_TICKS;

    private final BitmapFont fontMini;
    private final BitmapFont fontTitle;
    private final Texture pixel;
    private final GlyphLayout layout = new GlyphLayout();

    private final int maxLives;
    private final int maxBombs;
    private final int maxWeaponLevel;

    /**
     * The power-up's own duration, needed to turn {@link PlayerStatus#invulnerabilityRemaining()}
     * into the plate timer's fraction. Respawn and damage no longer read a duration here: both are
     * drawn entirely on the ship by {@code WorldRenderer}, on a fixed tick cadence rather than a
     * fraction of their {@code BalanceValues} duration — see that class for why.
     */
    private final float powerupDuration;

    /**
     * {@code null} until the first {@link #draw} call, so the very first frame — where every field
     * of a freshly spawned player reads as "gained" against an empty {@link PlayerStatus#NONE} —
     * never fires a flash for state that was never lost or gained, only started that way.
     */
    private PlayerStatus previousStatus;

    private int lifeLostIndex = -1;
    private int lifeLostTicks;
    private int bombUsedIndex = -1;
    private int bombUsedTicks;
    private int shieldLostTicks;
    private int attachmentLostTicks;
    private String attachmentLostId = "";
    private int weaponGainedIndex = -1;
    private int weaponGainedTicks;
    private int ruleFlashTicks;

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
        this.powerupDuration = balance.invulnerabilityPickupDuration();
    }

    /**
     * Draws both plates. The batch must already be between {@code begin()} and {@code end()}, and
     * projected onto the logical 480x270 space {@link LittleSpaceshipGame} uses.
     */
    public void draw(SpriteBatch batch, PlayerStatus status) {
        updateFeedback(status);
        drawFrame(batch);
        drawLeftPlate(batch, status);
        drawRightPlate(batch, status);
        decrementFeedback();
    }

    /**
     * Compares {@code status} against {@link #previousStatus} and starts a flash timer for every
     * event {@code 04-hud-layout.md}'s "Feedback" table names that just happened. Called once per
     * {@link #draw}, before drawing, so the same frame that shows the loss also starts the flash.
     */
    private void updateFeedback(PlayerStatus status) {
        if (previousStatus == null) {
            previousStatus = status;
            return;
        }
        if (status.lives() < previousStatus.lives()) {
            lifeLostIndex = previousStatus.lives() - 1;
            lifeLostTicks = LIFE_LOST_FLASH_TICKS;
            ruleFlashTicks = RULE_FLASH_TOTAL_TICKS;
        }
        if (status.bombs() < previousStatus.bombs()) {
            bombUsedIndex = previousStatus.bombs() - 1;
            bombUsedTicks = BOMB_USED_FLASH_TICKS;
        }
        if (previousStatus.shieldActive() && !status.shieldActive()) {
            shieldLostTicks = SHIELD_LOST_FLASH_TICKS;
        }
        if (!previousStatus.attachmentId().isEmpty() && status.attachmentId().isEmpty()) {
            attachmentLostTicks = ATTACHMENT_LOST_FLASH_TICKS;
            attachmentLostId = previousStatus.attachmentId();
        }
        if (status.weaponLevel() > previousStatus.weaponLevel()) {
            weaponGainedIndex = status.weaponLevel() - 1;
            weaponGainedTicks = WEAPON_GAINED_FLASH_TICKS;
        }
        previousStatus = status;
    }

    /** Counts every flash timer down by one tick, floored at zero. Called once per {@link #draw}. */
    private void decrementFeedback() {
        if (lifeLostTicks > 0) {
            lifeLostTicks--;
        }
        if (bombUsedTicks > 0) {
            bombUsedTicks--;
        }
        if (shieldLostTicks > 0) {
            shieldLostTicks--;
        }
        if (attachmentLostTicks > 0) {
            attachmentLostTicks--;
        }
        if (weaponGainedTicks > 0) {
            weaponGainedTicks--;
        }
        if (ruleFlashTicks > 0) {
            ruleFlashTicks--;
        }
    }

    /**
     * The plates' own fill and the two playfield rules, per the region table in
     * {@code 04-hud-layout.md}. The rules also carry the life-lost flash — {@code N7} for the first
     * {@link #RULE_FLASH_N7_TICKS} ticks, then {@code W3} for the rest — per the "Feedback" table's
     * "both playfield rules flash".
     */
    private void drawFrame(SpriteBatch batch) {
        Color ruleColor = Palette.N3;
        if (ruleFlashTicks > RULE_FLASH_W3_TICKS) {
            ruleColor = Palette.N7;
        } else if (ruleFlashTicks > 0) {
            ruleColor = Palette.W3;
        }
        rect(batch, 0, 0, 135, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N2);
        rect(batch, 135, 0, 1, LittleSpaceshipGame.LOGICAL_HEIGHT, ruleColor);
        rect(batch, 344, 0, 1, LittleSpaceshipGame.LOGICAL_HEIGHT, ruleColor);
        rect(batch, 345, 0, 135, LittleSpaceshipGame.LOGICAL_HEIGHT, Palette.N2);
    }

    private void drawLeftPlate(SpriteBatch batch, PlayerStatus status) {
        int x = LEFT_COLUMN_X;

        label(batch, "LIVES", x, 14);
        for (int i = 0; i < maxLives; i++) {
            boolean flashingLost = i == lifeLostIndex && lifeLostTicks > 0;
            boolean filled = i < status.lives() || flashingLost;
            Color fill = flashingLost ? Palette.N7 : (filled ? Palette.N6 : Palette.N2);
            Color edge = flashingLost ? Palette.N7 : (filled ? Palette.N0 : Palette.N3);
            rect(batch, x + i * 12, 24, 9, 9, fill);
            outline(batch, x + i * 12, 24, 9, 9, edge);
        }

        label(batch, "BOMBS", x, 44);
        for (int i = 0; i < maxBombs; i++) {
            boolean flashingUsed = i == bombUsedIndex && bombUsedTicks > 0;
            boolean filled = i < status.bombs() || flashingUsed;
            Color fill = flashingUsed ? Palette.N7 : (filled ? Palette.N6 : Palette.N2);
            Color edge = flashingUsed ? Palette.N7 : (filled ? Palette.W4 : Palette.N3);
            rect(batch, x + i * 12, 54, 9, 9, fill);
            outline(batch, x + i * 12, 54, 9, 9, edge);
        }

        label(batch, "POWER", x, 74);
        for (int i = 0; i < maxWeaponLevel; i++) {
            boolean filled = i < status.weaponLevel();
            boolean flashingGained = i == weaponGainedIndex && weaponGainedTicks > 0;
            rect(batch, x + i * 15, 84, 13, 7, filled ? Palette.C1 : Palette.N2);
            if (filled) {
                rect(batch, x + i * 15, 84, 13, 2, flashingGained ? Palette.F1 : Palette.C2);
            } else {
                outline(batch, x + i * 15, 84, 13, 7, Palette.N3);
            }
        }

        label(batch, "STATE", x, 104);
        if (status.shieldActive()) {
            rect(batch, x, 114, 13, 13, Palette.C1);
            outline(batch, x, 114, 13, 13, Palette.N6);
        } else if (shieldLostTicks > 0) {
            rect(batch, x, 114, 13, 13, Palette.N7);
            outline(batch, x, 114, 13, 13, Palette.N7);
        }

        // The plate icon is reserved for the power-up per "Invulnerability is shown on the ship, not
        // in the plate": respawn and damage grace periods read on the ship itself, drawn by
        // WorldRenderer, and never touch this widget.
        if (status.invulnerabilitySource() == InvulnerabilitySource.POWERUP) {
            rect(batch, x + 16, 114, 13, 13, Palette.W4);
            outline(batch, x + 16, 114, 13, 13, Palette.F1);
            float fraction = powerupDuration > 0f
                ? status.invulnerabilityRemaining() / powerupDuration : 0f;
            float remainingWidth = 13f * Math.max(0f, Math.min(1f, fraction));
            rect(batch, x + 16, 128, 13, 1, Palette.N2);
            rect(batch, x + 16, 128, remainingWidth, 1, Palette.F1);
        }

        boolean attachmentLostFlash = attachmentLostTicks > 0 && status.attachmentId().isEmpty();
        if (!status.attachmentId().isEmpty()) {
            label(batch, "MODULE", x, 146);
            rect(batch, x, 156, 17, 17, Palette.G2);
            outline(batch, x, 156, 17, 17, Palette.G3);
            value(batch, attachmentLabel(status.attachmentId()), x + 22, 161, Palette.N7);
        } else if (attachmentLostFlash) {
            label(batch, "MODULE", x, 146);
            rect(batch, x, 156, 17, 17, Palette.N7);
            outline(batch, x, 156, 17, 17, Palette.N7);
            value(batch, attachmentLabel(attachmentLostId), x + 22, 161, Palette.N7);
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
