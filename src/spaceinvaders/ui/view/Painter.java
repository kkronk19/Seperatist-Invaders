package spaceinvaders.ui.view;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;  // <-- use top-level Bullet

import java.awt.*;
import java.util.List;

/** Pure rendering. No IO, no state changes, no UI types except Component as image observer. */
public class Painter {

    /** Convenience: paint whole frame. */
    public void paintAll(Graphics g, GameState s, Component observer) {
        // background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, s.width, s.height);

        drawplayer(g, s, observer);
        drawInvaders(g, s, observer);
        drawBullets(g, s, observer);
    }

    public void drawplayer(Graphics g, GameState s, Component observer) {
        Image img = s.playerImage; // <- minimal cleanup
        if (img == null) return;
        int x = s.playerX;
        int y = s.height - s.playerHeight;
        g.drawImage(img, x, y, s.playerWidth, s.playerHeight, observer);
    }

    public void drawInvaders(Graphics g, GameState s, Component observer) {
        if (s.invaderImage == null) return;
        for (GameState.Invader inv : s.invaders) {
            g.drawImage(s.invaderImage, inv.x, inv.y, inv.size, inv.size, observer);
        }
    }

    public void drawBullets(Graphics g, GameState s, Component observer) {
        List<Bullet> bullets = s.bullets; // <-- switch to top-level Bullet

        switch (s.bulletType) {
            case TRIANGLE -> {
                g.setColor(Color.YELLOW);
                for (Bullet b : bullets) {
                    int[] xs = { b.x, b.x - 5, b.x + 5 };
                    int[] ys = { b.y, b.y + 10, b.y + 10 };
                    g.fillPolygon(xs, ys, 3);
                }
            }
            case CIRCLE -> {
                g.setColor(Color.CYAN);
                for (Bullet b : bullets) {
                    g.fillOval(b.x - 5, b.y - 5, 10, 10);
                }
            }
            case SQUARE -> {
                g.setColor(Color.MAGENTA);
                for (Bullet b : bullets) {
                    g.fillRect(b.x - 5, b.y - 5, 10, 10);
                }
            }
            case IMAGE -> {
                if (s.bulletImage == null) {
                    g.setColor(Color.YELLOW);
                    for (Bullet b : bullets) {
                        int[] xs = { b.x, b.x - 5, b.x + 5 };
                        int[] ys = { b.y, b.y + 10, b.y + 10 };
                        g.fillPolygon(xs, ys, 3);
                    }
                    return;
                }
                for (Bullet b : bullets) {
                    g.drawImage(s.bulletImage, b.x - 6, b.y - 6, 12, 12, observer);
                }
            }
        }
    }
}
