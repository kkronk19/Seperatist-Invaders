package spaceinvaders.weapons;

import java.util.*;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Missile;
import spaceinvaders.core.entities.Player;

public class MissileWeapon implements Weapon {

    private final Player player;

    // base stats
    private final int baseCooldownMs = 500;
    private final int vy = -10, size = 12, baseDmg = 2;

    // snake path params (used when NOT straight)
    private final double A = 36.0, W = 7.5;

    private final long maxLifeMs = 5000;

    private long lastShot = 0L;
    private final Map<Bullet, Long> born = new HashMap<>();
    private final Map<Bullet, Integer> x0 = new HashMap<>();
    private final Random rng = new Random();

    public MissileWeapon(Player player) {
        this.player = player;
    }

    @Override
    public void tryFire(long nowMs, int mx, int my, boolean isHeld, List<Bullet> out) {
        // Tap-fired via FireController secondary.
        int cd = effectiveCooldownMs();
        if (nowMs - lastShot < cd) return;
        lastShot = nowMs;

        int count = player.missileCount(); // 1..3
        int dmg   = player.missileDamage(baseDmg);

        // spacing so 2/3 missiles don't overlap
        int spacing = 16;

        int startOffset = 0;
        if (count == 2) startOffset = -spacing / 2;
        if (count == 3) startOffset = -spacing;

        for (int i = 0; i < count; i++) {
            int ox = startOffset + i * spacing;

            Missile m = new Missile(mx - size / 2 + ox, my, 0, vy, size, dmg);

            // ---- Smart targeting flag (MISSILE legendary) ----
            m.smartTargeting = player.upMissileSmart;

            // Straight flight if player bought straight OR smart targeting is on
            // (smart targeting must not fight the snake path)
            m.straightFlight = player.upMissileStraight || m.smartTargeting;

            out.add(m);
            born.put(m, nowMs);
            x0.put(m, mx + ox);
        }
    }

    private int effectiveCooldownMs() {
        double mult = player.missileFireRateMult();
        return (int) Math.max(120, Math.round(baseCooldownMs / mult));
    }

    @Override
    public void updateBullets(long nowMs, List<Bullet> bullets, int w, int h) {
        for (Bullet b : bullets) {
            if (!(b instanceof Missile m)) continue;

            Long t0 = born.get(m);
            Integer ox = x0.get(m);
            if (t0 == null || ox == null) continue;

            long age = nowMs - t0;
            if (age > maxLifeMs) {
                born.remove(m);
                x0.remove(m);
                continue;
            }

            // snake path only if not straightFlight
            if (!m.straightFlight) {
                double t = age / 1000.0;
                double centerX = ox + A * Math.sin(W * t);
                m.x = (int) Math.round(centerX - m.size / 2.0);
            }

            // trail particle at missile tail (centered)
            int cx = m.x + m.size / 2;
            int cy = m.y + m.size;        // tail
            int r  = 2 + rng.nextInt(2);  // 2..3 px
            m.trail.add(new Missile.Particle(cx, cy, r, 1.0f));

            // fade + cap
            for (Iterator<Missile.Particle> it = m.trail.iterator(); it.hasNext();) {
                Missile.Particle p = it.next();
                p.alpha *= 0.86f;
                p.y += 1;
                if (p.alpha < 0.06f) it.remove();
            }
            while (m.trail.size() > 40) m.trail.remove(0);
        }

        // cleanup maps for deleted missiles
        born.keySet().removeIf(b -> !bullets.contains(b));
        x0.keySet().removeIf(b -> !bullets.contains(b));
    }
}
