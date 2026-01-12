package spaceinvaders.core.entities;

import java.util.Random;

/**
 * Swarmer zig-zag:
 * - Every 2000ms choose direction: left-diagonal, right-diagonal, or straight down.
 * - If it hits a side boundary, force direction away and reset timer.
 * - Slower than basic enemies (intentional).
 */
public class SwarmerZigZagPattern implements MovementPattern {

    private static final int DECIDE_MS = 2000;

    // 🔻 Slowed movement (half speed)
    private static final int SPEED_Y = 2; // was 4
    private static final int SPEED_X = 1; // was 3

    private final Random rng = new Random();

    @Override
    public void update(Invader inv, long dtMs, int panelW, int panelH) {
        if (inv.swarmDirTimerMs <= 0) {
            pickRandomDir(inv);
        } else {
            inv.swarmDirTimerMs -= (int) dtMs;
            if (inv.swarmDirTimerMs <= 0) {
                pickRandomDir(inv);
            }
        }

        inv.x += inv.vx;
        inv.y += inv.vy;

        // Side boundary handling
        if (inv.x <= 0) {
            inv.x = 0;
            forceDir(inv, +1);
        } else if (inv.x + inv.width >= panelW) {
            inv.x = panelW - inv.width;
            forceDir(inv, -1);
        }
    }

    private void pickRandomDir(Invader inv) {
        int r = rng.nextInt(3); // 0,1,2
        int dir = (r == 0) ? -1 : (r == 1 ? 0 : 1);
        applyDir(inv, dir);
        inv.swarmDirTimerMs = DECIDE_MS;
    }

    private void forceDir(Invader inv, int dir) {
        applyDir(inv, dir);
        inv.swarmDirTimerMs = DECIDE_MS;
    }

    private void applyDir(Invader inv, int dir) {
        inv.swarmDir = dir;
        inv.vx = dir * SPEED_X;
        inv.vy = SPEED_Y;
    }
}
