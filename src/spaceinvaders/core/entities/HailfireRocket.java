package spaceinvaders.core.entities;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A deterministic, momentum-preserving Hailfire rocket.  It picks a broad
 * lower-field destination once, then makes bounded course arcs instead of
 * recalculating a direction to the player every frame.
 */
public final class HailfireRocket extends EnemyBullet {
    private double heading;
    private double speed;
    private final int firstTurnMs, secondTurnMs;
    private final double firstTurnRate, secondTurnRate;
    private int flightMs;
    public final Deque<int[]> trail = new ArrayDeque<>();

    /** Compatibility constructor for existing callers. */
    public HailfireRocket(int x, int y, int vx, int vy, double seed) {
        this(x, y, x + vx * 90, y + Math.max(120, vy * 110), (long) (seed * 1000));
    }

    public HailfireRocket(int x, int y, int targetX, int targetY, long seed) {
        super(x, y, 0, 0, 12, 5);
        heading = Math.atan2(targetY - y, targetX - x) + signed(seed, 17) * .11;
        heading = clampDownward(heading);
        speed = 6.2 + Math.floorMod(seed, 3) * .45;
        firstTurnMs = 260 + Math.floorMod(seed * 31, 420);
        secondTurnMs = 900 + Math.floorMod(seed * 19, 560);
        firstTurnRate = signed(seed, 7) * .0065;
        secondTurnRate = signed(seed, 13) * .0045;
    }

    @Override public void update() {
        flightMs += 16;
        double turn = 0;
        if (flightMs >= firstTurnMs && flightMs < firstTurnMs + 430) turn += firstTurnRate;
        if (flightMs >= secondTurnMs && flightMs < secondTurnMs + 330) turn += secondTurnRate;
        heading = clampDownward(heading + turn);
        speed = Math.min(8.2, speed + .006);
        vx = (int) Math.round(Math.cos(heading) * speed);
        vy = Math.max(2, (int) Math.round(Math.sin(heading) * speed));
        x += vx;
        y += vy;
        trail.addFirst(new int[] { x + size / 2 - vx, y + size / 2 - vy });
        while (trail.size() > 60) trail.removeLast(); // approximately one second at the normal 60 Hz tick
    }

    private static double signed(long seed, int salt) { return (Math.floorMod(seed * salt, 2) == 0) ? -1 : 1; }
    private static double clampDownward(double value) { return Math.max(.18, Math.min(Math.PI - .18, value)); }
}
