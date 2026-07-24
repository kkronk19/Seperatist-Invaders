package spaceinvaders.core.entities;

/** Allied support fire, deliberately independent of player weapon scaling. */
public final class CloneBullet extends Bullet {
    public CloneBullet(int x, int y, int vx, int vy, int size, int damage) {
        super(x, y, vx, vy, size, damage, BulletKind.BASIC);
    }
}
