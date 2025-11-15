package spaceinvaders.weapons;

import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Missile;
import java.util.*;

public class MissileWeapon implements Weapon {
    private final int cooldownMs = 500;
    private final int vy = -10, size = 12, dmg = 2;
    private final double A = 36.0, W = 7.5;
    private final long maxLifeMs = 5000;

    private long lastShot = 0L;
    private final Map<Bullet, Long>    born = new HashMap<>();
    private final Map<Bullet, Integer> x0   = new HashMap<>();
    private final Random rng = new Random();

    @Override
    public void tryFire(long nowMs, int mx, int my, boolean isHeld, List<Bullet> out) {
        if (nowMs - lastShot < cooldownMs) return;
        lastShot = nowMs;

        // 🔄 spawn an actual Missile (not a plain Bullet)
        Missile m = new Missile(mx - size/2, my, 0, vy, size, dmg);
        out.add(m);
        born.put(m, nowMs);
        x0.put(m, mx);
    }

    @Override
    public void updateBullets(long nowMs, List<Bullet> bullets, int w, int h) {
        for (Bullet b : bullets) {
            if (!(b instanceof Missile m)) continue;

            Long t0 = born.get(m);
            Integer ox = x0.get(m);
            if (t0 == null || ox == null) continue;

            long age = nowMs - t0;
            if (age > maxLifeMs) { born.remove(m); x0.remove(m); continue; }

            // snake path (absolute x)
            double t = age / 1000.0;
            double centerX = ox + A * Math.sin(W * t);
            m.x = (int)Math.round(centerX - m.size / 2.0);

            // 🔸 trail: add a particle at the missile tail
            int cx = m.x + m.size / 2;
            int cy = m.y + m.size;           // slightly behind
            int r  = 2 + rng.nextInt(2);     // 2..3 px
            m.trail.add(new Missile.Particle(cx, cy, r, 1.0f));

            // fade + drift particles
            for (Iterator<Missile.Particle> it = m.trail.iterator(); it.hasNext();) {
                Missile.Particle p = it.next();
                p.alpha *= 0.86f;   // fade
                p.y += 1;           // small downward drift
                if (p.alpha < 0.08f || m.trail.size() > 28) it.remove();
            }
        }

        born.keySet().removeIf(b -> !bullets.contains(b));
        x0.keySet().removeIf(b -> !bullets.contains(b));
    }
}
