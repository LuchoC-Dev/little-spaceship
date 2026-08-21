package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Drop;
import dev.luchoc.littlespaceship.core.domain.component.Pickup;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CleanupSystemTest {

    private static final float STEP = 1f / 60f;

    private final World world = new World(new TestContent(), new Rng(1), new GameEventQueue());
    private final CleanupSystem system = new CleanupSystem();

    @Test
    @DisplayName("destroys every entity marked this tick and forgets the list")
    void destroysMarkedEntities() {
        int marked = world.createEntity();
        int untouched = world.createEntity();
        world.markForDestruction(marked);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.isAlive(marked));
        assertTrue(world.isAlive(untouched));
        assertTrue(world.pendingDestruction().isEmpty());
    }

    @Test
    @DisplayName("marking the same entity twice destroys it once, harmlessly")
    void markingTwiceIsHarmless() {
        int entity = world.createEntity();
        world.markForDestruction(entity);
        world.markForDestruction(entity);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.isAlive(entity));
        assertEquals(0, world.entityCount());
    }

    @Test
    @DisplayName("nothing is destroyed when nothing was marked")
    void nothingMarkedIsHarmless() {
        int entity = world.createEntity();

        system.update(world, STEP, InputFrame.IDLE);

        assertTrue(world.isAlive(entity));
    }

    @Test
    @DisplayName("a destroyed entity with a drop spawns a pickup at its last position")
    void spawnsAPickupFromADrop() {
        int enemy = world.createEntity();
        world.transforms().set(enemy, new Transform(77f, 133f));
        world.drops().set(enemy, new Drop("shield"));
        world.markForDestruction(enemy);

        system.update(world, STEP, InputFrame.IDLE);

        int pickup = findPickup();
        Pickup component = world.pickups().get(pickup);
        assertEquals("shield", component.kind);
        Transform transform = world.transforms().get(pickup);
        assertEquals(77f, transform.x);
        assertEquals(133f, transform.y);
        assertEquals(CollisionLayer.PICKUP, world.colliders().get(pickup).layer);
    }

    @Test
    @DisplayName("a destroyed entity with no drop spawns nothing")
    void noDropSpawnsNothing() {
        int enemy = world.createEntity();
        world.transforms().set(enemy, new Transform(0f, 0f));
        world.markForDestruction(enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertEquals(0, world.pickups().size());
    }

    @Test
    @DisplayName("a drop with no transform spawns nothing, instead of crashing the tick")
    void dropWithNoTransformIsHarmless() {
        int enemy = world.createEntity();
        world.drops().set(enemy, new Drop("shield"));
        world.markForDestruction(enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertEquals(0, world.pickups().size());
        assertNull(world.transforms().get(enemy));
    }

    private int findPickup() {
        assertEquals(1, world.pickups().size(), "exactly one pickup should have spawned");
        return world.pickups().entityAt(0);
    }
}
