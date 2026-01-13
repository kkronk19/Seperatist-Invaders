package spaceinvaders.core.entities;

/** Marker bullet fired by enemies (so PlayMode can hurt the player). */
public class EnemyBullet extends Bullet {
    public EnemyBullet(int x, int y, int vx, int vy, int size, int damage) {
        super(x, y, vx, vy, size, damage, BulletKind.BASIC);
    }
}
