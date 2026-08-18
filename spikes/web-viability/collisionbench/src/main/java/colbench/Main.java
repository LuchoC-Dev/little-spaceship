package colbench;

/**
 * Benchmark de colisiones n x m, sin libGDX ni GPU, para poder ejecutarlo
 * tanto en la JVM como en Node y comparar el costo real de la logica.
 *
 * El primer benchmark del spike media n entidades contra UN punto, que es el
 * caso barato. Un shoot 'em up necesita ademas:
 *
 *   proyectiles del jugador  x  enemigos      <- el caso caro
 *   proyectiles enemigos     x  jugador
 *   enemigos                 x  jugador
 *
 * Se comparan dos estrategias sobre exactamente los mismos datos:
 *
 *   naive   todos contra todos, O(n*m)
 *   grid    rejilla uniforme, solo se prueban las celdas vecinas
 */
public final class Main {

    // Campo de juego del MVP: 208 x 270 unidades logicas.
    private static final float FIELD_W = 208f;
    private static final float FIELD_H = 270f;

    private static final int TICKS = 600;

    public static void main(String[] args) {
        log("--- benchmark de colisiones n x m ---");
        log("campo " + (int) FIELD_W + "x" + (int) FIELD_H + ", " + TICKS + " ticks por caso");
        log("");
        log("escenario                    pares      naive      grid");
        log("-------------------------------------------------------");

        run("MVP realista",      80,   40,  300);
        run("denso",            200,  100,  800);
        run("muy denso",        500,  200, 2000);
        run("absurdo",         1000,  500, 4000);

        log("");
        log("--- fin ---");
    }

    private static void run(String name, int bullets, int enemies, int enemyBullets) {
        World w = new World(bullets, enemies, enemyBullets);

        long pairs = (long) bullets * enemies + enemyBullets;

        float naive = measure(w, false);
        float grid = measure(w, true);

        log(pad(name, 22)
            + pad(Long.toString(pairs), 10)
            + pad(millis(naive), 10)
            + pad(millis(grid), 10));
    }

    private static float measure(World w, boolean useGrid) {
        w.reset();
        // Un tick de calentamiento no basta en JS: el JIT necesita ver el bucle.
        for (int i = 0; i < 60; i++) {
            w.step(useGrid);
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < TICKS; i++) {
            w.step(useGrid);
        }
        long elapsed = System.currentTimeMillis() - start;
        return elapsed / (float) TICKS;
    }

    private static String millis(float v) {
        int whole = (int) v;
        int frac = Math.abs(Math.round((v - whole) * 1000f));
        String f = Integer.toString(frac);
        while (f.length() < 3) f = "0" + f;
        return whole + "." + f + "ms";
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static void log(String s) {
        System.out.println(s);
    }

    /** Mundo minimo: tres grupos de entidades y las colisiones entre ellos. */
    static final class World {

        private final float[] bx, by, bvx, bvy;
        private final float[] ex, ey, evx, evy;
        private final boolean[] eAlive;
        private final float[] px, py, pvx, pvy;

        private final int bullets, enemies, enemyBullets;

        private final Grid grid;
        private int hits;

        private int seed = 12345;

        World(int bullets, int enemies, int enemyBullets) {
            this.bullets = bullets;
            this.enemies = enemies;
            this.enemyBullets = enemyBullets;

            bx = new float[bullets]; by = new float[bullets];
            bvx = new float[bullets]; bvy = new float[bullets];
            ex = new float[enemies]; ey = new float[enemies];
            evx = new float[enemies]; evy = new float[enemies];
            eAlive = new boolean[enemies];
            px = new float[enemyBullets]; py = new float[enemyBullets];
            pvx = new float[enemyBullets]; pvy = new float[enemyBullets];

            grid = new Grid(FIELD_W, FIELD_H, 16f);
        }

        /** Generador propio: Random no rinde igual en JVM y en TeaVM. */
        private float rnd(float min, float max) {
            seed = seed * 1103515245 + 12345;
            int v = (seed >>> 16) & 0x7fff;
            return min + (max - min) * (v / 32767f);
        }

        void reset() {
            seed = 12345;
            hits = 0;
            for (int i = 0; i < bullets; i++) {
                bx[i] = rnd(0, FIELD_W); by[i] = rnd(0, FIELD_H);
                bvx[i] = rnd(-20, 20); bvy[i] = rnd(200, 300);
            }
            for (int i = 0; i < enemies; i++) {
                ex[i] = rnd(0, FIELD_W); ey[i] = rnd(0, FIELD_H);
                evx[i] = rnd(-30, 30); evy[i] = rnd(-40, -10);
                eAlive[i] = true;
            }
            for (int i = 0; i < enemyBullets; i++) {
                px[i] = rnd(0, FIELD_W); py[i] = rnd(0, FIELD_H);
                pvx[i] = rnd(-40, 40); pvy[i] = rnd(-120, -60);
            }
        }

        void step(boolean useGrid) {
            move();
            if (useGrid) collideGrid(); else collideNaive();
            collidePlayer();
        }

        private void move() {
            for (int i = 0; i < bullets; i++) {
                bx[i] += bvx[i] * 0.016f; by[i] += bvy[i] * 0.016f;
                if (by[i] > FIELD_H) { by[i] = 0; bx[i] = rnd(0, FIELD_W); }
            }
            for (int i = 0; i < enemies; i++) {
                ex[i] += evx[i] * 0.016f; ey[i] += evy[i] * 0.016f;
                if (ey[i] < 0) { ey[i] = FIELD_H; ex[i] = rnd(0, FIELD_W); }
            }
            for (int i = 0; i < enemyBullets; i++) {
                px[i] += pvx[i] * 0.016f; py[i] += pvy[i] * 0.016f;
                if (py[i] < 0) { py[i] = FIELD_H; px[i] = rnd(0, FIELD_W); }
            }
        }

        /** Todos los proyectiles del jugador contra todos los enemigos. */
        private void collideNaive() {
            for (int b = 0; b < bullets; b++) {
                float x = bx[b], y = by[b];
                for (int e = 0; e < enemies; e++) {
                    if (!eAlive[e]) continue;
                    float dx = x - ex[e];
                    float dy = y - ey[e];
                    if (dx * dx + dy * dy < 36f) hits++;
                }
            }
        }

        /** Lo mismo, pero consultando solo las celdas vecinas. */
        private void collideGrid() {
            grid.clear();
            for (int e = 0; e < enemies; e++) {
                if (eAlive[e]) grid.insert(e, ex[e], ey[e]);
            }
            for (int b = 0; b < bullets; b++) {
                hits += grid.queryHits(bx[b], by[b], 6f, ex, ey);
            }
        }

        /** Proyectiles enemigos contra el jugador: siempre n contra 1. */
        private void collidePlayer() {
            float pxp = FIELD_W / 2f, pyp = 40f;
            for (int i = 0; i < enemyBullets; i++) {
                float dx = px[i] - pxp;
                float dy = py[i] - pyp;
                if (dx * dx + dy * dy < 16f) hits++;
            }
        }
    }

    /** Rejilla uniforme con listas encadenadas planas, sin asignar en cada tick. */
    static final class Grid {

        private final int cols, rows;
        private final float cell;
        private final int[] head;
        private final int[] next;

        Grid(float width, float height, float cell) {
            this.cell = cell;
            this.cols = (int) Math.ceil(width / cell) + 1;
            this.rows = (int) Math.ceil(height / cell) + 1;
            this.head = new int[cols * rows];
            this.next = new int[8192];
        }

        void clear() {
            for (int i = 0; i < head.length; i++) head[i] = -1;
        }

        void insert(int id, float x, float y) {
            int c = index(x, y);
            if (c < 0 || id >= next.length) return;
            next[id] = head[c];
            head[c] = id;
        }

        private int index(float x, float y) {
            int cx = (int) (x / cell);
            int cy = (int) (y / cell);
            if (cx < 0) cx = 0; if (cx >= cols) cx = cols - 1;
            if (cy < 0) cy = 0; if (cy >= rows) cy = rows - 1;
            return cy * cols + cx;
        }

        int queryHits(float x, float y, float radius, float[] ex, float[] ey) {
            int found = 0;
            int cx = (int) (x / cell);
            int cy = (int) (y / cell);
            float r2 = radius * radius;
            for (int oy = -1; oy <= 1; oy++) {
                int gy = cy + oy;
                if (gy < 0 || gy >= rows) continue;
                for (int ox = -1; ox <= 1; ox++) {
                    int gx = cx + ox;
                    if (gx < 0 || gx >= cols) continue;
                    for (int id = head[gy * cols + gx]; id != -1; id = next[id]) {
                        float dx = x - ex[id];
                        float dy = y - ey[id];
                        if (dx * dx + dy * dy < r2) found++;
                    }
                }
            }
            return found;
        }
    }
}
