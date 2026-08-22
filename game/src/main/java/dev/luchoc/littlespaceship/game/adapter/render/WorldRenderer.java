package dev.luchoc.littlespaceship.game.adapter.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.luchoc.littlespaceship.core.port.InvulnerabilitySource;
import dev.luchoc.littlespaceship.core.port.PlayerStatus;
import dev.luchoc.littlespaceship.core.port.SpriteId;
import dev.luchoc.littlespaceship.core.port.SpriteVisitor;
import dev.luchoc.littlespaceship.core.port.WorldView;
import dev.luchoc.littlespaceship.game.ui.Palette;
import java.util.HashSet;
import java.util.Set;

/**
 * Draws the simulation. The only thing this class ever asks the core for is a {@link WorldView}; it
 * never touches a component or an entity handle.
 *
 * <p>It implements {@link SpriteVisitor} itself instead of handing {@link WorldView#forEachSprite}
 * a lambda, which would otherwise allocate a new instance every call. Implementing the interface on
 * a field that already exists is what keeps the whole draw call free of per-entity allocation, the
 * property the visitor pattern exists to protect per {@code 12-architecture.md}.
 *
 * <p><b>The coordinate space it draws in is not the logical resolution.</b> {@code core.domain}'s
 * {@code Transform.x} is measured from the playfield's own left edge, in {@code [0, 208]} —
 * confirmed against {@code MotionSystem.PLAYFIELD_WIDTH} clamping the player to that exact range and
 * {@code SpawnSystem} computing a wave's anchor as {@code atX * PLAYFIELD_WIDTH} — while the logical
 * resolution is 480 wide with the playfield centred inside it. Drawing {@code x} unmodified puts
 * every entity in the left HUD margin instead of the playfield; {@code playfieldLeft} is the
 * correction. {@code y} needs no such shift: the playfield is the full 270 logical units tall, so
 * {@code Transform.y} already lines up with screen space.
 *
 * <p><b>Invulnerability is shown on the ship</b>, per {@code 04-hud-layout.md}'s "Invulnerability is
 * shown on the ship, not in the plate": respawn blinks the sprite out, damage absorbed by the shield
 * or the attachment flashes it white, and the power-up draws a glow ring behind it. Telling the
 * player's ship apart from every other sprite this class draws needs a content id, since {@link
 * SpriteVisitor#accept} carries no "this is the player" flag — {@link #PLAYER_SPRITE_ID} matches
 * {@code Simulation.PLAYER_SPRITE}'s value, the same way {@code HudRenderer} already treats
 * {@code attachmentId} as a plain content id crossing the boundary rather than as domain machinery.
 *
 * <p><b>The boss's tell</b> reads the same way: {@code BossSystem} steps a charging pod's or arm's
 * {@code Sprite.frame} 1 through 3 across the 0.75 s tell and drops it to 0 the instant the shot
 * leaves, per {@code docs/design/06-boss-presentation.md}. This class turns that frame into the
 * three-beat charge the document specifies — beat 1 tints the part {@code W4}, beat 2 tints it
 * {@code F1}, beat 3 traces a 1 px {@code N7} outline around it — entirely as a colour/outline
 * overlay on whatever region {@code sprite} resolves to, since the real art (an animated iris/rim)
 * lives on {@code feat/sprite-production}, not merged here. {@code boss-core} never receives a
 * nonzero frame — the core never charges — so it is never mistaken for a tell target.
 */
public final class WorldRenderer implements SpriteVisitor {

    /**
     * Matches {@code core.application.Simulation.PLAYER_SPRITE}'s value. Not exposed through
     * {@code core.port} today — see the class javadoc for why matching the content id is still the
     * right call rather than reaching back into the domain for a "which entity is the player" flag.
     */
    private static final String PLAYER_SPRITE_ID = "ship-basic";

    /** Respawn blinks 4 ticks drawn, 4 ticks dimmed, per {@code 04-hud-layout.md}. */
    private static final int RESPAWN_BLINK_TICKS = 4;
    private static final float RESPAWN_BLINK_ALPHA = 0.35f;

    /** Damage absorbed by the shield or the attachment flashes 3 ticks tinted, 3 ticks normal. */
    private static final int DAMAGE_FLASH_TICKS = 3;

    /** The power-up's glow ring, 21x21 per {@code 04-hud-layout.md}. */
    private static final float AURA_SIZE = 21f;

    /** The boss tell's three beats, per {@code docs/design/06-boss-presentation.md}. */
    private static final int TELL_BEAT_1 = 1;
    private static final int TELL_BEAT_2 = 2;
    private static final int TELL_BEAT_3 = 3;

    private final SpriteAtlas atlas;
    private final float playfieldLeft;
    private final Texture pixel;

    /**
     * Sprite ids already reported missing from {@link #atlas}, so a gap in placeholder art logs
     * once per id instead of once per frame — sixty times a second would flood the log and hide the
     * one line that matters. Not a per-frame allocation: only touched on the cold path where
     * {@code region} is null, which is rare by construction once real art lands.
     */
    private final Set<String> missingSpritesLogged = new HashSet<>();

    private SpriteBatch batch;
    private PlayerStatus playerStatus = PlayerStatus.NONE;

    /**
     * Counts ticks spent in the current {@link InvulnerabilitySource}, reset whenever the source
     * changes — that is what drives the blink/flash phase without a clock, matching {@code
     * CLAUDE.md}'s determinism rule that presentation should not need one either.
     */
    private int sourceTicks;
    private InvulnerabilitySource previousSource = InvulnerabilitySource.NONE;

    /**
     * @param atlas resolves a content sprite id to the region that draws it
     * @param playfieldLeft the playfield's left edge in logical units, added to every entity's
     *     {@code x} before drawing — see the class javadoc for why this is needed at all
     */
    public WorldRenderer(SpriteAtlas atlas, float playfieldLeft) {
        if (atlas == null) {
            throw new IllegalArgumentException("the renderer needs an atlas to resolve sprites");
        }
        this.atlas = atlas;
        this.playfieldLeft = playfieldLeft;
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        this.pixel = new Texture(pm);
        pm.dispose();
    }

    /**
     * Draws every sprite in the given view, using the batch that is already mid-{@code begin()}.
     *
     * @param view what to draw, read-only
     * @param batch the batch to draw into; must already be between {@code begin()} and {@code end()}
     * @param status the player's current status, read once per frame — needed here (not only in the
     *     HUD) to decide how the ship itself is drawn
     */
    public void draw(WorldView view, SpriteBatch batch, PlayerStatus status) {
        this.batch = batch;
        this.playerStatus = status;
        InvulnerabilitySource source = status.invulnerabilitySource();
        if (source != previousSource) {
            previousSource = source;
            sourceTicks = 0;
        } else {
            sourceTicks++;
        }
        view.forEachSprite(this);
        this.batch = null;
    }

    /**
     * Called once per drawable entity by {@link WorldView#forEachSprite}. Positions are logical
     * units with the origin at the entity's centre — the same convention
     * {@code docs/design/02-sprite-sizes.md} uses for collider offsets — so every sprite is drawn
     * centred on it.
     */
    @Override
    public void accept(SpriteId sprite, float x, float y, int frame, float rotation) {
        TextureRegion region = atlas.region(sprite);
        if (region == null) {
            // A content id with no placeholder registered yet. Skipping it beats crashing the
            // render loop over an asset that has not arrived, but a silent skip is only right while
            // nobody could look it up — log it once so a typo'd id is loud instead of invisible once
            // real art starts being wired against these same strings.
            if (missingSpritesLogged.add(sprite.value())) {
                Gdx.app.error("WorldRenderer", "no placeholder region for sprite id '"
                    + sprite.value() + "', skipping");
            }
            return;
        }
        float logicalX = x + playfieldLeft;
        float width = region.getRegionWidth();
        float height = region.getRegionHeight();

        boolean isPlayer = PLAYER_SPRITE_ID.equals(sprite.value());
        InvulnerabilitySource source = isPlayer ? playerStatus.invulnerabilitySource()
            : InvulnerabilitySource.NONE;

        if (source == InvulnerabilitySource.POWERUP) {
            drawAura(logicalX, y);
        }

        float alpha = 1f;
        Color tint = Color.WHITE;
        if (source == InvulnerabilitySource.RESPAWN) {
            boolean dimmed = (sourceTicks / RESPAWN_BLINK_TICKS) % 2 == 1;
            if (dimmed) {
                alpha = RESPAWN_BLINK_ALPHA;
            }
        } else if (source == InvulnerabilitySource.DAMAGE) {
            boolean flashed = (sourceTicks / DAMAGE_FLASH_TICKS) % 2 == 0;
            if (flashed) {
                tint = Palette.N7;
            }
        } else if (frame == TELL_BEAT_1) {
            tint = Palette.W4;
        } else if (frame == TELL_BEAT_2) {
            tint = Palette.F1;
        }

        batch.setColor(tint.r, tint.g, tint.b, alpha);
        batch.draw(region,
            logicalX - width / 2f, y - height / 2f,
            width / 2f, height / 2f,
            width, height,
            1f, 1f,
            rotation);
        batch.setColor(Color.WHITE);

        if (frame == TELL_BEAT_3) {
            drawTellOutline(logicalX, y, width, height);
        }
    }

    /**
     * The tell's third beat: a 1 px {@code N7} outline traced around the whole charging part, held
     * steady until the shot leaves, per {@code docs/design/06-boss-presentation.md}. Reuses
     * {@link #pixel} the same way {@link #drawAura} already does, rather than a second texture.
     */
    private void drawTellOutline(float centerX, float centerY, float width, float height) {
        float left = centerX - width / 2f;
        float bottom = centerY - height / 2f;
        batch.setColor(Palette.N7);
        batch.draw(pixel, left, bottom, width, 1f);
        batch.draw(pixel, left, bottom + height - 1f, width, 1f);
        batch.draw(pixel, left, bottom, 1f, height);
        batch.draw(pixel, left + width - 1f, bottom, 1f, height);
        batch.setColor(Color.WHITE);
    }

    /** The power-up's glow ring, drawn as a four-sided outline behind the ship, in {@code C1}. */
    private void drawAura(float centerX, float centerY) {
        float half = AURA_SIZE / 2f;
        float left = centerX - half;
        float bottom = centerY - half;
        batch.setColor(Palette.C1);
        batch.draw(pixel, left, bottom, AURA_SIZE, 1f);
        batch.draw(pixel, left, bottom + AURA_SIZE - 1f, AURA_SIZE, 1f);
        batch.draw(pixel, left, bottom, 1f, AURA_SIZE);
        batch.draw(pixel, left + AURA_SIZE - 1f, bottom, 1f, AURA_SIZE);
        batch.setColor(Color.WHITE);
    }

    public void dispose() {
        pixel.dispose();
    }
}
