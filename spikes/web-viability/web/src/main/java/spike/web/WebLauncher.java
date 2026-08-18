package spike.web;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;

import spike.core.SpikeConfig;
import spike.core.SpikeGame;

/**
 * Launcher web. El mismo core que desktop, sin ninguna rama especifica de
 * plataforma: si hiciera falta bifurcar aqui, ya seria una senal en contra
 * de mantener los dos targets.
 */
public class WebLauncher {

    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration();
        // Tamano explicito del canvas. Con 0/0 el backend hereda el tamano del
        // contenedor, que arranca en 0x0 y deja el preloader sin stage valido.
        config.width = SpikeConfig.LOGICAL_WIDTH * 2;
        config.height = SpikeConfig.LOGICAL_HEIGHT * 2;
        config.showDownloadLogs = true;
        new WebApplication(new SpikeGame(), config);
    }
}
