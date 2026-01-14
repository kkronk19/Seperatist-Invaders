package spaceinvaders.features;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
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
import spaceinvaders.input.FireController;
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
    private Player player; // alias to state.player

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Invader> invaders = new ArrayList<>();

    private boolean moveLeft, moveRight;
    private long nextSpawnMs = 0;

    // --- Blade weapon config / upgrades (sandbox-local for now) ---
    private int bladeCooldownMs = 0;
    private static final int BLADE_COOLDOWN = 850;
    private static final int BLADE_PIERCE  = 3;
    private static final int BLADE_BOUNCES = 3;

    private boolean bladeVerticalBounce = false;
    private boolean bladeLegendarySplit = false;

    public SandboxScene(GameState state, SceneManager scenes) {
        this.state = state;
        this.scenes = scenes;
    }

    private List<Bullet> spawnShrapnel(int cx, int cy) {
        final int baseShards = 5;
        final int size = 6;
        final int baseDmg = 1;
        final int speed = 6;

        // If you later want missile shrapnel upgrades, read them from player here
        int shards = baseShards; // player.missileShrapnelCount(baseShards);
        int dmg = baseDmg;       // player.missileShrapnelDamage(baseDmg);

        List<Bullet> out = new ArrayList<>(shards);
        for (int i = 0; i < shards; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            int vx = (int) Math.round(Math.cos(angle) * speed);
            int vy = (int) Math.round(Math.sin(angle) * speed);

            Bullet b = new Bullet(cx, cy, vx, vy, size, dmg, Bullet.BulletKind.BASIC);
            out.add(b);
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

        // Use the ONE true player from GameState
        player = state.player;

        // Weapons read upgrades from Player
        fire = new FireController(player, new BlasterWeapon(player), new MissileWeapon(player));


        // ---------- LOAD IMAGES ONCE ----------
        try {
            state.invaderImageBasic    = AssetLoader.imageFromResource("image/b1_droid.png");
            state.invaderImageTank     = AssetLoader.imageFromResource("image/aat.png");
            state.invaderImageShielded = AssetLoader.imageFromResource("image/droideka.png");
            state.invaderImageShooter  = AssetLoader.imageFromResource("image/bx_commando_droid.png");
            state.invaderImageSwarmer  = AssetLoader.imageFromResource("image/buzz_droid.png");
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    /** Spawns two angled blades if off cooldown. */
    private void tryFireBlades() {
        if (bladeCooldownMs > 0) return;

        int muzzleX = state.playerX + state.playerWidth / 2;
        int muzzleY = state.height - state.playerHeight - 10;

        int vx = 14;
        int vy = -4;
        int size = 10;

        // If you want blade upgrades from Player soon:
        // int pierce = BLADE_PIERCE + player.bladeExtraPierce();
        // int bounces = BLADE_BOUNCES + player.bladeExtraBounces();
        // int dmg = player.bladeDamage(1);
        // boolean ap = player.upBladeArmorPen;

        bullets.add(new Blade(
                muzzleX, muzzleY,
                -vx, vy,
                size,
                BLADE_BOUNCES,
                BLADE_PIERCE,
                bladeLegendarySplit,
                bladeVerticalBounce,
                false
        ));

        bullets.add(new Blade(
                muzzleX, muzzleY,
                +vx, vy,
                size,
                BLADE_BOUNCES,
                BLADE_PIERCE,
                bladeLegendarySplit,
                bladeVerticalBounce,
                false
        ));

        bladeCooldownMs = BLADE_COOLDOWN;

        try {
            AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/wpn_blade_fire.wav", 0.25f);
        } catch (Throwable ignored) {}
    }

    @Override
    public void update(double dtMillis) {
        long now = System.currentTimeMillis();
        int dtMs = (int) Math.max(1, Math.round(dtMillis));

        // --- Blade cooldown ---
        if (bladeCooldownMs > 0) bladeCooldownMs -= dtMs;

        // --- Player movement (use player.speedPx) ---
        int step = (player != null) ? player.speedPx : 8;
        if (moveLeft)  state.playerX -= step;
        if (moveRight) state.playerX += step;
        state.playerX = Math.max(0, Math.min(state.playerX, state.width - state.playerWidth));

        // --- Fire ---
        int muzzleX = state.playerX + state.playerWidth / 2;
        int muzzleY = state.height - state.playerHeight - 10;

        if (fire != null) {
            List<Bullet> spawned = fire.tick(now, muzzleX, muzzleY, bullets, state.width, state.height);

            if (!spawned.isEmpty()) {
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

        // --- Bullet movement ---
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

        // --- Invader spawning ---
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

        // --- Invader movement + cleanup ---
        for (Iterator<Invader> it = invaders.iterator(); it.hasNext();) {
            Invader inv = it.next();

            inv.update(dtMs, state.width, state.height);

            if (inv.shieldBreakFlashMs > 0) {
                inv.shieldBreakFlashMs = Math.max(0, inv.shieldBreakFlashMs - dtMs);
            }

            if (inv.y > state.height + 50) it.remove();
        }

        // --- Shooter attacks ---
        InvaderAttackSystem.spawnShooterBullets(invaders, bullets);

        // --- Collisions (passes player so kills award points) ---
        CollisionSystem.bulletsVsInvaders(bullets, invaders, player, this::spawnShrapnel);
    }

    @Override
    public void render(Graphics2D g, int width, int height) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (Bullet b : bullets) {
            spaceinvaders.core.render.BulletRenderers.render(g, b, state);
        }

        for (Invader inv : invaders) {
            spaceinvaders.core.render.InvaderRenderer.render(g, inv, state);
        }

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
