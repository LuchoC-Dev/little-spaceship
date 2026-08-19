package dev.luchoc.littlespaceship.core.domain;

import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.entity.EntityRegistry;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.SpriteVisitor;
import dev.luchoc.littlespaceship.core.port.WorldView;

/**
 * Everything the simulation knows: which entities exist, what components they hold, and the three
 * services the systems share —randomness, content and the event queue—.
 *
 * <p>Stores are named, one field per component type, instead of a map keyed by class. It is more
 * lines and it is worth them: the domain stays explicit, the compiler checks every access, and
 * nothing has to be cast. Adding a component means adding a field here and freeing it in
 * {@link #destroyEntity(int)}, which is the one place that has to know every store.
 *
 * <p>The world does not run the systems and does not know they exist. Composing and running them is
 * the job of the application layer, which is what keeps the dependency between the two pointing in
 * a single direction.
 *
 * <p>Presentation never receives this object: it receives {@link #view()}, which can only read.
 */
public final class World {

    private final EntityRegistry entities = new EntityRegistry();

    private final ComponentStore<Transform> transforms = new ComponentStore<>();
    private final ComponentStore<Motion> motions = new ComponentStore<>();
    private final ComponentStore<Collider> colliders = new ComponentStore<>();
    private final ComponentStore<Sprite> sprites = new ComponentStore<>();

    private final ContentSource content;
    private final Rng rng;
    private final GameEventQueue events;

    private final WorldView view = new View();

    /**
     * Creates an empty world.
     *
     * @param content where balance values and definitions come from
     * @param rng the seeded generator every random decision goes through
     * @param events the queue the systems drop their events into
     */
    public World(ContentSource content, Rng rng, GameEventQueue events) {
        if (content == null || rng == null || events == null) {
            throw new IllegalArgumentException("a world needs content, rng and an event queue");
        }
        this.content = content;
        this.rng = rng;
        this.events = events;
    }

    /**
     * Creates an entity with no components.
     *
     * @return the new entity handle
     */
    public int createEntity() {
        return entities.create();
    }

    /**
     * Destroys an entity and strips every component it had.
     *
     * <p>Every store is listed here on purpose: forgetting one would leave data attached to a slot
     * that is about to be handed out again, and the next entity to land on it would inherit it.
     *
     * @param entity the handle to destroy
     * @return true when something was actually destroyed
     */
    public boolean destroyEntity(int entity) {
        if (!entities.isAlive(entity)) {
            return false;
        }
        transforms.remove(entity);
        motions.remove(entity);
        colliders.remove(entity);
        sprites.remove(entity);
        return entities.destroy(entity);
    }

    /**
     * Tells whether a handle still refers to a live entity.
     *
     * @param entity the handle to check
     * @return true when the entity is alive
     */
    public boolean isAlive(int entity) {
        return entities.isAlive(entity);
    }

    /**
     * @return how many entities are alive right now
     */
    public int entityCount() {
        return entities.aliveCount();
    }

    /**
     * @return the entity registry, for the systems that create or destroy
     */
    public EntityRegistry entities() {
        return entities;
    }

    /**
     * @return positions
     */
    public ComponentStore<Transform> transforms() {
        return transforms;
    }

    /**
     * @return velocities
     */
    public ComponentStore<Motion> motions() {
        return motions;
    }

    /**
     * @return collision volumes
     */
    public ComponentStore<Collider> colliders() {
        return colliders;
    }

    /**
     * @return drawing state
     */
    public ComponentStore<Sprite> sprites() {
        return sprites;
    }

    /**
     * @return the content the simulation reads its numbers from
     */
    public ContentSource content() {
        return content;
    }

    /**
     * @return the seeded generator; no system may use any other source of randomness
     */
    public Rng rng() {
        return rng;
    }

    /**
     * @return the queue where events wait until the tick is over
     */
    public GameEventQueue events() {
        return events;
    }

    /**
     * Returns the read-only face of this world, the only thing presentation ever sees.
     *
     * @return a view that cannot modify anything
     */
    public WorldView view() {
        return view;
    }

    /**
     * Reads the world without exposing it. It is an inner class and not the world itself so that
     * nobody can cast the view back into something mutable.
     */
    private final class View implements WorldView {

        @Override
        public void forEachSprite(SpriteVisitor visitor) {
            if (visitor == null) {
                throw new IllegalArgumentException("a visitor cannot be null");
            }
            for (int i = 0; i < sprites.size(); i++) {
                int entity = sprites.entityAt(i);
                Transform transform = transforms.get(entity);
                if (transform == null) {
                    // Something drawable with no position is a bug in whoever built the entity,
                    // but the renderer is not the place to blow up over it.
                    continue;
                }
                Sprite sprite = sprites.valueAt(i);
                visitor.accept(sprite.id, transform.x, transform.y, sprite.frame, sprite.rotation);
            }
        }
    }
}
