package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Drop;
import dev.luchoc.littlespaceship.core.domain.component.Pickup;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.SpriteId;
import java.util.List;

/**
 * Destroys what was marked and frees identifiers, at the end of the tick and nowhere else.
 *
 * <p>This is the only system that calls {@code World.destroyEntity}. Every other system that decides
 * an entity should die calls {@link World#markForDestruction(int)} instead, which is what lets every
 * system earlier in the pipeline iterate its component stores without an entity vanishing under it
 * mid-tick.
 *
 * <p>It is also where a designed {@link Drop} becomes an actual {@link Pickup} entity: {@link
 * Drop}'s own javadoc says the resolution happens "when the holder is destroyed", and this is the
 * one place every path to destruction converges — ramming, a player projectile, a bomb, whatever
 * comes later — so a drop is honoured the same way regardless of what killed its holder, with no
 * per-source duplication. The spawned pickup only becomes collectable from the next tick's {@code
 * CollisionSystem} pass, which is the correct, unremarkable behaviour of an item appearing where an
 * enemy died.
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
            int entity = pending.get(i);
            spawnDropIfAny(world, entity);
            world.destroyEntity(entity);
        }
        pending.clear();
    }

    /**
     * Reads {@code entity}'s {@link Drop} and {@link Transform} before either is wiped out by {@link
     * World#destroyEntity(int)}, and spawns the pickup those describe. Silently does nothing for an
     * entity with no drop, or with a drop but no position to spawn the pickup at — the latter should
     * never happen for a real archetype, since every spawned entity gets a {@code Transform}, but a
     * hand-built test entity might have neither, and that is not this system's failure to report.
     */
    private static void spawnDropIfAny(World world, int entity) {
        Drop drop = world.drops().get(entity);
        if (drop == null) {
            return;
        }
        Transform source = world.transforms().get(entity);
        if (source == null) {
            return;
        }
        BalanceValues balance = world.content().balance();
        int pickup = world.createEntity();
        world.transforms().set(pickup, new Transform(source.x, source.y));
        world.colliders().set(pickup, new Collider(balance.pickupRadius(), CollisionLayer.PICKUP));
        world.sprites().set(pickup, new Sprite(new SpriteId("pickup-" + drop.pickupId)));
        world.pickups().set(pickup, new Pickup(drop.pickupId));
    }
}
