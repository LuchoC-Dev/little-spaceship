package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LifetimeSystemTest {

    private static final float STEP = 1f / 60f;

    private final World world = new World(new TestContent(), new Rng(1), new GameEventQueue());
    private final LifetimeSystem system = new LifetimeSystem();

    @Test
    @DisplayName("a player projectile still inside the playfield is not marked for destruction")
    void keepsAProjectileInsideThePlayfield() {
        int projectile = projectile(100f, 150f);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.pendingDestruction().contains(projectile));
    }

    @Test
    @DisplayName("a player projectile well past the top of the playfield is expired")
    void expiresAProjectileAboveThePlayfield() {
        int projectile = projectile(100f, SpawnSystem.PLAYFIELD_HEIGHT + 100f);

        system.update(world, STEP, InputFrame.IDLE);

        assertTrue(world.pendingDestruction().contains(projectile));
    }

    @Test
    @DisplayName("an enemy is never expired by leaving the playfield")
    void neverExpiresAnEnemy() {
        int enemy = world.createEntity();
        world.transforms().set(enemy, new Transform(100f, -500f));
        world.colliders().set(enemy, new Collider(4f, CollisionLayer.ENEMY));

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.pendingDestruction().contains(enemy));
    }

    private int projectile(float x, float y) {
        int entity = world.createEntity();
        world.transforms().set(entity, new Transform(x, y));
        world.colliders().set(entity, new Collider(1.5f, CollisionLayer.PLAYER_PROJECTILE));
        return entity;
    }
}
