package dev.luchoc.littlespaceship.game.adapter.audio;

import dev.luchoc.littlespaceship.core.port.InvulnerabilitySource;
import dev.luchoc.littlespaceship.core.port.PlayerStatus;
import dev.luchoc.littlespaceship.core.port.SpriteId;
import dev.luchoc.littlespaceship.core.port.SpriteVisitor;
import dev.luchoc.littlespaceship.core.port.WorldView;

/**
 * Turns frame-over-frame differences in {@link WorldView} into sound, exactly the way {@code
 * HudRenderer} already turns them into pixels — {@code core.domain.event.GameEvent} has no
 * implementations yet (see {@code docs/plan/08-audio-and-polish/status.md} for the full account),
 * so a snapshot read once per frame is the only contract available.
 *
 * <p>Every trigger below reads a field {@link PlayerStatus} already exposes and asks only "did this
 * change since last frame", never "what caused it" — the one question a diff cannot answer, which
 * is exactly {@link Sfx#EXPLOSION}'s gap. All six {@code PickupSystem} kinds are still fully
 * distinguishable from an ordinary kill this way, because unlike score — the field phase 06's review
 * found ambiguous — each pickup kind changes a field of its own that damage or a kill never touches:
 * lives only rises from the extra-life pickup, bombs only rises from the bomb-recharge pickup,
 * {@code shieldActive} only flips on from the shield pickup, and so on.
 *
 * <p>Implements {@link SpriteVisitor} itself, the same reasoning {@code WorldRenderer} gives for
 * doing the same: a lambda handed to {@link WorldView#forEachSprite} would allocate every frame.
 * Counting player shots costs a second full traversal of every drawable entity, on top of the one
 * {@code WorldRenderer} already does to draw them — accepted deliberately, since nothing here
 * allocates, and a second {@code O(entities)} pass is nowhere near {@code CLAUDE.md}'s actual
 * concern, which is allocation, not traversal count.
 */
public final class AudioDirector implements SpriteVisitor {

    /** Matches {@code core.domain.system.WeaponSystem}'s own constants — not exposed through {@code
     * core.port}, the same situation {@code WorldRenderer.PLAYER_SPRITE_ID} already documents a
     * justification for. */
    private static final SpriteId SHOT_P1 = new SpriteId("shot-p1");
    private static final SpriteId SHOT_P2 = new SpriteId("shot-p2");

    private final AudioSystem audio;

    private boolean initialized;
    private PlayerStatus previous = PlayerStatus.NONE;
    private int previousShotSprites;
    private int shotSpritesThisFrame;

    public AudioDirector(AudioSystem audio) {
        this.audio = audio;
    }

    /**
     * Reads one frame's {@link WorldView} and {@link PlayerStatus} and plays whatever changed since
     * the last call. The very first call only records a baseline: comparing against {@link
     * PlayerStatus#NONE} would otherwise read the run's starting lives and bombs as pickups
     * collected in frame one.
     */
    public void update(WorldView view, PlayerStatus current) {
        shotSpritesThisFrame = 0;
        view.forEachSprite(this);
        if (initialized && shotSpritesThisFrame > previousShotSprites) {
            audio.playSfx(Sfx.SHOOT);
        }
        previousShotSprites = shotSpritesThisFrame;

        if (initialized) {
            diffPlayerStatus(current);
        }
        previous = current;
        initialized = true;
    }

    @Override
    public void accept(SpriteId sprite, float x, float y, int frame, float rotation) {
        if (SHOT_P1.equals(sprite) || SHOT_P2.equals(sprite)) {
            shotSpritesThisFrame++;
        }
    }

    private void diffPlayerStatus(PlayerStatus current) {
        boolean pickedUp = current.lives() > previous.lives()
            || current.bombs() > previous.bombs()
            || current.weaponLevel() > previous.weaponLevel()
            || (current.shieldActive() && !previous.shieldActive())
            || (!current.attachmentId().isEmpty() && previous.attachmentId().isEmpty())
            || (current.invulnerabilitySource() == InvulnerabilitySource.POWERUP
                && previous.invulnerabilitySource() != InvulnerabilitySource.POWERUP);
        if (pickedUp) {
            audio.playSfx(Sfx.POWERUP);
        }

        if (current.bombs() < previous.bombs()) {
            audio.playSfx(Sfx.BOMB);
        }

        boolean justHit = current.invulnerabilitySource() != previous.invulnerabilitySource()
            && (current.invulnerabilitySource() == InvulnerabilitySource.DAMAGE
                || current.invulnerabilitySource() == InvulnerabilitySource.RESPAWN);
        if (justHit) {
            audio.playSfx(Sfx.IMPACT);
        }
    }
}
