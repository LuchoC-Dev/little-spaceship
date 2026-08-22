package dev.luchoc.littlespaceship.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import dev.luchoc.littlespaceship.game.adapter.audio.AudioSystem;
import dev.luchoc.littlespaceship.game.adapter.audio.MusicTrack;
import dev.luchoc.littlespaceship.game.screen.MenuScreen;
import dev.luchoc.littlespaceship.game.screen.PlayScreen;
import dev.luchoc.littlespaceship.game.ui.GameSkin;

/**
 * The composition root: the one place that assembles {@code core} with the libGDX adapters, and now
 * also the one place that owns what every screen shares — the {@link Skin} built once in {@link
 * #create()}, {@link GameSettings}, and the run seed.
 *
 * <p>Desktop and web share this class unchanged; only the launcher that constructs the backend
 * differs, per {@code docs/planning/12-architecture.md}.
 *
 * <p>Extending {@link Game} instead of {@code ApplicationAdapter} is what phase 06 task 13 needed:
 * the flow of six screens — menu, ship selection, options, pause, victory, defeat — is libGDX's own
 * {@link com.badlogic.gdx.Screen} abstraction, not a framework built for this project.
 * {@code dev.luchoc.littlespaceship.game.screen} holds one class per screen; gameplay itself moved
 * to {@code screen.PlayScreen} unchanged from what this class used to render directly.
 */
public final class LittleSpaceshipGame extends Game {

    /** Logical resolution width, per {@code docs/planning/10-mvp-initial-values.md}. */
    public static final int LOGICAL_WIDTH = 480;

    /** Logical resolution height, per {@code docs/planning/10-mvp-initial-values.md}. */
    public static final int LOGICAL_HEIGHT = 270;

    private final int seed;

    private Skin skin;
    private final GameSettings settings = new GameSettings();
    private AudioSystem audio;

    public LittleSpaceshipGame() {
        this((int) System.currentTimeMillis());
    }

    /** @param seed the run's seed; exposed so a specific run can be reproduced */
    public LittleSpaceshipGame(int seed) {
        this.seed = seed;
    }

    @Override
    public void create() {
        skin = GameSkin.build();
        settings.load();
        audio = new AudioSystem(settings);
        setScreen(new MenuScreen(this));
    }

    /**
     * Disposes the outgoing screen once the new one is in place. {@link Game#setScreen} never does
     * this on its own — it only calls {@code hide()} — and every screen here owns a texture or two,
     * so leaving that to the caller is how a session that walks menu -> ship select -> play -> menu
     * a few times leaks a batch and a stage per hop.
     *
     * <p>Also the one place that decides which music loop, if any, fits the screen being entered —
     * {@link PlayScreen} gets {@link MusicTrack#LEVEL}, {@link MenuScreen} and every other screen get
     * silence. {@link AudioSystem#playMusic} restarting the same already-playing track is a no-op, so
     * navigating Options -> Back -> menu again does not restart the loop mid-bar. Nothing plays here
     * before {@link AudioSystem#unlock()} has been called at least once — see its javadoc — so the
     * very first {@code MenuScreen}, set from {@link #create()} with no user gesture behind it yet,
     * asks for silence and gets exactly that.
     */
    @Override
    public void setScreen(com.badlogic.gdx.Screen screen) {
        com.badlogic.gdx.Screen previous = getScreen();
        super.setScreen(screen);
        if (previous != null) {
            previous.dispose();
        }
        if (audio != null) {
            if (screen instanceof PlayScreen) {
                audio.playMusic(MusicTrack.LEVEL);
            } else {
                audio.stopMusic();
            }
        }
    }

    /** @return the skin every screen styles its widgets from, built once for the whole run */
    public Skin skin() {
        return skin;
    }

    /** @return the options the flow's Options screen edits */
    public GameSettings settings() {
        return settings;
    }

    /** @return the sound effects and music every screen plays through */
    public AudioSystem audio() {
        return audio;
    }

    /** @return the seed this run was created with */
    public int seed() {
        return seed;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (skin != null) {
            skin.dispose();
        }
        if (audio != null) {
            audio.dispose();
        }
    }
}
