package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BombSystemTest {

    private static final float STEP = 1f / 60f;
    private static final InputFrame BOMB_INPUT = new InputFrame(0f, 0f, false, false, true);

    private final World world = new World(new TestContent(), new Rng(1), new GameEventQueue());
    private final BombSystem system = new BombSystem();

    @Test
    @DisplayName("clears fragile enemies and enemy projectiles, spends one bomb")
    void detonatesAndSpendsABomb() {
        int player = spawnPlayer(2);
        int fragileEnemy = enemy(true);
        int enemyProjectile = enemyProjectile();

        system.update(world, STEP, BOMB_INPUT);

        assertTrue(world.pendingDestruction().contains(fragileEnemy));
        assertTrue(world.pendingDestruction().contains(enemyProjectile));
        assertEquals(1, world.players().get(player).bombs);
    }

    @Test
    @DisplayName("a tank or heavy carrier is not destroyed by the bomb")
    void doesNotDestroyANonFragileEnemy() {
        spawnPlayer(2);
        int tank = enemy(false);

        system.update(world, STEP, BOMB_INPUT);

        assertFalse(world.pendingDestruction().contains(tank));
    }

    @Test
    @DisplayName("with no bomb requested, nothing happens")
    void doesNothingWithoutTheBombInput() {
        int player = spawnPlayer(2);
        int fragileEnemy = enemy(true);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.pendingDestruction().contains(fragileEnemy));
        assertEquals(2, world.players().get(player).bombs);
    }

    @Test
    @DisplayName("with no bomb charge left, requesting the bomb does nothing")
    void doesNothingWithNoBombsLeft() {
        int player = spawnPlayer(0);
        int fragileEnemy = enemy(true);

        system.update(world, STEP, BOMB_INPUT);

        assertFalse(world.pendingDestruction().contains(fragileEnemy));
        assertEquals(0, world.players().get(player).bombs);
    }

    @Test
    @DisplayName("does nothing when there is no player entity")
    void noPlayerIsHarmless() {
        int fragileEnemy = enemy(true);

        system.update(world, STEP, BOMB_INPUT);

        assertFalse(world.pendingDestruction().contains(fragileEnemy));
    }

    private int spawnPlayer(int bombs) {
        int player = world.createEntity();
        world.transforms().set(player, new Transform(100f, 50f));
        world.players().set(player, new Player(3, bombs, 1));
        return player;
    }

    private int enemy(boolean fragile) {
        int entity = world.createEntity();
        world.colliders().set(entity, new Collider(5f, CollisionLayer.ENEMY, fragile));
        return entity;
    }

    private int enemyProjectile() {
        int entity = world.createEntity();
        world.colliders().set(entity, new Collider(2f, CollisionLayer.ENEMY_PROJECTILE));
        return entity;
    }
}
