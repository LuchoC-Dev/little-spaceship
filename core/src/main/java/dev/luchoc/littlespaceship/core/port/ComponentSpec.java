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
 * <p>Every accessor is typed and every required one fails loudly, naming the field it could not
 * find. That failure crosses into {@code game}'s loader, which is the only place that also knows the
 * file name — the two together are what the content pipeline needs to point at the exact broken
 * line instead of an {@code NullPointerException} somewhere later.
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
     * Reads an optional numeric field.
     *
     * @param key the field name
     * @param defaultValue returned when the field is absent
     * @return the value, or {@code defaultValue}
     */
    float number(String key, float defaultValue);

    /**
     * Reads a required text field.
     *
     * @param key the field name
     * @return the value
     * @throws IllegalArgumentException if the field is missing or empty
     */
    String text(String key);

    /**
     * Reads an optional text field.
     *
     * @param key the field name
     * @param defaultValue returned when the field is absent
     * @return the value, or {@code defaultValue}
     */
    String text(String key, String defaultValue);

    /**
     * Reads an optional boolean field.
     *
     * @param key the field name
     * @param defaultValue returned when the field is absent
     * @return the value, or {@code defaultValue}
     */
    boolean flag(String key, boolean defaultValue);
}
