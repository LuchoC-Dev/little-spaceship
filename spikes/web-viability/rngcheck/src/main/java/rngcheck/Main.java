package rngcheck;

/**
 * Comprueba que el Rng del core produce la misma secuencia bajo TeaVM que
 * sobre la JVM. La clase Rng es copia literal de core; si divergiera, los
 * replays grabados en desktop no se reproducirian en el navegador.
 */
public final class Main {

    public static void main(String[] args) {
        Rng rng = new Rng(12345);
        StringBuilder ints = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) ints.append(", ");
            ints.append(rng.nextInt());
        }
        System.out.println("ints  [12345]: " + ints);

        Rng f = new Rng(12345);
        StringBuilder floats = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) floats.append(", ");
            floats.append(f.nextFloat());
        }
        System.out.println("floats[12345]: " + floats);

        Rng z = new Rng(0);
        StringBuilder zero = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) zero.append(", ");
            zero.append(z.nextInt());
        }
        System.out.println("ints  [0]    : " + zero);
    }
}
