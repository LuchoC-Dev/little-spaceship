package dev.luchoc.littlespaceship.core.port;

/**
 * What the player asked for during one tick.
 *
 * <p>The core never reads a keyboard, a mouse or a clock. The adapter builds this frame once per
 * tick and hands it in; that is the whole reason a replay can be reduced to a seed plus a sequence
 * of frames. Making it immutable is not decoration: a frame that could be modified after being
 * handed in would be an input the replay never recorded.
 *
 * <p>The movement vector is the sum of every enabled device, keyboard and mouse alike, and it is
 * not normalised. Clamping it to the ship's maximum speed is a game rule, so it belongs to the
 * simulation and not to the adapter.
 *
 * @param moveX horizontal intent, positive to the right
 * @param moveY vertical intent, positive upwards
 * @param fire whether the fire control is held
 * @param slow whether the precision control is held, which slows the ship down
 * @param bomb whether the bomb was requested this tick
 */
public record InputFrame(float moveX, float moveY, boolean fire, boolean slow, boolean bomb) {

    /** A frame with nothing pressed. Reused so idle ticks allocate nothing. */
    public static final InputFrame IDLE = new InputFrame(0f, 0f, false, false, false);

    /**
     * Rejects values that are not finite. A NaN reaching the simulation spreads silently through
     * every position that touches it and shows up much later as an entity that vanished.
     */
    public InputFrame {
        if (Float.isNaN(moveX) || Float.isInfinite(moveX)
            || Float.isNaN(moveY) || Float.isInfinite(moveY)) {
            throw new IllegalArgumentException(
                "movement must be finite, was (" + moveX + ", " + moveY + ")");
        }
    }
}
