package dev.luchoc.littlespaceship.core.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.collision.CollisionHit;
import dev.luchoc.littlespaceship.core.domain.collision.CollisionPair;
import dev.luchoc.littlespaceship.core.domain.component.Attachment;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.CollisionLayer;
import dev.luchoc.littlespaceship.core.domain.component.Invulnerable;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.domain.component.Shield;
import dev.luchoc.littlespaceship.core.domain.component.Transform;
import dev.luchoc.littlespaceship.core.domain.event.GameEventQueue;
import dev.luchoc.littlespaceship.core.domain.rng.Rng;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import dev.luchoc.littlespaceship.core.testsupport.TestBalance;
import dev.luchoc.littlespaceship.core.testsupport.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The defensive chain: invulnerability → shield → attachment → life, and its side effect.
 */
class DamageSystemTest {

    private static final float STEP = 1f / 60f;

    private final TestBalance balance = new TestBalance();
    private final World world = new World(new TestContent(balance), new Rng(1), new GameEventQueue());
    private final DamageSystem system = new DamageSystem();

    @Test
    @DisplayName("invulnerability absorbs the hit completely: nothing else is touched")
    void invulnerabilityAbsorbsTheHitEntirely() {
        int player = spawnPlayer(3, 2, 1);
        world.invulnerabilities().set(player, new Invulnerable(1f));
        world.shields().set(player, new Shield());
        world.attachments().set(player, new Attachment(1));
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertTrue(world.shields().has(player), "the shield must survive an absorbed hit");
        assertTrue(world.attachments().has(player), "the attachment must survive an absorbed hit");
        assertEquals(3, world.players().get(player).lives);
        assertTrue(world.pendingDestruction().isEmpty(), "the enemy must not die on an absorbed hit");
    }

    @Test
    @DisplayName("a shield absorbs the hit before the attachment or a life")
    void shieldAbsorbsBeforeAttachmentOrLife() {
        int player = spawnPlayer(3, 2, 1);
        world.shields().set(player, new Shield());
        world.attachments().set(player, new Attachment(1));
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.shields().has(player), "the shield must be consumed");
        assertTrue(world.attachments().has(player), "the attachment must not be touched");
        assertEquals(3, world.players().get(player).lives, "no life must be lost");
    }

    @Test
    @DisplayName("with no shield, the attachment absorbs the hit before a life")
    void attachmentAbsorbsBeforeLife() {
        int player = spawnPlayer(3, 2, 1);
        world.attachments().set(player, new Attachment(1));
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.attachments().has(player), "a one-durability attachment is destroyed by one hit");
        assertEquals(3, world.players().get(player).lives, "no life must be lost");
    }

    @Test
    @DisplayName("an attachment with durability left survives one hit")
    void attachmentSurvivesWhileDurabilityRemains() {
        int player = spawnPlayer(3, 2, 1);
        world.attachments().set(player, new Attachment(2));
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        Attachment attachment = world.attachments().get(player);
        assertEquals(1, attachment.durability);
    }

    @Test
    @DisplayName("with neither shield nor attachment, the hit costs a life")
    void withNoLayerLeftALifeIsLost() {
        int player = spawnPlayer(3, 2, 1);
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertEquals(2, world.players().get(player).lives);
    }

    @Test
    @DisplayName("losing a life does not clear shot level or bombs, which are consumed only by their own rule")
    void losingALifeDoesNotClearPersistentPowerUps() {
        int player = spawnPlayer(3, 2, 3);
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        Player state = world.players().get(player);
        assertEquals(2, state.lives);
        assertEquals(2, state.bombs);
        assertEquals(3, state.shotLevel);
    }

    @Test
    @DisplayName("damage absorbed by the shield grants invulnerability shorter than the respawn one")
    void shieldDamageGrantsTheShorterInvulnerability() {
        int player = spawnPlayer(3, 2, 1);
        world.shields().set(player, new Shield());
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        Invulnerable invulnerable = world.invulnerabilities().get(player);
        assertEquals(balance.damageInvulnerability, invulnerable.remaining, 0.0001f);
        assertTrue(balance.damageInvulnerability < balance.respawnInvulnerability);
    }

    @Test
    @DisplayName("losing a life grants the longer, respawn invulnerability")
    void lifeLossGrantsTheLongerInvulnerability() {
        int player = spawnPlayer(3, 2, 1);
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        Invulnerable invulnerable = world.invulnerabilities().get(player);
        assertEquals(balance.respawnInvulnerability, invulnerable.remaining, 0.0001f);
    }

    @Test
    @DisplayName("respawn keeps the player's position: it reappears where it died, not somewhere else")
    void respawnKeepsThePosition() {
        int player = spawnPlayer(3, 2, 1);
        Transform transform = world.transforms().get(player);
        transform.x = 77f;
        transform.y = 133f;
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertEquals(77f, transform.x);
        assertEquals(133f, transform.y);
    }

    @Test
    @DisplayName("a second hit in the same tick is absorbed by the invulnerability the first one granted")
    void aSecondHitInTheSameTickDoesNotChain() {
        int player = spawnPlayer(3, 2, 1);
        world.attachments().set(player, new Attachment(1));
        int firstEnemy = spawnFragileEnemy();
        int secondEnemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, firstEnemy);
        hitPlayerByEnemy(player, secondEnemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.attachments().has(player), "the first hit consumes the attachment");
        assertEquals(3, world.players().get(player).lives,
            "the second hit must be absorbed by the invulnerability the first one granted");
    }

    @Test
    @DisplayName("a weak enemy is destroyed by crashing into the player")
    void weakEnemyDiesOnCollision() {
        int player = spawnPlayer(3, 2, 1);
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertTrue(world.pendingDestruction().contains(enemy));
    }

    @Test
    @DisplayName("a tank or a heavy carrier survives crashing into the player")
    void heavyEnemySurvivesCollision() {
        int player = spawnPlayer(3, 2, 1);
        int enemy = spawnHeavyEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.pendingDestruction().contains(enemy));
    }

    @Test
    @DisplayName("an enemy projectile is consumed once it hits the player")
    void enemyProjectileIsConsumedOnHit() {
        int player = spawnPlayer(3, 2, 1);
        int projectile = world.createEntity();
        world.collisionHits().add(
            new CollisionHit(projectile, player, CollisionPair.ENEMY_PROJECTILE_VS_PLAYER));

        system.update(world, STEP, InputFrame.IDLE);

        assertTrue(world.pendingDestruction().contains(projectile));
    }

    @Test
    @DisplayName("while invulnerable, neither a weak enemy nor an enemy projectile is consumed")
    void invulnerabilityAlsoProtectsAgainstConsequencesForTheOther() {
        int player = spawnPlayer(3, 2, 1);
        world.invulnerabilities().set(player, new Invulnerable(1f));
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertFalse(world.pendingDestruction().contains(enemy));
    }

    @Test
    @DisplayName("grace time decays and expires, so a later hit is resolved normally again")
    void invulnerabilityDecaysAndExpires() {
        int player = spawnPlayer(3, 2, 1);
        world.invulnerabilities().set(player, new Invulnerable(STEP * 1.5f));

        system.update(world, STEP, InputFrame.IDLE);
        assertTrue(world.invulnerabilities().has(player));
        Invulnerable invulnerable = world.invulnerabilities().get(player);
        assertEquals(STEP * 0.5f, invulnerable.remaining, 0.0001f);

        system.update(world, STEP, InputFrame.IDLE);
        assertFalse(world.invulnerabilities().has(player));
    }

    @Test
    @DisplayName("a life is never taken below zero")
    void livesNeverGoNegative() {
        int player = spawnPlayer(0, 2, 1);
        int enemy = spawnFragileEnemy();
        hitPlayerByEnemy(player, enemy);

        system.update(world, STEP, InputFrame.IDLE);

        assertEquals(0, world.players().get(player).lives);
    }

    @Test
    @DisplayName("does nothing when there is no player entity")
    void noPlayerIsHarmless() {
        int enemy = spawnFragileEnemy();
        world.collisionHits().add(new CollisionHit(enemy, 999, CollisionPair.ENEMY_VS_PLAYER));

        system.update(world, STEP, InputFrame.IDLE);

        assertTrue(world.pendingDestruction().isEmpty());
    }

    private void hitPlayerByEnemy(int player, int enemy) {
        world.collisionHits().add(new CollisionHit(enemy, player, CollisionPair.ENEMY_VS_PLAYER));
    }

    private int spawnPlayer(int lives, int bombs, int shotLevel) {
        int player = world.createEntity();
        world.transforms().set(player, new Transform(0f, 0f));
        world.players().set(player, new Player(lives, bombs, shotLevel));
        return player;
    }

    private int spawnFragileEnemy() {
        int enemy = world.createEntity();
        world.colliders().set(enemy, new Collider(5f, CollisionLayer.ENEMY, true));
        return enemy;
    }

    private int spawnHeavyEnemy() {
        int enemy = world.createEntity();
        world.colliders().set(enemy, new Collider(5f, CollisionLayer.ENEMY, false));
        return enemy;
    }
}
