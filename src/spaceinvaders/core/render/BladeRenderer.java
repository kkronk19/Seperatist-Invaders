package spaceinvaders.core.render;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Blade;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Renders Blade projectiles with optional sprite + glow fallback. */
public class BladeRenderer implements BulletRenderer {

    private static BufferedImage bladeImg;

    private static BufferedImage loadBladeImage() {
        if (bladeImg != null) return bladeImg;
        try {
            bladeImg = ImageIO.read(BladeRenderer.class.getResource(
                "/spaceinvaders/resources/image/blade.png"
            ));
        } catch (Throwable ignored) {
            bladeImg = null;
        }
        return bladeImg;
    }

    @Override
    public void render(Graphics2D g, Bullet b, GameState state) {
        if (!(b instanceof Blade)) return;

        int s = Math.max(8, b.size);
        int x = b.x;
        int y = b.y;

        // If sprite exists, draw it
        BufferedImage img = loadBladeImage();
        if (img != null) {
            g.drawImage(img, x, y, s, s, null);
            return;
        }

        // Fallback: simple “energy blade” look
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
        } finally {
            g2.dispose();
        }
    }
}
