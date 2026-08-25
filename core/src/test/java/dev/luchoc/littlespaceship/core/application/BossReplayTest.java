package dev.luchoc.littlespaceship.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.port.FormationSlot;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.LevelOutcome;
import dev.luchoc.littlespaceship.core.port.SimpleBossDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleEnemyDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleFormationDefinition;
import dev.luchoc.littlespaceship.core.port.SimpleWaveTimeline;
import dev.luchoc.littlespaceship.core.port.SpawnEvent;
import dev.luchoc.littlespaceship.core.testsupport.TestBalance;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A full boss encounter run through the real MVP pipeline — {@code Simulation}, every system, the
 * boss included — for phase 07's acceptance criteria: the fight is deterministic and reproducible
 * from a seed, and a replay covers a full victory and a full defeat.
 */
class BossReplayTest {

    private static final String LEVEL = "level-01";
    private static final int TICKS = 400;

    @Test
    @DisplayName("a scripted victory run reproduces the same outcome and score twice")
    void victoryIsDeterministic() {
        String first = fingerprintOf(runVictory());
        String second = fingerprintOf(runVictory());

        assertEquals(first, second);
        assertEquals(LevelOutcome.COMPLETED, runVictory().world().view().outcome());
    }

    @Test
    @DisplayName("a scripted defeat run reproduces the same outcome twice")
    void defeatIsDeterministic() {
        assertEquals(LevelOutcome.DEFEATED, runDefeat().world().view().outcome());
        assertEquals(LevelOutcome.DEFEATED, runDefeat().world().view().outcome());
    }

    /**
     * The player holds fire and stays centred, directly under the boss's core — the only part a
     * level-1-shaped single shot ever reaches — with the boss never attacking back ({@code
     * patternCooldown} far longer than the run), so the outcome depends only on the core's health
     * against sustained weapon fire, both deterministic.
     */
    private static Simulation runVictory() {
        TestBalance balance = new TestBalance();
        Simulation simulation = new Simulation(victoryContent(balance), event -> {
        }, 7, LEVEL);

        InputFrame holdFireCentred = new InputFrame(0f, 0f, true, false, false);
        for (int tick = 0; tick < TICKS; tick++) {
            simulation.tick(GameLoop.STEP, holdFireCentred);
        }
        return simulation;
    }

    /** A single life and a rammer the player never dodges: one hit ends the run. */
    private static Simulation runDefeat() {
        TestBalance balance = new TestBalance();
        balance.initialLives = 1;
        Simulation simulation = new Simulation(defeatContent(balance), event -> {
        }, 7, LEVEL);

        for (int tick = 0; tick < TICKS; tick++) {
            simulation.tick(GameLoop.STEP, InputFrame.IDLE);
        }
        return simulation;
    }

    private static String fingerprintOf(Simulation simulation) {
        World world = simulation.world();
        Player player = world.players().get(world.playerEntity());
        return "outcome=" + world.view().outcome() + " score=" + player.score
            + " lives=" + player.lives;
    }

    private static TestContent victoryContent(TestBalance balance) {
        return new TestContent(balance)
            .withBoss(LEVEL, new SimpleBossDefinition(
                "boss-l1",
                0.2f, // entersAt: soon, so most of the run is the fight itself
                30, 999, 999, // coreHealth low enough to die within TICKS; pods/arms never targeted
                5000, 500, 800,
                600f, 60f, // entranceSpeed fast, combatY close to the player
                1000f, // patternCooldown far longer than the run: the boss never attacks
                100f, 100f))
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withEnemy(new SimpleEnemyDefinition("filler", List.of(
                new dev.luchoc.littlespaceship.core.port.MapComponentSpec("collider",
                    java.util.Map.of("radius", 3.0f, "fragile", true)),
                new dev.luchoc.littlespaceship.core.port.MapComponentSpec("sprite",
                    java.util.Map.of("id", "filler")))))
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(10_000f, "filler", "single", 0.5f, null))));
    }

    private static TestContent defeatContent(TestBalance balance) {
        return new TestContent(balance)
            .withFormation(new SimpleFormationDefinition("single", List.of(new FormationSlot(0f, 0f))))
            .withEnemy(new SimpleEnemyDefinition("rammer", List.of(
                new dev.luchoc.littlespaceship.core.port.MapComponentSpec("motion",
                    java.util.Map.of("trajectory", "straight-down")),
                new dev.luchoc.littlespaceship.core.port.MapComponentSpec("collider",
                    java.util.Map.of("radius", 20.0f, "fragile", true)),
                new dev.luchoc.littlespaceship.core.port.MapComponentSpec("sprite",
                    java.util.Map.of("id", "rammer")))))
            .withTrajectory(new dev.luchoc.littlespaceship.core.port.SimpleTrajectoryDefinition(
                "straight-down", 0f, -100f))
            // Spawned in the same column as the player's own (stationary) start position, with a
            // generous 20-unit radius: descending straight down, it is guaranteed to overlap the
            // player well within TICKS, with no dodging behaviour needed to reach the collision.
            .withTimeline(LEVEL, new SimpleWaveTimeline(
                List.of(new SpawnEvent(
                    0f,
                    "rammer",
                    "single",
                    balance.playerStartX / dev.luchoc.littlespaceship.core.domain.system.MotionSystem.PLAYFIELD_WIDTH,
                    null))));
    }
}
