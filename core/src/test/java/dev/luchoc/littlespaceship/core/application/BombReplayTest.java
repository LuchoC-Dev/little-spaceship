package dev.luchoc.littlespaceship.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.port.ComponentSpec;
import dev.luchoc.littlespaceship.core.port.FormationSlot;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.MapComponentSpec;
import dev.luchoc.littlespaceship.core.port.SimpleAttachmentDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleEnemyDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleFormationDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleTrajectoryDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleWaveTimeline;
import dev.luchoc.littlespaceship.core.port.SpawnEvent;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The bomb reaches into collision, damage, scoring and the drop pipeline within the same tick, all
 * through the real {@code Simulation} pipeline — the shape of scenario {@code 05-game-systems/plan.md}
 * calls out as a better fit for a replay than for unit tests alone.
 *
 * <p>Same limitation as {@code DamageReplayTest}: this compares two runs of the same build against
 * each other, with no golden fingerprint recorded anywhere. It proves the bomb's interaction across
 * systems is deterministic within a build; it does not catch a refactor that changes the outcome
 * while keeping both runs internally consistent — see issue #12.
 */
class BombReplayTest {

    private static final String LEVEL = "level-01";
    private static final int TICKS = 600;

    @Test
    @DisplayName("a level with the bomb used repeatedly ends with the same score both times")
    void bombedRunIsDeterministic() {
        assertEquals(fingerprintOf(run()), fingerprintOf(run()));
    }

    @Test
    @DisplayName("the bomb actually destroyed something and the run is not a vacuous pass")
    void bombActuallyScoredSomething() {
        Simulation simulation = run();
        Player player = simulation.world().players().get(simulation.world().playerEntity());

        assertTrue(player.score > 0, "at least one fragile enemy should have been cleared and scored");
    }

    private static Simulation run() {
        TestContent content = levelContent();
        Simulation simulation = new Simulation(content, event -> {
        }, 11, LEVEL);

        for (int tick = 0; tick < TICKS; tick++) {
            simulation.tick(GameLoop.STEP, scriptedFrame(tick));
        }
        return simulation;
    }

    /**
     * Bomb requested a few ticks after each of the first two waves has spawned — {@code BOMB} runs
     * before {@code SPAWN} in the fixed order, so a request on the exact tick a wave becomes due
     * would still find nothing on screen yet. The adapter is what would normally debounce a held
     * key; a test script can just script the discrete requests directly.
     */
    private static InputFrame scriptedFrame(int tick) {
        boolean bomb = tick == 65 || tick == 185;
        return new InputFrame(0f, 0f, false, false, bomb);
    }

    private static String fingerprintOf(Simulation simulation) {
        World world = simulation.world();
        Player player = world.players().get(world.playerEntity());
        return "score=" + player.score + " lives=" + player.lives + " bombs=" + player.bombs
            + " entities=" + world.entityCount();
    }

    private static TestContent levelContent() {
        return new TestContent()
            .withAttachment(new SimpleAttachmentDefinition("attachment", 2))
            .withTrajectory(new SimpleTrajectoryDefinition("crawl", 0f, -9f))
            .withEnemy(new SimpleEnemyDefinition("enemy-basic", List.of(
                motion("crawl"), sprite("enemy-basic"), collider(5.5f, true), score(100f))))
            .withFormation(new SimpleFormationDefinition("single",
                List.of(new FormationSlot(0f, 0f))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(List.of(
                new SpawnEvent(1f, "enemy-basic", "single", 0.3f, null),
                new SpawnEvent(3f, "enemy-basic", "single", 0.5f, "shield"),
                new SpawnEvent(5f, "enemy-basic", "single", 0.7f, null),
                new SpawnEvent(7f, "enemy-basic", "single", 0.4f, null))));
    }

    private static ComponentSpec motion(String trajectory) {
        return new MapComponentSpec("motion", Map.of("trajectory", trajectory));
    }

    private static ComponentSpec sprite(String id) {
        return new MapComponentSpec("sprite", Map.of("id", id));
    }

    private static ComponentSpec collider(float radius, boolean fragile) {
        return new MapComponentSpec("collider", Map.of("radius", radius, "fragile", fragile));
    }

    private static ComponentSpec score(float points) {
        return new MapComponentSpec("scoreValue", Map.of("points", points));
    }
}
