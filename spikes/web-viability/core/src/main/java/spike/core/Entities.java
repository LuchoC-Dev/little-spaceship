package spike.core;

/**
 * Almacenamiento plano de entidades. No pretende ser la arquitectura del juego:
 * el spike mide el techo del renderizador, así que evita a propósito cualquier
 * capa que pudiera enmascarar el costo real de dibujar y actualizar.
 */
public final class Entities {

    public final float[] x;
    public final float[] y;
    public final float[] vx;
    public final float[] vy;
    public final int[] kind;
    public int count;

    public Entities(int capacity) {
        x = new float[capacity];
        y = new float[capacity];
        vx = new float[capacity];
        vy = new float[capacity];
        kind = new int[capacity];
    }

    public void clear() {
        count = 0;
    }

    public void add(float px, float py, float pvx, float pvy, int k) {
        if (count >= x.length) return;
        x[count] = px;
        y[count] = py;
        vx[count] = pvx;
        vy[count] = pvy;
        kind[count] = k;
        count++;
    }

    /** Mueve todo y rebota contra los bordes, para que nada se escape de pantalla. */
    public void update(float delta, float minX, float maxX, float minY, float maxY) {
        for (int i = 0; i < count; i++) {
            x[i] += vx[i] * delta;
            y[i] += vy[i] * delta;
            if (x[i] < minX) { x[i] = minX; vx[i] = -vx[i]; }
            if (x[i] > maxX) { x[i] = maxX; vx[i] = -vx[i]; }
            if (y[i] < minY) { y[i] = minY; vy[i] = -vy[i]; }
            if (y[i] > maxY) { y[i] = maxY; vy[i] = -vy[i]; }
        }
    }

    /**
     * Colisión O(n) de cada entidad contra un único objetivo, que es la forma
     * que tiene el caso real: muchos proyectiles contra una nave.
     */
    public int collideAgainst(float tx, float ty, float radius) {
        int hits = 0;
        float r2 = radius * radius;
        for (int i = 0; i < count; i++) {
            float dx = x[i] - tx;
            float dy = y[i] - ty;
            if (dx * dx + dy * dy < r2) hits++;
        }
        return hits;
    }
}
