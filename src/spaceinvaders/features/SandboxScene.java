package spaceinvaders.features;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import spaceinvaders.core.GameState;
import spaceinvaders.core.Scene;
import spaceinvaders.core.SceneManager;
import spaceinvaders.core.entities.Blade;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Invader;
import spaceinvaders.core.entities.Player;
import spaceinvaders.core.entities.ShooterZigZagPattern;
import spaceinvaders.core.entities.SwarmerZigZagPattern;
import spaceinvaders.core.systems.CollisionSystem;
import spaceinvaders.core.systems.InvaderAttackSystem;
import spaceinvaders.core.systems.SmartTargetingSystem;
import spaceinvaders.input.FireController;
import spaceinvaders.input.SandboxCloneController;
import spaceinvaders.services.audio.AudioManager;
import spaceinvaders.services.loading.AssetLoader;
import spaceinvaders.weapons.BlasterWeapon;
import spaceinvaders.weapons.MissileWeapon;

/** Scene for free testing of weapons and enemies. */
public class SandboxScene implements Scene {

    private final GameState state;
    private final SceneManager scenes;
    private final Random rng = new Random();

    private FireController fire;
    private Player player;

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Invader> invaders = new ArrayList<>();

    // clones (sandbox AI)
    private final List<SandboxCloneController> clones = new ArrayList<>();
    private Image cloneImg; // optional

    private boolean moveLeft, moveRight;
    private long nextSpawnMs = 0;

    // Blade cooldown (player blade when pressing F)
    private int bladeCooldownMs = 0;
    private static final int BLADE_COOLDOWN = 850;

    // base blade stats
    private static final int BASE_BLADE_PIERCE  = 3;
    private static final int BASE_BLADE_BOUNCES = 3;

    public SandboxScene(GameState state, SceneManager scenes) {
        this.state = state;
        this.scenes = scenes;
    }

    private List<Bullet> spawnShrapnel(int cx, int cy) {
        final int baseShards = 5;
        final int size = 6;
        final int baseDmg = 1;
        final int speed = 6;

        int shards = baseShards;
        int dmg = baseDmg;

        List<Bullet> out = new ArrayList<>(shards);
        for (int i = 0; i < shards; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            int vx = (int) Math.round(Math.cos(angle) * speed);
            int vy = (int) Math.round(Math.sin(angle) * speed);
            out.add(new Bullet(cx, cy, vx, vy, size, dmg, Bullet.BulletKind.BASIC));
        }

        try {
            AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/exp_ord_rocket_small01.wav", 0.66f);
        } catch (Throwable ignored) {}

        return out;
    }

    @Override
    public void onEnter() {
        System.out.println("[Sandbox] Entered");
        state.mode = GameState.AppMode.SANDBOX;
        state.playerX = state.width / 2 - state.playerWidth / 2;

        player = state.player;

        fire = new FireController(player, new BlasterWeapon(player), new MissileWeapon(player));

        // LOAD IMAGES ONCE
        try {
            state.invaderImageBasic    = AssetLoader.imageFromResource("image/b1_droid.png");
            state.invaderImageTank     = AssetLoader.imageFromResource("image/aat.png");
            state.invaderImageShielded = AssetLoader.imageFromResource("image/droideka.png");
            state.invaderImageShooter  = AssetLoader.imageFromResource("image/bx_commando_droid.png");
            state.invaderImageSwarmer  = AssetLoader.imageFromResource("image/buzz_droid.png");

            // clone sprite (optional)
            cloneImg = AssetLoader.imageFromResource("image/p1_clone.png");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try { AudioManager.get().stopLoop("menu"); } catch (Throwable ignored) {}

        // sync initial clone count
        syncClonesToUpgrade();
    }

    @Override
    public void onExit() {
        System.out.println("[Sandbox] Exited");
    }

    /** Keep sandbox clones count exactly equal to player.upCloneReinforcement (0..5). */
    private void syncClonesToUpgrade() {
        if (player == null) return;

        int desired = Math.max(0, Math.min(5, player.upCloneReinforcement));

        // add missing
        while (clones.size() < desired) {
            int sx = rng.nextInt(Math.max(1, state.width - state.playerWidth));
            int sy = rng.nextInt(Math.max(1, state.height - state.playerHeight - 120));
            clones.add(new SandboxCloneController(sx, sy, System.nanoTime() ^ rng.nextLong()));
        }

        // remove extras
        while (clones.size() > desired) {
            clones.remove(clones.size() - 1);
        }
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

            case KeyEvent.VK_R:
                if (fire != null) fire.triggerSecondaryTap();
                break;

            case KeyEvent.VK_F:
                tryFireBlades();
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

    /** Player blade firing (F) */
    private void tryFireBlades() {
        if (player == null) return;
        if (bladeCooldownMs > 0) return;

        int muzzleX = state.playerX + state.playerWidth / 2;
        int muzzleY = state.height - state.playerHeight - 10;

        int vx = 14;
        int vy = -4;
        int size = 10;

        int pierce  = BASE_BLADE_PIERCE  + player.bladeExtraPierce();
        int bounces = BASE_BLADE_BOUNCES + player.bladeExtraBounces();
        boolean ap  = player.upBladeArmorPen;

        boolean verticalBounce = player.upBladeVerticalBounce;
        boolean legendarySplit = player.upBladeLegendarySplit;

        bullets.add(new Blade(muzzleX, muzzleY, -vx, vy, size, bounces, pierce, legendarySplit, verticalBounce, ap));
        bullets.add(new Blade(muzzleX, muzzleY, +vx, vy, size, bounces, pierce, legendarySplit, verticalBounce, ap));

        bladeCooldownMs = BLADE_COOLDOWN;

        try { AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/wpn_blade_fire.wav", 0.25f); }
        catch (Throwable ignored) {}
    }

    @Override
    public void update(double dtMillis) {
        long now = System.currentTimeMillis();
        int dtMs = (int) Math.max(1, Math.round(dtMillis));

        // keep clones in sync with menu every frame
        syncClonesToUpgrade();

        // Blade cooldown
        if (bladeCooldownMs > 0) bladeCooldownMs -= dtMs;

        // Player movement
        int step = (player != null) ? player.speedPx : 8;
        if (moveLeft)  state.playerX -= step;
        if (moveRight) state.playerX += step;
        state.playerX = Math.max(0, Math.min(state.playerX, state.width - state.playerWidth));

        // Player fire
        int muzzleX = state.playerX + state.playerWidth / 2;
        int muzzleY = state.height - state.playerHeight - 10;

        if (fire != null) {
            List<Bullet> spawned = fire.tick(now, muzzleX, muzzleY, bullets, state.width, state.height);

            if (!spawned.isEmpty()) {
                // SFX for player weapons (scene-owned)
                for (Bullet b : spawned) {
                    try {
                        if (b.kind == Bullet.BulletKind.MISSILE) {
                            AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/wpn_ywing_torpedo_fire.wav", 0.18f);
                        } else {
                            AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/ct_blaster_fire.wav", 0.15f);
                        }
                    } catch (Throwable ignored) {}
                }
                bullets.addAll(spawned);
            }
        }

        // Clone AI tick (random walk + firing). Controller decides weapons based on player upgrades.
        int cloneW = state.playerWidth;
        int cloneH = state.playerHeight;
        for (SandboxCloneController c : clones) {
            c.tick(dtMs, state.width, state.height, cloneW, cloneH, player, bullets);
        }

        // Bullet movement
        List<Bullet> pendingAdds = new ArrayList<>();

        for (Iterator<Bullet> it = bullets.iterator(); it.hasNext();) {
            Bullet b = it.next();

            if (b instanceof Blade blade) {
                List<Bullet> spawned = blade.updateBlade(dtMs, state.width, state.height);
                if (spawned != null && !spawned.isEmpty()) pendingAdds.addAll(spawned);
                if (blade.isDead()) it.remove();
                continue;
            }

            b.update();
            if (b.isOffScreen(state.width, state.height)) it.remove();
        }

        if (!pendingAdds.isEmpty()) bullets.addAll(pendingAdds);

        // Invader spawning
        if (now > nextSpawnMs) {
            int x = rng.nextInt(Math.max(1, state.width - 60));
            double roll = rng.nextDouble();

            if (roll < 0.22) {
                invaders.add(new Invader(x, -40, 40, 40, Invader.InvaderKind.BASIC, null));

            } else if (roll < 0.44) {
                invaders.add(new Invader(x, -40, 40, 40, Invader.InvaderKind.SHIELDED, null));

            } else if (roll < 0.62) {
                invaders.add(new Invader(x, -40, 26, 26, Invader.InvaderKind.SWARMER, new SwarmerZigZagPattern()));

            } else if (roll < 0.82) {
                Invader s = new Invader(x, -40, 40, 40, Invader.InvaderKind.SHOOTER, new ShooterZigZagPattern());
                s.hp = 1;
                s.touchDamage = 1;
                s.scoreValue = 10;
                s.vy = 1;
                invaders.add(s);

            } else {
                Invader t = new Invader(x, -60, 56, 56, Invader.InvaderKind.TANK, null);
                t.hp = 6;
                t.vy = 1;
                t.scoreValue = 40;
                t.touchDamage = 2;
                t.armored = true;
                invaders.add(t);
            }

            nextSpawnMs = now + 610 + rng.nextInt(600);
        }

        // Invader update + cleanup
        for (Iterator<Invader> it = invaders.iterator(); it.hasNext();) {
            Invader inv = it.next();
            inv.update(dtMs, state.width, state.height);

            if (inv.shieldBreakFlashMs > 0) {
                inv.shieldBreakFlashMs = Math.max(0, inv.shieldBreakFlashMs - dtMs);
            }

            if (inv.y > state.height + 50) it.remove();
        }

        // Shooter attacks
        InvaderAttackSystem.spawnShooterBullets(invaders, bullets);

        // Smart targeting update (affects bullets/missiles with flags set)
        SmartTargetingSystem.update(bullets, invaders);

        // Collisions
        CollisionSystem.bulletsVsInvaders(bullets, invaders, player, this::spawnShrapnel);
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // bullets
        for (Bullet b : bullets) {
            spaceinvaders.core.render.BulletRenderers.render(g, b, state);
        }

        // invaders
        for (Invader inv : invaders) {
            spaceinvaders.core.render.InvaderRenderer.render(g, inv, state);
        }

        // clones
        int cw = state.playerWidth;
        int ch = state.playerHeight;
        for (SandboxCloneController c : clones) {
            int cx = c.getX();
            int cy = c.getY();
            if (cloneImg != null) g.drawImage(cloneImg, cx, cy, cw, ch, null);
            else {
                g.setColor(new Color(180, 220, 255));
                g.fillRect(cx, cy, cw, ch);
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
