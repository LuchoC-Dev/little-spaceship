package dev.luchoc.littlespaceship.game;

/**
 * The options the flow's Options screen exposes, per {@code docs/planning/02-mvp-functional-spec.md}
 * — master, music and effects volume, and whether mouse control is enabled. Held on {@link
 * LittleSpaceshipGame} so it survives navigating away from the Options screen and back, the same way
 * a real settings store would; there is no persistence to disk yet because nothing in the MVP scope
 * asks for save/load outside a run.
 *
 * <p>Mutable and not a record on purpose: a slider's drag listener updates one field at a time, and
 * options changing independently of each other is the whole point of three separate sliders.
 */
public final class GameSettings {

    private float masterVolume = 0.8f;
    private float musicVolume = 0.6f;
    private float effectsVolume = 0.9f;
    private boolean mouseEnabled = true;

    public float masterVolume() {
        return masterVolume;
    }

    public void masterVolume(float value) {
        masterVolume = value;
    }

    public float musicVolume() {
        return musicVolume;
    }

    public void musicVolume(float value) {
        musicVolume = value;
    }

    public float effectsVolume() {
        return effectsVolume;
    }

    public void effectsVolume(float value) {
        effectsVolume = value;
    }

    public boolean mouseEnabled() {
        return mouseEnabled;
    }

    public void mouseEnabled(boolean value) {
        mouseEnabled = value;
    }
}
