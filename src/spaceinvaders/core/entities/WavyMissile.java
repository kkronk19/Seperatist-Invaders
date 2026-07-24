package spaceinvaders.core.entities;

/** Base player missile identity: a stable sine wave around its own centerline. */
public final class WavyMissile extends Missile {
    private int centerX;
    private final double phase;
    private int flightMs;
    public WavyMissile(int x, int y, int vy, int damage, double phase) { super(x, y, 0, vy, 14, damage); centerX = x; this.phase = phase; }
    @Override public void update() {
        flightMs += 16;
        y += vy;
        // Smart Missile steering changes the stable centerline, not the wave itself.
        centerX += vx;
        x = centerX + (int) Math.round(Math.sin(flightMs / 1000.0 * 7.0 + phase) * 36.0);
    }
}
