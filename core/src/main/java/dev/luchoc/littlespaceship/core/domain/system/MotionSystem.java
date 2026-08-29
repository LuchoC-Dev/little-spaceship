package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.Trajectory;
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
 *
 * <p>This is also where an entity's {@link Trajectory} advances: {@link Trajectory#elapsed} is
 * incremented by the fixed step, once per tick, before velocities are integrated — never read from
 * the system clock, so a replay reproduces it exactly. Evaluating that elapsed time into a shape's
 * velocity is not built yet; only the state and its advance are. See {@link Trajectory}'s own javadoc
 * and {@code docs/plan/11c-movement-shapes/shape-catalogue.md}.
 */
public final class MotionSystem implements GameSystem {

    /**
     * Width of the playfield, in logical units. {@code CLAUDE.md} states it flatly as one of the
     * project's invariants; {@code 10-mvp-initial-values.md} calls the same number, together with
     * the 480×270 logical resolution it lives inside, a "proposed starting point" whose "definitive
     * value is set during the technical prototype" — which {@code 11-technical-prototype-results.md}
     * records as done, confirming 208 as the starting point. It is a fixed dimension of the logical
     * resolution, not a balance value that changes with difficulty, so it lives here rather than in
     * {@code BalanceValues}.
     */
    public static final float PLAYFIELD_WIDTH = 208f;

    @Override
    public SystemOrder order() {
        return SystemOrder.MOTION;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        applyPlayerInput(world, input);
        advanceTrajectories(world, step);
        integrate(world, step);
        clampPlayerToPlayfield(world);
    }

    /**
     * Accumulates the fixed step into every entity's {@link Trajectory#elapsed}, before {@link
     * #integrate} runs — a future shape evaluator reads {@code elapsed} to decide this very tick's
     * velocity, so the value it sees must already include this tick's step.
     */
    private static void advanceTrajectories(World world, float step) {
        ComponentStore<Trajectory> trajectories = world.trajectories();
        for (int i = 0; i < trajectories.size(); i++) {
            trajectories.valueAt(i).elapsed += step;
        }
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
     * Keeps the player inside the playfield on both axes, collider edge against the boundary rather
     * than the ship's centre. Enemies leave freely, which is why this only ever touches the player's
     * entity. {@code 01-vision-and-scope.md} describes "free ship movement inside the playable area"
     * with no separate zone for the top edge, so the vertical bound uses the full {@link
     * SpawnSystem#PLAYFIELD_HEIGHT}, symmetric with the x clamp below rather than some narrower band
     * near the bottom where the ship merely starts.
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
        transform.x = clamp(transform.x, margin, PLAYFIELD_WIDTH - margin);
        transform.y = clamp(transform.y, margin, SpawnSystem.PLAYFIELD_HEIGHT - margin);
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
