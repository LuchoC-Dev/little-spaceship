package dev.luchoc.littlespaceship.core.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.architecture.CoreSources.SourceFile;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The dependency rule of the hexagonal architecture, checked instead of trusted.
 *
 * <p>Everything points inwards. The domain knows the ports it declares and nothing else; the ports
 * know no machinery; the application composes both. A single import in the wrong direction is
 * enough to lose the boundary, and it never announces itself.
 */
class LayerDependencyTest {

    private static final String ROOT = "dev.luchoc.littlespaceship";
    private static final String DOMAIN = ROOT + ".core.domain";
    private static final String APPLICATION = ROOT + ".core.application";
    private static final String PORT = ROOT + ".core.port";

    /** The pieces of the ECS. A port that named any of them would be leaking the machinery. */
    private static final List<String> MACHINERY = List.of(
        DOMAIN + ".component",
        DOMAIN + ".entity",
        DOMAIN + ".system",
        DOMAIN + ".World",
        DOMAIN + ".rng");

    @Test
    @DisplayName("the domain does not know the application layer exists")
    void domainDoesNotDependOnApplication() {
        List<String> violations = new ArrayList<>();
        for (SourceFile file : CoreSources.all()) {
            if (!file.packageName().startsWith(DOMAIN)) {
                continue;
            }
            for (String imported : file.imports()) {
                if (imported.startsWith(APPLICATION)) {
                    violations.add(file.path() + " imports " + imported);
                }
            }
        }

        assertTrue(violations.isEmpty(),
            () -> "the domain must not point outwards:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("the ports do not expose the machinery of the domain")
    void portsDoNotDependOnMachinery() {
        List<String> violations = new ArrayList<>();
        for (SourceFile file : CoreSources.all()) {
            if (!file.packageName().startsWith(PORT)) {
                continue;
            }
            for (String imported : file.imports()) {
                if (imported.startsWith(APPLICATION)) {
                    violations.add(file.path() + " imports " + imported);
                }
                for (String machinery : MACHINERY) {
                    if (imported.startsWith(machinery)) {
                        violations.add(file.path() + " imports " + imported);
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
            () -> "a port may name domain contracts, never the ECS:\n"
                + String.join("\n", violations));
    }

    @Test
    @DisplayName("nothing in core imports another module of the project")
    void coreDependsOnNoOtherModule() {
        List<String> violations = new ArrayList<>();
        for (SourceFile file : CoreSources.all()) {
            for (String imported : file.imports()) {
                if (imported.startsWith(ROOT) && !imported.startsWith(ROOT + ".core.")) {
                    violations.add(file.path() + " imports " + imported);
                }
            }
        }

        assertTrue(violations.isEmpty(),
            () -> "core is the innermost module:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("every source of core lives in one of the three layers")
    void everySourceBelongsToALayer() {
        List<String> strays = new ArrayList<>();
        for (SourceFile file : CoreSources.all()) {
            String packageName = file.packageName();
            boolean known = packageName.startsWith(DOMAIN)
                || packageName.startsWith(APPLICATION)
                || packageName.startsWith(PORT);
            if (!known) {
                strays.add(file.path());
            }
        }

        assertTrue(strays.isEmpty(),
            () -> "domain, application or port, nothing else:\n" + String.join("\n", strays));
    }
}
