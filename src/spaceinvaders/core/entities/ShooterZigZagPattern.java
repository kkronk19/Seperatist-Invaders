package spaceinvaders.core.entities;

import java.util.Random;

/**
 * Shooter zig-zag:
 * - Every 2 seconds chooses: left diag, straight down, right diag
 * - Boundary hit forces direction away and resets timer
 * - Shoots each time it chooses/forces a direction by setting inv.firePending = true
 *
 * NOTE: This pattern is stateful; create a new instance per invader.
 */
public class ShooterZigZagPattern implements MovementPattern {

    private static final int DECIDE_MS = 2000;

    // Half-speed (tank-like pacing)
    private static final int SPEED_Y = 1;
    private static final int SPEED_X = 1;

    private final Random rng = new Random();

    private int decideTimerMs = 0;
    private int dir = 0; // -1 left, 0 down, +1 right

    @Override
    public void update(Invader inv, long dtMs, int panelW, int panelH) {
        decideTimerMs -= (int) dtMs;

        if (decideTimerMs <= 0) {
            pickDirection(inv);
        }

        // move
        inv.x += inv.vx;
        inv.y += inv.vy;

        // boundary handling forces direction away for at least DECIDE_MS
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
        dir = (r == 0) ? -1 : (r == 1 ? 0 : 1);

        apply(inv, dir);

        // request a shot on every decision
        inv.firePending = true;

        decideTimerMs = DECIDE_MS;
    }

    private void forceDirection(Invader inv, int forcedDir) {
        dir = forcedDir;

        apply(inv, dir);

        // request a shot on forced direction change too
        inv.firePending = true;

        decideTimerMs = DECIDE_MS;
    }

    private void apply(Invader inv, int dir) {
        inv.vx = dir * SPEED_X;
        inv.vy = SPEED_Y;
    }
}
