package spike.core;

/** Parámetros del spike. Coinciden con las decisiones del MVP para que la medición sea representativa. */
public final class SpikeConfig {

    /** Resolución lógica propuesta en 10-valores-iniciales-mvp.md. */
    public static final int LOGICAL_WIDTH = 480;
    public static final int LOGICAL_HEIGHT = 270;

    /** Ancho del campo de juego vertical, centrado; el resto son márgenes de HUD. */
    public static final int PLAYFIELD_WIDTH = 208;

    /** Escalones de carga que recorre la prueba de estrés. */
    public static final int[] STRESS_STEPS = { 250, 500, 1000, 2000, 4000 };

    private SpikeConfig() {}
}
