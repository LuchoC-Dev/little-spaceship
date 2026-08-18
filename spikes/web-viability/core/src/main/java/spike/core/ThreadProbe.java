package spike.core;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sonda de concurrencia. Comprueba que compila y que hace en tiempo de
 * ejecucion, por separado: en TeaVM una cosa no implica la otra.
 *
 * Lo que interesa no es si el programa arranca, sino si dos hilos avanzan
 * de verdad al mismo tiempo. Por eso el hilo de trabajo gira ocupado
 * mientras el hilo principal sigue dibujando: si hay paralelismo real, el
 * contador del trabajador sube mientras el juego mantiene su framerate.
 */
public final class ThreadProbe {

    private final AtomicInteger workerTicks = new AtomicInteger();
    private final Object lock = new Object();
    private int guardedCounter;

    private volatile boolean running;
    private volatile String status = "sin iniciar";
    private Thread worker;

    private long startMillis;
    private int mainTicksAtStart;

    public String status() {
        return status;
    }

    public int workerTicks() {
        return workerTicks.get();
    }

    public int guardedCounter() {
        synchronized (lock) {
            return guardedCounter;
        }
    }

    /** Lanza el hilo de trabajo. Devuelve false si ni siquiera se pudo crear. */
    public boolean start() {
        if (running) return true;
        startMillis = System.currentTimeMillis();
        try {
            running = true;
            worker = new Thread(this::work, "spike-worker");
            worker.setDaemon(true);
            worker.start();
            status = "hilo lanzado";
            return true;
        } catch (Throwable t) {
            running = false;
            status = "fallo al lanzar: " + t.getClass().getSimpleName();
            return false;
        }
    }

    private void work() {
        try {
            while (running) {
                workerTicks.incrementAndGet();
                synchronized (lock) {
                    guardedCounter++;
                }
                // Sin pausa: si el runtime es cooperativo y no cede, esto
                // congela la pagina y el sintoma es inmediato.
                if (workerTicks.get() % 100000 == 0) {
                    Thread.yield();
                }
            }
        } catch (Throwable t) {
            status = "murio: " + t.getClass().getSimpleName();
        }
    }

    public void stop() {
        running = false;
    }

    /**
     * Veredicto tras unos segundos: si el trabajador nunca avanzo, no hay
     * paralelismo aunque el hilo se haya creado sin lanzar excepcion.
     */
    public String verdict(int mainTicks) {
        if (!running && status.startsWith("fallo")) return status;
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed < 2000) return "midiendo...";
        int ticks = workerTicks.get();
        if (ticks == 0) return "SIN paralelismo: worker 0 ticks";
        return "worker " + ticks + " ticks en " + elapsed + "ms";
    }

    public void markMainStart(int mainTicks) {
        mainTicksAtStart = mainTicks;
    }
}
