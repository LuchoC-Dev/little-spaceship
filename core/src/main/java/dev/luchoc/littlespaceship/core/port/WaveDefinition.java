package dev.luchoc.littlespaceship.core.port;

import java.util.List;

/**
 * A named, reusable unit of level design — an id, its spawns and one end condition — the structure
 * {@code post-mvp-roadmap.md}'s "Waves, first" section describes as flattened away when {@code
 * level-01.json} became 92 timestamped rows. Looked up by id through {@link
 * ContentSource#wave(String)}, the same way an {@link EnemyDefinition} or a {@link
 * FormationDefinition} is, which is what lets the same wave id be referenced twice in one level, or
 * once in two different levels, without copying anything.
 *
 * <p>Carries no placement of its own: where a wave sits in a level — the offset from the wave placed
 * before it — belongs to {@link WavePlacement}, not here. A wave that hardcoded its own offset could
 * only ever be placed in one spot, which would defeat the reuse this type exists for — the same "the
 * opening of level 1" reused in level 2 that {@code post-mvp-roadmap.md} names as the point of naming
 * waves at all.
 *
 * <p>Declares exactly these three things and nothing else, per {@code
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
     *     A level carries no absolute timestamps any more; {@link WavePlacement#offsetSeconds()} is
     *     the only place one is expressed, and even that one is relative.
     */
    List<SpawnEvent> spawns();

    /**
     * @return what ends this wave, never null
     */
    WaveEndCondition endCondition();
}
