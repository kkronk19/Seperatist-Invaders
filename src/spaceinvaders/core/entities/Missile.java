package spaceinvaders.core.entities;

import java.util.ArrayList;
import java.util.List;

public class Missile extends Bullet {

    /** Simple particle for the exhaust trail. */
    public static final class Particle {
        public int x, y, r;      // center + radius (px)
        public float alpha;      // 0..1
        public Particle(int x, int y, int r, float alpha) {
            this.x = x; this.y = y; this.r = r; this.alpha = alpha;
        }
    }

    /** Render-only particles. Updated by the weapon each frame. */
    public final List<Particle> trail = new ArrayList<>();

    public Missile(int x, int y, int vx, int vy, int size, int damage) {
        super(x, y, vx, vy, size, damage, BulletKind.MISSILE);
    }
}
