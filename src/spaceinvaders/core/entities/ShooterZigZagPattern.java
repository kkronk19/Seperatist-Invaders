package spaceinvaders.core.entities;

import java.util.Random;

/**
 * Shooter zig-zag:
 * - Same movement logic as Swarmer
 * - Shoots every time direction is chosen
 * - Slower movement so it stays back and pressures player
 */
public class ShooterZigZagPattern implements MovementPattern {

    private static final int DECIDE_MS = 2000;

    // 🔻 Slower than swarmer (tank-like pacing)
    private static final int SPEED_Y = 1; // was 2
    private static final int SPEED_X = 1; // was 2

    private final Random rng = new Random();

    @Override
    public void update(Invader inv, long dtMs, int panelW, int panelH) {
        if (inv.shootTimerMs <= 0) {
            pickDirAndShoot(inv);
        } else {
            inv.shootTimerMs -= (int) dtMs;
            if (inv.shootTimerMs <= 0) {
                pickDirAndShoot(inv);
            }
        }

        inv.x += inv.vx;
        inv.y += inv.vy;

        // Boundary handling
        if (inv.x <= 0) {
            inv.x = 0;
            forceDir(inv, +1);
        } else if (inv.x + inv.width >= panelW) {
            inv.x = panelW - inv.width;
            forceDir(inv, -1);
        }
    }

    private void pickDirAndShoot(Invader inv) {
        int r = rng.nextInt(3); // left, down, right
        int dir = (r == 0) ? -1 : (r == 1 ? 0 : 1);
        applyDir(inv, dir);

        // 🔫 mark that this invader should shoot
        inv.requestShoot = true;

        inv.shootTimerMs = DECIDE_MS;
    }

    private void forceDir(Invader inv, int dir) {
        applyDir(inv, dir);
        inv.requestShoot = true;
        inv.shootTimerMs = DECIDE_MS;
    }

    private void applyDir(Invader inv, int dir) {
        inv.vx = dir * SPEED_X;
        inv.vy = SPEED_Y;
    }
}
