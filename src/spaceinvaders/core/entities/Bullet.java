package spaceinvaders.core.entities;

public class Bullet {

    public enum BulletKind { BASIC, MISSILE, BLADE }

    public int x, y;
    public int vx, vy;
    public int size;
    public int damage;
    public BulletKind kind;

    public boolean explodesOnHit = false;

    // ---- upgrade-enabled gameplay fields ----
    public int pierce = 0;                 // how many additional enemies it can pass through
    public boolean armorPiercing = false;  // ignores armored resistance rules

    // ---- basic legendary flags (logic later) ----
    public boolean smartTargeting = false;
    public boolean moneyShot = false;      // doubles points if it gets the kill
    public boolean systemsFried = false;   // proc handled later
    public boolean overtuned = false;      // proc handled later

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
