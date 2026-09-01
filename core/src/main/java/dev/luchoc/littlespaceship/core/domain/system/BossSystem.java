package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Health;
import dev.luchoc.littlespaceship.core.domain.component.Motion;
import dev.luchoc.littlespaceship.core.domain.component.ScoreValue;
import dev.luchoc.littlespaceship.core.domain.component.Sprite;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.entity.EntityId;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.BossDefinition;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.SpriteId;

/**
 * Level 1's climax: a six-part boss with one phase and two alternating, tell-then-fire attack
 * patterns, per the decision recorded in {@code 08-decisions-and-open-items.md} under "Level 1
 * climax and length".
 *
 * <p>Not stateless, the same deliberate exception {@code SpawnSystem} already is: it tracks its own
 * elapsed level time, which part entities it spawned, and the pattern state machine across many
 * ticks. A fresh {@code Simulation} always builds a fresh {@code BossSystem}, so this state is
 * exactly as reproducible as everything else the composition root creates once per run.
 *
 * <p><b>Footprint is an art fact, not content.</b> Six parts, their offsets and their radii are
 * fixed in {@code docs/design/02-sprite-sizes.md}/{@code 06-boss-presentation.md} and hardcoded here
 * as constants — the same treatment {@code Simulation} already gives the player's collider radius —
 * rather than read from {@link BossDefinition}, which carries only what genuinely varies with
 * balancing: hit points, timing and projectile speed.
 *
 * <p><b>The sixth part, {@code core-keel}.</b> The original five colliders left a 25 px gap below the
 * core's own circle — the keel, exactly where a player shooting up into the boss aims first — through
 * which a projectile visibly passed with nothing registering. Found by {@code visual-designer}
 * drawing the parts against {@code 02-sprite-sizes.md}'s map, fixed here per
 * {@code 06-boss-presentation.md}'s proposal: one more entity, radius 13.0 at offset (0, −27), plus
 * moving the arms from offset y −18 to −22. The keel carries its own {@link Health} and {@link
 * ScoreValue} — the core's own numbers, since it reads as part of the core rather than as a
 * fourth kind of part — and no {@link Sprite}: it draws nothing of its own, it only extends where a
 * hit against the core's existing drawn sprite actually registers. It dies with the core exactly like
 * a pod or an arm; its own death, on its own, ends nothing.
 *
 * <p><b>The pattern state machine.</b> Fixed order, no randomness: spread and sweep alternate every
 * cycle, never chosen. Each cycle is a cooldown, then a three-beat, 0.75 s tell — the timing fixed in
 * {@code docs/design/06-boss-presentation.md} — then an instantaneous fire. During the tell, the
 * charging parts' {@link Sprite#frame} steps 1, 2, 3 and drops back to 0 the instant the shot leaves,
 * exactly the class javadoc there describes; that is the whole channel presentation needs to draw the
 * charge, so no separate "which part, how far" contract exists on {@link
 * dev.luchoc.littlespaceship.core.port.BossStatus}. Spread charges both pods, sweep charges both arms —
 * pods for spread, arms for sweep, per that same design document — and at the instant a tell begins,
 * {@link #lockAim} freezes the player's current position as the volley's aim point. Each charging part
 * then fires a fan of {@link #FAN_COUNT} projectiles aimed at that frozen point, not at a fixed outward
 * or inward angle: {@link #fireAimedFan} and {@link #FAN_SPREAD_RATIOS} carry the geometry and the
 * reasoning. This is the redesign task 4 of {@code docs/plan/11e-level-one-redesigned/plan.md} asked
 * for, replacing the fixed-angle fan that always missed a player parked at screen centre.
 *
 * <p><b>Defeat.</b> The core is the only part whose death ends the fight: once it is destroyed,
 * whatever keel, pods or arms remain are destroyed with it — a boss does not linger as a headless
 * husk — and {@link World#markBossDefeated()} is called, which is what {@code WorldView.outcome()}
 * requires for {@code LevelOutcome.COMPLETED} on a boss level. The keel, a pod or an arm can die
 * earlier, on its own, without ending anything; a pattern whose charging parts are both already dead
 * simply fires nothing and still completes its cycle, so the fight never stalls waiting on a part
 * that is gone.
 *
 * <p><b>Damage and score need nothing new.</b> Every part is an ordinary {@code ENEMY}-layer,
 * non-fragile {@link Collider} carrying {@link Health} and {@link ScoreValue}, so {@code
 * DamageSystem}, {@code BombSystem} and {@code ScoreSystem} already resolve a hit against it, a bomb
 * detonation, and the points it awards with no boss-specific code at all — {@code core-keel} included,
 * even though it draws nothing: nothing downstream of {@code Collider}/{@code Health}/{@code
 * ScoreValue} needs a {@link Sprite} to exist.
 */
public final class BossSystem implements GameSystem {

    // Footprint, from docs/design/02-sprite-sizes.md and 06-boss-presentation.md — synchronisation
    // point, not balance.
    private static final float CORE_RADIUS = 18.0f;
    private static final float POD_RADIUS = 12.0f;
    private static final float ARM_RADIUS = 14.0f;

    /** Closes the keel gap under the core; see the class javadoc. No {@code Sprite} of its own. */
    private static final float CORE_KEEL_RADIUS = 13.0f;

    private static final float POD_OFFSET_X = 34f;
    private static final float POD_OFFSET_Y = 6f;
    private static final float ARM_OFFSET_X = 44f;

    /** Moved from −18 to −22 alongside adding {@code core-keel}; see the class javadoc. */
    private static final float ARM_OFFSET_Y = -22f;

    private static final float CORE_KEEL_OFFSET_X = 0f;
    private static final float CORE_KEEL_OFFSET_Y = -27f;

    private static final SpriteId CORE_SPRITE = new SpriteId("boss-core");
    private static final SpriteId POD_SPRITE = new SpriteId("boss-pod");
    private static final SpriteId ARM_SPRITE = new SpriteId("boss-arm");

    /**
     * Sprite of a boss projectile. Not in {@code 02-sprite-sizes.md} yet: the boss is the first
     * enemy in the game to fire at all, so no enemy projectile art existed before this phase. Flagged
     * for the art lane in this phase's report.
     */
    private static final SpriteId SHOT_SPRITE = new SpriteId("boss-shot");

    /** Same order of magnitude as the player's own shots ({@code WeaponSystem.SHOT_P1_RADIUS}). */
    private static final float PROJECTILE_RADIUS = 2.0f;

    /**
     * The tell's timing, from {@code docs/design/06-boss-presentation.md}: three beats of a quarter
     * second each, 0.75 s total. An art fact fixed by that document, not a balance value.
     */
    private static final float BEAT_DURATION = 0.25f;

    private static final int BEATS = 3;
    private static final float TELL_DURATION = BEATS * BEAT_DURATION;

    /**
     * How many projectiles each charging part fires in one volley, fanned around the aim direction.
     * A design constant rather than a {@link BossDefinition} field: the shape of a volley is footprint,
     * the same reasoning that keeps the part radii and offsets above out of content.
     *
     * <p><b>Redesigned per {@code docs/STATUS.md}'s 25/08 diagnosis</b> — the previous, fixed-angle fan
     * always pointed outward (spread) or inward (sweep), so a player parked at screen centre was never
     * threatened by either pattern: a positioning problem solved once, not a dodge. This fan is now
     * built around a direction aimed at the player, locked once per volley (see {@link #lockAim}), and
     * widened from three rays to five — the suggestion recorded in {@code
     * docs/plan/11e-level-one-redesigned/plan.md}'s task 4 — rather than raising density again, which
     * {@code docs/STATUS.md} already tried (three rays per part) and found barely moved the difficulty.
     */
    private static final int FAN_COUNT = 5;

    /**
     * How far each ray of a volley strays from the straight line to the locked aim point, as a
     * fraction of that line's own length, applied along the perpendicular to it — narrowest (the
     * centre ray, dead on the aim point) to widest. Five values, matching {@link #FAN_COUNT}, kept
     * symmetric so the fan reads as centred on the player rather than biased to one side.
     *
     * <p>Built from vector arithmetic alone — addition, multiplication, {@link Math#sqrt} to
     * renormalise — never {@code Math.sin}/{@code cos}, for the same reason the previous fixed-ratio
     * fan avoided them: a transcendental function is not guaranteed to produce the identical float on
     * the JVM and under TeaVM, which a replay cannot afford. {@code Math.sqrt} is IEEE-754 exact and
     * already used this way elsewhere in {@code core} ({@code MotionSystem}'s velocity cap), so it
     * carries no such risk.
     */
    private static final float[] FAN_SPREAD_RATIOS = {-0.6f, -0.3f, 0f, 0.3f, 0.6f};

    /**
     * The aim point locked at the instant a pattern's tell begins — see {@link #lockAim} — read by
     * every ray of the volley that tell resolves into. Locking once, at the start of the 0.75 s tell
     * rather than at the fire instant, is what keeps the tell honest under an aimed attack: the player
     * dodges a point fixed before the tell started reacting to them, with the same 0.75 s reaction
     * window the un-aimed fan already gave, rather than a shot that keeps re-aiming at wherever they
     * are the instant it fires.
     */
    private float aimX;
    private float aimY;

    /**
     * The core's spawn height: comfortably above the playfield edge as seen through the lowest part
     * — the keel, now that it sits below the arms — exactly {@code SpawnSystem.lowestOffsetY}'s trick
     * applied to the boss's own six fixed offsets instead of a formation's slots.
     */
    private static final float CORE_SPAWN_Y =
        SpawnSystem.PLAYFIELD_HEIGHT + (CORE_KEEL_RADIUS - CORE_KEEL_OFFSET_Y);

    private enum Phase { AWAITING, ENTRANCE, FIGHT, DEFEATED }

    private enum FightStage { COOLDOWN, TELLING }

    private enum BossPattern { SPREAD, SWEEP }

    private final String levelId;

    private BossDefinition definition;
    private float levelTime;
    private Phase phase = Phase.AWAITING;

    private int core = EntityId.NONE;
    private int coreKeel = EntityId.NONE;
    private int podLeft = EntityId.NONE;
    private int podRight = EntityId.NONE;
    private int armLeft = EntityId.NONE;
    private int armRight = EntityId.NONE;

    private float coreX;
    private float coreY;

    private FightStage fightStage = FightStage.COOLDOWN;
    private float stageTimer;
    private BossPattern currentPattern = BossPattern.SPREAD;
    private BossPattern nextPattern = BossPattern.SPREAD;

    /**
     * @param levelId the content id of the level this system fights the boss for
     */
    public BossSystem(String levelId) {
        if (levelId == null || levelId.isEmpty()) {
            throw new IllegalArgumentException("a boss system needs a level id");
        }
        this.levelId = levelId;
    }

    @Override
    public SystemOrder order() {
        return SystemOrder.BOSS;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        levelTime += step;
        if (definition == null) {
            if (!world.content().hasBoss(levelId)) {
                // This level has no boss. Checked every tick instead of once, the same trade
                // SpawnSystem's own exhaustion flag makes: cheaper than a one-shot guard and just as
                // correct, since a level's content never changes mid-run.
                return;
            }
            definition = world.content().boss(levelId);
        }
        world.markBossLevel();

        switch (phase) {
            case AWAITING -> updateAwaiting(world, definition);
            case ENTRANCE, FIGHT -> updateSpawned(world, definition, step);
            case DEFEATED -> { }
        }
    }

    /**
     * Shared by {@code ENTRANCE} and {@code FIGHT}: both are "the boss exists" states, and the core
     * can in principle die in either — a bomb detonating during the entrance is unlikely to line up,
     * but not impossible, and the fight must end correctly either way, not only once the state
     * machine has formally reached {@code FIGHT}.
     */
    private void updateSpawned(World world, BossDefinition def, float step) {
        if (!world.isAlive(core)) {
            handleCoreDeath(world);
            return;
        }
        if (phase == Phase.ENTRANCE) {
            updateEntrance(world, def, step);
        } else {
            updateFight(world, def, step);
        }
    }

    private void updateAwaiting(World world, BossDefinition def) {
        if (levelTime < def.entersAt()) {
            return;
        }
        spawnParts(world, def);
        phase = Phase.ENTRANCE;
        reportStatus(world, def);
    }

    private void spawnParts(World world, BossDefinition def) {
        coreX = MotionSystem.PLAYFIELD_WIDTH / 2f;
        coreY = CORE_SPAWN_Y;

        core = createPart(world, coreX, coreY, CORE_SPRITE, CORE_RADIUS, def.coreHealth(), def.corePoints());
        coreKeel = createPart(world, coreX + CORE_KEEL_OFFSET_X, coreY + CORE_KEEL_OFFSET_Y, null,
            CORE_KEEL_RADIUS, def.coreHealth(), def.corePoints());
        podLeft = createPart(world, coreX - POD_OFFSET_X, coreY + POD_OFFSET_Y, POD_SPRITE, POD_RADIUS,
            def.podHealth(), def.podPoints());
        podRight = createPart(world, coreX + POD_OFFSET_X, coreY + POD_OFFSET_Y, POD_SPRITE, POD_RADIUS,
            def.podHealth(), def.podPoints());
        armLeft = createPart(world, coreX - ARM_OFFSET_X, coreY + ARM_OFFSET_Y, ARM_SPRITE, ARM_RADIUS,
            def.armHealth(), def.armPoints());
        armRight = createPart(world, coreX + ARM_OFFSET_X, coreY + ARM_OFFSET_Y, ARM_SPRITE, ARM_RADIUS,
            def.armHealth(), def.armPoints());
    }

    /**
     * @param sprite what to draw, or {@code null} for a part with no drawn sprite of its own —
     *     {@code core-keel}, which only exists to extend where a hit against the core's own drawn
     *     sprite registers
     */
    private static int createPart(
        World world, float x, float y, SpriteId sprite, float radius, int health, int points) {
        int entity = world.createEntity();
        world.transforms().set(entity, new Transform(x, y));
        world.colliders().set(entity, new Collider(radius, CollisionLayer.ENEMY, false));
        if (sprite != null) {
            world.sprites().set(entity, new Sprite(sprite));
        }
        world.healths().set(entity, new Health(health));
        world.scoreValues().set(entity, new ScoreValue(points));
        return entity;
    }

    private void updateEntrance(World world, BossDefinition def, float step) {
        coreY -= def.entranceSpeed() * step;
        boolean reached = coreY <= def.combatY();
        if (reached) {
            coreY = def.combatY();
        }
        positionParts(world);
        if (reached) {
            phase = Phase.FIGHT;
            fightStage = FightStage.COOLDOWN;
            stageTimer = def.patternCooldown();
            nextPattern = BossPattern.SPREAD;
        }
        reportStatus(world, def);
    }

    private void positionParts(World world) {
        setPosition(world, core, coreX, coreY);
        setPosition(world, coreKeel, coreX + CORE_KEEL_OFFSET_X, coreY + CORE_KEEL_OFFSET_Y);
        setPosition(world, podLeft, coreX - POD_OFFSET_X, coreY + POD_OFFSET_Y);
        setPosition(world, podRight, coreX + POD_OFFSET_X, coreY + POD_OFFSET_Y);
        setPosition(world, armLeft, coreX - ARM_OFFSET_X, coreY + ARM_OFFSET_Y);
        setPosition(world, armRight, coreX + ARM_OFFSET_X, coreY + ARM_OFFSET_Y);
    }

    private static void setPosition(World world, int entity, float x, float y) {
        if (entity == EntityId.NONE || !world.isAlive(entity)) {
            return;
        }
        Transform transform = world.transforms().get(entity);
        if (transform != null) {
            transform.x = x;
            transform.y = y;
        }
    }

    private void updateFight(World world, BossDefinition def, float step) {
        switch (fightStage) {
            case COOLDOWN -> updateCooldown(world, def, step);
            case TELLING -> updateTelling(world, def, step);
        }
        reportStatus(world, def);
    }

    private void updateCooldown(World world, BossDefinition def, float step) {
        stageTimer -= step;
        if (stageTimer <= 0f) {
            fightStage = FightStage.TELLING;
            stageTimer = 0f;
            currentPattern = nextPattern;
            lockAim(world);
        }
    }

    /**
     * Reads the player's current position, through the same fixed step every other system reads the
     * world under, and holds it in {@link #aimX}/{@link #aimY} for the whole tell and the volley it
     * resolves into. Called exactly once per pattern cycle, at the instant the tell begins — see the
     * class javadoc on {@link #FAN_SPREAD_RATIOS} for why locking here rather than at fire time is
     * what keeps the tell honest.
     *
     * <p>No player entity — the boss level's own test fixtures routinely omit one — falls back to the
     * playfield's horizontal centre at {@code playerStartY}, {@link BalanceValues}' own content value
     * rather than a hardcoded one, so the fan still points somewhere plausible instead of at (0, 0).
     */
    private void lockAim(World world) {
        int player = world.playerEntity();
        Transform transform = player == EntityId.NONE ? null : world.transforms().get(player);
        if (transform != null) {
            aimX = transform.x;
            aimY = transform.y;
        } else {
            aimX = MotionSystem.PLAYFIELD_WIDTH / 2f;
            aimY = world.content().balance().playerStartY();
        }
    }

    private void updateTelling(World world, BossDefinition def, float step) {
        stageTimer += step;
        int beat = Math.min(BEATS - 1, (int) (stageTimer / BEAT_DURATION));
        applyTellFrame(world, currentPattern, beat + 1);
        if (stageTimer < TELL_DURATION) {
            return;
        }
        fire(world, def, currentPattern);
        applyTellFrame(world, currentPattern, 0);
        nextPattern = currentPattern == BossPattern.SPREAD ? BossPattern.SWEEP : BossPattern.SPREAD;
        fightStage = FightStage.COOLDOWN;
        stageTimer = def.patternCooldown();
    }

    /**
     * Sets the charging parts' {@link Sprite#frame} to the tell's current beat, 1 through 3, or back
     * to 0 once the shot leaves — the whole contract presentation needs, per the class javadoc.
     */
    private void applyTellFrame(World world, BossPattern pattern, int frame) {
        int left = pattern == BossPattern.SPREAD ? podLeft : armLeft;
        int right = pattern == BossPattern.SPREAD ? podRight : armRight;
        setFrame(world, left, frame);
        setFrame(world, right, frame);
    }

    private static void setFrame(World world, int entity, int frame) {
        if (entity == EntityId.NONE || !world.isAlive(entity)) {
            return;
        }
        Sprite sprite = world.sprites().get(entity);
        if (sprite != null) {
            sprite.frame = frame;
        }
    }

    private void fire(World world, BossDefinition def, BossPattern pattern) {
        if (pattern == BossPattern.SPREAD) {
            fireAimedFan(world, podLeft, def.spreadProjectileSpeed());
            fireAimedFan(world, podRight, def.spreadProjectileSpeed());
        } else {
            fireAimedFan(world, armLeft, def.sweepProjectileSpeed());
            fireAimedFan(world, armRight, def.sweepProjectileSpeed());
        }
    }

    /**
     * Fires {@link #FAN_COUNT} projectiles from {@code part} in the same tick, fanned around the
     * straight line from {@code part}'s own current position to the aim point {@link #lockAim} froze
     * for this volley — not around a fixed outward or inward angle, which is exactly the change this
     * method exists for. Both a spread pod and a sweep arm call this the same way; the two patterns
     * differ only in which parts fire and at what speed, not in how a volley is shaped.
     *
     * <p>Each ray takes the unit aim direction, adds a multiple of its perpendicular — {@link
     * #FAN_SPREAD_RATIOS}, narrowest to widest — and renormalises, so every ray still travels at
     * exactly {@code speed} regardless of how far it strays from dead-on. The centre ratio, {@code 0f},
     * reproduces the un-fanned aim direction exactly.
     */
    private void fireAimedFan(World world, int part, float speed) {
        if (part == EntityId.NONE || !world.isAlive(part)) {
            // The charging part died mid-tell: that side of the attack simply does not fire. The
            // cycle still completes and alternates normally.
            return;
        }
        Transform origin = world.transforms().get(part);
        if (origin == null) {
            return;
        }
        float dx = aimX - origin.x;
        float dy = aimY - origin.y;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 1e-6f) {
            // The locked aim point sits (almost) on top of the firing part — degenerate only, never
            // observed in play at this boss's footprint, but a direction must still be well defined.
            // Straight down, toward where the player always is relative to the boss.
            dx = 0f;
            dy = -1f;
            lengthSquared = 1f;
        }
        float length = (float) Math.sqrt(lengthSquared);
        float ux = dx / length;
        float uy = dy / length;
        float perpX = -uy;
        float perpY = ux;
        for (float ratio : FAN_SPREAD_RATIOS) {
            float rx = ux + ratio * perpX;
            float ry = uy + ratio * perpY;
            float rayLength = (float) Math.sqrt(rx * rx + ry * ry);
            fireFrom(world, origin, speed * rx / rayLength, speed * ry / rayLength);
        }
    }

    private static void fireFrom(World world, Transform origin, float vx, float vy) {
        int projectile = world.createEntity();
        world.transforms().set(projectile, new Transform(origin.x, origin.y));
        world.motions().set(projectile, new Motion(vx, vy));
        world.colliders().set(projectile, new Collider(PROJECTILE_RADIUS, CollisionLayer.ENEMY_PROJECTILE));
        world.sprites().set(projectile, new Sprite(SHOT_SPRITE));
    }

    /**
     * The core is destroyed: whatever pods and arms remain go with it, the fight ends, and {@link
     * World#markBossDefeated()} is what lets {@code WorldView.outcome()} report victory.
     */
    private void handleCoreDeath(World world) {
        markDefeatedPart(world, coreKeel);
        markDefeatedPart(world, podLeft);
        markDefeatedPart(world, podRight);
        markDefeatedPart(world, armLeft);
        markDefeatedPart(world, armRight);
        phase = Phase.DEFEATED;
        world.markBossDefeated();
    }

    private static void markDefeatedPart(World world, int entity) {
        if (entity != EntityId.NONE && world.isAlive(entity)) {
            world.markForDestruction(entity);
        }
    }

    /**
     * Sums current hit points across every surviving part and reports the total, plus the fixed
     * starting total, to {@link World#setBossStatus(int, int)}. A dead part — {@code
     * EntityId.NONE} before it spawns, or destroyed later — simply contributes zero, which is what
     * makes the bar shorten the instant a part dies, not only while it is merely damaged.
     */
    private void reportStatus(World world, BossDefinition def) {
        int hp = healthOf(world, core) + healthOf(world, coreKeel) + healthOf(world, podLeft)
            + healthOf(world, podRight) + healthOf(world, armLeft) + healthOf(world, armRight);
        int hpMax = 2 * def.coreHealth() + 2 * def.podHealth() + 2 * def.armHealth();
        world.setBossStatus(hp, hpMax);
    }

    private static int healthOf(World world, int entity) {
        if (entity == EntityId.NONE || !world.isAlive(entity)) {
            return 0;
        }
        Health health = world.healths().get(entity);
        return health == null ? 0 : health.points;
    }
}
