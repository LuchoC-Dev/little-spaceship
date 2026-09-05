package dev.luchoc.littlespaceship.game.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.files.FileHandle;
import dev.luchoc.littlespaceship.game.screen.TestScenarios.Scenario;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #311: {@link TestScenarios#discover} replaces a hardcoded list, so what used to be visibly
 * correct by reading a list literal now needs to be asserted instead — the order it comes out in and
 * the label each entry gets. Only the discovery mechanism itself is exercised here, against a fixture
 * directory, never against the real {@code assets/data} — the game's own content is {@code
 * level-designer}'s, not this test's business.
 */
final class TestScenariosTest {

    @TempDir
    Path tempDir;

    @Test
    void discoversOnlyTestPrefixedLevelFiles() throws IOException {
        writeLevel("test-b", "{ \"waves\": [] }");
        writeLevel("test-a", "{ \"waves\": [] }");
        writeLevel("level-01", "{ \"waves\": [] }"); // not a scenario: no "test-" prefix
        writeTrajectories("{ \"trajectories\": [] }");

        List<Scenario> scenarios = TestScenarios.discover(dataDir());

        assertEquals(List.of("test-a", "test-b"),
            scenarios.stream().map(Scenario::levelId).toList());
    }

    @Test
    void ordersAlphabeticallyByLevelIdRegardlessOfCreationOrder() throws IOException {
        writeLevel("test-zebra", "{ \"waves\": [] }");
        writeLevel("test-apple", "{ \"waves\": [] }");
        writeLevel("test-mango", "{ \"waves\": [] }");
        writeTrajectories("{ \"trajectories\": [] }");

        List<Scenario> scenarios = TestScenarios.discover(dataDir());

        assertEquals(List.of("test-apple", "test-mango", "test-zebra"),
            scenarios.stream().map(Scenario::levelId).toList());
    }

    @Test
    void aLevelWithABossIsLabelledBoss() throws IOException {
        writeLevel("test-boss", "{ \"boss\": { \"id\": \"b\" }, \"waves\": [] }");
        writeTrajectories("{ \"trajectories\": [] }");

        assertEquals("BOSS", labelOf("test-boss"));
    }

    @Test
    void aConstantTrajectoryIsLabelledLine() throws IOException {
        writeLevel("test-cross", "{ \"waves\": [ { \"wave\": \"w\" } ] }");
        writeWaves("""
            { "waves": [ { "id": "w", "spawns": [
                { "at": 0, "spawn": "e", "formation": "f", "atX": 0.5, "trajectory": "cross-left" }
            ], "end": { "type": "fixedDuration", "seconds": 1 } } ] }
            """);
        writeTrajectories("""
            { "trajectories": [ { "id": "cross-left", "vx": -45, "vy": -45 } ] }
            """);

        assertEquals("LINE: CROSS", labelOf("test-cross"));
    }

    @Test
    void aRelativeSegmentPathIsLabelledPath() throws IOException {
        writeLevel("test-path-turn", "{ \"waves\": [ { \"wave\": \"w\" } ] }");
        writeWaves("""
            { "waves": [ { "id": "w", "spawns": [
                { "at": 0, "spawn": "e", "formation": "f", "atX": 0.5, "trajectory": "turn" }
            ], "end": { "type": "fixedDuration", "seconds": 1 } } ] }
            """);
        writeTrajectories("""
            { "trajectories": [ { "id": "turn", "type": "path", "segments": [
                { "vx": 0, "vy": -45, "duration": 3.0 } ] } ] }
            """);

        assertEquals("PATH: TURN", labelOf("test-path-turn"));
    }

    @Test
    void anAbsoluteWaypointPathIsLabelledAbs() throws IOException {
        writeLevel("test-hold-line", "{ \"waves\": [ { \"wave\": \"w\" } ] }");
        writeWaves("""
            { "waves": [ { "id": "w", "spawns": [
                { "at": 0, "spawn": "e", "formation": "f", "atX": 0.5, "trajectory": "hold" }
            ], "end": { "type": "fixedDuration", "seconds": 1 } } ] }
            """);
        writeTrajectories("""
            { "trajectories": [ { "id": "hold", "type": "path", "waypoints": [
                { "x": 104, "y": 270 } ] } ] }
            """);

        assertEquals("ABS: HOLD LINE", labelOf("test-hold-line"));
    }

    @Test
    void anArcTrajectoryIsLabelledArc() throws IOException {
        writeLevel("test-strike", "{ \"waves\": [ { \"wave\": \"w\" } ] }");
        writeWaves("""
            { "waves": [ { "id": "w", "spawns": [
                { "at": 0, "spawn": "e", "formation": "f", "atX": 0.5, "trajectory": "strike-run" }
            ], "end": { "type": "fixedDuration", "seconds": 1 } } ] }
            """);
        writeTrajectories("""
            { "trajectories": [ { "id": "strike-run", "type": "arc", "vx": 0, "vy": -110, "ay": 27 } ] }
            """);

        assertEquals("ARC: STRIKE", labelOf("test-strike"));
    }

    @Test
    void aMirroredTrajectoryTakesTheKindOfWhatItMirrors() throws IOException {
        writeLevel("test-mirror-check", "{ \"waves\": [ { \"wave\": \"w\" } ] }");
        writeWaves("""
            { "waves": [ { "id": "w", "spawns": [
                { "at": 0, "spawn": "e", "formation": "f", "atX": 0.5, "trajectory": "turn-right" }
            ], "end": { "type": "fixedDuration", "seconds": 1 } } ] }
            """);
        writeTrajectories("""
            { "trajectories": [
                { "id": "turn-left", "type": "path", "segments": [
                    { "vx": 0, "vy": -45, "duration": 3.0 } ] },
                { "id": "turn-right", "mirrorOf": "turn-left" }
            ] }
            """);

        assertEquals("PATH: MIRROR CHECK", labelOf("test-mirror-check"));
    }

    @Test
    void aSpawnWithNoTrajectoryOverrideFallsBackToTheIdDerivedName() throws IOException {
        writeLevel("test-wave-04", """
            { "waves": [ { "wave": "w" } ] }
            """);
        writeWaves("""
            { "waves": [ { "id": "w", "spawns": [
                { "at": 0, "spawn": "e", "formation": "f", "atX": 0.5 }
            ], "end": { "type": "fixedDuration", "seconds": 1 } } ] }
            """);
        writeTrajectories("{ \"trajectories\": [] }");

        assertEquals("WAVE 04", labelOf("test-wave-04"));
    }

    @Test
    void aCyclicalMirrorFallsBackRatherThanLoopingForever() throws IOException {
        writeLevel("test-cycle", "{ \"waves\": [ { \"wave\": \"w\" } ] }");
        writeWaves("""
            { "waves": [ { "id": "w", "spawns": [
                { "at": 0, "spawn": "e", "formation": "f", "atX": 0.5, "trajectory": "a" }
            ], "end": { "type": "fixedDuration", "seconds": 1 } } ] }
            """);
        writeTrajectories("""
            { "trajectories": [
                { "id": "a", "mirrorOf": "b" },
                { "id": "b", "mirrorOf": "a" }
            ] }
            """);

        assertEquals("CYCLE", labelOf("test-cycle"));
    }

    private String labelOf(String levelId) {
        List<Scenario> scenarios = TestScenarios.discover(dataDir());
        return scenarios.stream()
            .filter(s -> s.levelId().equals(levelId))
            .findFirst()
            .map(Scenario::label)
            .orElseThrow(() -> new AssertionError("no scenario discovered for " + levelId));
    }

    private FileHandle dataDir() {
        return new FileHandle(tempDir.toFile());
    }

    private void writeLevel(String levelId, String json) throws IOException {
        Files.writeString(new File(tempDir.toFile(), levelId + ".json").toPath(), json);
    }

    private void writeWaves(String json) throws IOException {
        Files.writeString(new File(tempDir.toFile(), "waves.json").toPath(), json);
    }

    private void writeTrajectories(String json) throws IOException {
        Files.writeString(new File(tempDir.toFile(), "trajectories.json").toPath(), json);
    }
}
