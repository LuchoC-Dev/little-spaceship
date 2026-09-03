package dev.luchoc.littlespaceship.game.screen;

import java.util.List;

/**
 * The named scenarios {@link TestMenuScreen} lists, per {@code docs/plan/11h-test-mode/plan.md}'s
 * table: three waves chosen to exercise the level's range and the boss, chosen to prove a
 * scenario's starting state is configurable.
 *
 * <p>Each {@link Scenario#levelId()} is a level file under {@code assets/data/} in the existing
 * format — {@code game/adapter/content/JsonContentSource.java} loads it exactly as it loads
 * {@code level-01}, by that id, from {@code <levelId>.json} in the data directory. This class
 * assumes the naming convention {@code test-wave-04}, {@code test-wave-09}, {@code test-wave-12}
 * and {@code test-boss}; it does not enforce it; if {@code level-designer}'s scenario files use
 * different ids, only this list needs to change.
 */
final class TestScenarios {

    private TestScenarios() {
    }

    /** One entry in the TESTS submenu: {@code levelId} is also the scenario's level file name. */
    record Scenario(String levelId, String label) {
    }

    static final List<Scenario> ALL = List.of(
        new Scenario("test-wave-04", "WAVE 4"),
        new Scenario("test-wave-09", "WAVE 9"),
        new Scenario("test-wave-12", "WAVE 12"),
        new Scenario("test-boss", "BOSS")
    );
}
