package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
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
}
