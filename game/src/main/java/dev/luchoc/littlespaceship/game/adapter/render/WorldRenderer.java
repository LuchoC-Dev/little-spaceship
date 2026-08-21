package dev.luchoc.littlespaceship.game.adapter.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.luchoc.littlespaceship.core.port.SpriteId;
import dev.luchoc.littlespaceship.core.port.SpriteVisitor;
import dev.luchoc.littlespaceship.core.port.WorldView;
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
 */
public final class WorldRenderer implements SpriteVisitor {

    private final PlaceholderAtlas atlas;
    private final float playfieldLeft;

    /**
     * Sprite ids already reported missing from {@link #atlas}, so a gap in placeholder art logs
     * once per id instead of once per frame — sixty times a second would flood the log and hide the
     * one line that matters. Not a per-frame allocation: only touched on the cold path where
     * {@code region} is null, which is rare by construction once real art lands.
     */
    private final Set<String> missingSpritesLogged = new HashSet<>();

    private SpriteBatch batch;

    /**
     * @param atlas resolves a content sprite id to the region that draws it
     * @param playfieldLeft the playfield's left edge in logical units, added to every entity's
     *     {@code x} before drawing — see the class javadoc for why this is needed at all
     */
    public WorldRenderer(PlaceholderAtlas atlas, float playfieldLeft) {
        if (atlas == null) {
            throw new IllegalArgumentException("the renderer needs an atlas to resolve sprites");
        }
        this.atlas = atlas;
        this.playfieldLeft = playfieldLeft;
    }

    /**
     * Draws every sprite in the given view, using the batch that is already mid-{@code begin()}.
     *
     * @param view what to draw, read-only
     * @param batch the batch to draw into; must already be between {@code begin()} and {@code end()}
     */
    public void draw(WorldView view, SpriteBatch batch) {
        this.batch = batch;
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
        batch.draw(region,
            logicalX - width / 2f, y - height / 2f,
            width / 2f, height / 2f,
            width, height,
            1f, 1f,
            rotation);
    }
}
