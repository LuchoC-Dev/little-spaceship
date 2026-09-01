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
import dev.luchoc.littlespaceship.core.port.CompletionBonus;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.LevelOutcome;
import dev.luchoc.littlespaceship.core.port.PlayerStatus;
import dev.luchoc.littlespaceship.core.port.WorldView;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import dev.luchoc.littlespaceship.game.adapter.audio.AudioDirector;
import dev.luchoc.littlespaceship.game.adapter.content.JsonContentSource;
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
 * <p>The HUD reads {@link WorldView#player()} once per frame — a fresh snapshot, not cached —
 * and {@link #render} checks {@link WorldView#outcome()} right after advancing the tick, switching
 * to {@link VictoryScreen} or {@link DefeatScreen} the moment it leaves {@link
 * LevelOutcome#IN_PROGRESS}. Both signals arrived from {@code core-domain} after this class first
 * shipped with a fixed placeholder state and a debug-key stand-in for the two end screens; neither
 * survives in this version.
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
    private AudioDirector audioDirector;

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
        input = new InputAdapter(viewport);

        content = new JsonContentSource(Gdx.files.internal("data"), game.levelId());
        hudRenderer = new HudRenderer(game.skin(), content.balance(), atlas);
        // AudioDirector doubles as the simulation's GameEventSink — see its javadoc — so it must
        // exist before Simulation does.
        audioDirector = new AudioDirector(game.audio());
        simulation = new Simulation(content, audioDirector, game.seed(), game.levelId());
        loop = new GameLoop(simulation);

        buildPauseStage();
    }

    private void buildPauseStage() {
        pauseStage = new Stage(viewport, new SpriteBatch());
        Table root = new Table();
        root.setFillParent(true);
        pauseStage.addActor(root);

        // Plate behind the panel, per docs/design/mockups/src/05-screens.js's pause screen: N2 fill,
        // N3 frame, so the panel reads as a surface over the frozen playfield instead of text
        // floating on it.
        Table panel = new Table();
        panel.setBackground(game.skin().getDrawable("n2-panel"));
        panel.pad(16f);
        panel.add(new Label("PAUSED", game.skin(), "title")).padBottom(16f).row();
        java.util.List<KeyboardFocusable> focusables = new java.util.ArrayList<>();
        MenuEntries.add(panel, game, game.skin(), "RESUME", this::resumeGameplay, focusables);
        MenuEntries.add(panel, game, game.skin(), "QUIT TO MENU", () -> game.setScreen(new MenuScreen(game)), focusables);
        root.add(panel).width(160f).height(86f).center();
        new MenuNavigator(pauseStage, focusables);

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
            InputFrame frame = input.sample(delta, content.balance(), game.settings().mouseEnabled());
            if (input.pointerCaptureLostUnexpectedly()) {
                // The browser revoked pointer lock on its own — a notification, alt-tab, or a click
                // outside the canvas — not the player pressing Escape. Issue #41: without this, the
                // ship kept reading deltas from a now-free cursor, losing its centring and then
                // losing movement entirely once the cursor hit a screen edge. Pausing, rather than
                // falling back to keyboard-only or a click-to-resume prompt, means a player who
                // alt-tabs mid-fight comes back to a game waiting for them instead of a ship that
                // silently drifted while they were away. This frame's mouse-derived deltas in
                // `frame` are discarded along with it.
                pauseGameplay();
            } else {
                loop.advance(delta, frame);

                WorldView view = simulation.view();
                audioDirector.update(view, view.player());
                LevelOutcome outcome = view.outcome();
                if (outcome != LevelOutcome.IN_PROGRESS) {
                    PlayerStatus status = view.player();
                    if (outcome == LevelOutcome.DEFEATED) {
                        game.setScreen(new DefeatScreen(game, status.score()));
                    } else {
                        CompletionBonus bonus = view.completionBonus();
                        game.setScreen(new VictoryScreen(
                            game, status.score(), bonus.livesBonus(), bonus.bombsBonus()));
                    }
                    return;
                }
            }
        }

        ScreenUtils.clear(Palette.N0.r, Palette.N0.g, Palette.N0.b, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        WorldView drawView = simulation.view();
        PlayerStatus drawStatus = drawView.player();
        batch.begin();
        checkerboard.draw(batch, PLAYFIELD_LEFT, 0f, PLAYFIELD_WIDTH, LittleSpaceshipGame.LOGICAL_HEIGHT);
        worldRenderer.draw(drawView, batch, drawStatus);
        hudRenderer.draw(batch, drawStatus, drawView.bossStatus());
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
        worldRenderer.dispose();
        hudRenderer.dispose();
        if (pauseStage != null) {
            pauseStage.dispose();
        }
        if (dimTexture != null) {
            dimTexture.dispose();
        }
    }
}
