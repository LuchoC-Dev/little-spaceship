package probe;

/**
 * Sonda de concurrencia sobre TeaVM puro, sin libGDX ni WebGL, para poder
 * ejecutarla en Node y obtener un veredicto reproducible.
 *
 * Distingue tres cosas que se confunden con facilidad:
 *
 *   compila            el compilador acepta la API
 *   arranca            el hilo se crea sin lanzar excepcion
 *   corre en paralelo  el hilo avanza mientras el principal trabaja
 *
 * La tercera es la unica que importa para decidir si un diseno multihilo
 * sirve de algo en el navegador.
 */
public final class Main {

    private static volatile int workerTicks;
    private static volatile boolean workerDone;
    private static final Object lock = new Object();
    private static int guarded;

    public static void main(String[] args) {
        log("--- sonda de concurrencia TeaVM ---");
        log("hilo actual: " + Thread.currentThread().getName());
        log("procesadores reportados: " + Runtime.getRuntime().availableProcessors());

        Thread worker = launchWorker();
        if (worker == null) return;

        // Trabajo del hilo principal SIN ceder el control en ningun momento.
        // Si el modelo fuera de paralelismo real, el trabajador avanzaria aqui.
        long spin = 0;
        for (int i = 0; i < 20000000; i++) spin += i;

        log("");
        log("[1] main termino su trabajo sin ceder (spin=" + spin + ")");
        log("    workerTicks = " + workerTicks + "   workerDone = " + workerDone);
        log(workerTicks == 0
            ? "    -> el trabajador NO avanzo: no hay paralelismo real"
            : "    -> el trabajador avanzo en paralelo");

        // Ahora si cedemos el control.
        try {
            Thread.sleep(50);
            log("");
            log("[2] tras Thread.sleep(50)");
            log("    workerTicks = " + workerTicks + "   workerDone = " + workerDone);
            log(workerTicks > 0
                ? "    -> avanza solo cuando el principal cede: concurrencia cooperativa"
                : "    -> sigue sin avanzar");
        } catch (Throwable t) {
            log("[2] Thread.sleep fallo: " + t);
        }

        try {
            worker.join();
            log("");
            log("[3] join() completado");
            log("    workerTicks = " + workerTicks + "   guarded = " + guarded);
        } catch (Throwable t) {
            log("[3] join() fallo: " + describe(t));
        }

        log("");
        log("--- APIs de java.util.concurrent ---");
        probeAtomic();
        probeConcurrentMap();
        // Executors, ExecutorService, CompletableFuture y ReentrantLock no existen
        // en la biblioteca de TeaVM 0.15.0: no dan aviso, rompen la compilacion.
        log("ExecutorService      NO EXISTE en TeaVM (rompe la compilacion)");
        log("CompletableFuture    NO EXISTE en TeaVM (rompe la compilacion)");
        log("ReentrantLock        NO EXISTE en TeaVM (rompe la compilacion)");

        log("");
        log("--- fin ---");
    }

    private static Thread launchWorker() {
        try {
            Thread t = new Thread(() -> {
                for (int i = 0; i < 2000; i++) {
                    workerTicks++;
                    synchronized (lock) {
                        guarded++;
                    }
                }
                workerDone = true;
            }, "worker");
            t.start();
            log("Thread.start() no lanzo excepcion");
            return t;
        } catch (Throwable t) {
            log("Thread.start() FALLO: " + describe(t));
            return null;
        }
    }

    private static void probeAtomic() {
        try {
            java.util.concurrent.atomic.AtomicInteger a = new java.util.concurrent.atomic.AtomicInteger();
            a.incrementAndGet();
            a.compareAndSet(1, 5);
            log("AtomicInteger        ok (valor " + a.get() + ")");
        } catch (Throwable t) {
            log("AtomicInteger        FALLA: " + describe(t));
        }
    }

    private static void probeConcurrentMap() {
        try {
            java.util.Map<String, Integer> m = new java.util.concurrent.ConcurrentHashMap<>();
            m.put("a", 1);
            log("ConcurrentHashMap    ok (size " + m.size() + ")");
        } catch (Throwable t) {
            log("ConcurrentHashMap    FALLA: " + describe(t));
        }
    }




    private static String describe(Throwable t) {
        String name = t.getClass().getName();
        String msg = t.getMessage();
        return msg == null ? name : name + ": " + msg;
    }

    private static void log(String s) {
        System.out.println(s);
    }
}
