package spaceinvaders.features;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Bullet.BulletKind;
import spaceinvaders.services.audio.AudioManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Self-contained animation for the title/start menu background. */
public class StartMenuDemo {

    // Player sprites (just positions/sizes; the actual image is loaded here for the demo)
    private int leftX, rightX, floorY;
    private final int playerW = 50;
    private final int playerH = 60;

    private Image playerImage;
    private Image bulletImage;
    private Image invaderImage;
    private Image starsImage;

    public Image starsImage() { return starsImage; }
    public Image playerImage() { return playerImage; }
    public Image bulletImage() { return bulletImage; }
    public Image invaderImage() { return invaderImage; }

    // Demo entities (typed; owned internally)
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<GameState.Invader> invaders = new ArrayList<>();

    // timing & motion
    private final Random rng = new Random();
    private int fireMs   = 0;
    private int spawnMs  = 0;
    private int dirMs    = 0;
    private int dirNext  = 1500;          // randomized to 1000–6000ms
    private int dxL = 1, dxR = -1;        // -1/0/1
    private int fireNextMs = 180;         // 180–360 each time
    private final int speed = 2;

    /** Call once at startup. Fractions are 0..1 positions across screen width. */
    public void init(int width, int height, double leftFrac, double rightFrac) {
        int maxX = Math.max(1, width - playerW);
        leftX  = (int)Math.round(clamp01(leftFrac)  * maxX);
        rightX = (int)Math.round(clamp01(rightFrac) * maxX);
        floorY = Math.max(0, height - playerH);
        dirNext = 1000 + rng.nextInt(5001); // 1000–6000
        fireNextMs = 180 + rng.nextInt(181);

        // menu music loop (safe even if resource missing)
        try {
            AudioManager.get().playLoop("menu", "/spaceinvaders/resources/audio/the_battle_in_the_snow.wav");
        } catch (Throwable ignored) {}

    try {
        playerImage = ImageIO.read(getClass().getResource("/spaceinvaders/resources/image/p1_clone.png"));
        bulletImage = ImageIO.read(getClass().getResource("/spaceinvaders/resources/image/blaster_bolt.png"));
        invaderImage = ImageIO.read(getClass().getResource("/spaceinvaders/resources/image/b1_droid.png"));
        starsImage  = ImageIO.read(getClass().getResource("/spaceinvaders/resources/image/title_background.png"));
    } catch (java.io.IOException | IllegalArgumentException e) {
        System.err.println("Warning: could not load start-menu sprites: " + e.getMessage());
    }
    }

    /** Call on every resize so positions scale with the window size. */
    public void onResize(int width, int height, double leftFrac, double rightFrac) {
        int maxX = Math.max(1, width - playerW);
        leftX  = (int)Math.round(clamp01(leftFrac)  * maxX);
        rightX = (int)Math.round(clamp01(rightFrac) * maxX);
        floorY = Math.max(0, height - playerH);
    }

    private static double clamp01(double v){ return v < 0 ? 0 : (v > 1 ? 1 : v); }

    /** Advance the simulation by frameMs milliseconds. */
    public void update(int frameMs, int width, int height) {
        // Change horizontal directions every 1–6s
        dirMs += frameMs;
        if (dirMs >= dirNext) {
            dxL = rng.nextBoolean() ? 1 : -1;
            dxR = rng.nextBoolean() ? 1 : -1;
            dirMs  = 0;
            dirNext = 1000 + rng.nextInt(5001);
        }

        int maxX = Math.max(0, width - playerW);
        floorY = Math.max(0, height - playerH);

        // Move
        leftX  += dxL * speed;
        rightX += dxR * speed;

        // Bounce on walls
        if (leftX <= 0)           { leftX = 0;      dxL =  1; }
        else if (leftX >= maxX)   { leftX = maxX;   dxL = -1; }

        if (rightX <= 0)          { rightX = 0;     dxR =  1; }
        else if (rightX >= maxX)  { rightX = maxX;  dxR = -1; }

        // Fire every ~180–360 ms (randomized each time)
        fireMs += frameMs;
        if (fireMs >= fireNextMs) {
            // Create upward-moving basic bullets using top-level Bullet
            bullets.add(new Bullet(leftX + playerW / 2, floorY, 0, -10, 6, 1, BulletKind.BASIC));
            bullets.add(new Bullet(rightX + playerW / 2, floorY, 0, -10, 6, 1, BulletKind.BASIC));

            try {
                AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/wpn_cis_blaster_fire.wav", 0.12f);
            } catch (Throwable ignored) {}

            fireMs = 0;
            fireNextMs = 180 + rng.nextInt(181);
        }

        // Spawn invaders ~every 550 ms
        spawnMs += frameMs;
        if (spawnMs >= 550) {
            int size = 40, margin = 160;
            int usable = Math.max(1, width - margin * 2 - size);
            int x = margin + rng.nextInt(usable);
            invaders.add(new GameState.Invader(x, -size, size));
            spawnMs = 0;
        }

        // Advance bullets/invaders & cull
        for (int i = 0; i < bullets.size(); i++) bullets.get(i).update();
        for (int i = bullets.size() - 1; i >= 0; i--)
            if (bullets.get(i).isOffScreen(width, height)) bullets.remove(i);

        for (int i = 0; i < invaders.size(); i++) invaders.get(i).y += 2;
        for (int i = invaders.size() - 1; i >= 0; i--)
            if (invaders.get(i).y > height) invaders.remove(i);

        // Collision detection + death SFX
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            Rectangle br = new Rectangle(b.x - 5, b.y - 5, 10, 10);
            boolean hit = false;
            for (int j = invaders.size() - 1; j >= 0 && !hit; j--) {
                GameState.Invader v = invaders.get(j);
                Rectangle ir = new Rectangle(v.x, v.y, v.size, v.size);
                if (br.intersects(ir)) {
                    bullets.remove(i);
                    invaders.remove(j);
                    hit = true;

                    try {
                        AudioManager.get().playRandomSfx(
                            1.10f,
                            "/spaceinvaders/resources/audio/sfx/CICOM401.wav",
                            "/spaceinvaders/resources/audio/sfx/CICOM408.wav",
                            "/spaceinvaders/resources/audio/sfx/CICOM409.wav"
                        );
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    // ---- Read-only snapshots for painting (thread-safe for EDT) ----
    public List<GameState.Invader> invadersSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(invaders));
    }

    public List<Bullet> bulletsSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(bullets));
    }

    // ---- Geometry getters ----
    public int leftX()  { return leftX; }
    public int rightX() { return rightX; }
    public int floorY() { return floorY; }
    public int playerW(){ return playerW; }
    public int playerH(){ return playerH; }
}
