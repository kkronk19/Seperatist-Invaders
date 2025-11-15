package spaceinvaders.weapons;

import spaceinvaders.core.entities.Bullet;
import java.util.List;

public class BlasterWeapon implements Weapon {
    private long lastShot = 0L;
    private final int cooldownMs = 180;

    @Override
    public void tryFire(long nowMs, int mx, int my, boolean isHeld, List<Bullet> out) {
        if (!isHeld) return;                 // Space: hold-to-fire
        if (nowMs - lastShot < cooldownMs) return;
        lastShot = nowMs;
        out.add(new Bullet(mx - 2, my, 0, -10, 8, 1, Bullet.BulletKind.BASIC));
    }
}
