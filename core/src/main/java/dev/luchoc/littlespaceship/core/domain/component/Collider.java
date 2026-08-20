package dev.luchoc.littlespaceship.core.domain.component;

/**
 * Collision volume of an entity: a circle around its position.
 *
 * <p>A circle and not a rectangle because the comparison is a squared distance, with no square root
 * and no branching, and because in a shoot 'em up a generous circular hitbox reads as fairer than
 * an exact one.
 */
public final class Collider {

    /** Radius in logical units. */
    public float radius;

    /** Side this entity collides as. */
    public CollisionLayer layer;

    /**
     * Creates a collider.
     *
     * @param radius radius in logical units
     * @param layer the side this entity collides as
     */
    public Collider(float radius, CollisionLayer layer) {
        this.radius = radius;
        this.layer = layer;
    }
}
