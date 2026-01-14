package spaceinvaders.input;

import java.util.List;
import java.util.Random;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Bullet.BulletKind;
import spaceinvaders.services.audio.AudioManager;

/** Controls ONE title-screen clone. Spawn two instances for two clones. */
public final class NpcController {

    private final Random rng;

    private int x;
    private int dx;                 // -1 or +1
    private final int speed = 2;    // matches your old demo feel

    // direction change timing (like old demo: 1–6s)
    private int dirMs = 0;
    private int dirNextMs;

    // firing timing (like old demo: 180–360ms)
    private int fireMs = 0;
    private int fireNextMs;

    public NpcController(int startX, long seed) {
        this.x = startX;
        this.rng = new Random(seed);

        this.dx = rng.nextBoolean() ? 1 : -1;

        this.dirNextMs  = 1000 + rng.nextInt(5001); // 1000–6000
        this.fireNextMs = 180  + rng.nextInt(181);  // 180–360

        // desync at spawn
        this.dirMs  = rng.nextInt(dirNextMs);
        this.fireMs = rng.nextInt(fireNextMs);
    }

    /**
     * Call every frame while in START_MENU.
     *
     * @param frameMs    delta time (your FRAME_TIME)
     * @param worldW     virtual width
     * @param playerW    clone sprite width
     * @param floorY     y where clone stands
     * @param outBullets list to append bullets into
     */
    public void tick(int frameMs, int worldW, int playerW, int floorY, List<Bullet> outBullets) {
        // Change direction every 1–6s
        dirMs += frameMs;
        if (dirMs >= dirNextMs) {
            dx = rng.nextBoolean() ? 1 : -1;
            dirMs = 0;
            dirNextMs = 1000 + rng.nextInt(5001);
        }

        // Move + bounce on walls
        int maxX = Math.max(0, worldW - playerW);
        x += dx * speed;

        if (x <= 0) { x = 0; dx = 1; }
        else if (x >= maxX) { x = maxX; dx = -1; }

        // Fire every ~180–360ms
        fireMs += frameMs;
        if (fireMs >= fireNextMs) {
            final int size = 8;

            int muzzleCenterX = x + playerW / 2;
            int muzzleY = floorY - 12; // slightly above the feet/ground line

            // IMPORTANT: Bullet uses TOP-LEFT coordinates
            outBullets.add(new Bullet(
                    muzzleCenterX - size / 2,
                    muzzleY,
                    0,
                    -10,
                    size,
                    1,
                    BulletKind.BASIC
            ));

            try {
                AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/ct_blaster_fire.wav", 0.12f);
            } catch (Throwable ignored) {}

            fireMs = 0;
            fireNextMs = 180 + rng.nextInt(181);
        }
    }

    public int getX() { return x; }
}
