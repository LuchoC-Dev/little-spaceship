package dev.luchoc.littlespaceship.core.port;

import java.util.List;

/**
 * A level as a sequence of timestamped events — the intensity curve in executable form, per
 * {@code 03-game-systems.md} and {@code 12-architecture.md}.
 *
 * <p>{@code SpawnSystem} walks {@link #events()} once per level with a single advancing cursor,
 * which only produces the correct wave at the correct time when the list is sorted by
 * {@link SpawnEvent#at()}. Implementations must enforce that themselves; {@link
 * SimpleWaveTimeline} does.
 */
public interface WaveTimeline {

    /**
     * @return the events of this level, sorted by {@link SpawnEvent#at()}, never null
     */
    List<SpawnEvent> events();
}
