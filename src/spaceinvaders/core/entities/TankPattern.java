package spaceinvaders.core.entities;

/**
 * Tank: slow, steady descent. Optional tiny horizontal drift.
 */
public class TankPattern implements MovementPattern {

    private static final int VY = 1;     // slow drop
    private static final int VX = 0;     // keep 0 for now (can add drift later)

    @Override
    public void update(Invader inv, long dtMs, int panelW, int panelH) {
        inv.vx = VX;
        inv.vy = VY;

        inv.x += inv.vx;
        inv.y += inv.vy;

        // clamp (just in case)
        if (inv.x < 0) inv.x = 0;
        if (inv.x + inv.width > panelW) inv.x = panelW - inv.width;
    }
}
