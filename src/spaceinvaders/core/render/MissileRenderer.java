package spaceinvaders.core.render;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Missile;

import java.awt.*;

public class MissileRenderer implements BulletRenderer {
    @Override
    public void render(Graphics2D g, Bullet b, GameState state) {
        if (!(b instanceof Missile m)) {
            return; // safety
        }

        // trail
        for (Missile.Particle p : m.trail) {
            g.setColor(new Color(255, 190, 80, Math.min(255, (int)(p.alpha * 255))));
            g.fillOval(p.x - p.r, p.y - p.r, p.r * 2, p.r * 2);
        }

        // missile body (orange + white tip)
        int s = b.size;
        g.setColor(new Color(255, 140, 0));
        g.fillRect(b.x, b.y, s, s);
        g.setColor(Color.WHITE);
        g.fillRect(b.x + s/4, b.y - 2, s/2, 3);
    }
}
