package dev.luchoc.littlespaceship.core.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.architecture.CoreSources.SourceFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * No public type of the core hands an implementation class to the outside.
 *
 * <p>This is the rule that decays first. One getter returning the concrete class, added for
 * convenience, and the boundary is gone: whoever receives it can reach the entity registry, the
 * component stores and the world. Checking it by reflection is what keeps the convenience from
 * winning.
 *
 * <p>What is allowed to cross: primitives, strings, the ports themselves, what the application
 * layer composes with, and the domain events, which are immutable and exist precisely to be read
 * from outside.
 */
class PublicContractTest {

    private static final String ROOT = "dev.luchoc.littlespaceship.core";
    private static final List<String> BOUNDARY_PACKAGES = List.of(
        ROOT + ".port", ROOT + ".application");
    private static final List<String> ALLOWED_PACKAGES = List.of(
        ROOT + ".port", ROOT + ".application", ROOT + ".domain.event");

    @Test
    @DisplayName("what crosses the boundary is a contract, never the machinery behind it")
    void boundaryExposesOnlyContracts() {
        List<String> violations = new ArrayList<>();

        for (Class<?> type : boundaryTypes()) {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (Modifier.isPublic(constructor.getModifiers())) {
                    checkParameters(constructor, violations);
                }
            }
            for (Method method : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                    continue;
                }
                checkParameters(method, violations);
                if (!isAllowed(method.getReturnType())) {
                    violations.add(type.getName() + "." + method.getName()
                        + " returns " + method.getReturnType().getName());
                }
            }
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers()) && !isAllowed(field.getType())) {
                    violations.add(type.getName() + "." + field.getName()
                        + " exposes " + field.getType().getName());
                }
            }
        }

        assertTrue(violations.isEmpty(),
            () -> "implementation classes leaking out of core:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("the boundary is actually being inspected")
    void inspectsTheBoundary() {
        assertTrue(boundaryTypes().size() >= 8,
            "ports and application types should be more than this");
    }

    private static void checkParameters(Executable executable, List<String> violations) {
        for (Class<?> parameter : executable.getParameterTypes()) {
            if (!isAllowed(parameter)) {
                violations.add(executable.getDeclaringClass().getName()
                    + "." + executable.getName() + " takes " + parameter.getName());
            }
        }
    }

    private static boolean isAllowed(Class<?> type) {
        Class<?> component = type;
        while (component.isArray()) {
            component = component.getComponentType();
        }
        if (component.isPrimitive()) {
            return true;
        }
        String name = component.getName();
        if (name.startsWith("java.lang.") || name.startsWith("java.util.")) {
            return true;
        }
        for (String allowed : ALLOWED_PACKAGES) {
            if (name.startsWith(allowed + ".")) {
                return true;
            }
        }
        return false;
    }

    private static List<Class<?>> boundaryTypes() {
        List<Class<?>> types = new ArrayList<>();
        for (SourceFile file : CoreSources.all()) {
            String className = file.path().replace(".java", "").replace('/', '.');
            boolean atBoundary = false;
            for (String boundary : BOUNDARY_PACKAGES) {
                atBoundary |= file.packageName().equals(boundary);
            }
            if (!atBoundary) {
                continue;
            }
            Class<?> type;
            try {
                type = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("cannot load " + className, e);
            }
            if (!Modifier.isPublic(type.getModifiers())) {
                continue;
            }
            types.add(type);
            for (Class<?> nested : type.getDeclaredClasses()) {
                if (Modifier.isPublic(nested.getModifiers())) {
                    types.add(nested);
                }
            }
        }
        return types;
    }
}
