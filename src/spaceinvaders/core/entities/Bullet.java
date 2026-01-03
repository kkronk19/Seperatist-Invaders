package spaceinvaders.core.entities;

public class Bullet {
    public enum BulletKind { BASIC, MISSILE, BLADE } // <- include New bullet kinds here

    public int x, y;
    public int vx, vy;
    public int size;
    public int damage;
    public BulletKind kind;

    public Bullet(int x, int y, int vx, int vy, int size, int damage, BulletKind kind) {
        this.x = x; this.y = y;
        this.vx = vx; this.vy = vy;
        this.size = size;
        this.damage = damage;
        this.kind = kind;
    }

    public void update() {
        x += vx;
        y += vy;
    }

    public boolean isOffScreen(int w, int h) {
        return x < -size || x > w + size || y < -size || y > h + size;
    }
}
