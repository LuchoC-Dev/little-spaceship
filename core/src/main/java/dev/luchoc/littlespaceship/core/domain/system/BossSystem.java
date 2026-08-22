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
import dev.luchoc.littlespaceship.core.port.BossDefinition;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.port.SpriteId;

/**
 * Level 1's climax: a five-part boss with one phase and two alternating, tell-then-fire attack
 * patterns, per the decision recorded in {@code 08-decisions-and-open-items.md} under "Level 1
 * climax and length".
 *
 * <p>Not stateless, the same deliberate exception {@code SpawnSystem} already is: it tracks its own
 * elapsed level time, which part entities it spawned, and the pattern state machine across many
 * ticks. A fresh {@code Simulation} always builds a fresh {@code BossSystem}, so this state is
 * exactly as reproducible as everything else the composition root creates once per run.
 *
 * <p><b>Footprint is an art fact, not content.</b> Five parts, their offsets and their radii are
 * fixed in {@code docs/design/02-sprite-sizes.md} and hardcoded here as constants — the same
 * treatment {@code Simulation} already gives the player's collider radius — rather than read from
 * {@link BossDefinition}, which carries only what genuinely varies with balancing: hit points,
 * timing and projectile speed.
 *
 * <p><b>The pattern state machine.</b> Fixed order, no randomness: spread and sweep alternate every
 * cycle, never chosen. Each cycle is a cooldown, then a three-beat, 0.75 s tell — the timing fixed in
 * {@code docs/design/06-boss-presentation.md} — then an instantaneous fire. During the tell, the
 * charging parts' {@link Sprite#frame} steps 1, 2, 3 and drops back to 0 the instant the shot leaves,
 * exactly the class javadoc there describes; that is the whole channel presentation needs to draw the
 * charge, so no separate "which part, how far" contract exists on {@link
 * dev.luchoc.littlespaceship.core.port.BossStatus}. Spread charges both pods and fires outward from
 * them; sweep charges both arms and fires converging toward the centre — pods for spread, arms for
 * sweep, per that same design document, so the part that lights carries the dodge direction.
 *
 * <p><b>Defeat.</b> The core is the only part whose death ends the fight: once it is destroyed,
 * whatever pods or arms remain are destroyed with it — a boss does not linger as a headless
 * husk — and {@link World#markBossDefeated()} is called, which is what {@code WorldView.outcome()}
 * requires for {@code LevelOutcome.COMPLETED} on a boss level. A pod or an arm can die earlier, on
 * its own, without ending anything; a pattern whose charging parts are both already dead simply
 * fires nothing and still completes its cycle, so the fight never stalls waiting on a part that is
 * gone.
 *
 * <p><b>Damage and score need nothing new.</b> Every part is an ordinary {@code ENEMY}-layer,
 * non-fragile {@link Collider} carrying {@link Health} and {@link ScoreValue}, so {@code
 * DamageSystem}, {@code BombSystem} and {@code ScoreSystem} already resolve a hit against it, a bomb
 * detonation, and the points it awards with no boss-specific code at all.
 */
public final class BossSystem implements GameSystem {

    // Footprint, from docs/design/02-sprite-sizes.md — synchronisation point, not balance.
    private static final float CORE_RADIUS = 18.0f;
    private static final float POD_RADIUS = 12.0f;
    private static final float ARM_RADIUS = 14.0f;

    private static final float POD_OFFSET_X = 34f;
    private static final float POD_OFFSET_Y = 6f;
    private static final float ARM_OFFSET_X = 44f;
    private static final float ARM_OFFSET_Y = -18f;

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
     * How far a spread shot fans out sideways versus how fast it falls, and the same pair for a
     * sweep shot converging inward. Fixed shapes, like {@code WeaponSystem.SHOT_SPACING} — {@link
     * BossDefinition} supplies the speed each ratio scales, not the shape itself. Fixed ratios rather
     * than a runtime {@code Math.sin}/{@code cos} on purpose: a transcendental function is not
     * guaranteed to produce the identical float on the JVM and under TeaVM, which a replay cannot
     * afford.
     */
    private static final float SPREAD_VX_RATIO = 0.45f;
    private static final float SPREAD_VY_RATIO = -0.90f;
    private static final float SWEEP_VX_RATIO = 0.75f;
    private static final float SWEEP_VY_RATIO = -0.65f;

    /**
     * The core's spawn height: tangent to the playfield edge as seen through the lowest part, the
     * arm, exactly {@code SpawnSystem.lowestOffsetY}'s trick applied to the boss's own five fixed
     * offsets instead of a formation's slots.
     */
    private static final float CORE_SPAWN_Y =
        SpawnSystem.PLAYFIELD_HEIGHT + (ARM_RADIUS - ARM_OFFSET_Y);

    private enum Phase { AWAITING, ENTRANCE, FIGHT, DEFEATED }

    private enum FightStage { COOLDOWN, TELLING }

    private enum BossPattern { SPREAD, SWEEP }

    private final String levelId;

    private BossDefinition definition;
    private float levelTime;
    private Phase phase = Phase.AWAITING;

    private int core = EntityId.NONE;
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
        podLeft = createPart(world, coreX - POD_OFFSET_X, coreY + POD_OFFSET_Y, POD_SPRITE, POD_RADIUS,
            def.podHealth(), def.podPoints());
        podRight = createPart(world, coreX + POD_OFFSET_X, coreY + POD_OFFSET_Y, POD_SPRITE, POD_RADIUS,
            def.podHealth(), def.podPoints());
        armLeft = createPart(world, coreX - ARM_OFFSET_X, coreY + ARM_OFFSET_Y, ARM_SPRITE, ARM_RADIUS,
            def.armHealth(), def.armPoints());
        armRight = createPart(world, coreX + ARM_OFFSET_X, coreY + ARM_OFFSET_Y, ARM_SPRITE, ARM_RADIUS,
            def.armHealth(), def.armPoints());
    }

    private static int createPart(
        World world, float x, float y, SpriteId sprite, float radius, int health, int points) {
        int entity = world.createEntity();
        world.transforms().set(entity, new Transform(x, y));
        world.colliders().set(entity, new Collider(radius, CollisionLayer.ENEMY, false));
        world.sprites().set(entity, new Sprite(sprite));
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
            case COOLDOWN -> updateCooldown(def, step);
            case TELLING -> updateTelling(world, def, step);
        }
        reportStatus(world, def);
    }

    private void updateCooldown(BossDefinition def, float step) {
        stageTimer -= step;
        if (stageTimer <= 0f) {
            fightStage = FightStage.TELLING;
            stageTimer = 0f;
            currentPattern = nextPattern;
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
            fireFrom(world, podLeft, -SPREAD_VX_RATIO * def.spreadProjectileSpeed(),
                SPREAD_VY_RATIO * def.spreadProjectileSpeed());
            fireFrom(world, podRight, SPREAD_VX_RATIO * def.spreadProjectileSpeed(),
                SPREAD_VY_RATIO * def.spreadProjectileSpeed());
        } else {
            fireFrom(world, armLeft, SWEEP_VX_RATIO * def.sweepProjectileSpeed(),
                SWEEP_VY_RATIO * def.sweepProjectileSpeed());
            fireFrom(world, armRight, -SWEEP_VX_RATIO * def.sweepProjectileSpeed(),
                SWEEP_VY_RATIO * def.sweepProjectileSpeed());
        }
    }

    private static void fireFrom(World world, int part, float vx, float vy) {
        if (part == EntityId.NONE || !world.isAlive(part)) {
            // The charging part died mid-tell: that side of the attack simply does not fire. The
            // cycle still completes and alternates normally.
            return;
        }
        Transform origin = world.transforms().get(part);
        if (origin == null) {
            return;
        }
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
        int hp = healthOf(world, core) + healthOf(world, podLeft) + healthOf(world, podRight)
            + healthOf(world, armLeft) + healthOf(world, armRight);
        int hpMax = def.coreHealth() + 2 * def.podHealth() + 2 * def.armHealth();
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
