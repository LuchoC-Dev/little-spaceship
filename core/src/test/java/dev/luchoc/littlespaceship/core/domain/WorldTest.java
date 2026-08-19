package dev.luchoc.littlespaceship.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.SpriteId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorldTest {

    private final World world = new World(new NoContent(), new Rng(1), new GameEventQueue());

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
     * Forgetting a store here would leave data hanging from a slot that is about to be handed out
     * again, and the next entity to land on it would inherit it.
     */
    @Test
    @DisplayName("destroying an entity strips every component it had")
    void destroyStripsEveryComponent() {
        int entity = world.createEntity();
        world.transforms().set(entity, new Transform(1f, 2f));
        world.motions().set(entity, new Motion(3f, 4f));
        world.colliders().set(entity, new Collider(5f, CollisionLayer.ENEMY));
        world.sprites().set(entity, new Sprite(new SpriteId("enemy-basic")));

        assertTrue(world.destroyEntity(entity));

        assertFalse(world.isAlive(entity));
        assertEquals(0, world.entityCount());
        assertEquals(0, world.transforms().size());
        assertEquals(0, world.motions().size());
        assertEquals(0, world.colliders().size());
        assertEquals(0, world.sprites().size());
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
            () -> new World(new NoContent(), null, new GameEventQueue()));
        assertThrows(IllegalArgumentException.class,
            () -> new World(new NoContent(), new Rng(1), null));
    }

    private static final class NoContent implements ContentSource {

        @Override
        public BalanceValues balance() {
            throw new UnsupportedOperationException("no system reads balance values yet");
        }
    }
}
