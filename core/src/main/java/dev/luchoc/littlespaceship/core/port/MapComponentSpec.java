package dev.luchoc.littlespaceship.core.port;

import java.util.Map;

/**
 * A {@link ComponentSpec} backed by a plain map, for whoever builds one — the loader in {@code game}
 * reading a JSON object, or a core test building content by hand.
 *
 * <p>Values are boxed {@link Number}, {@link String} or {@link Boolean}; anything else at a given key
 * is treated as absent by the typed accessors, which is what keeps a wrongly-typed JSON field failing
 * with a named message instead of a {@link ClassCastException}.
 */
public final class MapComponentSpec implements ComponentSpec {

    private final String name;
    private final Map<String, Object> params;

    /**
     * @param name the component name, never null or empty
     * @param params the raw parameters, copied defensively; null is treated as empty
     */
    public MapComponentSpec(String name, Map<String, Object> params) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("a component spec needs a name");
        }
        this.name = name;
        this.params = params == null ? Map.of() : Map.copyOf(params);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public float number(String key) {
        Object value = params.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                "component '" + name + "' is missing required numeric field '" + key + "'");
        }
        return number.floatValue();
    }

    @Override
    public float number(String key, float defaultValue) {
        Object value = params.get(key);
        return value instanceof Number number ? number.floatValue() : defaultValue;
    }

    @Override
    public String text(String key) {
        Object value = params.get(key);
        if (!(value instanceof String text) || text.isEmpty()) {
            throw new IllegalArgumentException(
                "component '" + name + "' is missing required text field '" + key + "'");
        }
        return text;
    }

    @Override
    public String text(String key, String defaultValue) {
        Object value = params.get(key);
        return value instanceof String text ? text : defaultValue;
    }

    @Override
    public boolean flag(String key, boolean defaultValue) {
        Object value = params.get(key);
        return value instanceof Boolean flag ? flag : defaultValue;
    }
}
