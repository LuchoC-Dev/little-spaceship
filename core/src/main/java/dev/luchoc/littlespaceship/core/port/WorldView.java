package dev.luchoc.littlespaceship.core.port;

/**
 * Read-only window onto the simulation, for whoever has to draw it.
 *
 * <p>The presentation layer never touches the ECS: it does not know entities, components or stores
 * exist. It asks this view, and what comes back is either a primitive or another contract. That is
 * what allows the internals to change without the renderer noticing.
 *
 * <p>The view grows with the simulation. The player and boss status arrive with the phases that
 * create them; adding them now would mean inventing what they report.
 */
public interface WorldView {

    /**
     * Walks every entity that has to be drawn.
     *
     * @param visitor receives each entity; never null
     */
    void forEachSprite(SpriteVisitor visitor);
}
