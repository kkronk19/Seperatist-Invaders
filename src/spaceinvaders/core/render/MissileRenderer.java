package spaceinvaders.core.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Missile;

public class MissileRenderer implements BulletRenderer {

    @Override
    public void render(Graphics2D g, Bullet b, GameState state) {
        if (!(b instanceof Missile m)) return;

        // --- trail (unchanged) ---
        for (Missile.Particle p : m.trail) {
            g.setColor(new Color(255, 190, 80, Math.min(255, (int) (p.alpha * 255))));
            g.fillOval(p.x - p.r, p.y - p.r, p.r * 2, p.r * 2);
        }

        int s = b.size;

        // center of missile
        double cx = b.x + s / 2.0;
        double cy = b.y + s / 2.0;

        // Compute angle from velocity.
        // atan2 gives angle where 0 rad points right; we want sprite "nose" to point along velocity.
        // Our drawn missile below points UP by default, so rotate by (atan2(vy, vx) + PI/2).
        double vx = b.vx;
        double vy = b.vy;

        // If vx/vy are 0 (possible for first frame), default to "up"
        double ang = (vx == 0 && vy == 0) ? (-Math.PI / 2.0) : Math.atan2(vy, vx);
        double theta = ang + Math.PI / 2.0;

        // Save transform
        AffineTransform oldTx = g.getTransform();

        // Move origin to center, rotate, draw missile centered
        g.translate(cx, cy);
        g.rotate(theta);

        // --- missile body (drawn pointing UP in local coordinates) ---
        // Body: orange rectangle
        g.setColor(new Color(255, 140, 0));
        g.fillRect(-s / 2, -s / 2, s, s);

        // Nose: white tip (a small rectangle at the "top")
        g.setColor(Color.WHITE);
        g.fillRect(-s / 4, -s / 2 - 3, s / 2, 4);

        // Optional: little fin stripe for motion feel
        g.setColor(new Color(255, 200, 120));
        g.fillRect(-1, -s / 4, 2, s / 2);

        // Restore transform
        g.setTransform(oldTx);
    }
}
