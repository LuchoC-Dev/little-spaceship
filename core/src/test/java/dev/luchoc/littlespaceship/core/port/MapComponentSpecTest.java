package dev.luchoc.littlespaceship.core.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapComponentSpecTest {

    @Test
    @DisplayName("reads the fields it was given, typed")
    void readsFields() {
        ComponentSpec spec = new MapComponentSpec("collider",
            Map.of("radius", 5.5f, "layer", "enemy", "fragile", true));

        assertEquals("collider", spec.name());
        assertEquals(5.5f, spec.number("radius"));
        assertEquals("enemy", spec.text("layer"));
        assertTrue(spec.flag("fragile", false));
    }

    @Test
    @DisplayName("a missing required numeric field names the component and the field")
    void missingRequiredNumberFails() {
        ComponentSpec spec = new MapComponentSpec("collider", Map.of());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> spec.number("radius"));

        assertTrue(e.getMessage().contains("collider"));
        assertTrue(e.getMessage().contains("radius"));
    }

    @Test
    @DisplayName("a missing required text field names the component and the field")
    void missingRequiredTextFails() {
        ComponentSpec spec = new MapComponentSpec("sprite", Map.of());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> spec.text("id"));

        assertTrue(e.getMessage().contains("sprite"));
        assertTrue(e.getMessage().contains("id"));
    }

    @Test
    @DisplayName("optional fields fall back to their default when absent")
    void optionalFieldsFallBack() {
        ComponentSpec spec = new MapComponentSpec("motion", Map.of());

        assertEquals(1f, spec.number("speedScale", 1f));
        assertEquals("straight", spec.text("trajectory", "straight"));
        assertFalse(spec.flag("fragile", false));
    }

    @Test
    @DisplayName("a field of the wrong type is treated as absent, not cast")
    void wrongTypeIsAbsent() {
        ComponentSpec spec = new MapComponentSpec("collider", Map.of("radius", "not a number"));

        assertThrows(IllegalArgumentException.class, () -> spec.number("radius"));
        assertEquals(9f, spec.number("radius", 9f));
    }

    @Test
    @DisplayName("rejects a spec with no name")
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new MapComponentSpec("", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new MapComponentSpec(null, Map.of()));
    }

    @Test
    @DisplayName("a null parameter map is treated as empty, not a NullPointerException later")
    void nullParamsIsEmpty() {
        ComponentSpec spec = new MapComponentSpec("sprite", null);

        assertEquals("fallback", spec.text("id", "fallback"));
    }
}
