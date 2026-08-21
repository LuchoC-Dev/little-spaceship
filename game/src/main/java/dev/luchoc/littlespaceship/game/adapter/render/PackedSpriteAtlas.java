package dev.luchoc.littlespaceship.game.adapter.render;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.luchoc.littlespaceship.core.port.SpriteId;

/**
 * The real atlas: a {@code .atlas}/{@code .png} pair produced by libGDX's own {@code TexturePacker},
 * loaded through {@link TextureAtlas} — a plain-text pack file and no reflection, so it costs
 * nothing extra under TeaVM per {@code CLAUDE.md}'s "prefer boring APIs".
 *
 * <p>This is the seam the art lane's PNGs cross into the running game. Nothing in {@code
 * WorldRenderer} or the composition root changes when a real sprite replaces a placeholder one:
 * packing the finished PNGs into {@code assets/atlas/sprites.atlas} and having {@link
 * PlaceholderAtlas} give way to this class in {@link #load} is the entire integration step. A build
 * step that runs {@code TexturePacker.process} was considered and rejected for now — it would need a
 * Gradle task wired to the art lane's own output directory, which does not exist yet since art
 * production, {@code docs/plan/06-presentation/plan.md} tasks 6-11, has not started. Packing by hand
 * once art lands, and adding the Gradle task once the source directory is stable, costs less than
 * building the automation today against a directory that is still empty.
 */
public final class PackedSpriteAtlas implements SpriteAtlas {

    /** Where a packed atlas is expected, relative to the assets root both launchers agree on. */
    static final String ATLAS_PATH = "atlas/sprites.atlas";

    private final TextureAtlas atlas;

    private PackedSpriteAtlas(TextureAtlas atlas) {
        this.atlas = atlas;
    }

    /**
     * @param assetsRoot the assets root, e.g. {@code Gdx.files.internal("")}
     * @return a real atlas if {@link #ATLAS_PATH} exists under it, or a generated placeholder set
     *     otherwise
     */
    public static SpriteAtlas load(FileHandle assetsRoot) {
        FileHandle packed = assetsRoot.child(ATLAS_PATH);
        if (packed.exists()) {
            return new PackedSpriteAtlas(new TextureAtlas(packed));
        }
        return new PlaceholderAtlas();
    }

    @Override
    public TextureRegion region(SpriteId sprite) {
        return atlas.findRegion(sprite.value());
    }

    @Override
    public void dispose() {
        atlas.dispose();
    }
}
