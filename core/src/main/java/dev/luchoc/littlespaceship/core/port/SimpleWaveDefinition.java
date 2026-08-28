package dev.luchoc.littlespaceship.core.port;

import java.util.List;

/**
 * The straightforward {@link WaveDefinition}.
 *
 * @param id the content id
 * @param spawns the spawns of this wave; rejected if empty or not sorted by {@link SpawnEvent#at()}
 * @param endCondition what ends this wave
 * @param offsetSeconds seconds after the previous wave ends when this one starts; may be negative
 */
public record SimpleWaveDefinition(
    String id, List<SpawnEvent> spawns, WaveEndCondition endCondition, float offsetSeconds)
    implements WaveDefinition {

    /**
     * Rejects a wave that names nothing, has no spawn, spawns out of order, or has no end condition.
     * A hand-edited {@code waves.json} with a typo in one spawn's timestamp would otherwise make
     * whatever walks {@link #spawns()} with a single cursor (issue #112) skip or reorder them
     * silently — the same reasoning {@link SimpleWaveTimeline} already applies to a level.
     */
    public SimpleWaveDefinition {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("a wave needs an id");
        }
        if (spawns == null || spawns.isEmpty()) {
            throw new IllegalArgumentException("wave '" + id + "' has no spawns");
        }
        for (int i = 1; i < spawns.size(); i++) {
            if (spawns.get(i).at() < spawns.get(i - 1).at()) {
                throw new IllegalArgumentException(
                    "wave '" + id + "' spawn " + i + " is out of order: "
                        + spawns.get(i).at() + "s comes after " + spawns.get(i - 1).at() + "s");
            }
        }
        if (endCondition == null) {
            throw new IllegalArgumentException("wave '" + id + "' needs an end condition");
        }
        if (Float.isNaN(offsetSeconds) || Float.isInfinite(offsetSeconds)) {
            throw new IllegalArgumentException(
                "wave '" + id + "' has a non-finite offset " + offsetSeconds);
        }
        spawns = List.copyOf(spawns);
    }
}
