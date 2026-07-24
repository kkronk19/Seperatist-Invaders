package spaceinvaders.core.entities;

/** Dark hostile rocket with a deterministic purple-wavering flight path. */
public final class HailfireRocket extends EnemyBullet {
    private final double phase;
    public HailfireRocket(int x, int y, int vx, int vy, double phase) { super(x, y, vx, vy, 12, 5); this.phase = phase; }
    @Override public void update() { x += vx + (int) Math.round(Math.sin((y + phase) * .075) * 2); y += vy; }
}
