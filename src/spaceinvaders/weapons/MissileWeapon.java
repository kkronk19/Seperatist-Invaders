package spaceinvaders.weapons;

import java.util.*;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Missile;

public class MissileWeapon implements Weapon {
    private final int cooldownMs = 500;
    private final int vy = -10, size = 12, dmg = 6;
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

            // snake path (absolute x) ONLY when not in straightFlight
            if (!m.straightFlight) {
                double t = age / 1000.0;
                double centerX = ox + A * Math.sin(W * t);
                m.x = (int)Math.round(centerX - m.size / 2.0);
            }

            // ------------------------------------------------------------
            // Trail spawn at missile "tail" along velocity direction
            // ------------------------------------------------------------
            double vx = m.vx;
            double vy = m.vy;
            double len = Math.hypot(vx, vy);

            // if velocity is degenerate, assume upward
            double nx, ny;
            if (len < 0.001) {
                nx = 0.0;
                ny = -1.0;
            } else {
                nx = vx / len;
                ny = vy / len;
            }

            // center of missile
            double cx = m.x + m.size / 2.0;
            double cy = m.y + m.size / 2.0;

            // tail point is opposite direction of travel
            double tailDist = m.size * 0.55; // tweak for "behind the body"
            int tx = (int)Math.round(cx - nx * tailDist);
            int ty = (int)Math.round(cy - ny * tailDist);

            int r  = 2 + rng.nextInt(2);     // 2..3 px
            m.trail.add(new Missile.Particle(tx, ty, r, 1.0f));

            // fade + drift particles opposite the missile direction
            // (small drift looks like exhaust; not always "down")
            double drift = 0.9;
            int driftX = (int)Math.round(-nx * drift);
            int driftY = (int)Math.round(-ny * drift);

            for (Iterator<Missile.Particle> it = m.trail.iterator(); it.hasNext();) {
                Missile.Particle p = it.next();
                p.alpha *= 0.90f; // slower fade (longer trail)
                p.x += driftX;
                p.y += driftY;

                // cap trail length a bit higher so it doesn't feel "too short"
                if (p.alpha < 0.06f || m.trail.size() > 42) it.remove();
            }
        }

        born.keySet().removeIf(b -> !bullets.contains(b));
        x0.keySet().removeIf(b -> !bullets.contains(b));
    }
}
