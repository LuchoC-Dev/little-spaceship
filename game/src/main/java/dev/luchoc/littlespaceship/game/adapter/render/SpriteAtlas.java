package dev.luchoc.littlespaceship.game.adapter.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.luchoc.littlespaceship.core.port.SpriteId;

/**
 * Resolves a content sprite id to the region that draws it. One texture behind every region is the
 * whole point: {@link WorldRenderer} binds whichever atlas it is handed once per frame, not once per
 * entity, and that only holds if every id in play comes from the same {@link
 * com.badlogic.gdx.graphics.Texture}.
 *
 * <p>Two implementations exist because the art lane and this lane run in parallel and finish on
 * different days. {@link PackedSpriteAtlas} loads a real {@code TexturePacker} atlas from disk —
 * what the game ships with, once art lands. {@link PlaceholderAtlas} draws silhouettes in code and
 * is what runs until then. {@link #load} picks between them by asking whether the packed atlas file
 * exists, so nothing else in the composition root has to know which one is running.
 */
public interface SpriteAtlas {

    /**
     * @param sprite the id the core handed the visitor
     * @return the region to draw, or null if this atlas does not cover it
     */
    TextureRegion region(SpriteId sprite);

    void dispose();
}
