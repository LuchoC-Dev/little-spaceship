package langprobe;

/**
 * Caracteristicas estables de Java 21 que interesarian al dominio:
 * patrones sobre registros y switch exhaustivo sobre jerarquias selladas.
 */
public final class Main21 {

    sealed interface Event permits Destroyed, Hit {}
    record Destroyed(int id, int score) implements Event {}
    record Hit(int lives) implements Event {}

    public static void main(String[] args) {
        System.out.println("--- caracteristicas de Java 21 sobre TeaVM ---");

        try {
            Event e = new Destroyed(7, 500);
            // record pattern: descompone el registro en la propia condicion
            if (e instanceof Destroyed(int id, int score)) {
                System.out.println("record pattern            ok  (id=" + id + ", score=" + score + ")");
            }
        } catch (Throwable t) {
            System.out.println("record pattern            FALLA: " + t);
        }

        try {
            Event e = new Hit(2);
            // switch exhaustivo sobre sealed, sin rama default
            String out = switch (e) {
                case Destroyed d -> "destruido " + d.score();
                case Hit h -> "golpeado " + h.lives();
            };
            System.out.println("switch sobre sealed       ok  (" + out + ")");
        } catch (Throwable t) {
            System.out.println("switch sobre sealed       FALLA: " + t);
        }

        System.out.println("--- fin ---");
    }
}
