package spaceinvaders.core.render;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Blade;

import java.awt.*;

/** Renders Blade projectiles with ghost trail + bounce flash. */
public class BladeRenderer implements BulletRenderer {

    @Override
    public void render(Graphics2D g, Bullet b, GameState state) {
        if (!(b instanceof Blade blade)) return;

        int s = Math.max(8, b.size);
        int x = b.x;
        int y = b.y;

        // --- Ghost trail (draw oldest first) ---
        for (Blade.Ghost ghost : blade.trail) {
            float a = Math.max(0f, Math.min(1f, ghost.a));
            Graphics2D tg = (Graphics2D) g.create();
            try {
                tg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.9f));
                tg.setColor(new Color(120, 200, 255));
                tg.fillRoundRect(ghost.x, ghost.y + s / 3, s, s / 3, 8, 8);
            } finally {
                tg.dispose();
            }
        }

        // --- Core blade ---
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // glow
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.setColor(new Color(120, 200, 255));
            g2.fillOval(x - s / 2, y - s / 2, s * 2, s * 2);

            // core
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(new Color(120, 200, 255));
            g2.fillRoundRect(x, y + s / 3, s, s / 3, 8, 8);

            // bright edge
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x + 2, y + s / 3 + 1, Math.max(2, s - 4), Math.max(2, s / 3 - 2), 8, 8);

            // --- White flash on bounce ---
            if (blade.flashMs > 0) {
                float f = Math.min(1f, blade.flashMs / 110f); // 0..1
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f * f));
                g2.setColor(Color.WHITE);
                g2.fillOval(x - s / 3, y - s / 3, (int)(s * 1.6), (int)(s * 1.6));
            }
        } finally {
            g2.dispose();
        }
    }
}
