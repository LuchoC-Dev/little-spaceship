package dev.luchoc.littlespaceship.core.port;

/**
 * The straightforward {@link TrajectoryDefinition}.
 *
 * @param id the content id
 * @param vx horizontal velocity, in logical units per second
 * @param vy vertical velocity, in logical units per second
 */
public record SimpleTrajectoryDefinition(String id, float vx, float vy) implements TrajectoryDefinition {

    /**
     * Rejects a trajectory that names nothing.
     */
    public SimpleTrajectoryDefinition {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("a trajectory needs an id");
        }
    }
}
