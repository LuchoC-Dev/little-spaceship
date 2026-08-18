package spike.core;

/**
 * Acumula tiempos de fotograma. Interesa el mínimo y el percentil 1, no el
 * promedio: un shoot 'em up se siente roto por los tirones, no por la media.
 */
public final class Metrics {

    private static final int WINDOW = 600;

    private final float[] frameMillis = new float[WINDOW];
    private int written;
    private int cursor;

    private float updateAccum;
    private float drawAccum;
    private int samples;

    public void reset() {
        written = 0;
        cursor = 0;
        updateAccum = 0f;
        drawAccum = 0f;
        samples = 0;
    }

    public void sample(float delta, float updateMillis, float drawMillis) {
        frameMillis[cursor] = delta * 1000f;
        cursor = (cursor + 1) % WINDOW;
        if (written < WINDOW) written++;

        updateAccum += updateMillis;
        drawAccum += drawMillis;
        samples++;
    }

    /** FPS promedio de la ventana. Util solo junto al minimo y al percentil 1. */
    public float avgFps() {
        if (written == 0) return 0f;
        float total = 0f;
        for (int i = 0; i < written; i++) total += frameMillis[i];
        float mean = total / written;
        return mean <= 0f ? 0f : 1000f / mean;
    }

    public float avgUpdateMillis() {
        return samples == 0 ? 0f : updateAccum / samples;
    }

    public float avgDrawMillis() {
        return samples == 0 ? 0f : drawAccum / samples;
    }

    /** El fotograma más lento de la ventana, expresado en FPS. */
    public float minFps() {
        if (written == 0) return 0f;
        float worst = 0f;
        for (int i = 0; i < written; i++) {
            if (frameMillis[i] > worst) worst = frameMillis[i];
        }
        return worst <= 0f ? 0f : 1000f / worst;
    }

    /**
     * Percentil 1 de FPS: el 1 % de fotogramas peores. Es la métrica que
     * delata los tirones que el promedio esconde.
     */
    public float percentile1Fps() {
        if (written == 0) return 0f;
        float[] copy = new float[written];
        System.arraycopy(frameMillis, 0, copy, 0, written);
        java.util.Arrays.sort(copy);
        int index = Math.min(written - 1, (int) (written * 0.99f));
        float millis = copy[index];
        return millis <= 0f ? 0f : 1000f / millis;
    }
}
