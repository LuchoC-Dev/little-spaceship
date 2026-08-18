package spike.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import spike.core.SpikeConfig;
import spike.core.SpikeGame;

/**
 * Launcher desktop. Sirve de linea base: cualquier medicion web solo tiene
 * sentido comparada contra lo que el mismo core rinde sobre la JVM.
 */
public class DesktopLauncher {

    public static void main(String[] args) {
        boolean bench = args.length > 0 && "--bench".equals(args[0]);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Spike de viabilidad - desktop");
        config.setWindowedMode(SpikeConfig.LOGICAL_WIDTH * 3, SpikeConfig.LOGICAL_HEIGHT * 3);
        // Sin vsync ni tope de FPS: con el limite puesto no se ve donde esta el techo.
        config.useVsync(false);
        config.setForegroundFPS(0);
        new Lwjgl3Application(new SpikeGame(bench), config);
    }
}
