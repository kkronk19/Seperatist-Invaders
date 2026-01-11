package spaceinvaders.core.systems;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import spaceinvaders.core.entities.Blade;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Invader;
import spaceinvaders.services.audio.AudioManager;

public final class CollisionSystem {

    public interface ShrapnelSpawner {
        List<Bullet> spawnShrapnel(int cx, int cy);
    }

    private CollisionSystem() {}

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
            br.setBounds(b.x, b.y, b.size, b.size);

            boolean removeBullet = false;
            boolean hitSomething = false;

            for (Iterator<Invader> iit = invaders.iterator(); iit.hasNext();) {
                Invader inv = iit.next();
                ir.setBounds(inv.x, inv.y, inv.width, inv.height);

                if (!br.intersects(ir)) continue;

                // ---- HIT ----
                hitSomething = true;

                // apply shield/hp rules
                Invader.HitResult res = inv.takeHit(b.damage);

                if (res == Invader.HitResult.KILLED) {
                    iit.remove();

                    // death sfx only on kill
                    AudioManager.get().playRandomSfx(
                        1.1f,
                        "/spaceinvaders/resources/audio/sfx/CICOM401.wav",
                        "/spaceinvaders/resources/audio/sfx/CICOM408.wav",
                        "/spaceinvaders/resources/audio/sfx/CICOM409.wav"
                    );
                }

                // Special: blade vs shield absorb => bounce blade, don't consume pierce
                if (res == Invader.HitResult.ABSORBED && b instanceof Blade) {
                    // flip vertical to get it away from the invader (prevents immediate re-hit)
                    b.vy = -b.vy;

                    // nudge out of the invader hitbox so it won't intersect next tick
                    if (b.vy > 0) {
                        // now moving down
                        b.y = inv.y + inv.height + 2;
                    } else {
                        // now moving up
                        b.y = inv.y - b.size - 2;
                    }

                    // blade stays alive
                    removeBullet = false;
                } else {
                    // normal bullet behavior on hit
                    if (b.kind == Bullet.BulletKind.MISSILE) {
                        int cx = b.x + b.size / 2;
                        int cy = b.y + b.size / 2;
                        if (shrapnelSpawner != null) pendingAdds.addAll(shrapnelSpawner.spawnShrapnel(cx, cy));
                        removeBullet = true;
                    } else if (b instanceof Blade blade) {
                        removeBullet = blade.onHitInvader();
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
}
