package spaceinvaders.core.render;

import java.awt.Color;
import java.awt.Graphics2D;
import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;

public class DefaultBulletRenderer implements BulletRenderer {

    @Override
    public void render(Graphics2D g, Bullet b, GameState state) {
        // Color by bullet kind (simple + robust)
        Color c;
        switch (b.kind) {
            case MISSILE: c = new Color(255, 140, 0); break;   // orange (missile fallback)
            case BLADE:   c = new Color(120, 200, 255); break; // cyan (blade fallback)
            case BASIC:
            default:      c = new Color(255, 230, 120); break; // gold
        }

        g.setColor(c);
        g.fillOval(b.x, b.y, b.size, b.size);
    }
}
