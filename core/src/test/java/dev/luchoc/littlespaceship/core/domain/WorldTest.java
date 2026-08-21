package dev.luchoc.littlespaceship.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.component.Attachment;
import dev.luchoc.littlespaceship.core.domain.component.BombState;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Drop;
import dev.luchoc.littlespaceship.core.domain.component.Health;
import dev.luchoc.littlespaceship.core.domain.component.Invulnerable;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.Pickup;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.domain.component.ScoreValue;
import dev.luchoc.littlespaceship.core.domain.component.Shield;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.component.Weapon;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.SpriteId;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorldTest {

    private final World world = new World(new TestContent(), new Rng(1), new GameEventQueue());

    @Test
    @DisplayName("an entity starts with no components")
    void entitiesStartEmpty() {
        int entity = world.createEntity();

        assertTrue(world.isAlive(entity));
        assertEquals(1, world.entityCount());
        assertFalse(world.transforms().has(entity));
        assertFalse(world.motions().has(entity));
        assertFalse(world.colliders().has(entity));
        assertFalse(world.sprites().has(entity));
    }

    /**
     * Forgetting a store in {@code World.destroyEntity} would leave data hanging from a slot that
     * is about to be handed out again, and the next entity to land on it would inherit it — the
     * exact hazard phase 01 recorded {@code destroyEntity} as the method that has to know every
     * store.
     *
     * <p>A hand-picked list of assertions cannot actually guard that: it is exactly as easy to
     * forget a store here as it is to forget clearing it in {@code destroyEntity} itself, and this
     * test drifted that way once already — four stores checked while {@code World} already held
     * thirteen. Reflection is what closes the gap: {@link #componentStoreFields()} discovers every
     * {@code ComponentStore} field {@code World} declares, with no list to fall out of sync. The
     * test then asserts every discovered store was actually populated by {@link
     * #populateEveryComponent(int)} — so adding a fourteenth store without extending that method
     * fails loudly, here, instead of silently passing a vacuous "still empty" check — and, after
     * destruction, that every discovered store is empty.
     */
    @Test
    @DisplayName("destroying an entity strips every component store the world declares")
    void destroyStripsEveryComponent() throws ReflectiveOperationException {
        int entity = world.createEntity();
        populateEveryComponent(entity);

        List<Field> stores = componentStoreFields();
        assertTrue(stores.size() >= 13, "World should declare at least as many stores as it did "
            + "when this guard was rewritten; found " + stores.size());
        for (Field field : stores) {
            ComponentStore<?> store = (ComponentStore<?>) field.get(world);
            assertTrue(store.size() > 0,
                "'" + field.getName() + "' was not populated by populateEveryComponent — extend it "
                    + "for the new store before this guard can trust the assertions below");
        }

        assertTrue(world.destroyEntity(entity));

        assertFalse(world.isAlive(entity));
        assertEquals(0, world.entityCount());
        for (Field field : stores) {
            ComponentStore<?> store = (ComponentStore<?>) field.get(world);
            assertEquals(0, store.size(),
                "'" + field.getName() + "' was not cleared by World.destroyEntity");
        }
    }

    /**
     * One component of every type {@code World} stores, all on the same entity. Extend this
     * whenever a new {@code ComponentStore} field is added to {@code World} — {@link
     * #destroyStripsEveryComponent()} fails with a clear message if this method falls behind.
     */
    private void populateEveryComponent(int entity) {
        world.transforms().set(entity, new Transform(1f, 2f));
        world.motions().set(entity, new Motion(3f, 4f));
        world.colliders().set(entity, new Collider(5f, CollisionLayer.ENEMY));
        world.sprites().set(entity, new Sprite(new SpriteId("enemy-basic")));
        world.players().set(entity, new Player(3, 2, 1));
        world.invulnerabilities().set(entity, new Invulnerable(1f));
        world.shields().set(entity, new Shield());
        world.attachments().set(entity, new Attachment(1));
        world.scoreValues().set(entity, new ScoreValue(100));
        world.drops().set(entity, new Drop("shield"));
        world.weapons().set(entity, new Weapon());
        world.pickups().set(entity, new Pickup("shield"));
        world.healths().set(entity, new Health(10));
        world.bombStates().set(entity, new BombState());
    }

    /**
     * Every field {@code World} declares whose type is exactly {@code ComponentStore} — generics are
     * erased, so this finds all of them regardless of the component type each one holds.
     */
    private static List<Field> componentStoreFields() {
        List<Field> fields = new ArrayList<>();
        for (Field field : World.class.getDeclaredFields()) {
            if (field.getType() == ComponentStore.class) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }

    @Test
    @DisplayName("a recycled slot does not inherit the components of the entity before it")
    void recycledSlotIsClean() {
        int old = world.createEntity();
        world.transforms().set(old, new Transform(1f, 2f));
        world.destroyEntity(old);

        int reused = world.createEntity();

        assertFalse(world.transforms().has(reused));
        assertNull(world.transforms().get(reused));
    }

    @Test
    @DisplayName("destroying a stale handle changes nothing")
    void destroyingStaleHandleIsHarmless() {
        int entity = world.createEntity();
        world.destroyEntity(entity);

        assertFalse(world.destroyEntity(entity));
    }

    @Test
    @DisplayName("the view walks everything that has a sprite and a position")
    void viewWalksDrawableEntities() {
        int visible = world.createEntity();
        world.transforms().set(visible, new Transform(10f, 20f));
        world.sprites().set(visible, new Sprite(new SpriteId("ship-basic"), 2, 90f));

        int invisible = world.createEntity();
        world.transforms().set(invisible, new Transform(30f, 40f));

        List<String> drawn = new ArrayList<>();
        world.view().forEachSprite(
            (sprite, x, y, frame, rotation) ->
                drawn.add(sprite.value() + " " + x + " " + y + " " + frame + " " + rotation));

        assertEquals(List.of("ship-basic 10.0 20.0 2 90.0"), drawn);
    }

    @Test
    @DisplayName("the view skips a sprite with no position instead of blowing up")
    void viewSkipsSpritesWithoutPosition() {
        int broken = world.createEntity();
        world.sprites().set(broken, new Sprite(new SpriteId("ship-basic")));

        List<String> drawn = new ArrayList<>();
        world.view().forEachSprite((sprite, x, y, frame, rotation) -> drawn.add(sprite.value()));

        assertTrue(drawn.isEmpty());
    }

    @Test
    @DisplayName("the view rejects a null visitor")
    void viewRejectsNullVisitor() {
        assertThrows(IllegalArgumentException.class, () -> world.view().forEachSprite(null));
    }

    @Test
    @DisplayName("a world needs content, randomness and an event queue")
    void rejectsMissingDependencies() {
        assertThrows(IllegalArgumentException.class,
            () -> new World(null, new Rng(1), new GameEventQueue()));
        assertThrows(IllegalArgumentException.class,
            () -> new World(new TestContent(), null, new GameEventQueue()));
        assertThrows(IllegalArgumentException.class,
            () -> new World(new TestContent(), new Rng(1), null));
    }
}
