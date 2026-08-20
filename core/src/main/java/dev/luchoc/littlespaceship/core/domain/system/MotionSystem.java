package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.entity.EntityId;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.InputFrame;

/**
 * Applies velocities to every entity, and turns the player's raw input vector into one.
 *
 * <p>The player's {@link Motion} is overwritten from {@link InputFrame} before the generic
 * integration runs, so both share one pass over the world. {@link InputFrame#moveX()} and {@link
 * InputFrame#moveY()} already carry the sum of every enabled device — that is the adapter's job, not
 * this system's — so what is left here is exactly the game rule the spec assigns to the simulation:
 * clamping the result to the ship's top speed.
 *
 * <p>The clamp is on the vector's magnitude, not per axis. Clamping each axis independently would
 * let a diagonal input, whose magnitude is larger by a factor of up to √2, end up faster than a
 * single axis at the same nominal intensity. A magnitude clamp forbids that by construction: whatever
 * direction the input points in, the result can never exceed the configured top speed.
 *
 * <p>Slow movement is the same clamp with a smaller cap — a multiplier, not a separate mode, per the
 * confirmed rule in {@code 02-mvp-functional-spec.md}.
 */
public final class MotionSystem implements GameSystem {

    /**
     * Width of the playfield, in logical units. Confirmed in {@code 10-mvp-initial-values.md} and
     * verified in the technical prototype; it is a fixed dimension of the logical resolution, not a
     * balance value that changes with difficulty, so it lives here rather than in {@code
     * BalanceValues}.
     */
    public static final float PLAYFIELD_WIDTH = 208f;

    @Override
    public SystemOrder order() {
        return SystemOrder.MOTION;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        applyPlayerInput(world, input);
        integrate(world, step);
        clampPlayerToPlayfield(world);
    }

    private static void applyPlayerInput(World world, InputFrame input) {
        int player = world.playerEntity();
        if (player == EntityId.NONE) {
            return;
        }
        Motion motion = world.motions().get(player);
        if (motion == null) {
            // The composition root did not attach a Motion to the player: nothing to drive.
            return;
        }
        BalanceValues balance = world.content().balance();
        float cap = balance.playerSpeed() * (input.slow() ? balance.playerSlowFactor() : 1f);
        setClamped(motion, input.moveX(), input.moveY(), cap);
    }

    private static void setClamped(Motion motion, float x, float y, float cap) {
        float lengthSquared = x * x + y * y;
        float capSquared = cap * cap;
        if (lengthSquared > capSquared && lengthSquared > 0f) {
            float scale = cap / (float) Math.sqrt(lengthSquared);
            x *= scale;
            y *= scale;
        }
        motion.vx = x;
        motion.vy = y;
    }

    private static void integrate(World world, float step) {
        ComponentStore<Motion> motions = world.motions();
        ComponentStore<Transform> transforms = world.transforms();
        for (int i = 0; i < motions.size(); i++) {
            int entity = motions.entityAt(i);
            Motion motion = motions.valueAt(i);
            Transform transform = transforms.get(entity);
            if (transform == null) {
                continue;
            }
            transform.x += motion.vx * step;
            transform.y += motion.vy * step;
        }
    }

    /**
     * Keeps the player inside the playfield's width. Enemies leave freely, which is why this only
     * ever touches the player's entity.
     */
    private static void clampPlayerToPlayfield(World world) {
        int player = world.playerEntity();
        if (player == EntityId.NONE) {
            return;
        }
        Transform transform = world.transforms().get(player);
        if (transform == null) {
            return;
        }
        Collider collider = world.colliders().get(player);
        float margin = collider == null ? 0f : collider.radius;
        float min = margin;
        float max = PLAYFIELD_WIDTH - margin;
        if (transform.x < min) {
            transform.x = min;
        } else if (transform.x > max) {
            transform.x = max;
        }
    }
}
