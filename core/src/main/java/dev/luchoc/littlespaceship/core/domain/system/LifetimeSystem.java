package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.port.InputFrame;

/**
 * Expires a projectile once it has fully left the playfield, so a run does not accumulate an
 * unbounded number of them over a level lasting several minutes.
 *
 * <p>No {@code Lifetime} timer component was needed for this: every MVP projectile is a straight
 * line, so "has it left the playfield" is a position check against the same bounds {@code
 * SpawnSystem} and {@code MotionSystem} already use, not a countdown. This is the same reasoning
 * that kept trajectories separate from firing patterns in phase 04 — build what an existing need
 * asks for, not the shape a future one might. A timer-based {@code Lifetime} remains unbuilt; it
 * would earn its place the day something needs to expire on a clock instead of on position, such as
 * a visual effect with no {@code Collider} to bound it.
 *
 * <p>Only the two projectile layers are checked. Nothing yet asks an enemy or a pickup to expire by
 * leaving the playfield, and guessing that rule now would be exactly the mistake this system's own
 * javadoc argues against.
 */
public final class LifetimeSystem implements GameSystem {

    /**
     * Extra distance past the playfield edge before a projectile is expired, in logical units.
     * Generous on purpose: an entity is destroyed only once every pixel of it, not just its centre,
     * is off screen, so the margin has to clear the largest projectile radius the MVP defines.
     */
    private static final float MARGIN = 16f;

    @Override
    public SystemOrder order() {
        return SystemOrder.LIFETIME;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        ComponentStore<Collider> colliders = world.colliders();
        ComponentStore<Transform> transforms = world.transforms();
        for (int i = 0; i < colliders.size(); i++) {
            int entity = colliders.entityAt(i);
            Collider collider = colliders.valueAt(i);
            if (collider.layer != CollisionLayer.PLAYER_PROJECTILE
                && collider.layer != CollisionLayer.ENEMY_PROJECTILE) {
                continue;
            }
            Transform transform = transforms.get(entity);
            if (transform == null) {
                continue;
            }
            if (isOffPlayfield(transform)) {
                world.markForDestruction(entity);
            }
        }
    }

    private static boolean isOffPlayfield(Transform transform) {
        return transform.y < -MARGIN
            || transform.y > SpawnSystem.PLAYFIELD_HEIGHT + MARGIN
            || transform.x < -MARGIN
            || transform.x > MotionSystem.PLAYFIELD_WIDTH + MARGIN;
    }
}
