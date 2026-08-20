package dev.luchoc.littlespaceship.core.domain;

import dev.luchoc.littlespaceship.core.domain.collision.CollisionHit;
import dev.luchoc.littlespaceship.core.domain.component.Attachment;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Drop;
import dev.luchoc.littlespaceship.core.domain.component.Invulnerable;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.domain.component.ScoreValue;
import dev.luchoc.littlespaceship.core.domain.component.Shield;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.entity.EntityId;
import dev.luchoc.littlespaceship.core.domain.entity.EntityRegistry;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.SpriteVisitor;
import dev.luchoc.littlespaceship.core.port.WorldView;
import java.util.ArrayList;
import java.util.List;

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
    private final ComponentStore<Player> players = new ComponentStore<>();
    private final ComponentStore<Invulnerable> invulnerabilities = new ComponentStore<>();
    private final ComponentStore<Shield> shields = new ComponentStore<>();
    private final ComponentStore<Attachment> attachments = new ComponentStore<>();
    private final ComponentStore<ScoreValue> scoreValues = new ComponentStore<>();
    private final ComponentStore<Drop> drops = new ComponentStore<>();

    /**
     * Overlaps detected by {@code CollisionSystem} this tick, consumed by {@code DamageSystem} right
     * after in the same tick. Internal to the domain: it never crosses towards presentation and is
     * unrelated to {@link #events}, which drains only once the whole tick is over.
     */
    private final List<CollisionHit> collisionHits = new ArrayList<>();

    /**
     * Entities marked for destruction this tick, resolved by {@code CleanupSystem} and nowhere else.
     * Marking instead of destroying immediately is what keeps every other system safe to iterate a
     * component store without an entity vanishing under it mid-tick.
     */
    private final List<Integer> pendingDestruction = new ArrayList<>();

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
        players.remove(entity);
        invulnerabilities.remove(entity);
        shields.remove(entity);
        attachments.remove(entity);
        scoreValues.remove(entity);
        drops.remove(entity);
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
     * @return the player's persistent stats; holds at most one entity
     */
    public ComponentStore<Player> players() {
        return players;
    }

    /**
     * @return remaining grace time against damage
     */
    public ComponentStore<Invulnerable> invulnerabilities() {
        return invulnerabilities;
    }

    /**
     * @return active shields
     */
    public ComponentStore<Shield> shields() {
        return shields;
    }

    /**
     * @return the equipped attachment, if any
     */
    public ComponentStore<Attachment> attachments() {
        return attachments;
    }

    /**
     * @return points awarded for destroying each entity that carries one
     */
    public ComponentStore<ScoreValue> scoreValues() {
        return scoreValues;
    }

    /**
     * @return the designed drop of each entity that carries one
     */
    public ComponentStore<Drop> drops() {
        return drops;
    }

    /**
     * Finds the player's entity.
     *
     * <p>Exactly one entity holds {@link Player} at a time in the MVP; this is a lookup convenience
     * for the systems that need it, not a claim that a second one could not exist mechanically.
     *
     * @return the player's handle, or {@link EntityId#NONE} if no entity holds {@link Player}
     */
    public int playerEntity() {
        return players.size() > 0 ? players.entityAt(0) : EntityId.NONE;
    }

    /**
     * @return overlaps detected this tick, for {@code DamageSystem} to resolve right after
     */
    public List<CollisionHit> collisionHits() {
        return collisionHits;
    }

    /**
     * Marks an entity to be destroyed once {@code CleanupSystem} runs, at the end of the tick.
     *
     * <p>No system destroys an entity directly outside of that stage: doing so mid-tick would reorder
     * a component store's dense array under whichever system is iterating it.
     *
     * @param entity the handle to mark; a dead or stale handle is ignored
     */
    public void markForDestruction(int entity) {
        if (isAlive(entity)) {
            pendingDestruction.add(entity);
        }
    }

    /**
     * @return entities marked for destruction this tick, resolved and cleared by {@code
     *     CleanupSystem}
     */
    public List<Integer> pendingDestruction() {
        return pendingDestruction;
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
