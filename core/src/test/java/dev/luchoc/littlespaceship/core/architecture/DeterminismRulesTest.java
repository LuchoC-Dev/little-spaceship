package dev.luchoc.littlespaceship.core.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.architecture.CoreSources.SourceFile;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The invariants that no functional test can defend.
 *
 * <p>A nanosecond clock added for a log, an unseeded random number in a spawn, a thread started to
 * load something: none of those break a unit test. They break every replay, silently and days
 * later. This test is the only thing standing between the project and that class of change.
 */
class DeterminismRulesTest {

    private static final List<Rule> RULES = List.of(
        new Rule("com.badlogic.gdx",
            "core does not depend on libGDX, not even on its math utilities"),
        new Rule("System.currentTimeMillis",
            "the core does not read the clock: it receives a fixed step"),
        new Rule("System.nanoTime",
            "the core does not read the clock: it receives a fixed step"),
        new Rule("Math.random",
            "randomness goes through the seeded Rng, or replays diverge"),
        new Rule("java.util.Random",
            "java.util.Random is not guaranteed to produce the same stream under TeaVM"),
        new Rule("Thread",
            "the simulation is single-threaded; TeaVM offers no real parallelism"),
        new Rule("ExecutorService",
            "ExecutorService does not exist in TeaVM and breaks the web build"),
        new Rule("CompletableFuture",
            "CompletableFuture does not exist in TeaVM and breaks the web build"),
        new Rule("ReentrantLock",
            "ReentrantLock does not exist in TeaVM and breaks the web build"),
        new Rule("synchronized",
            "nothing is shared between threads, because there is only one"));

    @Test
    @DisplayName("no source in core breaks a determinism or platform invariant")
    void noForbiddenApi() {
        List<String> violations = new ArrayList<>();
        for (SourceFile file : CoreSources.all()) {
            for (Rule rule : RULES) {
                if (JavaSource.containsToken(file.content(), rule.token())) {
                    violations.add(file.path() + " uses " + rule.token()
                        + " -- " + rule.reason());
                }
            }
        }

        assertTrue(violations.isEmpty(),
            () -> "forbidden in core:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("the sources are actually being read, so the check cannot pass on an empty list")
    void readsTheSources() {
        assertTrue(CoreSources.all().size() >= 10, "the core should have more sources than this");
    }

    /**
     * The search is a plain text one, on purpose: it has to find exactly what a grep would find, so
     * that the check and the reviewer never disagree. The price is that not even a comment may
     * spell out a forbidden name.
     */
    @Test
    @DisplayName("the search finds a real use and ignores a longer word that contains it")
    void searchesWholeWords() {
        assertTrue(JavaSource.containsToken("double x = Math.random();", "Math.random"));
        assertTrue(JavaSource.containsToken("new Thread(task);", "Thread"));
        assertFalse(JavaSource.containsToken("int threadCount = 1;", "Thread"));
        assertFalse(JavaSource.containsToken("class Threading {}", "Thread"));
    }

    private record Rule(String token, String reason) {
    }
}
