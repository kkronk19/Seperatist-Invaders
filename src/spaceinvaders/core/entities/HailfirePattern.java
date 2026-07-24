package spaceinvaders.core.entities;

/** Heavy unit: long fast horizontal passes and half-speed vertical descent. */
public final class HailfirePattern implements MovementPattern {
    private final int initialDirection;
    private int directionTimerMs;
    private int verticalAccumulatorMs;
    public HailfirePattern(int seed) { initialDirection = (seed & 1) == 0 ? 1 : -1; directionTimerMs = 2500 + Math.floorMod(seed, 2501); }
    @Override public void update(Invader inv, long dtMs, int panelW, int panelH) {
        if (inv.vx == 0) inv.vx = initialDirection * 9;
        directionTimerMs -= (int) dtMs;
        if (directionTimerMs <= 0) { inv.vx = -inv.vx; directionTimerMs = 3000; }
        inv.x += inv.vx;
        verticalAccumulatorMs += dtMs;
        if (verticalAccumulatorMs >= 128) { inv.y++; verticalAccumulatorMs = 0; }
        if (inv.x <= 0 || inv.x + inv.width >= panelW) {
            inv.x = Math.max(0, Math.min(panelW - inv.width, inv.x));
            inv.vx = -inv.vx;
            directionTimerMs = 3000;
        }
    }
}
