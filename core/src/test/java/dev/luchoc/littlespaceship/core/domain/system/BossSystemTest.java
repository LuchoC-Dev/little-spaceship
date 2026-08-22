package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.BossStatus;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.LevelOutcome;
import dev.luchoc.littlespaceship.core.port.SimpleBossDefinition;
import dev.luchoc.littlespaceship.core.testsupport.TestBalance;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code BossSystem} against content built inline, per the same reasoning {@code SpawnSystemTest}
 * states for the ordinary wave system.
 */
class BossSystemTest {

    private static final String LEVEL = "level-01";
    private static final float STEP = 1f / 60f;

    private final TestBalance balance = new TestBalance();

    @Test
    @DisplayName("rejects being built without a level id")
    void rejectsMissingLevelId() {
        assertThrows(IllegalArgumentException.class, () -> new BossSystem(null));
        assertThrows(IllegalArgumentException.class, () -> new BossSystem(""));
    }

    @Test
    @DisplayName("a level with no boss never reports one present, and never registers as a boss level")
    void levelWithNoBossStaysAbsent() {
        World world = new World(new TestContent(balance), new Rng(1), new GameEventQueue());
        BossSystem system = new BossSystem(LEVEL);

        for (int i = 0; i < 10; i++) {
            system.update(world, STEP, InputFrame.IDLE);
        }

        assertEquals(BossStatus.NONE, world.view().bossStatus());
        assertEquals(0, world.colliders().size());
        // Old, pre-boss outcome rule still governs a boss-less level: no wave timeline was ever
        // registered here, so the run simply never completes, which is the correct behaviour for a
        // run with no level to finish, exactly as the class it delegates to documents.
        assertEquals(LevelOutcome.IN_PROGRESS, world.view().outcome());
    }

    @Test
    @DisplayName("the boss stays absent before entersAt and spawns five parts once it is reached")
    void spawnsFivePartsAtEntersAt() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(1f));
        World world = new World(content, new Rng(1), new GameEventQueue());
        BossSystem system = new BossSystem(LEVEL);

        system.update(world, 0.5f, InputFrame.IDLE);
        assertEquals(BossStatus.NONE, world.view().bossStatus());
        assertEquals(0, world.colliders().size());

        system.update(world, 0.6f, InputFrame.IDLE);
        assertEquals(5, world.colliders().size());
        BossStatus status = world.view().bossStatus();
        assertTrue(status.present());
        assertEquals(status.hpMax(), status.hp());
        for (int i = 0; i < world.colliders().size(); i++) {
            assertEquals(CollisionLayer.ENEMY, world.colliders().valueAt(i).layer);
            assertFalse(world.colliders().valueAt(i).fragile);
        }
    }

    @Test
    @DisplayName("the health bar is hidden before the boss spawns and after it is defeated")
    void presentOnlyDuringTheFight() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(1), new GameEventQueue());
        BossSystem system = new BossSystem(LEVEL);

        system.update(world, STEP, InputFrame.IDLE);
        assertTrue(world.view().bossStatus().present());

        int core = coreEntity(world);
        world.destroyEntity(core);
        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.view().bossStatus().present());
    }

    @Test
    @DisplayName("defeating the core ends the fight, clears remaining parts and wins with a life left")
    void defeatingTheCoreWinsTheRun() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(1), new GameEventQueue());
        int player = world.createEntity();
        world.players().set(player, new dev.luchoc.littlespaceship.core.domain.component.Player(3, 2, 1));
        BossSystem system = new BossSystem(LEVEL);

        system.update(world, STEP, InputFrame.IDLE);
        assertEquals(LevelOutcome.IN_PROGRESS, world.view().outcome());

        int core = coreEntity(world);
        world.destroyEntity(core);
        system.update(world, STEP, InputFrame.IDLE);

        assertEquals(LevelOutcome.COMPLETED, world.view().outcome());
        // Every part but the core is marked for destruction with it: a defeated boss does not linger
        // as a headless husk. Actual removal is CleanupSystem's job, not exercised in this isolated
        // BossSystem test, so what is checked here is the marking, not the collider count.
        assertEquals(4, world.pendingDestruction().size());
    }

    @Test
    @DisplayName("losing every life defeats the run even mid-fight, and DEFEATED wins a same-tick tie")
    void defeatWinsATieWithBossDefeat() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(1), new GameEventQueue());
        int player = world.createEntity();
        dev.luchoc.littlespaceship.core.domain.component.Player state =
            new dev.luchoc.littlespaceship.core.domain.component.Player(3, 2, 1);
        world.players().set(player, state);
        BossSystem system = new BossSystem(LEVEL);

        system.update(world, STEP, InputFrame.IDLE);
        int core = coreEntity(world);
        world.destroyEntity(core);
        state.lives = 0;
        system.update(world, STEP, InputFrame.IDLE);

        assertEquals(LevelOutcome.DEFEATED, world.view().outcome());
    }

    @Test
    @DisplayName("the entrance holds the boss off screen and settles it at combatY before attacking")
    void entranceDescendsToCombatY() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(1), new GameEventQueue());
        BossSystem system = new BossSystem(LEVEL);

        system.update(world, STEP, InputFrame.IDLE);
        int core = coreEntity(world);
        float spawnY = world.transforms().get(core).y;
        assertTrue(spawnY > SpawnSystem.PLAYFIELD_HEIGHT);

        // A generous number of ticks at a fast entrance speed is enough to reach combatY.
        for (int i = 0; i < 600; i++) {
            system.update(world, STEP, InputFrame.IDLE);
        }

        assertEquals(120f, world.transforms().get(core).y, 0.01f);
    }

    @Test
    @DisplayName("the tell steps a charging pod's frame through 1, 2, 3 and back to 0 on fire")
    void tellStepsThroughThreeBeatsThenFires() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(1), new GameEventQueue());
        BossSystem system = new BossSystem(LEVEL);

        // Reach the fight: one tick to spawn, one generous single step to clear the whole entrance
        // in one jump, so FIGHT starts with its cooldown untouched instead of already part spent.
        system.update(world, STEP, InputFrame.IDLE);
        system.update(world, 1f, InputFrame.IDLE);

        int pod = podEntity(world);
        Sprite sprite = world.sprites().get(pod);
        assertEquals(0, sprite.frame);

        // Real, small ticks: the cooldown (0.2s) elapses, then the tell steps 1, 2, 3 in order, and
        // fires once it reaches 0.75s, dropping the frame back to zero the same tick.
        int collidersBeforeFire = world.colliders().size();
        boolean sawOne = false;
        boolean sawTwo = false;
        boolean sawThree = false;
        boolean fired = false;
        for (int i = 0; i < 200 && !fired; i++) {
            system.update(world, STEP, InputFrame.IDLE);
            int frame = sprite.frame;
            sawOne |= frame == 1;
            sawTwo |= sawOne && frame == 2;
            sawThree |= sawTwo && frame == 3;
            fired = sawThree && frame == 0;
        }

        assertTrue(sawOne, "never saw beat 1");
        assertTrue(sawTwo, "never saw beat 2");
        assertTrue(sawThree, "never saw beat 3");
        assertTrue(fired, "the tell never resolved into a fire");
        assertTrue(world.colliders().size() > collidersBeforeFire);
    }

    private static int coreEntity(World world) {
        for (int i = 0; i < world.colliders().size(); i++) {
            if (world.colliders().valueAt(i).radius > 17f) {
                return world.colliders().entityAt(i);
            }
        }
        throw new IllegalStateException("no core entity found");
    }

    private static int podEntity(World world) {
        for (int i = 0; i < world.colliders().size(); i++) {
            Collider collider = world.colliders().valueAt(i);
            if (collider.radius > 11f && collider.radius < 13f) {
                return world.colliders().entityAt(i);
            }
        }
        throw new IllegalStateException("no pod entity found");
    }

    private static SimpleBossDefinition boss(float entersAt) {
        return new SimpleBossDefinition(
            "boss-l1", entersAt,
            10, 5, 5,
            5000, 500, 800,
            1000f, 120f,
            0.2f,
            180f, 160f);
    }
}
