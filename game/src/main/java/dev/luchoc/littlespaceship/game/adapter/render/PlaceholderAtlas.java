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
 * <p>Phase 04's content pipeline names seven content sprite ids across the level 1 roster; no real
 * art file exists for any of them yet, so every one is a generated silhouette here. What matters is
 * that sizes and collider radii already agree with {@code docs/design/02-sprite-sizes.md} —
 * synchronisation point 1 — since a placeholder whose silhouette lies about size would force hitbox
 * rework once real art arrives. Colours come from the closed {@code ls32} palette in
 * {@code docs/design/01-palette.md}: {@code C1}/{@code C2} for the player's engine, the alien hull
 * ramp {@code V2 -> V3 -> V4} for every enemy, {@code N0} for every outline.
 *
 * <p>Every sprite lives in one {@link Texture}, packed left to right with a 1px gap between entries,
 * so drawing the world only ever binds one texture. The checkerboard distortion probe is deliberately
 * not here: it needs {@code Repeat} texture wrapping to tile across the playfield, which would bleed
 * into neighbouring regions if it shared this atlas — see {@link CheckerboardBackground}.
 */
public final class PlaceholderAtlas {

    // ls32, docs/design/01-palette.md.
    private static final int N0_OUTLINE = 0x0B0E14FF;
    private static final int N5_HULL_SHADE = 0x8D9CB5FF;
    private static final int N6_HULL_LIGHT = 0xC9D6E8FF;
    private static final int C1_ENGINE = 0x2FBFD4FF;
    private static final int C2_ENGINE_CORE = 0x9DF2FAFF;
    private static final int V2_ALIEN_DARK = 0x382050FF;
    private static final int V3_ALIEN_MID = 0x58347AFF;
    private static final int V4_ALIEN_LIGHT = 0x8E5CB8FF;

    /** Every sprite id and size this placeholder set covers, from {@code docs/design/02-sprite-sizes.md}. */
    private static final Sprite[] SPRITES = {
        new Sprite("ship-basic", 15, 17),
        new Sprite("enemy-basic", 13, 13),
        new Sprite("enemy-light", 11, 13),
        new Sprite("enemy-shooter", 15, 15),
        new Sprite("enemy-rush", 9, 15),
        new Sprite("enemy-tank", 23, 23),
        new Sprite("enemy-carrier", 39, 31),
    };

    private static final int GAP = 1;

    private final Texture texture;
    private final Map<String, TextureRegion> regions = new HashMap<>();

    public PlaceholderAtlas() {
        int atlasWidth = GAP;
        int atlasHeight = 0;
        for (Sprite sprite : SPRITES) {
            atlasWidth += sprite.width + GAP;
            atlasHeight = Math.max(atlasHeight, sprite.height);
        }
        atlasHeight += 2 * GAP;

        Pixmap pixmap = new Pixmap(atlasWidth, atlasHeight, Pixmap.Format.RGBA8888);

        // First pass: draw every silhouette into the pixmap, remembering where each one landed.
        int[] originX = new int[SPRITES.length];
        int cursorX = GAP;
        for (int i = 0; i < SPRITES.length; i++) {
            Sprite sprite = SPRITES[i];
            originX[i] = cursorX;
            if (sprite.id.equals("ship-basic")) {
                drawShip(pixmap, cursorX, GAP, sprite.width, sprite.height);
            } else {
                drawEnemy(pixmap, cursorX, GAP, sprite.width, sprite.height);
            }
            cursorX += sprite.width + GAP;
        }

        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();

        // Second pass: regions can only be built against the real texture, not the pixmap.
        for (int i = 0; i < SPRITES.length; i++) {
            Sprite sprite = SPRITES[i];
            regions.put(sprite.id,
                new TextureRegion(texture, originX[i], GAP, sprite.width, sprite.height));
        }
    }

    /**
     * A simple hull silhouette, correctly sized and centred the way a real sprite would be: the
     * collider in {@code docs/design/02-sprite-sizes.md} is concentric with this box.
     */
    private static void drawShip(Pixmap pixmap, int originX, int originY, int width, int height) {
        setColor(pixmap, N0_OUTLINE);
        pixmap.drawRectangle(originX, originY, width, height);
        setColor(pixmap, N5_HULL_SHADE);
        pixmap.fillRectangle(originX + 1, originY + 1, width - 2, height - 2);
        setColor(pixmap, N6_HULL_LIGHT);
        pixmap.fillRectangle(originX + 5, originY + 2, width - 10, 5);
        setColor(pixmap, C1_ENGINE);
        pixmap.fillRectangle(originX + 5, originY + height - 4, width - 10, 3);
        setColor(pixmap, C2_ENGINE_CORE);
        pixmap.fillRectangle(originX + 6, originY + height - 3, width - 12, 1);
    }

    /**
     * A generic alien hull silhouette shared by every enemy archetype. Placeholders are told apart
     * by size alone, the same way the real art will eventually be told apart by shape — nothing here
     * encodes which archetype is which beyond the dimensions {@code docs/design/02-sprite-sizes.md}
     * already fixes.
     */
    private static void drawEnemy(Pixmap pixmap, int originX, int originY, int width, int height) {
        setColor(pixmap, N0_OUTLINE);
        pixmap.drawRectangle(originX, originY, width, height);
        setColor(pixmap, V2_ALIEN_DARK);
        pixmap.fillRectangle(originX + 1, originY + 1, width - 2, height - 2);
        setColor(pixmap, V3_ALIEN_MID);
        int inset = Math.max(1, Math.min(width, height) / 4);
        pixmap.fillRectangle(originX + inset, originY + inset, width - 2 * inset, height - 2 * inset);
        setColor(pixmap, V4_ALIEN_LIGHT);
        int core = Math.max(1, Math.min(width, height) / 6);
        int centerX = originX + width / 2;
        int centerY = originY + height / 2;
        pixmap.fillRectangle(centerX - core, centerY - core, core * 2, core * 2);
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

    private record Sprite(String id, int width, int height) {
    }
}
