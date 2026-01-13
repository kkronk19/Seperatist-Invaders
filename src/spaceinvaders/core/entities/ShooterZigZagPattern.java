package spaceinvaders.core.entities;

import java.util.Random;

/**
 * Shooter zig-zag:
 * - Every 2 seconds chooses: diag left, straight down, diag right
 * - If it hits a side wall, it forces direction away and resets timer
 * - On every decision/forced-change, it requests a shot via inv.firePending
 *
 * NOTE: This pattern is stateful. Use a NEW instance per shooter invader.
 */
public class ShooterZigZagPattern implements MovementPattern {

    private static final int DECIDE_MS = 2000;

    // Shooter pacing (half speed “tank-like”). Tweak if needed.
    private static final int SPEED_Y = 1;
    private static final int SPEED_X = 1;

    private final Random rng = new Random();

    private int decideTimerMs = 0;
    private int dir = 0; // -1 left, 0 down, +1 right

    @Override
    public void update(Invader inv, long dtMs, int panelW, int panelH) {
        // countdown
        decideTimerMs -= (int) dtMs;

        // decide a new direction every DECIDE_MS
        if (decideTimerMs <= 0) {
            pickDirection(inv);
        }

        // move using inv.vx/vy
        inv.x += inv.vx;
        inv.y += inv.vy;

        // boundary handling (force away + reset timer so it moves away for >= 2s)
        if (inv.x <= 0) {
            inv.x = 0;
            forceDirection(inv, +1);
        } else if (inv.x + inv.width >= panelW) {
            inv.x = panelW - inv.width;
            forceDirection(inv, -1);
        }
    }

    private void pickDirection(Invader inv) {
        int r = rng.nextInt(3); // 0 left, 1 down, 2 right
        dir = (r == 0) ? -1 : (r == 2 ? +1 : 0);

        apply(inv, dir);

        // request a shot on every decision
        inv.firePending = true;

        decideTimerMs = DECIDE_MS;
    }

    private void forceDirection(Invader inv, int forcedDir) {
        dir = forcedDir;
        apply(inv, dir);

        // request a shot on forced change too
        inv.firePending = true;

        decideTimerMs = DECIDE_MS;
    }

    private void apply(Invader inv, int d) {
        inv.vy = SPEED_Y;
        inv.vx = d * SPEED_X;
    }
}
