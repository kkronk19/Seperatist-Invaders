package spaceinvaders.weapons;

import spaceinvaders.core.entities.Bullet;
import java.util.List;

public interface Weapon {
    void tryFire(long nowMs, int muzzleX, int muzzleY, boolean isHeld, List<Bullet> out);
    default void updateBullets(long nowMs, List<Bullet> bullets, int panelW, int panelH) {}
}
