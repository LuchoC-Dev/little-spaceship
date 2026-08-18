package spike.core;

import com.badlogic.gdx.Gdx;

/**
 * Recorre los escalones de carga sin intervencion manual y publica el
 * resultado por log. Existe para que la decision de plataforma se tome
 * sobre numeros comparables entre desktop y navegador, y no mirando el
 * HUD a ojo en cada uno.
 */
public final class Benchmark {

    /** Tiempo descartado al entrar en cada escalon, para no medir el calentamiento. */
    private static final float WARMUP_SECONDS = 1.0f;
    private static final float MEASURE_SECONDS = 3.0f;

    private final StringBuilder report = new StringBuilder();
    private final java.util.List<String> lines = new java.util.ArrayList<>();

    private int step = -1;
    private float elapsed;
    private boolean measuring;
    private boolean finished;

    private final Metrics metrics = new Metrics();

    public interface StepListener {
        void onStep(int stepIndex);
    }

    public boolean isFinished() {
        return finished;
    }

    /** Informe en lineas, para poder dibujarlo tambien en el navegador. */
    public java.util.List<String> lines() {
        return lines;
    }

    public int currentStep() {
        return step;
    }

    /** Devuelve true cuando acaba de cambiar de escalon y hay que repoblar. */
    public boolean update(float delta, float updateMillis, float drawMillis, StepListener listener) {
        if (finished) return false;

        if (step < 0) {
            step = 0;
            elapsed = 0f;
            measuring = false;
            metrics.reset();
            listener.onStep(step);
            return true;
        }

        elapsed += delta;

        if (!measuring) {
            if (elapsed >= WARMUP_SECONDS) {
                measuring = true;
                elapsed = 0f;
                metrics.reset();
            }
            return false;
        }

        metrics.sample(delta, updateMillis, drawMillis);

        if (elapsed < MEASURE_SECONDS) return false;

        recordStep();

        step++;
        if (step >= SpikeConfig.STRESS_STEPS.length) {
            finished = true;
            publish();
            return false;
        }

        elapsed = 0f;
        measuring = false;
        metrics.reset();
        listener.onStep(step);
        return true;
    }

    private void recordStep() {
        int entities = SpikeConfig.STRESS_STEPS[step];
        report.append("  ")
            .append(pad(Integer.toString(entities), 6))
            .append(" entidades   avg ")
            .append(pad(round(metrics.avgFps()), 4))
            .append(" fps   p1 ")
            .append(pad(round(metrics.percentile1Fps()), 4))
            .append("   min ")
            .append(pad(round(metrics.minFps()), 4))
            .append("   upd ")
            .append(twoDecimals(metrics.avgUpdateMillis()))
            .append("ms   draw ")
            .append(twoDecimals(metrics.avgDrawMillis()))
            .append("ms\n");
    }

    private void publish() {
        StringBuilder out = new StringBuilder();
        out.append("\n===== BENCHMARK =====\n");
        out.append("  resolucion logica ")
            .append(SpikeConfig.LOGICAL_WIDTH).append("x").append(SpikeConfig.LOGICAL_HEIGHT)
            .append("   ventana ")
            .append(Gdx.graphics.getWidth()).append("x").append(Gdx.graphics.getHeight())
            .append("\n");
        out.append(report);
        out.append("=====================\n");
        Gdx.app.log("spike", out.toString());
    }

    private static String round(float value) {
        return Integer.toString(Math.round(value));
    }

    private static String twoDecimals(float value) {
        int whole = (int) value;
        int frac = Math.abs(Math.round((value - whole) * 100f));
        return whole + "." + (frac < 10 ? "0" + frac : Integer.toString(frac));
    }

    private static String pad(String value, int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = value.length(); i < width; i++) sb.append(' ');
        return sb.append(value).toString();
    }
}
