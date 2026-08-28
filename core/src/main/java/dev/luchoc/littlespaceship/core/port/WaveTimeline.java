package dev.luchoc.littlespaceship.core.port;

import java.util.List;

/**
 * A level as a flat sequence of timestamped events — the intensity curve in executable form, per
 * {@code 03-game-systems.md} and {@code 12-architecture.md}.
 *
 * <p>{@code SpawnSystem} walks {@link #events()} once per level with a single advancing cursor,
 * which only produces the correct wave at the correct time when the list is sorted by
 * {@link SpawnEvent#at()}. Implementations must enforce that themselves; {@link
 * SimpleWaveTimeline} does.
 *
 * <p><b>Superseded, not yet retired.</b> Since {@link WaveDefinition} exists, a level is no longer
 * meant to be authored as one flat list of absolute timestamps — it is a sequence of named waves,
 * each carrying its own spawns and its own offset from the wave before it. This interface still
 * describes exactly what it does today: {@code SpawnSystem}'s single cursor over a flat, absolute-
 * time list. It stays that way, and {@code ContentSource.timeline(String)} keeps returning it,
 * until issue #112 migrates {@code SpawnSystem} onto {@link WaveDefinition} and either retires this
 * type or repoints it — the boundary this class's own contract may not cross without breaking the
 * one system that reads it, which #112 owns.
 */
public interface WaveTimeline {

    /**
     * @return the events of this level, sorted by {@link SpawnEvent#at()}, never null
     */
    List<SpawnEvent> events();
}
