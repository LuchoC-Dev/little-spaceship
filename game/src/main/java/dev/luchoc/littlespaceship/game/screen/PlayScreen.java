package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import dev.luchoc.littlespaceship.core.application.GameLoop;
import dev.luchoc.littlespaceship.core.application.Simulation;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import dev.luchoc.littlespaceship.game.adapter.content.JsonContentSource;
import dev.luchoc.littlespaceship.game.adapter.hud.InvulnerabilitySource;
import dev.luchoc.littlespaceship.game.adapter.hud.PlayerHudState;
import dev.luchoc.littlespaceship.game.adapter.input.InputAdapter;
import dev.luchoc.littlespaceship.game.adapter.render.CheckerboardBackground;
import dev.luchoc.littlespaceship.game.adapter.render.HudRenderer;
import dev.luchoc.littlespaceship.game.adapter.render.PackedSpriteAtlas;
import dev.luchoc.littlespaceship.game.adapter.render.PixelPerfectViewport;
import dev.luchoc.littlespaceship.game.adapter.render.SpriteAtlas;
import dev.luchoc.littlespaceship.game.adapter.render.WorldRenderer;
import dev.luchoc.littlespaceship.game.ui.Palette;

/**
 * Level 1: the gameplay canvas plus the HUD plates and the pause overlay, in one screen because the
 * spec's pause freezes the very playfield it appears over rather than replacing it with a menu.
 *
 * <p>What renders here is exactly what {@link LittleSpaceshipGame} rendered before this phase
 * introduced a screen flow; nothing about the simulation or the drawing changed, only where the
 * loop lives.
 *
 * <p><b>The HUD state is a placeholder.</b> {@link #hudState} is filled from {@code
 * ContentSource.balance()} once at {@link #show()} and never updated per tick: {@code core.port}
 * does not expose a read-only player status yet, which is exactly the gap {@code
 * docs/plan/06-presentation/plan.md} task 14 names and the game-presentation agent's report proposes
 * a contract for. The layout, the slot art and the draw order are all real; the numbers behind them
 * are not live.
 *
 * <p><b>F5/F6 jump to {@link VictoryScreen}/{@link DefeatScreen} directly.</b> Neither screen is
 * reachable through actual play yet, for the same reason: no core signal says the run ended. The
 * keys exist only so the two screens can be reviewed against the mock; they are not part of the
 * flow the spec describes and should be removed once {@code core} reports a run's outcome.
 */
public final class PlayScreen implements Screen {

    private static final float PLAYFIELD_WIDTH = 208f;
    private static final float PLAYFIELD_LEFT =
        (LittleSpaceshipGame.LOGICAL_WIDTH - PLAYFIELD_WIDTH) / 2f;

    private final LittleSpaceshipGame game;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;

    private SpriteAtlas atlas;
    private CheckerboardBackground checkerboard;
    private WorldRenderer worldRenderer;
    private HudRenderer hudRenderer;
    private InputAdapter input;

    private ContentSource content;
    private Simulation simulation;
    private GameLoop loop;

    private PlayerHudState hudState;

    private boolean paused;
    private Stage pauseStage;
    private Texture dimTexture;

    public PlayScreen(LittleSpaceshipGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new PixelPerfectViewport(
            LittleSpaceshipGame.LOGICAL_WIDTH, LittleSpaceshipGame.LOGICAL_HEIGHT, camera);

        atlas = PackedSpriteAtlas.load(Gdx.files.internal(""));
        checkerboard = new CheckerboardBackground();
        worldRenderer = new WorldRenderer(atlas, PLAYFIELD_LEFT);
        hudRenderer = new HudRenderer(game.skin());
        input = new InputAdapter(viewport);

        content = new JsonContentSource(Gdx.files.internal("data"));
        simulation = new Simulation(content, event -> { }, game.seed(), JsonContentSource.LEVEL_ID);
        loop = new GameLoop(simulation);

        hudState = new PlayerHudState(
            content.balance().initialLives(), content.balance().maxLives(),
            content.balance().initialBombs(), content.balance().maxBombs(),
            1, content.balance().weaponLevels(),
            false, InvulnerabilitySource.NONE, 0f, null, null, 0);

        buildPauseStage();
    }

    private void buildPauseStage() {
        pauseStage = new Stage(viewport, new SpriteBatch());
        Table root = new Table();
        root.setFillParent(true);
        pauseStage.addActor(root);

        Table panel = new Table();
        panel.add(new Label("PAUSED", game.skin(), "title")).padBottom(16f).row();
        MenuEntries.add(panel, game.skin(), "RESUME", this::resumeGameplay);
        MenuEntries.add(panel, game.skin(), "QUIT TO MENU", () -> game.setScreen(new MenuScreen(game)));
        root.add(panel).center();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(new Color(0f, 0f, 0f, 0.55f));
        pm.fill();
        dimTexture = new Texture(pm);
        pm.dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (paused) {
                resumeGameplay();
            } else {
                pauseGameplay();
            }
        }
        if (!paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
                game.setScreen(new VictoryScreen(game, hudState.score(),
                    content.balance().lifeCompletionBonus() * hudState.lives(),
                    content.balance().bombCompletionBonus() * hudState.bombs()));
                return;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
                game.setScreen(new DefeatScreen(game, hudState.score()));
                return;
            }
            InputFrame frame = input.sample(delta, content.balance(), game.settings().mouseEnabled());
            loop.advance(delta, frame);
        }

        ScreenUtils.clear(Palette.N0.r, Palette.N0.g, Palette.N0.b, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        checkerboard.draw(batch, PLAYFIELD_LEFT, 0f, PLAYFIELD_WIDTH, LittleSpaceshipGame.LOGICAL_HEIGHT);
        worldRenderer.draw(simulation.view(), batch);
        hudRenderer.draw(batch, hudState);
        if (paused) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(dimTexture, 0f, 0f,
                LittleSpaceshipGame.LOGICAL_WIDTH, LittleSpaceshipGame.LOGICAL_HEIGHT);
        }
        batch.end();

        if (paused) {
            pauseStage.act(delta);
            pauseStage.draw();
        }
    }

    /**
     * The spec's pause: freezes the tick and shows the {@code RESUME}/{@code QUIT TO MENU} panel.
     * Not named {@code pause()}/{@code resume()} — those are {@link Screen}'s own lifecycle hooks,
     * which the backend calls on focus loss independently of the player's own Escape key, and
     * reusing them here would conflate the two.
     */
    private void pauseGameplay() {
        if (!paused) {
            paused = true;
            Gdx.input.setInputProcessor(new InputMultiplexer(pauseStage));
        }
    }

    private void resumeGameplay() {
        if (paused) {
            paused = false;
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void pause() {
        // Intentionally empty; see the note on pauseGameplay().
    }

    @Override
    public void resume() {
        // Intentionally empty; see the note on pauseGameplay().
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        batch.dispose();
        atlas.dispose();
        checkerboard.dispose();
        hudRenderer.dispose();
        if (pauseStage != null) {
            pauseStage.dispose();
        }
        if (dimTexture != null) {
            dimTexture.dispose();
        }
    }
}
