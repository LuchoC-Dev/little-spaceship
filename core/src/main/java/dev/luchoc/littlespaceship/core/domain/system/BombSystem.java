package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.domain.entity.EntityId;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
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
 * Collider#fragile} enemy is destroyed outright, the same as by ramming — the bomb is whole-body
 * impact, so {@code fragile} answers the same question here it does everywhere else, regardless of
 * any {@code Health} the enemy happens to carry. A non-fragile enemy ("resistant") instead loses
 * {@link BalanceValues#bombDamage()} hit points through the shared {@link HealthDamage}, the same
 * mechanism {@code DamageSystem} uses for a player projectile — this is what turns "heavy damage to
 * resistant enemies" from an unimplemented phrase into an actual number, once {@code Health} exists.
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
        detonate(world, world.content().balance());
    }

    private static void detonate(World world, BalanceValues balance) {
        ComponentStore<Collider> colliders = world.colliders();
        for (int i = 0; i < colliders.size(); i++) {
            int entity = colliders.entityAt(i);
            Collider collider = colliders.valueAt(i);
            if (collider.layer == CollisionLayer.ENEMY_PROJECTILE) {
                world.markForDestruction(entity);
            } else if (collider.layer == CollisionLayer.ENEMY) {
                if (collider.fragile) {
                    world.markForDestruction(entity);
                } else {
                    HealthDamage.apply(world, entity, balance.bombDamage());
                }
            }
        }
    }
}
