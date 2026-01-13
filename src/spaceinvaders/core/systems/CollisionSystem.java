package spaceinvaders.core.systems;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import spaceinvaders.core.entities.Blade;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.EnemyBullet;   // <-- ADD THIS
import spaceinvaders.core.entities.Invader;
import spaceinvaders.core.entities.Missile;
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

    private static final double ROCKET_DEFLECT_CHANCE = 0.25;

    /** Bullet vs Invader collisions. Mutates lists safely. */
    public static void bulletsVsInvaders(
            List<Bullet> bullets,
            List<Invader> invaders,
            ShrapnelSpawner shrapnelSpawner
    ) {
        Rectangle br = new Rectangle();
        Rectangle ir = new Rectangle();

        List<Bullet> pendingAdds = new ArrayList<>();

        for (Iterator<Bullet> bit = bullets.iterator(); bit.hasNext();) {
            Bullet b = bit.next();

            // ✅ Enemy bullets should NEVER hit invaders (prevents shooter "suicide")
            if (b instanceof EnemyBullet) {
                continue;
            }

            br.setBounds(b.x, b.y, b.size, b.size);

            boolean removeBullet = false;
            boolean hitSomething = false;

            for (Iterator<Invader> iit = invaders.iterator(); iit.hasNext();) {
                Invader inv = iit.next();
                ir.setBounds(inv.x, inv.y, inv.width, inv.height);

                if (!br.intersects(ir)) continue;

                hitSomething = true;

                boolean hadShield = inv.shieldHits > 0;
                Invader.HitResult res = inv.takeHit(b.damage);
                boolean shieldBroke = hadShield && inv.shieldHits == 0 && res == Invader.HitResult.ABSORBED;

                if (res == Invader.HitResult.KILLED) {
                    iit.remove();
                    AudioManager.get().playRandomSfx(
                        1.1f,
                        "/spaceinvaders/resources/audio/sfx/CICOM401.wav",
                        "/spaceinvaders/resources/audio/sfx/CICOM408.wav",
                        "/spaceinvaders/resources/audio/sfx/CICOM409.wav"
                    );
                }

                // --------------------------
                // SHIELD ABSORBS
                // --------------------------
                if (res == Invader.HitResult.ABSORBED) {

                    // Blade vs shield: reflect + bounce, don't consume pierce
                    if (b instanceof Blade) {
                        try { AudioManager.get().playRandomSfx(0.40f, SFX_REFLECT_1, SFX_REFLECT_2, SFX_REFLECT_3); }
                        catch (Throwable ignored) {}

                        if (shieldBroke) {
                            try { AudioManager.get().playSfx(SFX_SHIELD_DEPLETED, 0.60f); } catch (Throwable ignored) {}
                        }

                        b.vy = -b.vy;
                        if (b.vy > 0) b.y = inv.y + inv.height + 2;
                        else          b.y = inv.y - b.size - 2;

                        removeBullet = false;
                    }

                    // Missile vs shield: 25% deflect (KEEP missile look + trail, NO SINE)
                    else if (b.kind == Bullet.BulletKind.MISSILE) {
                        boolean deflect = Math.random() < ROCKET_DEFLECT_CHANCE;

                        if (deflect) {
                            try { AudioManager.get().playRandomSfx(0.45f, SFX_REFLECT_1, SFX_REFLECT_2, SFX_REFLECT_3); }
                            catch (Throwable ignored) {}

                            if (shieldBroke) {
                                try { AudioManager.get().playSfx(SFX_SHIELD_DEPLETED, 0.60f); } catch (Throwable ignored) {}
                            }

                            if (b instanceof Missile m) {
                                m.straightFlight = true;
                            }

                            b.vy = -1;
                            b.vx = (Math.random() < 0.5) ? -4 : 4;
                            b.y = inv.y - b.size - 2;

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
                    removeBullet = blade.onHitInvader();
                } else {
                    removeBullet = true;
                }

                break;
            }

            if (hitSomething && removeBullet) bit.remove();
        }

        if (!pendingAdds.isEmpty()) bullets.addAll(pendingAdds);
    }
}
