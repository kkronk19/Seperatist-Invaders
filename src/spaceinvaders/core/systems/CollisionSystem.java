package spaceinvaders.core.systems;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import spaceinvaders.core.entities.Blade;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.EnemyBullet;
import spaceinvaders.core.entities.Invader;
import spaceinvaders.core.entities.Missile;
import spaceinvaders.core.entities.Player;
import spaceinvaders.services.audio.AudioManager;

public final class CollisionSystem {

    public interface ShrapnelSpawner {
        List<Bullet> spawnShrapnel(int cx, int cy);
    }

    private CollisionSystem() {}

    private static final String SFX_REFLECT_1 = "/spaceinvaders/resources/audio/sfx/reflect1.wav";
    private static final String SFX_REFLECT_2 = "/spaceinvaders/resources/audio/sfx/reflect2.wav";
    private static final String SFX_REFLECT_3 = "/spaceinvaders/resources/audio/sfx/reflect3.wav";
    private static final String SFX_SHIELD_DEPLETED = "/spaceinvaders/resources/audio/sfx/shield_depleted.wav";

    // tank death sounds
    private static final String SFX_TANK_DEATH_1 = "/spaceinvaders/resources/audio/sfx/tank_death1.wav";
    private static final String SFX_TANK_DEATH_2 = "/spaceinvaders/resources/audio/sfx/tank_death2.wav";
    private static final String SFX_TANK_DEATH_3 = "/spaceinvaders/resources/audio/sfx/tank_death3.wav";

    // swarmer death sounds
    private static final String SFX_SWARMER_DEATH_1 = "/spaceinvaders/resources/audio/sfx/bz_droid_death1.wav";
    private static final String SFX_SWARMER_DEATH_2 = "/spaceinvaders/resources/audio/sfx/bz_droid_death2.wav";
    private static final String SFX_SWARMER_DEATH_3 = "/spaceinvaders/resources/audio/sfx/bz_droid_death3.wav";
    private static final String SFX_SWARMER_DEATH_4 = "/spaceinvaders/resources/audio/sfx/bz_droid_death4.wav";
    private static final String SFX_SWARMER_DEATH_5 = "/spaceinvaders/resources/audio/sfx/bz_droid_death5.wav";
    private static final String SFX_SWARMER_DEATH_6 = "/spaceinvaders/resources/audio/sfx/bz_droid_death6.wav";
    private static final String SFX_SWARMER_DEATH_7 = "/spaceinvaders/resources/audio/sfx/bz_droid_death7.wav";

    // shielded death sounds
    private static final String SFX_SHIELDED_DEATH_1 = "/spaceinvaders/resources/audio/sfx/droideka_death1.wav";
    private static final String SFX_SHIELDED_DEATH_2 = "/spaceinvaders/resources/audio/sfx/droideka_death2.wav";
    private static final String SFX_SHIELDED_DEATH_3 = "/spaceinvaders/resources/audio/sfx/droideka_death3.wav";
    private static final String SFX_SHIELDED_DEATH_4 = "/spaceinvaders/resources/audio/sfx/droideka_death4.wav";

    // shooter death sounds
    private static final String SFX_SHOOTER_DEATH_1 = "/spaceinvaders/resources/audio/sfx/commando_droid_death1.wav";
    private static final String SFX_SHOOTER_DEATH_2 = "/spaceinvaders/resources/audio/sfx/commando_droid_death2.wav";
    private static final String SFX_SHOOTER_DEATH_3 = "/spaceinvaders/resources/audio/sfx/commando_droid_death3.wav";
    private static final String SFX_SHOOTER_DEATH_4 = "/spaceinvaders/resources/audio/sfx/commando_droid_death4.wav";

    // basic death sounds
    private static final String SFX_BASIC_DEATH_1 = "/spaceinvaders/resources/audio/sfx/drd_droid_hit_01.wav";
    private static final String SFX_BASIC_DEATH_2 = "/spaceinvaders/resources/audio/sfx/drd_droid_hit_02.wav";
    private static final String SFX_BASIC_DEATH_3 = "/spaceinvaders/resources/audio/sfx/drd_droid_hit_03.wav";
    private static final String SFX_BASIC_DEATH_4 = "/spaceinvaders/resources/audio/sfx/drd_droid_hit_04.wav";
    private static final String SFX_BASIC_DEATH_5 = "/spaceinvaders/resources/audio/sfx/drd_droid_hit_05.wav";
    private static final String SFX_BASIC_DEATH_6 = "/spaceinvaders/resources/audio/sfx/drd_droid_hit_06.wav";

    private static final double ROCKET_DEFLECT_CHANCE = 0.25;

    /** Bullet vs Invader collisions. Mutates lists safely. */
    public static void bulletsVsInvaders(
            List<Bullet> bullets,
            List<Invader> invaders,
            Player player,
            ShrapnelSpawner shrapnelSpawner
    ) {
        Rectangle br = new Rectangle();
        Rectangle ir = new Rectangle();

        List<Bullet> pendingAdds = new ArrayList<>();

        for (Iterator<Bullet> bit = bullets.iterator(); bit.hasNext();) {
            Bullet b = bit.next();

            // Enemy bullets should NEVER collide with invaders
            if (b instanceof EnemyBullet) continue;

            br.setBounds(b.x, b.y, b.size, b.size);

            boolean removeBullet = false;
            boolean hitSomething = false;

            for (Iterator<Invader> iit = invaders.iterator(); iit.hasNext();) {
                Invader inv = iit.next();
                ir.setBounds(inv.x, inv.y, inv.width, inv.height);

                if (!br.intersects(ir)) continue;

                hitSomething = true;

                boolean hadShield = inv.shieldHits > 0;

                // --------------------------
                // DAMAGE MODIFIERS / SPECIAL RULES
                // --------------------------
                int appliedDamage = b.damage;

                // If armored and bullet is NOT armor piercing -> reduce to 1 dmg.
                // Additionally, armored targets stop non-AP, non-blade projectiles (prevents "pierced shots").
                boolean forceStopNonBlade = false;

                if (inv.armored) {
                    if (b instanceof Blade blade) {
                        // blades already have their own armor rule
                        if (!blade.armorPiercing) {
                            appliedDamage = 1;
                            // blade consumes all pierce after this hit (handled below)
                        }
                    } else {
                        if (!b.armorPiercing) {
                            appliedDamage = 1;
                            forceStopNonBlade = true; // armored "soaks" the projectile (no pierce-through)
                        }
                    }
                }

                // Blade-only "consume all pierce" flag when it hits armor without AP
                boolean forceBladeStop = false;
                if (b instanceof Blade blade) {
                    if (inv.armored && !blade.armorPiercing) {
                        forceBladeStop = true;
                    }
                }

                Invader.HitResult res = inv.takeHit(appliedDamage);

                boolean shieldBroke = hadShield && inv.shieldHits == 0 && res == Invader.HitResult.ABSORBED;
                if (shieldBroke) {
                    inv.shieldBreakFlashMs = 140; // visual flash on break
                }

                // --------------------------
                // KILL: remove invader + award points
                // --------------------------
                if (res == Invader.HitResult.KILLED) {
                    iit.remove();

                    if (player != null) {
                        int award = inv.scoreValue;
                        if (b.moneyShot) award *= 2; // legendary: double points on kill
                        player.addPoints(award);
                    }

                    try { playDeathSfxFor(inv); } catch (Throwable ignored) {}
                }

                // --------------------------
                // SHIELD ABSORBS
                // --------------------------
                if (res == Invader.HitResult.ABSORBED) {

                    // Blade vs shield: reflect + bounce, don't consume pierce
                    if (b instanceof Blade) {
                        try {
                            AudioManager.get().playRandomSfx(0.40f, SFX_REFLECT_1, SFX_REFLECT_2, SFX_REFLECT_3);
                        } catch (Throwable ignored) {}

                        if (shieldBroke) {
                            try { AudioManager.get().playSfx(SFX_SHIELD_DEPLETED, 0.60f); } catch (Throwable ignored) {}
                        }

                        b.vy = -b.vy;
                        if (b.vy > 0) b.y = inv.y + inv.height + 2;
                        else          b.y = inv.y - b.size - 2;

                        removeBullet = false;
                    }

                    // Missile vs shield: 25% deflect
                    else if (b.kind == Bullet.BulletKind.MISSILE) {
                        boolean deflect = Math.random() < ROCKET_DEFLECT_CHANCE;

                        if (deflect) {
                            try {
                                AudioManager.get().playRandomSfx(0.45f, SFX_REFLECT_1, SFX_REFLECT_2, SFX_REFLECT_3);
                            } catch (Throwable ignored) {}

                            if (shieldBroke) {
                                try { AudioManager.get().playSfx(SFX_SHIELD_DEPLETED, 0.60f); } catch (Throwable ignored) {}
                            }

                            if (b instanceof Missile m) {
                                m.straightFlight = true;
                            }

                            b.vy = -1;
                            b.vx = (Math.random() < 0.5) ? -4 : 4;
                            b.y  = inv.y - b.size - 2;

                            removeBullet = false;
                        } else {
                            int cx = b.x + b.size / 2;
                            int cy = b.y + b.size / 2;
                            if (shrapnelSpawner != null) pendingAdds.addAll(shrapnelSpawner.spawnShrapnel(cx, cy));
                            removeBullet = true;
                        }
                    }

                    // Other bullets absorbed by shield
                    else {
                        if (shieldBroke) {
                            try { AudioManager.get().playSfx(SFX_SHIELD_DEPLETED, 0.60f); } catch (Throwable ignored) {}
                        }
                        removeBullet = true;
                    }

                    break;
                }

                // --------------------------
                // NORMAL HIT (not absorbed)
                // --------------------------
                boolean explode = (b.kind == Bullet.BulletKind.MISSILE) || b.explodesOnHit;

                if (explode) {
                    int cx = b.x + b.size / 2;
                    int cy = b.y + b.size / 2;
                    if (shrapnelSpawner != null) pendingAdds.addAll(shrapnelSpawner.spawnShrapnel(cx, cy));
                    removeBullet = true;

                } else if (b instanceof Blade blade) {
                    if (forceBladeStop) {
                        blade.consumeAllPierce();
                        removeBullet = true;
                    } else {
                        removeBullet = blade.onHitInvader();
                    }

                } else {
                    // BASIC (and any other non-blade, non-missile):
                    // If armor forced stop, remove it.
                    if (forceStopNonBlade) {
                        removeBullet = true;
                    } else if (b.pierce > 0) {
                        // pierce means it survives THIS hit and can hit more enemies later
                        b.pierce--;
                        removeBullet = false;

                        // tiny nudge so it doesn't re-hit same invader due to overlap
                        b.y += (b.vy < 0) ? -2 : 2;
                    } else {
                        removeBullet = true;
                    }
                }

                break; // one invader hit per bullet per frame
            }

            if (hitSomething && removeBullet) bit.remove();
        }

        if (!pendingAdds.isEmpty()) bullets.addAll(pendingAdds);
    }

    private static void playDeathSfxFor(Invader inv) {
        switch (inv.kind) {
            case TANK:
                AudioManager.get().playRandomSfx(1.0f, SFX_TANK_DEATH_1, SFX_TANK_DEATH_2, SFX_TANK_DEATH_3);
                break;

            case SWARMER:
                AudioManager.get().playRandomSfx(
                        0.85f,
                        SFX_SWARMER_DEATH_1, SFX_SWARMER_DEATH_2, SFX_SWARMER_DEATH_3,
                        SFX_SWARMER_DEATH_4, SFX_SWARMER_DEATH_5, SFX_SWARMER_DEATH_6, SFX_SWARMER_DEATH_7
                );
                break;

            case SHIELDED:
                AudioManager.get().playRandomSfx(
                        0.95f,
                        SFX_SHIELDED_DEATH_1, SFX_SHIELDED_DEATH_2, SFX_SHIELDED_DEATH_3, SFX_SHIELDED_DEATH_4
                );
                break;

            case SHOOTER:
                AudioManager.get().playRandomSfx(
                        0.95f,
                        SFX_SHOOTER_DEATH_1, SFX_SHOOTER_DEATH_2, SFX_SHOOTER_DEATH_3, SFX_SHOOTER_DEATH_4
                );
                break;

            case BASIC:
            default:
                AudioManager.get().playRandomSfx(
                        0.95f,
                        "/spaceinvaders/resources/audio/sfx/CICOM401.wav",
                        "/spaceinvaders/resources/audio/sfx/CICOM408.wav",
                        "/spaceinvaders/resources/audio/sfx/CICOM409.wav"
                );
                break;
        }
    }
}
