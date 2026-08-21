package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.domain.entity.EntityId;
import dev.luchoc.littlespaceship.core.port.InputFrame;

/**
 * The special attack: on request, spends a bomb charge and clears the screen of the threats an
 * MVP-scale simulation can actually resolve in one tick.
 *
 * <p>{@link InputFrame#bomb()}'s own javadoc already reads as an edge, not a level — "whether the
 * bomb was requested this tick" — so this system does not debounce it itself; the adapter that
 * builds the frame is the one place that knows whether the control was just pressed.
 *
 * <p>Per {@code 02-mvp-functional-spec.md}, a bomb "removes most threats/projectiles on screen and
 * deals heavy damage to resistant enemies". Every enemy projectile and every {@link
 * Collider#fragile} enemy is destroyed outright — the same one-hit outcome {@code DamageSystem}
 * already applies to a player projectile reaching any enemy, for the identical reason: no {@code
 * Health} value exists yet to size "heavy damage" against a tank or a heavy carrier, so those two
 * survive a bomb untouched rather than receive an invented, unverifiable amount of damage. This is a
 * known, documented gap against the letter of the spec, not an oversight — closing it needs the same
 * missing number this system's javadoc keeps pointing at everywhere else it comes up.
 *
 * <p>Detonating destroys entities directly through {@link World#markForDestruction(int)} rather than
 * through a {@code CollisionHit}: a bomb's range is the whole screen, not a shape two colliders can
 * overlap, so there is nothing for {@code CollisionSystem} to detect. What happens next to a
 * destroyed enemy — a designed drop, points scored — still goes through the exact same shared
 * pipeline every other death does: {@code CleanupSystem} resolves the drop, {@code ScoreSystem}
 * sweeps the {@code ScoreValue}. Nothing here special-cases the bomb as a source of death.
 */
public final class BombSystem implements GameSystem {

    @Override
    public SystemOrder order() {
        return SystemOrder.BOMB;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        if (!input.bomb()) {
            return;
        }
        int player = world.playerEntity();
        if (player == EntityId.NONE) {
            return;
        }
        Player state = world.players().get(player);
        if (state == null || state.bombs <= 0) {
            return;
        }
        state.bombs--;
        detonate(world);
    }

    private static void detonate(World world) {
        ComponentStore<Collider> colliders = world.colliders();
        for (int i = 0; i < colliders.size(); i++) {
            int entity = colliders.entityAt(i);
            Collider collider = colliders.valueAt(i);
            if (collider.layer == CollisionLayer.ENEMY_PROJECTILE
                || (collider.layer == CollisionLayer.ENEMY && collider.fragile)) {
                world.markForDestruction(entity);
            }
        }
    }
}
