package dev.luchoc.littlespaceship.core.domain.content;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.ScoreValue;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.port.ComponentSpec;
import dev.luchoc.littlespaceship.core.port.SpriteId;
import dev.luchoc.littlespaceship.core.port.TrajectoryDefinition;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code name -> ComponentFactory}, so a new component type is one registration and no change to
 * {@code SpawnSystem} or to the loader in {@code game}.
 *
 * <p>Lookup is by exact name — the registry never iterates its map, so its own order of registration
 * carries no observable behaviour and cannot affect a replay.
 */
public final class ComponentFactoryRegistry {

    private final Map<String, ComponentFactory> factories = new HashMap<>();

    /**
     * Registers a factory under a name, replacing whatever was registered before it.
     *
     * @param name the component name this factory handles
     * @param factory what builds and attaches the component
     * @return this registry, so registrations can be chained
     */
    public ComponentFactoryRegistry register(String name, ComponentFactory factory) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("a component factory needs a name to register under");
        }
        if (factory == null) {
            throw new IllegalArgumentException("a component factory cannot be null");
        }
        factories.put(name, factory);
        return this;
    }

    /**
     * Looks up the factory for {@code spec.name()} and attaches its component to {@code entity}.
     *
     * @param world the world to attach into
     * @param entity the entity to attach to
     * @param spec the component to build
     * @throws IllegalArgumentException if no factory is registered for {@code spec.name()}
     */
    public void attach(World world, int entity, ComponentSpec spec) {
        ComponentFactory factory = factories.get(spec.name());
        if (factory == null) {
            throw new IllegalArgumentException(
                "no component factory registered for '" + spec.name() + "'");
        }
        factory.attach(world, entity, spec);
    }

    /**
     * Builds the registry with the factories the MVP's content already needs: motion, collider,
     * sprite, scoreValue. Weapon and health stay unregistered — phase 05 owns the systems that would
     * consume them, and registering a factory nothing reads would be exactly the guessed shape this
     * project avoids building ahead of a real consumer.
     *
     * @return a registry ready to attach the MVP's archetypes
     */
    public static ComponentFactoryRegistry withDefaults() {
        return new ComponentFactoryRegistry()
            .register("motion", ComponentFactoryRegistry::attachMotion)
            .register("collider", ComponentFactoryRegistry::attachCollider)
            .register("sprite", ComponentFactoryRegistry::attachSprite)
            .register("scoreValue", ComponentFactoryRegistry::attachScoreValue);
    }

    /**
     * Resolves the {@code "trajectory"} field against {@link World#content()} and attaches its
     * velocity as-is. Trajectories carry the whole vector on purpose — see
     * {@link TrajectoryDefinition} — so there is no per-archetype speed override to apply here.
     */
    private static void attachMotion(World world, int entity, ComponentSpec spec) {
        String trajectoryId = spec.text("trajectory");
        TrajectoryDefinition trajectory = world.content().trajectory(trajectoryId);
        world.motions().set(entity, new Motion(trajectory.vx(), trajectory.vy()));
    }

    /**
     * Every archetype spawned through this registry is an enemy — the content pipeline does not
     * spawn pickups or structures yet — so the layer is fixed here rather than read from a field
     * that would always carry the same value.
     *
     * <p>{@code "fragile"} is required, not defaulted. Four of the level 1 roster's six archetypes
     * are fragile per {@code 02-mvp-functional-spec.md}, so a false default would have been wrong
     * for the majority and would have silently let a weak enemy survive ramming the player whenever
     * content simply omitted the field. Requiring it forces every archetype's collider to say which
     * side of the crash rule it is on.
     */
    private static void attachCollider(World world, int entity, ComponentSpec spec) {
        float radius = spec.number("radius");
        boolean fragile = spec.flag("fragile");
        world.colliders().set(entity, new Collider(radius, CollisionLayer.ENEMY, fragile));
    }

    private static void attachSprite(World world, int entity, ComponentSpec spec) {
        String id = spec.text("id");
        world.sprites().set(entity, new Sprite(new SpriteId(id)));
    }

    private static void attachScoreValue(World world, int entity, ComponentSpec spec) {
        float points = spec.number("points");
        world.scoreValues().set(entity, new ScoreValue(Math.round(points)));
    }
}
