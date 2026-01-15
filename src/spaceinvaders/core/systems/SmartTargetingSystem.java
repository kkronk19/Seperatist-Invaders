package spaceinvaders.core.systems;

import java.util.List;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Invader;
import spaceinvaders.core.entities.Missile;

public final class SmartTargetingSystem {
    private SmartTargetingSystem() {}

    /**
     * Unified homing for any projectile with b.smartTargeting=true.
     * - BASIC: gentle nudge so it still feels like blaster fire.
     * - MISSILE: stronger nudge (missiles should actually track).
     *
     * Call once per frame BEFORE bullet movement (b.update()).
     */
    public static void update(List<Bullet> bullets, List<Invader> invaders) {
        if (bullets == null || bullets.isEmpty()) return;
        if (invaders == null || invaders.isEmpty()) return;

        for (Bullet b : bullets) {
            if (!b.smartTargeting) continue;

            Invader target = findNearestAhead(b, invaders);
            if (target == null) continue;

            int bx = b.x + b.size / 2;
            int by = b.y + b.size / 2;

            int tx = target.x + target.width / 2;
            int ty = target.y + target.height / 2;

            // Only home if target is "ahead" (above projectile for player-fired shots)
            if (ty > by) continue;

            int dx = tx - bx;

            if (b instanceof Missile m) {
                // ---- MISSILE HOMING (stronger) ----
                // Ensure snake-path isn't fighting the homing.
                // (MissileWeapon already should set straightFlight when smart, but this is a safe backstop)
                m.straightFlight = true;

                // If far away, correct more aggressively.
                if (dx > 6) m.vx += 2;
                else if (dx < -6) m.vx -= 2;
                else {
                    // small correction near center
                    if (dx > 1) m.vx += 1;
                    else if (dx < -1) m.vx -= 1;
                }

                // clamp so it doesn't go insane
                if (m.vx > 8) m.vx = 8;
                if (m.vx < -8) m.vx = -8;

            } else {
                // ---- BASIC HOMING (light) ----
                if (dx > 8) b.vx += 1;
                else if (dx < -8) b.vx -= 1;

                if (b.vx > 6) b.vx = 6;
                if (b.vx < -6) b.vx = -6;
            }
        }
    }

    private static Invader findNearestAhead(Bullet b, List<Invader> invaders) {
        Invader best = null;
        long bestDist2 = Long.MAX_VALUE;

        int bx = b.x + b.size / 2;
        int by = b.y + b.size / 2;

        for (Invader inv : invaders) {
            int ix = inv.x + inv.width / 2;
            int iy = inv.y + inv.height / 2;

            // only consider invaders above projectile (ahead)
            if (iy > by) continue;

            long dx = (long) ix - bx;
            long dy = (long) iy - by;
            long d2 = dx * dx + dy * dy;

            if (d2 < bestDist2) {
                bestDist2 = d2;
                best = inv;
            }
        }
        return best;
    }
}
