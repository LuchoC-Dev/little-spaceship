package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import java.util.List;

/**
 * Destroys what was marked and frees identifiers, at the end of the tick and nowhere else.
 *
 * <p>This is the only system that calls {@code World.destroyEntity}. Every other system that decides
 * an entity should die calls {@link World#markForDestruction(int)} instead, which is what lets every
 * system earlier in the pipeline iterate its component stores without an entity vanishing under it
 * mid-tick.
 */
public final class CleanupSystem implements GameSystem {

    @Override
    public SystemOrder order() {
        return SystemOrder.CLEANUP;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        List<Integer> pending = world.pendingDestruction();
        for (int i = 0; i < pending.size(); i++) {
            world.destroyEntity(pending.get(i));
        }
        pending.clear();
    }
}
