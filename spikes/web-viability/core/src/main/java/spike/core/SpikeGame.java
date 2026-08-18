package spike.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * Spike de viabilidad web. Deliberadamente feo y descartable.
 *
 * Mide, en un unico sitio, lo que la especificacion del MVP necesita saber
 * antes de comprometerse con la plataforma:
 *
 *   1. cuantas entidades aguanta el renderizador a framerate estable;
 *   2. si teclado y mouse funcionan de forma simultanea y aditiva;
 *   3. si el audio responde y la musica puede cambiarse en caliente;
 *   4. si el pixel-art escala sin deformarse;
 *   5. cuanto tarda el arranque.
 *
 * Controles:
 *   Flechas    mover la nave
 *   Shift      movimiento lento
 *   Espacio    efecto de sonido
 *   M          cambiar de musica
 *   1..5       fijar la carga de entidades
 *   +/-        subir o bajar un escalon de carga
 *   TAB        capturar o liberar el puntero
 *   R          reiniciar las metricas
 */
public class SpikeGame extends ApplicationAdapter {


    private static final int PROJECTILE = 0;
    private static final int ENEMY = 1;

    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;
    private FitViewport viewport;

    private Texture projectileTexture;
    private Texture enemyTexture;
    private Texture shipTexture;
    private Texture checkerTexture;

    private Sound blip;
    private Music[] tracks;
    private int currentTrack = -1;

    private Entities entities;
    private int stepIndex = 1;

    private float shipX;
    private float shipY;
    private final float shipSpeed = 90f;

    private final Metrics metrics = new Metrics();
    private Benchmark benchmark;
    private final ThreadProbe threads = new ThreadProbe();
    private int frameCount;
    private int lastHits;
    private boolean pointerLocked;
    private long startupNanos;
    private float assetLoadMillis;

    private final boolean autoBenchmark;
    /** En navegador no hay consola a la vista, asi que el informe se queda dibujado. */
    private boolean exitWhenBenchmarkEnds;

    public SpikeGame() {
        this(false);
    }

    /** El modo benchmark recorre los escalones solo y termina publicando el informe. */
    public SpikeGame(boolean autoBenchmark) {
        this.autoBenchmark = autoBenchmark;
        this.exitWhenBenchmarkEnds = autoBenchmark;
    }

    @Override
    public void create() {
        startupNanos = System.nanoTime();

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setUseIntegerPositions(true);

        camera = new OrthographicCamera();
        viewport = new FitViewport(SpikeConfig.LOGICAL_WIDTH, SpikeConfig.LOGICAL_HEIGHT, camera);

        long assetStart = System.nanoTime();
        projectileTexture = solid(2, 4, Color.CYAN);
        enemyTexture = solid(8, 8, Color.SALMON);
        shipTexture = solid(10, 10, Color.WHITE);
        checkerTexture = checker(16, 16);
        loadAudio();
        assetLoadMillis = (System.nanoTime() - assetStart) / 1000000f;

        entities = new Entities(SpikeConfig.STRESS_STEPS[SpikeConfig.STRESS_STEPS.length - 1]);
        shipX = SpikeConfig.LOGICAL_WIDTH / 2f;
        shipY = 40f;
        populate();

        if (autoBenchmark) {
            benchmark = new Benchmark();
        }
    }

    /**
     * Genera las texturas por codigo en vez de cargarlas de disco.
     * El spike mide el techo del renderizado, y asi no depende de assets
     * que todavia no existen ni de licencias que aun no se eligieron.
     */
    private Texture solid(int w, int h, Color color) {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    /** Tablero de ajedrez: cualquier suavizado o escala no entera se ve de inmediato. */
    private Texture checker(int w, int h) {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean on = ((x + y) & 1) == 0;
                pixmap.setColor(on ? 0x2a2a3aff : 0x14141eff);
                pixmap.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pixmap.dispose();
        return texture;
    }

    private void loadAudio() {
        try {
            blip = Gdx.audio.newSound(Gdx.files.internal("audio/blip.wav"));
            tracks = new Music[] {
                Gdx.audio.newMusic(Gdx.files.internal("audio/track-a.wav")),
                Gdx.audio.newMusic(Gdx.files.internal("audio/track-b.wav"))
            };
        } catch (Exception e) {
            // El audio se evalua aparte; que falte no debe tumbar el resto de las mediciones.
            Gdx.app.error("spike", "audio no disponible: " + e.getMessage());
            blip = null;
            tracks = null;
        }
    }

    private void populate() {
        entities.clear();
        int target = SpikeConfig.STRESS_STEPS[stepIndex];
        float left = playfieldLeft();
        float right = playfieldRight();
        for (int i = 0; i < target; i++) {
            boolean enemy = (i % 12) == 0;
            entities.add(
                MathUtils.random(left, right),
                MathUtils.random(0f, SpikeConfig.LOGICAL_HEIGHT),
                MathUtils.random(-40f, 40f),
                MathUtils.random(-70f, -20f),
                enemy ? ENEMY : PROJECTILE
            );
        }
        metrics.reset();
    }

    private float playfieldLeft() {
        return (SpikeConfig.LOGICAL_WIDTH - SpikeConfig.PLAYFIELD_WIDTH) / 2f;
    }

    private float playfieldRight() {
        return playfieldLeft() + SpikeConfig.PLAYFIELD_WIDTH;
    }

    @Override
    public void render() {
        frameCount++;
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        handleInput(delta);

        long updateStart = System.nanoTime();
        entities.update(delta, playfieldLeft(), playfieldRight(), 0f, SpikeConfig.LOGICAL_HEIGHT);
        lastHits = entities.collideAgainst(shipX, shipY, 5f);
        float updateMillis = (System.nanoTime() - updateStart) / 1000000f;

        long drawStart = System.nanoTime();
        draw();
        float drawMillis = (System.nanoTime() - drawStart) / 1000000f;

        metrics.sample(delta, updateMillis, drawMillis);

        if (benchmark != null && !benchmark.isFinished()) {
            benchmark.update(delta, updateMillis, drawMillis, index -> {
                stepIndex = index;
                populate();
            });
            // En desktop el informe ya salio por consola; ahi si conviene cerrar solo.
            if (benchmark.isFinished() && exitWhenBenchmarkEnds) {
                Gdx.app.exit();
            }
        }
    }

    /**
     * Teclado y mouse aditivos, tal como quedo decidido para el MVP:
     * ambos aportan un vector y la suma se limita a la velocidad maxima,
     * de modo que direcciones opuestas se cancelan.
     */
    private void handleInput(float delta) {
        float kx = 0f;
        float ky = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) kx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) kx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) ky -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) ky += 1f;

        // El mouse es relativo: aporta desplazamiento, no una posicion de destino.
        float scale = viewport.getWorldWidth() / Math.max(1f, Gdx.graphics.getWidth());
        float mx = Gdx.input.getDeltaX() * scale / Math.max(delta, 0.0001f) / shipSpeed;
        float my = -Gdx.input.getDeltaY() * scale / Math.max(delta, 0.0001f) / shipSpeed;

        float dx = kx + mx;
        float dy = ky + my;

        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 1f) {
            dx /= length;
            dy /= length;
        }

        float speed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? shipSpeed * 0.4f : shipSpeed;
        shipX = MathUtils.clamp(shipX + dx * speed * delta, playfieldLeft(), playfieldRight());
        shipY = MathUtils.clamp(shipY + dy * speed * delta, 0f, SpikeConfig.LOGICAL_HEIGHT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && blip != null) blip.play(0.5f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) cycleMusic();
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) metrics.reset();
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) togglePointerLock();
        if (Gdx.input.isKeyJustPressed(Input.Keys.B) && benchmark == null) benchmark = new Benchmark();
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) threads.start();

        for (int i = 0; i < SpikeConfig.STRESS_STEPS.length; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                stepIndex = i;
                populate();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS) && stepIndex < SpikeConfig.STRESS_STEPS.length - 1) {
            stepIndex++;
            populate();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS) && stepIndex > 0) {
            stepIndex--;
            populate();
        }
    }

    private void togglePointerLock() {
        pointerLocked = !pointerLocked;
        Gdx.input.setCursorCatched(pointerLocked);
    }

    /** Cambiar de pista en caliente es exactamente lo que pasa al entrar el boss. */
    private void cycleMusic() {
        if (tracks == null) return;
        if (currentTrack >= 0) tracks[currentTrack].stop();
        currentTrack = (currentTrack + 1) % tracks.length;
        tracks[currentTrack].setLooping(true);
        tracks[currentTrack].setVolume(0.4f);
        tracks[currentTrack].play();
    }

    private void draw() {
        ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(checkerTexture, playfieldLeft(), 0f,
            SpikeConfig.PLAYFIELD_WIDTH, SpikeConfig.LOGICAL_HEIGHT,
            0, 0, SpikeConfig.PLAYFIELD_WIDTH / 16, SpikeConfig.LOGICAL_HEIGHT / 16);

        for (int i = 0; i < entities.count; i++) {
            if (entities.kind[i] == ENEMY) {
                batch.draw(enemyTexture, entities.x[i], entities.y[i]);
            } else {
                batch.draw(projectileTexture, entities.x[i], entities.y[i]);
            }
        }
        batch.draw(shipTexture, shipX - 5f, shipY - 5f);
        drawHud();
        batch.end();
    }

    private void drawHud() {
        float x = 4f;
        float y = SpikeConfig.LOGICAL_HEIGHT - 6f;
        font.draw(batch, "entidades " + entities.count, x, y);
        font.draw(batch, "fps " + Gdx.graphics.getFramesPerSecond(), x, y - 12f);
        font.draw(batch, "min " + fmt(metrics.minFps()) + " p1 " + fmt(metrics.percentile1Fps()), x, y - 24f);
        font.draw(batch, "upd " + fmt2(metrics.avgUpdateMillis()) + "ms", x, y - 36f);
        font.draw(batch, "draw " + fmt2(metrics.avgDrawMillis()) + "ms", x, y - 48f);
        font.draw(batch, "hits " + lastHits, x, y - 60f);
        font.draw(batch, "assets " + fmt(assetLoadMillis) + "ms", x, y - 72f);
        font.draw(batch, "puntero " + (pointerLocked ? "capturado" : "libre"), x, y - 84f);
        font.draw(batch, "audio " + (blip != null ? "ok" : "no"), x, y - 96f);

        float boot = (System.nanoTime() - startupNanos) / 1000000000f;
        font.draw(batch, "t " + fmt2(boot) + "s", x, y - 108f);
        font.draw(batch, "hilos " + threads.status(), x, y - 120f);
        font.draw(batch, threads.verdict(frameCount), x, y - 132f);

        if (benchmark != null) {
            float by = SpikeConfig.LOGICAL_HEIGHT - 6f;
            float bx = SpikeConfig.LOGICAL_WIDTH - 150f;
            font.draw(batch, benchmark.isFinished() ? "BENCH LISTO" : "BENCH corriendo...", bx, by);
            java.util.List<String> lines = benchmark.lines();
            for (int i = 0; i < lines.size(); i++) {
                font.draw(batch, lines.get(i), bx, by - 14f - i * 12f);
            }
        }
    }

    /** String.format no esta disponible de forma fiable en todos los backends de TeaVM. */
    private static String fmt(float value) {
        return Integer.toString(Math.round(value));
    }

    private static String fmt2(float value) {
        int whole = (int) value;
        int frac = Math.abs(Math.round((value - whole) * 100f));
        return whole + "." + (frac < 10 ? "0" + frac : Integer.toString(frac));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        projectileTexture.dispose();
        enemyTexture.dispose();
        shipTexture.dispose();
        checkerTexture.dispose();
        if (blip != null) blip.dispose();
        if (tracks != null) {
            for (Music track : tracks) track.dispose();
        }
    }
}
