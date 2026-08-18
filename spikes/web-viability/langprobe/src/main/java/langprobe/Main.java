package langprobe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sonda de caracteristicas del lenguaje. Comprueba que digiere TeaVM, para
 * elegir la version de Java del proyecto segun lo que el runtime soporta y
 * no segun lo que sea mas nuevo.
 *
 * Solo caracteristicas estables: nada marcado como preview.
 */
public final class Main {

    // --- record: contratos inmutables del dominio ---
    record InputFrame(float moveX, float moveY, boolean fire, boolean slow, boolean bomb) {}

    record Vec2(float x, float y) {
        Vec2 plus(Vec2 o) {
            return new Vec2(x + o.x, y + o.y);
        }
    }

    // --- sealed: eventos del dominio como jerarquia cerrada ---
    sealed interface GameEvent permits EnemyDestroyed, PlayerHit, PowerUpTaken {}

    record EnemyDestroyed(int entityId, int score) implements GameEvent {}
    record PlayerHit(int livesLeft) implements GameEvent {}
    record PowerUpTaken(String kind) implements GameEvent {}

    // --- interfaz funcional: el visitante de render ---
    @FunctionalInterface
    interface SpriteVisitor {
        void accept(int sprite, float x, float y);
    }

    public static void main(String[] args) {
        log("--- sonda de caracteristicas del lenguaje sobre TeaVM ---");

        probeRecords();
        probeSealedAndPatterns();
        probeSwitchExpression();
        probeTextBlock();
        probeCollectionFactories();
        probeOptional();
        probeLambdasAndVisitor();
        probeStringFormatting();
        probeInterfaceDefaults();

        log("");
        log("--- fin ---");
    }

    private static void probeRecords() {
        try {
            var frame = new InputFrame(1f, 0f, true, false, false);
            var a = new Vec2(1f, 2f);
            var b = new Vec2(3f, 4f);
            var sum = a.plus(b);
            boolean equalsWorks = new Vec2(1f, 2f).equals(a);
            boolean hashWorks = new Vec2(1f, 2f).hashCode() == a.hashCode();
            log("record                    ok  (moveX=" + frame.moveX()
                + ", suma=" + sum.x() + "/" + sum.y()
                + ", equals=" + equalsWorks + ", hashCode=" + hashWorks + ")");
            log("record toString           " + a);
        } catch (Throwable t) {
            log("record                    FALLA: " + t);
        }
    }

    private static void probeSealedAndPatterns() {
        try {
            List<GameEvent> events = new ArrayList<>();
            events.add(new EnemyDestroyed(7, 500));
            events.add(new PlayerHit(2));
            events.add(new PowerUpTaken("shield"));

            int score = 0;
            for (GameEvent e : events) {
                // pattern matching para instanceof, estable desde Java 16
                if (e instanceof EnemyDestroyed destroyed) {
                    score += destroyed.score();
                } else if (e instanceof PlayerHit hit) {
                    score -= hit.livesLeft();
                }
            }
            log("sealed + instanceof       ok  (score=" + score + ")");
        } catch (Throwable t) {
            log("sealed + instanceof       FALLA: " + t);
        }
    }

    private static void probeSwitchExpression() {
        try {
            String kind = "shield";
            // switch como expresion, estable desde Java 14
            int value = switch (kind) {
                case "shield" -> 10;
                case "life" -> 50;
                default -> 0;
            };
            log("switch expression         ok  (" + value + ")");
        } catch (Throwable t) {
            log("switch expression         FALLA: " + t);
        }
    }

    private static void probeTextBlock() {
        try {
            String json = """
                { "id": "tanque" }""";
            log("text block                ok  (" + json.length() + " chars)");
        } catch (Throwable t) {
            log("text block                FALLA: " + t);
        }
    }

    private static void probeCollectionFactories() {
        try {
            var list = List.of("a", "b", "c");
            var map = Map.of("x", 1, "y", 2);
            log("List.of / Map.of          ok  (" + list.size() + " / " + map.size() + ")");
        } catch (Throwable t) {
            log("List.of / Map.of          FALLA: " + t);
        }
    }

    private static void probeOptional() {
        try {
            Optional<String> o = Optional.of("valor");
            log("Optional                  ok  (" + o.orElse("vacio") + ")");
        } catch (Throwable t) {
            log("Optional                  FALLA: " + t);
        }
    }

    private static void probeLambdasAndVisitor() {
        try {
            int[] count = { 0 };
            SpriteVisitor visitor = (sprite, x, y) -> count[0]++;
            for (int i = 0; i < 5; i++) {
                visitor.accept(i, i, i);
            }
            log("lambda + visitante        ok  (" + count[0] + " visitas)");
        } catch (Throwable t) {
            log("lambda + visitante        FALLA: " + t);
        }
    }

    private static void probeStringFormatting() {
        try {
            String s = String.format("%.2f", 3.14159f);
            log("String.format             ok  (" + s + ")");
        } catch (Throwable t) {
            log("String.format             FALLA: " + describe(t));
        }
    }

    private static void probeInterfaceDefaults() {
        try {
            var visitor = new SpriteVisitor() {
                @Override
                public void accept(int sprite, float x, float y) {
                }
            };
            visitor.accept(1, 0, 0);
            log("clase anonima             ok");
        } catch (Throwable t) {
            log("clase anonima             FALLA: " + t);
        }
    }

    private static String describe(Throwable t) {
        String msg = t.getMessage();
        return msg == null ? t.getClass().getName() : t.getClass().getName() + ": " + msg;
    }

    private static void log(String s) {
        System.out.println(s);
    }
}
