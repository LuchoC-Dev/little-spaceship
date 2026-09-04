package dev.luchoc.littlespaceship.game.screen;

import java.util.List;

/**
 * The named scenarios {@link TestMenuScreen} lists, per {@code docs/plan/11h-test-mode/plan.md}'s
 * table: three waves chosen to exercise the level's range and the boss, chosen to prove a
 * scenario's starting state is configurable; plus four path scenarios added in phase 11i to
 * exercise the {@code path} trajectory vocabulary — a turn, a mirrored pair, a wait and a bounded
 * loop; plus an oscillation scenario added in phase 11j.
 *
 * <p>Each {@link Scenario#levelId()} is a level file under {@code assets/data/} in the existing
 * format — {@code game/adapter/content/JsonContentSource.java} loads it exactly as it loads
 * {@code level-01}, by that id, from {@code <levelId>.json} in the data directory. This class
 * assumes the naming convention {@code test-wave-04}, {@code test-wave-09}, {@code test-wave-12}
 * and {@code test-boss}, and, for the path scenarios, {@code test-path-turn},
 * {@code test-path-mirror}, {@code test-path-wait}, {@code test-path-loop} and
 * {@code test-path-oscillate}; it does not enforce any of it — if {@code level-designer}'s
 * scenario files use different ids, only this list needs to change.
 */
final class TestScenarios {

    private TestScenarios() {
    }

    /** One entry in the TESTS submenu: {@code levelId} is also the scenario's level file name. */
    record Scenario(String levelId, String label) {
    }

    /**
     * This list is a stack, not a queue: newest scenario on top. Add a new entry at the
     * <em>front</em> of the list, not the back — the whole point is that whatever was just added
     * is the one you are looking at, and it should not need scrolling to reach. A batch added
     * together (e.g. the four path scenarios from phase 11i) keeps its own internal order below
     * the newest batch; only the batch as a whole moves to the front, not each entry
     * individually. Decided in #291.
     */
    static final List<Scenario> ALL = List.of(
        new Scenario("test-path-oscillate", "PATH: OSCILLATE"),
        new Scenario("test-path-turn", "PATH: TURN"),
        new Scenario("test-path-mirror", "PATH: MIRROR"),
        new Scenario("test-path-wait", "PATH: WAIT"),
        new Scenario("test-path-loop", "PATH: LOOP"),
        new Scenario("test-wave-04", "WAVE 4"),
        new Scenario("test-wave-09", "WAVE 9"),
        new Scenario("test-wave-12", "WAVE 12"),
        new Scenario("test-boss", "BOSS")
    );
}
