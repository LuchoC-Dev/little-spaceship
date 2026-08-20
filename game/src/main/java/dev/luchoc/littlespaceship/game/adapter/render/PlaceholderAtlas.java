package dev.luchoc.littlespaceship.game.adapter.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.luchoc.littlespaceship.core.port.SpriteId;
import java.util.HashMap;
import java.util.Map;

/**
 * Placeholder art, generated in code into a single texture instead of loaded from disk.
 *
 * <p>Phase 03 has no content pipeline yet — {@code docs/plan/03-first-playable/plan.md} says so
 * explicitly — so there are no real sprite files to load. What matters this phase is that the
 * silhouette sizes and the collider radii already agree, per {@code docs/design/02-sprite-sizes.md}:
 * a placeholder whose size lies would force hitbox rework once real art arrives. Colours come from
 * the closed {@code ls32} palette in {@code docs/design/01-palette.md}.
 *
 * <p>Everything a {@link dev.luchoc.littlespaceship.core.port.SpriteId} can resolve to lives in one
 * {@link Texture}, so drawing the world only ever binds one texture. The checkerboard distortion
 * probe is deliberately not here: it needs {@code Repeat} texture wrapping to tile across the
 * playfield, which would bleed into neighbouring regions if it shared this atlas — see
 * {@link CheckerboardBackground}.
 */
public final class PlaceholderAtlas {

    /** {@code docs/design/02-sprite-sizes.md}: the basic player ship, 15x17. */
    public static final SpriteId SHIP_BASIC = new SpriteId("ship-basic");

    private static final int ATLAS_SIZE = 32;
    private static final int SHIP_WIDTH = 15;
    private static final int SHIP_HEIGHT = 17;

    // ls32, docs/design/01-palette.md.
    private static final int N0_OUTLINE = 0x0B0E14FF;
    private static final int N5_HULL_SHADE = 0x8D9CB5FF;
    private static final int N6_HULL_LIGHT = 0xC9D6E8FF;
    private static final int C1_ENGINE = 0x2FBFD4FF;
    private static final int C2_ENGINE_CORE = 0x9DF2FAFF;

    private final Texture texture;
    private final Map<String, TextureRegion> regions = new HashMap<>();

    public PlaceholderAtlas() {
        Pixmap pixmap = new Pixmap(ATLAS_SIZE, ATLAS_SIZE, Pixmap.Format.RGBA8888);

        drawShip(pixmap, 1, 1);

        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();

        regions.put(SHIP_BASIC.value(), new TextureRegion(texture, 1, 1, SHIP_WIDTH, SHIP_HEIGHT));
    }

    /**
     * A simple hull silhouette, correctly sized and centred the way a real sprite would be: the
     * collider in {@code docs/design/02-sprite-sizes.md} is concentric with this box.
     */
    private static void drawShip(Pixmap pixmap, int originX, int originY) {
        // Outline.
        setColor(pixmap, N0_OUTLINE);
        pixmap.drawRectangle(originX, originY, SHIP_WIDTH, SHIP_HEIGHT);
        // Hull fill.
        setColor(pixmap, N5_HULL_SHADE);
        pixmap.fillRectangle(originX + 1, originY + 1, SHIP_WIDTH - 2, SHIP_HEIGHT - 2);
        // Canopy highlight along the nose.
        setColor(pixmap, N6_HULL_LIGHT);
        pixmap.fillRectangle(originX + 5, originY + 2, SHIP_WIDTH - 10, 5);
        // Engine glow at the tail.
        setColor(pixmap, C1_ENGINE);
        pixmap.fillRectangle(originX + 5, originY + SHIP_HEIGHT - 4, SHIP_WIDTH - 10, 3);
        setColor(pixmap, C2_ENGINE_CORE);
        pixmap.fillRectangle(originX + 6, originY + SHIP_HEIGHT - 3, SHIP_WIDTH - 12, 1);
    }

    private static void setColor(Pixmap pixmap, int rgba8888) {
        pixmap.setColor(
            ((rgba8888 >>> 24) & 0xFF) / 255f,
            ((rgba8888 >>> 16) & 0xFF) / 255f,
            ((rgba8888 >>> 8) & 0xFF) / 255f,
            (rgba8888 & 0xFF) / 255f);
    }

    /**
     * Resolves a content sprite id to the region that draws it.
     *
     * @param sprite the id the core handed the visitor
     * @return the region to draw, or null if this placeholder set does not cover it yet
     */
    public TextureRegion region(SpriteId sprite) {
        return regions.get(sprite.value());
    }

    public void dispose() {
        texture.dispose();
    }
}
