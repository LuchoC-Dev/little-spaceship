package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.collision.CollisionHit;
import dev.luchoc.littlespaceship.core.domain.collision.CollisionPair;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import java.util.List;

/**
 * Detects overlaps by layer pair, never everything against everything.
 *
 * <p>Four pairs, confirmed in {@code 12-architecture.md}: player projectile × enemy, enemy
 * projectile × player, enemy × player, pickup × player. Comparison inside a pair is naive — every
 * holder of one layer against every holder of the other — which the {@code collisionbench} benchmark
 * measured at 0.028 ms for the MVP scenario. No spatial structure is introduced ahead of that cost
 * actually mattering.
 *
 * <p>The result is not a {@code GameEvent}: it is {@link World#collisionHits()}, cleared and refilled
 * here every tick and read by {@code DamageSystem} right after, within the same tick.
 */
public final class CollisionSystem implements GameSystem {

    @Override
    public SystemOrder order() {
        return SystemOrder.COLLISION;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        List<CollisionHit> hits = world.collisionHits();
        hits.clear();
        detectPair(world, CollisionLayer.PLAYER_PROJECTILE, CollisionLayer.ENEMY,
            CollisionPair.PLAYER_PROJECTILE_VS_ENEMY, hits);
        detectPair(world, CollisionLayer.ENEMY_PROJECTILE, CollisionLayer.PLAYER,
            CollisionPair.ENEMY_PROJECTILE_VS_PLAYER, hits);
        detectPair(world, CollisionLayer.ENEMY, CollisionLayer.PLAYER,
            CollisionPair.ENEMY_VS_PLAYER, hits);
        detectPair(world, CollisionLayer.PICKUP, CollisionLayer.PLAYER,
            CollisionPair.PICKUP_VS_PLAYER, hits);
    }

    private static void detectPair(
        World world, CollisionLayer layerA, CollisionLayer layerB, CollisionPair pair,
        List<CollisionHit> hits) {
        ComponentStore<Collider> colliders = world.colliders();
        ComponentStore<Transform> transforms = world.transforms();
        for (int i = 0; i < colliders.size(); i++) {
            int a = colliders.entityAt(i);
            Collider colliderA = colliders.valueAt(i);
            if (colliderA.layer != layerA) {
                continue;
            }
            Transform transformA = transforms.get(a);
            if (transformA == null) {
                continue;
            }
            for (int j = 0; j < colliders.size(); j++) {
                int b = colliders.entityAt(j);
                Collider colliderB = colliders.valueAt(j);
                if (colliderB.layer != layerB) {
                    continue;
                }
                Transform transformB = transforms.get(b);
                if (transformB == null) {
                    continue;
                }
                if (overlaps(transformA, colliderA, transformB, colliderB)) {
                    hits.add(new CollisionHit(a, b, pair));
                }
            }
        }
    }

    private static boolean overlaps(Transform a, Collider colliderA, Transform b, Collider colliderB) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float radius = colliderA.radius + colliderB.radius;
        return dx * dx + dy * dy <= radius * radius;
    }
}
