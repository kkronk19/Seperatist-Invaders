package spaceinvaders.weapons;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Bullet.BulletKind;

import java.util.Random;

public class RadialShrapnelEffect implements ImpactEffect {
    private final int count;
    private final double minSpeed;
    private final double maxSpeed;
    private final Random rng = new Random();

    public RadialShrapnelEffect(int count, double minSpeed, double maxSpeed) {
        this.count = count;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
    }

    @Override
    public void apply(GameState state, int cx, int cy) {
        for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double speed = minSpeed + rng.nextDouble() * (maxSpeed - minSpeed);
            int vx = (int) Math.round(Math.cos(angle) * speed);
            int vy = (int) Math.round(Math.sin(angle) * speed);

            Bullet b = new Bullet(
                cx,
                cy,
                vx,
                vy,
                6,  // small fragment size
                1,  // base blaster damage
                BulletKind.BASIC
            );

            state.bullets.add(b);
        }
    }
}
