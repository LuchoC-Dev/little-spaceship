package dev.luchoc.littlespaceship.core.port;

/**
 * One entry of an {@link EnemyDefinition}'s component list: a name and a bag of parameters.
 *
 * <p>This is what makes "an enemy is a list of components, not a class" real. The core does not
 * know what a {@code "collider"} or a {@code "motion"} component is — it looks the name up in a
 * registry of factories and hands the factory this generic bag to read. Adding a new component type
 * to the game is one factory registration, never a change to this contract or to whoever parses the
 * JSON.
 *
 * <p>Every accessor is required and typed, and fails loudly, naming the component and the field, on
 * either a missing key or one present with the wrong type. That failure crosses into {@code game}'s
 * loader, which is the only place that also knows the file name — the two together are what the
 * content pipeline needs to point at the exact broken line instead of a
 * {@code NullPointerException} somewhere later.
 *
 * <p>There is no optional accessor with a default. One existed for each type until review found that
 * a default silently produces the wrong game rule whenever a required field — {@code "fragile"}, for
 * the collider component — is simply omitted from content, and that nothing in this codebase actually
 * called the other two. A component that genuinely wants an optional field is free to add one back
 * once a real archetype needs it; guessing that shape ahead of that need is exactly what this project
 * avoids.
 */
public interface ComponentSpec {

    /**
     * @return the component name this spec describes, such as {@code "motion"} or {@code "collider"}
     */
    String name();

    /**
     * Reads a required numeric field.
     *
     * @param key the field name
     * @return the value
     * @throws IllegalArgumentException if the field is missing or not numeric
     */
    float number(String key);

    /**
     * Reads a required text field.
     *
     * @param key the field name
     * @return the value
     * @throws IllegalArgumentException if the field is missing or empty
     */
    String text(String key);

    /**
     * Reads a required boolean field.
     *
     * @param key the field name
     * @return the value
     * @throws IllegalArgumentException if the field is missing or not a boolean
     */
    boolean flag(String key);
}
