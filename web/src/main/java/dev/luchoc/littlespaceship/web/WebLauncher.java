package dev.luchoc.littlespaceship.web;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;

/**
 * TeaVM launcher. The same {@link LittleSpaceshipGame} as desktop, with no platform-specific branch
 * — if one were ever needed here, that would already be a sign against keeping both targets on one
 * core.
 */
public final class WebLauncher {

    private WebLauncher() {
    }

    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration();
        // Explicit canvas size. CLAUDE.md: with 0/0 the backend inherits the container's size,
        // which starts at 0x0 and leaves the preloader without a valid stage.
        config.width = LittleSpaceshipGame.LOGICAL_WIDTH * 2;
        config.height = LittleSpaceshipGame.LOGICAL_HEIGHT * 2;
        config.showDownloadLogs = true;
        new WebApplication(new LittleSpaceshipGame(), config);
    }
}
