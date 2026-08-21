package dev.luchoc.littlespaceship.core.domain.system;

import dev.luchoc.littlespaceship.core.domain.World;
import dev.luchoc.littlespaceship.core.domain.collision.CollisionHit;
import dev.luchoc.littlespaceship.core.domain.collision.CollisionPair;
import dev.luchoc.littlespaceship.core.domain.component.Attachment;
import dev.luchoc.littlespaceship.core.domain.component.Collider;
import dev.luchoc.littlespaceship.core.domain.component.ComponentStore;
import dev.luchoc.littlespaceship.core.domain.component.Invulnerable;
import dev.luchoc.littlespaceship.core.domain.component.Player;
import dev.luchoc.littlespaceship.core.domain.entity.EntityId;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.InputFrame;
import java.util.ArrayList;
import java.util.List;

/**
 * The single place the defensive chain lives: invulnerability → shield → attachment → life.
 *
 * <p>Every hit that reaches the player is resolved by walking that order and stopping at the first
 * layer able to absorb it. Invulnerability, if active, absorbs the hit completely and nothing else
 * happens — no layer is consumed, no enemy is destroyed, no projectile disappears, because as far as
 * the chain is concerned the hit never landed. Otherwise the shield is consumed before the
 * attachment, and the attachment before a life, which is the confirmed priority in
 * {@code 03-game-systems.md}. Each layer absorbs at most one hit per call: there is no fall-through
 * from shield to attachment within the same resolution.
 *
 * <p>This is also the only place that grants invulnerability, and it does so after any damage
 * absorbed, not only after a life is lost — a late, confirmed correction to the spec recorded in
 * {@code 08-decisions-and-open-items.md}. The duration is shorter for a hit absorbed by the shield or
 * the attachment than for one that costs a life, both read from {@link BalanceValues}. Granting it
 * immediately is also what stops several hits from chaining within the same tick: once one hit sets
 * {@link Invulnerable#remaining}, every later hit processed in this same call sees the player already
 * protected.
 *
 * <p>Losing a life never touches the shield, the attachment or any other persistent power-up:
 * {@code lives} is the only field this system decrements on that branch. Persistent power-ups are
 * cleared only by their own rule, never as a side effect of a life being lost.
 *
 * <p>Colliding with the player also has a consequence for the other entity involved. A weak enemy
 * — one whose {@link Collider#fragile} is {@code true} — is destroyed in the crash; a tank or a heavy
 * carrier is not. An enemy projectile is always consumed on contact. Neither consequence happens
 * while the hit itself was absorbed by invulnerability, since then the collision had no effect at
 * all.
 *
 * <p>This system also resolves the other direction: a player projectile reaching an enemy. No
 * {@code Health} component exists yet — no enemy hit-point value is decided anywhere in {@code
 * docs/planning/}, and the MVP roster's "tank" and "heavy carrier" being tougher than a basic enemy
 * is aspirational language in {@code 02-mvp-functional-spec.md}, not a number. Until that number
 * exists, every enemy a player projectile reaches is destroyed in one hit, the projectile included.
 * This is a deliberate simplification, not a guess at a shape nothing consumes yet — {@link
 * Collider#fragile} already draws the "destroyed on contact" line for a body collision, and this
 * reuses the same one-hit outcome for a weapon hit, on every archetype, until real hit points exist
 * to differentiate them.
 */
public final class DamageSystem implements GameSystem {

    @Override
    public SystemOrder order() {
        return SystemOrder.DAMAGE;
    }

    @Override
    public void update(World world, float step, InputFrame input) {
        decayInvulnerability(world, step);

        List<CollisionHit> hits = world.collisionHits();
        int player = world.playerEntity();
        BalanceValues balance = player == EntityId.NONE ? null : world.content().balance();
        for (int i = 0; i < hits.size(); i++) {
            CollisionHit hit = hits.get(i);
            if (player != EntityId.NONE && hit.pair() == CollisionPair.ENEMY_VS_PLAYER
                && hit.second() == player) {
                resolvePlayerHit(world, balance, player, hit.first(), true);
            } else if (player != EntityId.NONE
                && hit.pair() == CollisionPair.ENEMY_PROJECTILE_VS_PLAYER && hit.second() == player) {
                resolvePlayerHit(world, balance, player, hit.first(), false);
            } else if (hit.pair() == CollisionPair.PLAYER_PROJECTILE_VS_ENEMY) {
                resolveEnemyHit(world, hit.first(), hit.second());
            }
        }
    }

    /**
     * A player projectile reaching an enemy: both are consumed. See the class javadoc for why this
     * is a one-hit outcome regardless of archetype, until a real {@code Health} value exists.
     */
    private static void resolveEnemyHit(World world, int projectile, int enemy) {
        world.markForDestruction(projectile);
        world.markForDestruction(enemy);
    }

    /**
     * Ticks every grace period down and removes what expired.
     *
     * <p>Expired handles are collected first and removed afterwards: removing from a {@link
     * ComponentStore} while walking its dense array reorders it and would skip an entry, the same
     * hazard {@link ComponentStore}'s own documentation warns about for entity destruction.
     */
    private static void decayInvulnerability(World world, float step) {
        ComponentStore<Invulnerable> invulnerable = world.invulnerabilities();
        List<Integer> expired = null;
        for (int i = 0; i < invulnerable.size(); i++) {
            Invulnerable state = invulnerable.valueAt(i);
            state.remaining -= step;
            if (state.remaining <= 0f) {
                if (expired == null) {
                    expired = new ArrayList<>();
                }
                expired.add(invulnerable.entityAt(i));
            }
        }
        if (expired != null) {
            for (int entity : expired) {
                invulnerable.remove(entity);
            }
        }
    }

    private static void resolvePlayerHit(
        World world, BalanceValues balance, int player, int source, boolean fromEnemyBody) {
        if (world.invulnerabilities().has(player)) {
            return;
        }

        if (world.shields().has(player)) {
            world.shields().remove(player);
            grantInvulnerability(world, player, balance.damageInvulnerability());
        } else if (world.attachments().has(player)) {
            Attachment attachment = world.attachments().get(player);
            attachment.durability--;
            if (attachment.durability <= 0) {
                world.attachments().remove(player);
            }
            grantInvulnerability(world, player, balance.damageInvulnerability());
        } else {
            Player playerState = world.players().get(player);
            if (playerState != null && playerState.lives > 0) {
                playerState.lives--;
            }
            grantInvulnerability(world, player, balance.respawnInvulnerability());
        }

        if (fromEnemyBody) {
            Collider collider = world.colliders().get(source);
            if (collider != null && collider.fragile) {
                world.markForDestruction(source);
            }
        } else {
            world.markForDestruction(source);
        }
    }

    private static void grantInvulnerability(World world, int entity, float duration) {
        Invulnerable state = world.invulnerabilities().get(entity);
        if (state == null) {
            world.invulnerabilities().set(entity, new Invulnerable(duration));
        } else {
            state.remaining = duration;
        }
    }
}
