package dev.luchoc.littlespaceship.core.port;

/**
 * Where the simulation gets the content it does not invent: balance values, enemy definitions,
 * trajectories, formations and level timelines.
 *
 * <p>The core parses nothing. It declares what it needs and the adapter hands it over already
 * built, which is why a test can assemble content by hand without reading a single file and why
 * changing the content format touches no game rule.
 *
 * <p>Every lookup here should fail by naming the id it could not resolve, not by returning null or
 * letting a caller reach a {@code NullPointerException} later. The loader in {@code game} is the
 * only place that also knows the file name, so it wraps whatever this throws with that context.
 */
public interface ContentSource {

    /**
     * @return the balance values for this run, never null
     */
    BalanceValues balance();

    /**
     * @param id the archetype's content id
     * @return the enemy definition, never null
     * @throws IllegalArgumentException if no archetype has that id
     */
    EnemyDefinition enemy(String id);

    /**
     * @param id the trajectory's content id
     * @return the trajectory, never null
     * @throws IllegalArgumentException if no trajectory has that id
     */
    TrajectoryDefinition trajectory(String id);

    /**
     * @param id the formation's content id
     * @return the formation, never null
     * @throws IllegalArgumentException if no formation has that id
     */
    FormationDefinition formation(String id);

    /**
     * @param levelId the level's content id
     * @return the level's wave timeline, never null
     * @throws IllegalArgumentException if no level has that id
     */
    WaveTimeline timeline(String levelId);
}
