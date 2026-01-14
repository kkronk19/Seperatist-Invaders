package spaceinvaders.core.entities;

import java.util.ArrayList;
import java.util.List;

/** Missile bullet with a render-only particle trail. */
public class Missile extends Bullet {

    /** If true, MissileWeapon will NOT apply sine "snake" pathing. */
    public boolean straightFlight = false;

    /** Render-only trail particles (filled by MissileWeapon.updateBullets). */
    public final List<Particle> trail = new ArrayList<>();

    /** Simple trail particle. */
    public static final class Particle {
        public int x, y;
        public int r;
        public float alpha; // 0..1
        public Particle(int x, int y, int r, float alpha) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.alpha = alpha;
        }
    }

    public Missile(int x, int y, int vx, int vy, int size, int damage) {
        super(x, y, vx, vy, size, damage, BulletKind.MISSILE);

        // missiles should explode on hit (CollisionSystem already treats MISSILE as explode)
        this.explodesOnHit = true;
    }
}
