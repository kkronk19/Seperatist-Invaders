package spaceinvaders.features;

import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.GameState;
import spaceinvaders.core.Scene;
import spaceinvaders.core.SceneManager;
import spaceinvaders.input.FireController;
import spaceinvaders.weapons.BlasterWeapon;
import spaceinvaders.weapons.MissileWeapon;
import spaceinvaders.services.audio.AudioManager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Scene for free testing of weapons and enemies. */
public class SandboxScene implements Scene {

    private final GameState state;
    private final SceneManager scenes;
    private final Random rng = new Random();

    private FireController fire;

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<GameState.Invader> invaders = new ArrayList<>();

    private boolean moveLeft, moveRight;
    private long nextSpawnMs = 0;

    // CHANGED: return shards instead of mutating bullets during iteration
    private List<Bullet> spawnShrapnel(int cx, int cy) {
        final int shards = 5;
        final int size   = 6;
        final int dmg    = 1;
        final int speed  = 6;

        List<Bullet> out = new ArrayList<>(shards);
        for (int i = 0; i < shards; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            int vx = (int) Math.round(Math.cos(angle) * speed);
            int vy = (int) Math.round(Math.sin(angle) * speed);
            out.add(new Bullet(cx, cy, vx, vy, size, dmg, Bullet.BulletKind.BASIC));
        }

        AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/exp_ord_rocket_small01.wav", 0.66f);
        return out;
    }

    public SandboxScene(GameState state, SceneManager scenes) {
        this.state = state;
        this.scenes = scenes;
    }

    @Override
    public void onEnter() {
        System.out.println("[Sandbox] Entered");
        state.mode = GameState.AppMode.SANDBOX;
        state.playerX = state.width / 2 - state.playerWidth / 2;

        // Weapons: Space (primary hold), R (secondary tap)
        fire = new FireController(new BlasterWeapon(), new MissileWeapon());

        // Safe even if nothing is playing
        try { AudioManager.get().stopLoop("menu"); } catch (Throwable ignored) {}
    }

    @Override
    public void onExit() {
        System.out.println("[Sandbox] Exited");
    }

    @Override
    public void handleKeyPressed(int key) {
        switch (key) {
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:  moveLeft = true;  break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT: moveRight = true; break;

            case KeyEvent.VK_SPACE:
                if (fire != null) fire.setPrimaryHeld(true);
                break;

            case KeyEvent.VK_R:     // missile tap
                if (fire != null) fire.triggerSecondaryTap();
                break;
        }
    }

    @Override
    public void handleKeyReleased(int key) {
        switch (key) {
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:  moveLeft = false;  break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT: moveRight = false; break;

            case KeyEvent.VK_SPACE:
                if (fire != null) fire.setPrimaryHeld(false);
                break;
        }
    }

    @Override
    public void update(double dtMillis) {
        long now = System.currentTimeMillis();

        // --- Player movement ---
        if (moveLeft)  state.playerX -= 8;
        if (moveRight) state.playerX += 8;
        state.playerX = Math.max(0, Math.min(state.playerX, state.width - state.playerWidth));

        // --- Fire (run BEFORE the generic bullet update so missiles get their snake x) ---
        int muzzleX = state.playerX + state.playerWidth / 2;
        int muzzleY = state.height - state.playerHeight - 10;

        if (fire != null) {
            List<Bullet> spawned = fire.tick(now, muzzleX, muzzleY, bullets, state.width, state.height);

            // SFX for newly spawned bullets (guarded for missing assets)
            if (!spawned.isEmpty()) {
                for (Bullet b : spawned) {
                    try {
                        if (b.kind == Bullet.BulletKind.MISSILE) {
                            AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/wpn_ywing_torpedo_fire.wav", 0.18f);
                        } else {
                            AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/wpn_cis_blaster_fire.wav", 0.15f);
                        }
                    } catch (Throwable ignored) {}
                }
                bullets.addAll(spawned);
            }
        }

        // --- Bullet movement ---
        for (Iterator<Bullet> it = bullets.iterator(); it.hasNext();) {
            Bullet b = it.next();
            b.update(); // applies vx, vy (missile x is already set in weapon update)
            if (b.isOffScreen(state.width, state.height)) it.remove();
        }

        // --- Invader spawning ---
        if (now > nextSpawnMs) {
            int x = rng.nextInt(Math.max(1, state.width - 50));
            invaders.add(new GameState.Invader(x, -40, 40));
            nextSpawnMs = now + 800 + rng.nextInt(600);
        }

        // --- Invader movement ---
        for (Iterator<GameState.Invader> it = invaders.iterator(); it.hasNext();) {
            GameState.Invader inv = it.next();
            inv.y += 3;
            if (inv.y > state.height + 50) it.remove();
        }

        // --- Collisions ---
        Rectangle br = new Rectangle();
        Rectangle ir = new Rectangle();

        // CHANGED: collect adds to avoid modifying bullets during iteration
        List<Bullet> pendingAdds = new ArrayList<>();

        for (Iterator<Bullet> bit = bullets.iterator(); bit.hasNext();) {
            Bullet b = bit.next();
            br.setBounds(b.x, b.y, b.size, b.size);
            boolean hit = false;

            for (Iterator<GameState.Invader> iit = invaders.iterator(); iit.hasNext();) {
                GameState.Invader inv = iit.next();
                ir.setBounds(inv.x, inv.y, inv.size, inv.size);
                if (br.intersects(ir)) {
                    iit.remove();
                    hit = true;

                    // if this was a missile, explode into shrapnel at impact point
                    if (b.kind == Bullet.BulletKind.MISSILE) {
                        int cx = b.x + b.size / 2;
                        int cy = b.y + b.size / 2;
                        pendingAdds.addAll(spawnShrapnel(cx, cy)); // collect, don't add now
                    }

                    AudioManager.get().playRandomSfx(
                        1.1f,
                        "/spaceinvaders/resources/audio/sfx/CICOM401.wav",
                        "/spaceinvaders/resources/audio/sfx/CICOM408.wav",
                        "/spaceinvaders/resources/audio/sfx/CICOM409.wav"
                    );
                    break;
                }
            }
            if (hit) bit.remove();
        }

        // CHANGED: add shards only after iterating
        if (!pendingAdds.isEmpty()) {
            bullets.addAll(pendingAdds);
        }
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        // background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // bullets via renderers
        for (Bullet b : bullets) {
            spaceinvaders.core.render.BulletRenderers.render(g, b, state);
        }

        // invaders
        Image invaderImg = state.invaderImage;
        for (GameState.Invader inv : invaders) {
            if (invaderImg != null) {
                g.drawImage(invaderImg, inv.x, inv.y, inv.size, inv.size, null);
            } else {
                g.setColor(Color.GREEN);
                g.fillRect(inv.x, inv.y, inv.size, inv.size);
            }
        }

        // legacy bullet shapes/image (kept intact)
        for (Bullet b : bullets) {
            if (state.bulletType == GameState.BulletType.IMAGE && state.bulletImage != null) {
                g.drawImage(state.bulletImage, b.x, b.y, b.size, b.size, null);
                continue;
            }

            Color c;
            switch (state.bulletType) {
                case TRIANGLE: c = new Color(100, 180, 255); break;
                case CIRCLE:   c = new Color(255, 230, 120); break;
                case SQUARE:   c = new Color(230, 250, 255); break;
                default:       c = Color.YELLOW;
            }
            g.setColor(c);

            switch (state.bulletType) {
                case CIRCLE:
                    g.fillOval(b.x, b.y, b.size, b.size);
                    break;
                case SQUARE:
                    g.fillRect(b.x, b.y, b.size, b.size);
                    break;
                case TRIANGLE:
                default: {
                    int s = b.size;
                    int[] xs = { b.x + s / 2, b.x, b.x + s };
                    int[] ys = { b.y, b.y + s, b.y + s };
                    g.fillPolygon(xs, ys, 3);
                    break;
                }
            }
        }

        // player
        Image playerImg = state.playerImage;
        int px = state.playerX;
        int py = height - state.playerHeight - 10;
        if (playerImg != null) {
            g.drawImage(playerImg, px, py, state.playerWidth, state.playerHeight, null);
        } else {
            g.setColor(Color.CYAN);
            g.fillRect(px, py, state.playerWidth, state.playerHeight);
        }
    }
}
