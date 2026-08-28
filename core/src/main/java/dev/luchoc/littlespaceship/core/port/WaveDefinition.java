package dev.luchoc.littlespaceship.core.port;

import java.util.List;

/**
 * A named, reusable unit of level design — an id, its spawns, one end condition and one placement —
 * the structure {@code post-mvp-roadmap.md}'s "Waves, first" section describes as flattened away
 * when {@code level-01.json} became 92 timestamped rows. Looked up by id through {@link
 * ContentSource#wave(String)}, the same way an {@link EnemyDefinition} or a {@link
 * FormationDefinition} is, which is what lets the same wave id be referenced twice in one level, or
 * once in two different levels, without copying anything.
 *
 * <p>Declares exactly these four things and nothing else, per {@code
 * docs/planning/08-decisions-and-open-items.md}, "The 11 group, 27/08/2026": a wave takes no
 * parameters in the 11 group — invariant 6, revisited in phase 12 once a real case for reuse-with-
 * variation exists.
 */
public interface WaveDefinition {

    /**
     * @return the content id
     */
    String id();

    /**
     * @return the spawns of this wave, sorted by {@link SpawnEvent#at()}, never null or empty. Each
     *     event's {@code at} is seconds since this wave itself started — never since the level did.
     *     A level carries no absolute timestamps any more; only {@link #offsetSeconds()} does.
     */
    List<SpawnEvent> spawns();

    /**
     * @return what ends this wave, never null
     */
    WaveEndCondition endCondition();

    /**
     * How this wave is placed inside a level: relative to the end of the wave placed immediately
     * before it, never to an absolute level time. For the first wave of a level, "the wave before it"
     * is level time zero itself.
     *
     * @return seconds after the previous wave ends when this one starts; negative overlaps them
     */
    float offsetSeconds();
}
