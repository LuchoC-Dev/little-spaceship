package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
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
        assertEquals(6, world.colliders().size());
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
        assertEquals(5, world.pendingDestruction().size());
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

        // A generous number of ticks at a fast entrance speed is enough to reach combatY, but not so
        // many that the fight's first cooldown-then-tell (0.2s + 0.75s = 0.95s, this fixture's own
        // boss()) elapses and the boss starts its first move away from combatY: since #325, the boss
        // travels to a star point once a volley resolves, so this test now needs to observe the
        // settle before that happens rather than well after it, unlike before movement existed.
        for (int i = 0; i < 20; i++) {
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

    @Test
    @DisplayName(
        "a spread volley fans five rays per pod, a sweep volley fans five per arm, "
            + "alternating, every ray at the pattern's fixed speed")
    void volleyFansFiveRaysPerSideAndAlternatesPattern() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(1), new GameEventQueue());
        BossSystem system = new BossSystem(LEVEL);

        // Reach the fight the same way tellStepsThroughThreeBeatsThenFires does.
        system.update(world, STEP, InputFrame.IDLE);
        system.update(world, 1f, InputFrame.IDLE);

        java.util.Set<Integer> seen = new java.util.HashSet<>();
        java.util.List<dev.luchoc.littlespaceship.core.domain.component.Motion> firstVolley =
            runToNextVolley(world, system, seen);
        assertVolleySpeeds(firstVolley, 180f);

        java.util.List<dev.luchoc.littlespaceship.core.domain.component.Motion> secondVolley =
            runToNextVolley(world, system, seen);
        assertVolleySpeeds(secondVolley, 160f);
    }

    @Test
    @DisplayName("the fan aims at the player's position as locked at the start of the tell, not at fire time")
    void volleyAimsAtThePlayerLockedAtTellStart() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(1), new GameEventQueue());
        int player = world.createEntity();
        world.players().set(player, new dev.luchoc.littlespaceship.core.domain.component.Player(3, 2, 1));
        world.transforms().set(
            player, new dev.luchoc.littlespaceship.core.domain.component.Transform(20f, 30f));
        BossSystem system = new BossSystem(LEVEL);

        // Reach the fight: spawn, then the entrance in one jump, then the fixed 0.2 s cooldown of
        // this fixture's own boss() definition.
        system.update(world, STEP, InputFrame.IDLE);
        system.update(world, 1f, InputFrame.IDLE);
        system.update(world, 0.2f, InputFrame.IDLE);

        // The tell has now begun and the aim is locked at (20, 30). Move the player far away before
        // the volley actually fires: an honest tell dodges the frozen point, not a live-tracking one.
        world.transforms().get(player).x = 150f;

        java.util.Set<Integer> seen = new java.util.HashSet<>();
        java.util.List<dev.luchoc.littlespaceship.core.domain.component.Motion> volley =
            runToNextVolley(world, system, seen);

        int pod = podEntity(world);
        Transform podOrigin = world.transforms().get(pod);
        float expectedDx = 20f - podOrigin.x;
        float expectedDy = 30f - podOrigin.y;
        float expectedLength = (float) Math.sqrt(expectedDx * expectedDx + expectedDy * expectedDy);
        // The centre ray of the fan (ratio 0) reproduces the locked aim direction exactly.
        dev.luchoc.littlespaceship.core.domain.component.Motion centreRay = null;
        float bestAlignment = -2f;
        for (dev.luchoc.littlespaceship.core.domain.component.Motion motion : volley) {
            float speed = (float) Math.sqrt(motion.vx * motion.vx + motion.vy * motion.vy);
            float alignment = (motion.vx * expectedDx + motion.vy * expectedDy) / (speed * expectedLength);
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                centreRay = motion;
            }
        }
        assertTrue(bestAlignment > 0.99f, "no ray points at the position locked at tell start");
        assertTrue(centreRay != null);
    }

    /**
     * Drives {@code system} until new {@code ENEMY_PROJECTILE} colliders appear beyond those already in
     * {@code seen}, then returns exactly the newly spawned ones — a whole volley, since {@code
     * BossSystem} fires a pattern's full fan within a single {@code update} call, and adds them to
     * {@code seen} so a later call for the next volley does not re-count them. No {@code MotionSystem}
     * or {@code CleanupSystem} runs in this isolated test, so a fired projectile stays a live collider
     * forever — {@code seen} is what stands in for their absence.
     */
    private static java.util.List<dev.luchoc.littlespaceship.core.domain.component.Motion> runToNextVolley(
        World world, BossSystem system, java.util.Set<Integer> seen) {
        for (int i = 0; i < 200; i++) {
            system.update(world, STEP, InputFrame.IDLE);
            java.util.List<dev.luchoc.littlespaceship.core.domain.component.Motion> fresh =
                new java.util.ArrayList<>();
            for (int j = 0; j < world.colliders().size(); j++) {
                if (world.colliders().valueAt(j).layer != CollisionLayer.ENEMY_PROJECTILE) {
                    continue;
                }
                int entity = world.colliders().entityAt(j);
                if (seen.add(entity)) {
                    fresh.add(world.motions().get(entity));
                }
            }
            if (!fresh.isEmpty()) {
                return fresh;
            }
        }
        throw new IllegalStateException("no volley fired within the tick budget");
    }

    /**
     * A volley must be exactly ten projectiles — {@code FAN_COUNT} (five) rays from each of the two
     * firing parts — every one travelling at exactly {@code speed}, since the aimed fan spreads
     * direction, never magnitude. The rays' directions are covered separately by {@code
     * volleyAimsAtThePlayerLockedAtTellStart}.
     */
    private static void assertVolleySpeeds(
        java.util.List<dev.luchoc.littlespaceship.core.domain.component.Motion> volley, float speed) {
        assertEquals(10, volley.size(), "a volley must fan five rays from each of two firing parts");
        for (dev.luchoc.littlespaceship.core.domain.component.Motion motion : volley) {
            float magnitude = (float) Math.sqrt(motion.vx * motion.vx + motion.vy * motion.vy);
            assertEquals(speed, magnitude, 0.05f, "every ray of an aimed volley travels at the pattern's own speed");
        }
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

    @Test
    @DisplayName("the star points and the boss's whole six-collider extent stay inside the playfield, "
        + "clear of the bottom of the screen")
    void starPointsKeepTheWholeBossOnScreen() {
        // Mirrors the offsets and radii BossSystem hardcodes for its six parts, to check the extent
        // rather than the centre — the arm is the widest part, the keel the lowest, the core/pod the
        // highest, per the class javadoc on STAR_X/STAR_Y.
        float armHalfWidth = 44f + 14f;
        float keelBelowCore = 27f + 13f;
        float coreOrPodAboveCore = 18f;

        for (int i = 0; i < BossSystem.STAR_POINT_COUNT; i++) {
            float x = BossSystem.STAR_X[i];
            float y = BossSystem.STAR_Y[i];
            assertTrue(x - armHalfWidth >= 0f, "point " + (i + 1) + " lets an arm cross the left edge");
            assertTrue(x + armHalfWidth <= MotionSystem.PLAYFIELD_WIDTH,
                "point " + (i + 1) + " lets an arm cross the right edge");
            assertTrue(y - keelBelowCore >= 0f,
                "point " + (i + 1) + " lets the keel reach the bottom of the screen");
            assertTrue(y + coreOrPodAboveCore <= SpawnSystem.PLAYFIELD_HEIGHT,
                "point " + (i + 1) + " lets the core or a pod cross the top edge");
        }
    }

    @Test
    @DisplayName("the boss travels only between the ten star points, "
        + "never more than three perimeter steps per move")
    void bossVisitsOnlyStarPointsWithinThreeSteps() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(7), new GameEventQueue());
        BossSystem system = new BossSystem(LEVEL);

        java.util.List<Integer> starIndices = driveAndCollectStarIndices(world, system, 12);

        assertTrue(starIndices.size() >= 8, "not enough star points visited to check the rule");
        for (int i = 1; i < starIndices.size(); i++) {
            int previous = starIndices.get(i - 1);
            int current = starIndices.get(i);
            int forward = Math.floorMod(current - previous, BossSystem.STAR_POINT_COUNT);
            int backward = BossSystem.STAR_POINT_COUNT - forward;
            int steps = Math.min(forward, backward);
            assertTrue(steps >= 1 && steps <= 3,
                "move from point " + (previous + 1) + " to point " + (current + 1)
                    + " is " + steps + " perimeter steps, outside the legal 1-3 range");
        }
    }

    @Test
    @DisplayName("the same seed produces the same sequence of star points")
    void samePatternForTheSameSeed() {
        java.util.List<Integer> first = driveAndCollectStarIndices(
            new World(new TestContent(balance).withBoss(LEVEL, boss(0f)), new Rng(42), new GameEventQueue()),
            new BossSystem(LEVEL), 6);
        java.util.List<Integer> second = driveAndCollectStarIndices(
            new World(new TestContent(balance).withBoss(LEVEL, boss(0f)), new Rng(42), new GameEventQueue()),
            new BossSystem(LEVEL), 6);

        assertEquals(first, second);
    }

    @Test
    @DisplayName("after moving to a star point, the next attack still fires from a standstill, "
        + "aimed at the player position locked at that tell's start")
    void firesFromStandstillAfterMoving() {
        TestContent content = new TestContent(balance).withBoss(LEVEL, boss(0f));
        World world = new World(content, new Rng(3), new GameEventQueue());
        int player = world.createEntity();
        world.players().set(player, new dev.luchoc.littlespaceship.core.domain.component.Player(3, 2, 1));
        world.transforms().set(
            player, new dev.luchoc.littlespaceship.core.domain.component.Transform(50f, 40f));
        BossSystem system = new BossSystem(LEVEL);

        // Reach the fight and let the first volley fire, then wait through the move to the first
        // star point.
        system.update(world, STEP, InputFrame.IDLE);
        system.update(world, 1f, InputFrame.IDLE);
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        runToNextVolley(world, system, seen);

        int core = coreEntity(world);
        // Drive until the core stops changing position for two consecutive ticks: the move to the
        // first star point has completed.
        float lastX = world.transforms().get(core).x;
        float lastY = world.transforms().get(core).y;
        boolean settled = false;
        for (int i = 0; i < 300 && !settled; i++) {
            system.update(world, STEP, InputFrame.IDLE);
            float x = world.transforms().get(core).x;
            float y = world.transforms().get(core).y;
            settled = x == lastX && y == lastY;
            lastX = x;
            lastY = y;
        }
        assertTrue(settled, "the boss never settled after its first move");
        float settledX = lastX;
        float settledY = lastY;

        // The player moves away right after the boss has settled, before the next tell locks aim.
        world.transforms().get(player).x = 10f;
        world.transforms().get(player).y = 200f;

        java.util.List<dev.luchoc.littlespaceship.core.domain.component.Motion> secondVolley =
            runToNextVolley(world, system, seen);

        // The boss fired without having moved again: its position at fire time is exactly where it
        // settled.
        assertEquals(settledX, world.transforms().get(core).x, 0.001f);
        assertEquals(settledY, world.transforms().get(core).y, 0.001f);

        int pod = podEntity(world);
        Transform podOrigin = world.transforms().get(pod);
        float expectedDx = 10f - podOrigin.x;
        float expectedDy = 200f - podOrigin.y;
        float expectedLength = (float) Math.sqrt(expectedDx * expectedDx + expectedDy * expectedDy);
        float bestAlignment = -2f;
        for (dev.luchoc.littlespaceship.core.domain.component.Motion motion : secondVolley) {
            float speed = (float) Math.sqrt(motion.vx * motion.vx + motion.vy * motion.vy);
            float alignment = (motion.vx * expectedDx + motion.vy * expectedDy) / (speed * expectedLength);
            bestAlignment = Math.max(bestAlignment, alignment);
        }
        assertTrue(bestAlignment > 0.99f, "the post-move volley does not aim at the locked player position");
    }

    /**
     * Drives {@code system} through the fight and returns the sequence of star-point indices it
     * settles on — every position held for more than one consecutive tick once matched against {@link
     * BossSystem#STAR_X}/{@link BossSystem#STAR_Y} within a small tolerance. The very first hold is
     * the entrance's own landing spot at {@code combatY}, which the star does not claim as one of its
     * own, so it is dropped: only positions that actually match a star point are returned.
     */
    private static java.util.List<Integer> driveAndCollectStarIndices(
        World world, BossSystem system, int minStarStops) {
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        Float previousX = null;
        Float previousY = null;
        boolean previousWasHold = false;
        int core = -1;
        for (int i = 0; i < 20000 && indices.size() < minStarStops; i++) {
            system.update(world, STEP, InputFrame.IDLE);
            if (core == -1 || !world.isAlive(core)) {
                try {
                    core = coreEntity(world);
                } catch (IllegalStateException notYetSpawned) {
                    continue;
                }
            }
            Transform transform = world.transforms().get(core);
            float x = transform.x;
            float y = transform.y;
            boolean sameAsPrevious = previousX != null && x == previousX && y == previousY;
            if (sameAsPrevious && !previousWasHold) {
                int index = findStarIndex(x, y);
                if (index >= 0) {
                    indices.add(index);
                }
                previousWasHold = true;
            } else if (!sameAsPrevious) {
                previousWasHold = false;
            }
            previousX = x;
            previousY = y;
        }
        return indices;
    }

    private static int findStarIndex(float x, float y) {
        for (int i = 0; i < BossSystem.STAR_POINT_COUNT; i++) {
            if (Math.abs(BossSystem.STAR_X[i] - x) < 0.01f && Math.abs(BossSystem.STAR_Y[i] - y) < 0.01f) {
                return i;
            }
        }
        return -1;
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
