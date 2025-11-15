package spaceinvaders.core.render;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;

import java.awt.*;

public class DefaultBulletRenderer implements BulletRenderer {
    @Override
    public void render(Graphics2D g, Bullet b, GameState state) {
        // If using an image for bullets
        if (state.bulletType == GameState.BulletType.IMAGE && state.bulletImage != null) {
            g.drawImage(state.bulletImage, b.x, b.y, b.size, b.size, null);
            return;
        }

        // color per shape (your palette)
        Color c;
        switch (state.bulletType) {
            case TRIANGLE: c = new Color(100, 180, 255); break; // blue
            case CIRCLE:   c = new Color(255, 230, 120); break; // gold
            case SQUARE:   c = new Color(230, 250, 255); break; // soft white/cyan
            default:       c = Color.YELLOW;
        }
        g.setColor(c);

        switch (state.bulletType) {
            case CIRCLE -> g.fillOval(b.x, b.y, b.size, b.size);
            case SQUARE -> g.fillRect(b.x, b.y, b.size, b.size);
            case TRIANGLE -> {
                int s = b.size;
                int[] xs = { b.x + s/2, b.x, b.x + s };
                int[] ys = { b.y, b.y + s, b.y + s };
                g.fillPolygon(xs, ys, 3);
            }
        }
    }
}
