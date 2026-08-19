package dev.luchoc.littlespaceship.core.application;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.domain.system.SystemPipeline;
import dev.luchoc.littlespaceship.core.port.ContentSource;
import dev.luchoc.littlespaceship.core.port.GameEventSink;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.WorldView;

/**
 * One run of the game, assembled and ready to be ticked.
 *
 * <p>This is what the composition root builds, and the only thing it needs to know about the core:
 * everything it takes and everything it returns is a contract. The world, the entity registry, the
 * component stores and the systems stay inside, which is what stops presentation from reaching into
 * the ECS.
 *
 * <p>It takes a seed and not a generator, so no source of randomness other than the one in the core
 * can be injected. A run is reproducible from that seed plus the sequence of input frames, and that
 * is exactly what a replay stores.
 *
 * <p>Events are drained once the tick is over, never in the middle of it.
 */
public final class Simulation implements TickHandler {

    private final World world;
    private final SystemPipeline pipeline;
    private final GameEventQueue events;
    private final GameEventSink sink;

    private int tickCount;

    /**
     * Assembles a run with the systems of the MVP.
     *
     * @param content where the balance values and definitions come from
     * @param sink where the events of each tick are delivered
     * @param seed the value that makes this run reproducible
     */
    public Simulation(ContentSource content, GameEventSink sink, int seed) {
        this(content, sink, seed, mvpPipeline());
    }

    /**
     * Assembles a run with an explicit pipeline. Visible to the tests of this package, which need
     * to observe the world with systems of their own, without that possibility leaking outside the
     * core.
     */
    Simulation(ContentSource content, GameEventSink sink, int seed, SystemPipeline pipeline) {
        if (content == null || sink == null || pipeline == null) {
            throw new IllegalArgumentException("a simulation needs content, a sink and a pipeline");
        }
        this.events = new GameEventQueue();
        this.world = new World(content, new Rng(seed), events);
        this.pipeline = pipeline;
        this.sink = sink;
    }

    /**
     * Runs every system once, in the fixed order, and then delivers what happened.
     *
     * @param step seconds elapsed, always the same fixed value
     * @param input what the player asked for during this tick
     */
    @Override
    public void tick(float step, InputFrame input) {
        if (input == null) {
            throw new IllegalArgumentException("a tick needs an input frame");
        }
        pipeline.run(world, step, input);
        events.drainTo(sink);
        tickCount++;
    }

    /**
     * Returns the read-only view of the run, which is all presentation ever gets.
     *
     * @return a view that cannot modify anything
     */
    public WorldView view() {
        return world.view();
    }

    /**
     * Returns how many ticks this run has simulated.
     *
     * @return the tick count
     */
    public int tickCount() {
        return tickCount;
    }

    /**
     * The systems of the MVP, in the one place they are listed.
     *
     * <p>Empty for now. Each system joins with the phase that implements it, and where it runs is
     * decided by the stage it declares and not by its position in this list.
     */
    private static SystemPipeline mvpPipeline() {
        return SystemPipeline.of();
    }

    /**
     * The world behind the view. Package-private on purpose: the tests of this package assert
     * against the simulation state, and nothing outside the core can reach it.
     */
    World world() {
        return world;
    }
}
