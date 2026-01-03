package spaceinvaders.features;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.services.audio.AudioManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Title/start menu background demo:
 * - Loads images + plays music
 * - Spawns/moves invaders for ambiance
 *
 * NOTE: Clone movement + bullets are now controlled externally (NpcController),
 * so StartMenuDemo no longer owns bullets or player movement timers.
 */
public class StartMenuDemo {

    // Player anchor positions (used by GamePanel as "spawn points" for title clones)
    private int leftX, rightX, floorY;
    private final int playerW = 50;
    private final int playerH = 60;

    private Image playerImage;
    private Image bulletImage;
    private Image invaderImage;
    private Image starsImage;

    public Image starsImage()  { return starsImage; }
    public Image playerImage() { return playerImage; }
    public Image bulletImage() { return bulletImage; }
    public Image invaderImage(){ return invaderImage; }

    // Invaders only (owned internally)
    private final List<GameState.Invader> invaders = new ArrayList<>();

    // timing
    private final Random rng = new Random();
    private int spawnMs = 0;

    // Call from GamePanel after bullets/invaders update.
    // Uses demo's internal invader list and plays death SFX.
    public void handleTitleCollisions(List<Bullet> bullets) {
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

    /** Call once at startup. Fractions are 0..1 positions across screen width. */
    public void init(int width, int height, double leftFrac, double rightFrac) {
        updateAnchors(width, height, leftFrac, rightFrac);

        // menu music loop
        try {
            AudioManager.get().playLoop("menu", "/spaceinvaders/resources/audio/the_battle_in_the_snow.wav");
        } catch (Throwable ignored) {}

        try {
            playerImage  = ImageIO.read(getClass().getResource("/spaceinvaders/resources/image/p1_clone.png"));
            bulletImage  = ImageIO.read(getClass().getResource("/spaceinvaders/resources/image/blaster_bolt.png"));
            invaderImage = ImageIO.read(getClass().getResource("/spaceinvaders/resources/image/b1_droid.png"));
            starsImage   = ImageIO.read(getClass().getResource("/spaceinvaders/resources/image/title_background.png"));
        } catch (java.io.IOException | IllegalArgumentException e) {
            System.err.println("Warning: could not load start-menu sprites: " + e.getMessage());
        }
    }

    /** Call on resize (keeps anchor positions consistent with layout fractions). */
    public void onResize(int width, int height, double leftFrac, double rightFrac) {
        updateAnchors(width, height, leftFrac, rightFrac);
    }

    private void updateAnchors(int width, int height, double leftFrac, double rightFrac) {
        int maxX = Math.max(1, width - playerW);
        leftX  = (int) Math.round(clamp01(leftFrac)  * maxX);
        rightX = (int) Math.round(clamp01(rightFrac) * maxX);
        floorY = Math.max(0, height - playerH);
    }

    private static double clamp01(double v){ return v < 0 ? 0 : (v > 1 ? 1 : v); }

    /** Advance only invader ambiance by frameMs milliseconds. */
    public void update(int frameMs, int width, int height) {
        floorY = Math.max(0, height - playerH);

        // Spawn invaders ~every 550 ms
        spawnMs += frameMs;
        if (spawnMs >= 550) {
            int size = 40, margin = 160;
            int usable = Math.max(1, width - margin * 2 - size);
            int x = margin + rng.nextInt(usable);
            invaders.add(new GameState.Invader(x, -size, size));
            spawnMs = 0;
        }

        // Move invaders downward and cull
        for (int i = 0; i < invaders.size(); i++) invaders.get(i).y += 2;
        for (int i = invaders.size() - 1; i >= 0; i--)
            if (invaders.get(i).y > height) invaders.remove(i);
    }

    // ---- Read-only snapshots for painting ----
    public List<GameState.Invader> invadersSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(invaders));
    }

    // ---- Geometry getters ----
    public int leftX()   { return leftX; }
    public int rightX()  { return rightX; }
    public int floorY()  { return floorY; }
    public int playerW() { return playerW; }
    public int playerH() { return playerH; }
}
