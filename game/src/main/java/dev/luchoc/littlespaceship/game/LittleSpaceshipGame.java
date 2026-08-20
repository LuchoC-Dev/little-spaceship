package dev.luchoc.littlespaceship.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.luchoc.littlespaceship.core.application.GameLoop;
import dev.luchoc.littlespaceship.core.application.Simulation;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.WorldView;
import dev.luchoc.littlespaceship.game.adapter.content.JsonContentSource;
import dev.luchoc.littlespaceship.game.adapter.input.InputAdapter;
import dev.luchoc.littlespaceship.game.adapter.render.CheckerboardBackground;
import dev.luchoc.littlespaceship.game.adapter.render.PixelPerfectViewport;
import dev.luchoc.littlespaceship.game.adapter.render.PlaceholderAtlas;
import dev.luchoc.littlespaceship.game.adapter.render.WorldRenderer;

/**
 * The composition root: the one place that assembles {@code core} with the libGDX adapters.
 *
 * <p>This is the only class in the project allowed to know both worlds exist. Everything it hands
 * to the core is a contract — {@link ContentSource}, {@link InputFrame} — and everything it reads
 * back is a contract too, {@link Simulation#view()}. No component, no entity handle and no system
 * ever crosses this boundary.
 *
 * <p>Desktop and web share this class unchanged; only the launcher that constructs the backend
 * differs, per {@code docs/planning/12-architecture.md}.
 *
 * <p>Content comes from {@link JsonContentSource}, reading {@code assets/data/*.json} — phase 04's
 * loader, which is what finally gives {@link Simulation}'s 4-argument constructor a level id to
 * build a wave timeline from. A run now starts with the player already in the world (a phase 04
 * fix to {@code Simulation} itself) and enemies spawn as {@code level-01.json}'s timeline reaches
 * them, so nothing about this class works around the empty-world gap phase 03 originally reported.
 */
public final class LittleSpaceshipGame extends ApplicationAdapter {

    /** Logical resolution width, per {@code docs/planning/10-mvp-initial-values.md}. */
    public static final int LOGICAL_WIDTH = 480;

    /** Logical resolution height, per {@code docs/planning/10-mvp-initial-values.md}. */
    public static final int LOGICAL_HEIGHT = 270;

    /**
     * Playfield width in logical units. Duplicated from {@code MotionSystem.PLAYFIELD_WIDTH}
     * deliberately: that class lives in {@code core.domain}, not in {@code core.port}, and
     * {@code game} does not import concrete domain classes even for a read-only constant — the
     * contract only exposes what crosses through {@code core.port}. Both values trace back to the
     * same source, {@code docs/planning/10-mvp-initial-values.md}, confirmed in
     * {@code docs/planning/11-technical-prototype-results.md}.
     */
    private static final float PLAYFIELD_WIDTH = 208f;

    /** Left edge of the centred playfield within the logical resolution. */
    private static final float PLAYFIELD_LEFT = (LOGICAL_WIDTH - PLAYFIELD_WIDTH) / 2f;

    private final int seed;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private PixelPerfectViewport viewport;

    private PlaceholderAtlas atlas;
    private CheckerboardBackground checkerboard;
    private WorldRenderer worldRenderer;
    private InputAdapter input;

    private ContentSource content;
    private Simulation simulation;
    private GameLoop loop;

    public LittleSpaceshipGame() {
        this((int) System.currentTimeMillis());
    }

    /** @param seed the run's seed; exposed so a specific run can be reproduced */
    public LittleSpaceshipGame(int seed) {
        this.seed = seed;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new PixelPerfectViewport(LOGICAL_WIDTH, LOGICAL_HEIGHT, camera);

        atlas = new PlaceholderAtlas();
        checkerboard = new CheckerboardBackground();
        worldRenderer = new WorldRenderer(atlas, PLAYFIELD_LEFT);
        input = new InputAdapter(viewport);

        // "data" resolves under the working directory both launchers already agree on: the
        // desktop `run` task sets it to assets/ (desktop/build.gradle.kts), and gdxTeaVM bundles
        // the same assets/ tree into the web build's classpath (web/build.gradle.kts, when enabled).
        content = new JsonContentSource(Gdx.files.internal("data"));
        // No sink yet: HUD and audio, the events' only consumers so far, do not exist until later
        // phases. An event dropped on the floor here is not a bug; it is nothing reacting to it yet.
        simulation = new Simulation(content, event -> { }, seed, JsonContentSource.LEVEL_ID);
        loop = new GameLoop(simulation);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        float frameDelta = Gdx.graphics.getDeltaTime();
        InputFrame frame = input.sample(frameDelta, content.balance());
        loop.advance(frameDelta, frame);

        ScreenUtils.clear(0.043f, 0.055f, 0.078f, 1f); // N0, the palette's void colour.
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        checkerboard.draw(batch, PLAYFIELD_LEFT, 0f, PLAYFIELD_WIDTH, LOGICAL_HEIGHT);
        worldRenderer.draw(simulation.view(), batch);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        atlas.dispose();
        checkerboard.dispose();
    }
}
