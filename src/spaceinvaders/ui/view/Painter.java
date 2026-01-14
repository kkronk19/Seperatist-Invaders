package spaceinvaders.ui.view;

import java.awt.*;
import java.util.List;
import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;

public class Painter {

    public void paintAll(Graphics g, GameState s, Component observer) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, s.width, s.height);

        drawPlayer(g, s, observer);
        drawInvaders(g, s, observer);
        drawBullets(g, s, observer);
    }

    private void drawPlayer(Graphics g, GameState s, Component observer) {
        if (s.playerImage == null) return;
        int y = s.height - s.playerHeight;
        g.drawImage(s.playerImage, s.playerX, y, s.playerWidth, s.playerHeight, observer);
    }

    private void drawInvaders(Graphics g, GameState s, Component observer) {
        if (s.invaderImage == null) return;
        for (GameState.Invader inv : s.invaders) {
            g.drawImage(s.invaderImage, inv.x, inv.y, inv.size, inv.size, observer);
        }
    }

    private void drawBullets(Graphics g, GameState s, Component observer) {
        List<Bullet> bullets = s.bullets;

        switch (s.bulletType) {
            case TRIANGLE -> {
                g.setColor(Color.YELLOW);
                for (Bullet b : bullets) {
                    int ssz = Math.max(6, b.size);
                    int half = ssz / 2;

                    int[] xs = { b.x, b.x - half, b.x + half };
                    int[] ys = { b.y, b.y + ssz, b.y + ssz };
                    g.fillPolygon(xs, ys, 3);
                }
            }
            case CIRCLE -> {
                g.setColor(Color.CYAN);
                for (Bullet b : bullets) {
                    int ssz = Math.max(6, b.size);
                    int half = ssz / 2;
                    g.fillOval(b.x - half, b.y - half, ssz, ssz);
                }
            }
            case SQUARE -> {
                g.setColor(Color.MAGENTA);
                for (Bullet b : bullets) {
                    int ssz = Math.max(6, b.size);
                    int half = ssz / 2;
                    g.fillRect(b.x - half, b.y - half, ssz, ssz);
                }
            }
            case IMAGE -> {
                if (s.bulletImage == null) {
                    g.setColor(Color.YELLOW);
                    for (Bullet b : bullets) {
                        int ssz = Math.max(6, b.size);
                        int half = ssz / 2;
                        int[] xs = { b.x, b.x - half, b.x + half };
                        int[] ys = { b.y, b.y + ssz, b.y + ssz };
                        g.fillPolygon(xs, ys, 3);
                    }
                } else {
                    for (Bullet b : bullets) {
                        int ssz = Math.max(6, b.size);
                        int half = ssz / 2;
                        g.drawImage(s.bulletImage, b.x - half, b.y - half, ssz, ssz, observer);
                    }
                }
            }
        }
    }
}
