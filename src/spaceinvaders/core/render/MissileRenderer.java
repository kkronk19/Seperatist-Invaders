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

        // --- trail ---
        for (Missile.Particle p : m.trail) {
            g.setColor(new Color(255, 190, 80, Math.min(255, (int) (p.alpha * 255))));
            g.fillOval(p.x - p.r, p.y - p.r, p.r * 2, p.r * 2);
        }

        int s = b.size;

        double cx = b.x + s / 2.0;
        double cy = b.y + s / 2.0;

        double vx = b.vx;
        double vy = b.vy;

        double ang = (vx == 0 && vy == 0) ? (-Math.PI / 2.0) : Math.atan2(vy, vx);
        double theta = ang + Math.PI / 2.0;

        AffineTransform oldTx = g.getTransform();

        g.translate(cx, cy);
        g.rotate(theta);

        // body
        g.setColor(new Color(255, 140, 0));
        g.fillRect(-s / 2, -s / 2, s, s);

        // nose
        g.setColor(Color.WHITE);
        g.fillRect(-s / 4, -s / 2 - 3, s / 2, 4);

        // stripe
        g.setColor(new Color(255, 200, 120));
        g.fillRect(-1, -s / 4, 2, s / 2);

        g.setTransform(oldTx);
    }
}
