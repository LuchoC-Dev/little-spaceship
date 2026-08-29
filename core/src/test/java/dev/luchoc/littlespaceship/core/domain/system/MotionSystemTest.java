package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.domain.component.Trajectory;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.testsupport.TestBalance;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MotionSystemTest {

    private static final float STEP = 1f / 60f;

    private final TestBalance balance = new TestBalance();
    private final World world = new World(new TestContent(balance), new Rng(1), new GameEventQueue());
    private final MotionSystem system = new MotionSystem();

    @Test
    @DisplayName("keyboard and mouse pushing opposite ways leave the ship still")
    void oppositeDirectionsCancel() {
        int player = spawnPlayer(100f, 100f);

        // The adapter already summed keyboard and mouse before this frame reached the core; a
        // cancelling pair of devices is exactly a moveX/moveY of zero.
        system.update(world, STEP, new InputFrame(0f, 0f, false, false, false));

        Transform transform = world.transforms().get(player);
        assertEquals(100f, transform.x);
        assertEquals(100f, transform.y);
        Motion motion = world.motions().get(player);
        assertEquals(0f, motion.vx);
        assertEquals(0f, motion.vy);
    }

    @Test
    @DisplayName("a diagonal input is not faster than a single axis at the same intensity")
    void diagonalIsNotFasterThanAxis() {
        int player = spawnPlayer(100f, 100f);

        // Both inputs exceed the cap: a naive per-axis clamp would let the diagonal one keep an
        // extra factor of up to sqrt(2), which is exactly what the magnitude clamp forbids.
        system.update(world, STEP, new InputFrame(balance.playerSpeed, balance.playerSpeed, false, false, false));
        float diagonalSpeed = speedOf(player);

        resetPlayer(player, 100f, 100f);
        system.update(world, STEP, new InputFrame(balance.playerSpeed, 0f, false, false, false));
        float axisSpeed = speedOf(player);

        assertEquals(axisSpeed, diagonalSpeed, 0.001f);
        assertEquals(balance.playerSpeed, diagonalSpeed, 0.001f);
    }

    @Test
    @DisplayName("an input below the cap is not scaled up to it")
    void belowCapIsUntouched() {
        int player = spawnPlayer(100f, 100f);

        system.update(world, STEP, new InputFrame(10f, 0f, false, false, false));

        Motion motion = world.motions().get(player);
        assertEquals(10f, motion.vx);
        assertEquals(0f, motion.vy);
    }

    @Test
    @DisplayName("slow movement is the same clamp with a smaller cap, not a separate mode")
    void slowMovementReducesTheCap() {
        int player = spawnPlayer(100f, 100f);

        system.update(world, STEP, new InputFrame(balance.playerSpeed * 10f, 0f, false, true, false));

        Motion motion = world.motions().get(player);
        assertEquals(balance.playerSpeed * balance.playerSlowFactor, motion.vx, 0.001f);
    }

    @Test
    @DisplayName("the player cannot leave the playfield's width")
    void playerIsClampedToThePlayfield() {
        int player = spawnPlayer(2f, 100f);
        Collider collider = world.colliders().get(player);

        system.update(world, STEP, new InputFrame(-balance.playerSpeed, 0f, false, false, false));

        Transform transform = world.transforms().get(player);
        assertEquals(collider.radius, transform.x, 0.001f);
    }

    @Test
    @DisplayName("the player is clamped on the right edge too")
    void playerIsClampedOnTheRightEdge() {
        int player = spawnPlayer(MotionSystem.PLAYFIELD_WIDTH - 2f, 100f);
        Collider collider = world.colliders().get(player);

        system.update(world, STEP, new InputFrame(balance.playerSpeed, 0f, false, false, false));

        Transform transform = world.transforms().get(player);
        assertEquals(MotionSystem.PLAYFIELD_WIDTH - collider.radius, transform.x, 0.001f);
    }

    @Test
    @DisplayName("the player cannot fly off the top edge, even after many ticks of sustained input")
    void playerIsClampedOnTheTopEdge() {
        int player = spawnPlayer(100f, MotionSystem.PLAYFIELD_WIDTH / 2f);
        Collider collider = world.colliders().get(player);
        InputFrame upward = new InputFrame(0f, balance.playerSpeed, false, false, false);

        // A single tick would land well short of the edge; only sustained input over many ticks
        // reproduces the reported bug of the ship flying off-screen indefinitely.
        for (int i = 0; i < 600; i++) {
            system.update(world, STEP, upward);
        }

        Transform transform = world.transforms().get(player);
        assertEquals(SpawnSystem.PLAYFIELD_HEIGHT - collider.radius, transform.y, 0.001f);
    }

    @Test
    @DisplayName("the player cannot fly off the bottom edge, even after many ticks of sustained input")
    void playerIsClampedOnTheBottomEdge() {
        int player = spawnPlayer(100f, MotionSystem.PLAYFIELD_WIDTH / 2f);
        Collider collider = world.colliders().get(player);
        InputFrame downward = new InputFrame(0f, -balance.playerSpeed, false, false, false);

        for (int i = 0; i < 600; i++) {
            system.update(world, STEP, downward);
        }

        Transform transform = world.transforms().get(player);
        assertEquals(collider.radius, transform.y, 0.001f);
    }

    @Test
    @DisplayName("sustained input against the right edge still stops at the collider's radius")
    void playerIsClampedOnTheRightEdgeUnderSustainedInput() {
        int player = spawnPlayer(100f, 100f);
        Collider collider = world.colliders().get(player);
        InputFrame rightward = new InputFrame(balance.playerSpeed, 0f, false, false, false);

        for (int i = 0; i < 600; i++) {
            system.update(world, STEP, rightward);
        }

        Transform transform = world.transforms().get(player);
        assertEquals(MotionSystem.PLAYFIELD_WIDTH - collider.radius, transform.x, 0.001f);
    }

    @Test
    @DisplayName("enemies leave the playfield freely")
    void enemiesAreNotClamped() {
        int enemy = world.createEntity();
        world.transforms().set(enemy, new Transform(MotionSystem.PLAYFIELD_WIDTH - 1f, 100f));
        world.motions().set(enemy, new Motion(500f, 0f));

        system.update(world, STEP, InputFrame.IDLE);

        Transform transform = world.transforms().get(enemy);
        assertTrue(transform.x > MotionSystem.PLAYFIELD_WIDTH,
            "an enemy must be able to fly past the playfield's edge");
    }

    @Test
    @DisplayName("does nothing when there is no player entity")
    void noPlayerIsHarmless() {
        int enemy = world.createEntity();
        world.transforms().set(enemy, new Transform(10f, 10f));
        world.motions().set(enemy, new Motion(5f, 0f));

        system.update(world, STEP, new InputFrame(1f, 1f, false, false, false));

        assertEquals(10f + 5f * STEP, world.transforms().get(enemy).x, 0.001f);
    }

    @Test
    @DisplayName("does nothing when the player has no Motion component to drive")
    void playerWithoutMotionIsHarmless() {
        int player = world.createEntity();
        world.transforms().set(player, new Transform(50f, 50f));
        world.players().set(player, new Player(3, 2, 1));

        system.update(world, STEP, new InputFrame(1f, 1f, false, false, false));

        assertEquals(50f, world.transforms().get(player).x);
    }

    @Test
    @DisplayName("a trajectory's elapsed time accumulates from the fixed step, one tick at a time")
    void trajectoryElapsedAccumulatesFromTheFixedStep() {
        int enemy = world.createEntity();
        world.transforms().set(enemy, new Transform(50f, 200f));
        world.motions().set(enemy, new Motion(0f, -30f));
        Trajectory trajectory = new Trajectory();
        world.trajectories().set(enemy, trajectory);

        system.update(world, STEP, InputFrame.IDLE);
        assertEquals(STEP, trajectory.elapsed, 0.0001f);

        system.update(world, STEP, InputFrame.IDLE);
        assertEquals(STEP * 2f, trajectory.elapsed, 0.0001f);
    }

    @Test
    @DisplayName("an entity with no Trajectory is unaffected by the advance")
    void entityWithoutTrajectoryIsHarmless() {
        int enemy = world.createEntity();
        world.transforms().set(enemy, new Transform(10f, 10f));
        world.motions().set(enemy, new Motion(5f, 0f));

        system.update(world, STEP, InputFrame.IDLE);

        assertEquals(10f + 5f * STEP, world.transforms().get(enemy).x, 0.001f);
    }

    private float speedOf(int entity) {
        Motion motion = world.motions().get(entity);
        return (float) Math.sqrt(motion.vx * motion.vx + motion.vy * motion.vy);
    }

    private void resetPlayer(int player, float x, float y) {
        Transform transform = world.transforms().get(player);
        transform.x = x;
        transform.y = y;
        Motion motion = world.motions().get(player);
        motion.vx = 0f;
        motion.vy = 0f;
    }

    private int spawnPlayer(float x, float y) {
        int player = world.createEntity();
        world.transforms().set(player, new Transform(x, y));
        world.motions().set(player, new Motion(0f, 0f));
        world.colliders().set(player, new Collider(4f, CollisionLayer.PLAYER));
        world.players().set(player, new Player(3, 2, 1));
        return player;
    }
}
