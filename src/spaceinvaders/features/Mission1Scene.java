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
import spaceinvaders.input.FireController;
import spaceinvaders.services.audio.AudioManager;
import spaceinvaders.services.loading.AssetLoader;
import spaceinvaders.weapons.BlasterWeapon;
import spaceinvaders.weapons.MissileWeapon;

/**
 * Mission 1: scripted intro waves.
 * - Space: hold fire (blaster)
 * - R: missile tap
 * - (Optional) F: blades if you want to keep them in missions
 */
public class Mission1Scene implements Scene {

    private final GameState state;
    private final SceneManager scenes;
    private final Random rng = new Random();

    private Player player;
    private FireController fire;

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Invader> invaders = new ArrayList<>();

    private boolean moveLeft, moveRight;

    // mission state
    private int wave = 0;                  // 0 = not started, 1..3 waves
    private long nowMs = 0;
    private long nextSpawnMs = 0;
    private int remainingToSpawn = 0;
    private boolean missionComplete = false;
    private boolean missionFailed = false;

    // blade (optional)
    private int bladeCooldownMs = 0;
    private static final int BLADE_COOLDOWN = 850;
    private static final int BLADE_PIERCE  = 3;
    private static final int BLADE_BOUNCES = 3;

    private boolean bladeVerticalBounce = false;
    private boolean bladeLegendarySplit = false;

    public Mission1Scene(GameState state, SceneManager scenes) {
        this.state = state;
        this.scenes = scenes;
    }

    @Override
    public void onEnter() {
        state.mode = GameState.AppMode.PLAY;

        // ONE true player
        player = state.player;

        // reset core stats for mission start
        player.pointsBanked = 0;
        player.pointsEarned = 0;
        player.pointsSpent = 0;
        player.nextUpgradeCost = 100;

        player.hp = player.maxHp;
        player.borderHp = player.maxBorderHp;

        // position player
        state.playerX = state.width / 2 - state.playerWidth / 2;

        // weapons
        fire = new FireController(player, new BlasterWeapon(player), new MissileWeapon(player));

        // clear entities
        bullets.clear();
        invaders.clear();

        // load images once (safe to call)
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

        // start wave 1
        startWave(1);
    }

    @Override
    public void onExit() {}

    @Override
    public void handleKeyPressed(int key) {
        if (missionComplete || missionFailed) {
            if (key == KeyEvent.VK_ENTER) {
                // go back to start menu mode + clear active scene
                state.mode = GameState.AppMode.START_MENU;
                scenes.set(null); // ✅ FIX: SceneManager uses set(...)
            }
            return;
        }

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

            // Optional: blades in mission
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

    private void startWave(int w) {
        wave = w;
        remainingToSpawn = switch (wave) {
            case 1 -> 14;
            case 2 -> 16;
            case 3 -> 18; // includes tank/shooter mix
            default -> 0;
        };
        nextSpawnMs = 0;
    }

    private void tryFireBlades() {
        if (bladeCooldownMs > 0) return;

        int muzzleX = state.playerX + state.playerWidth / 2;
        int muzzleY = state.height - state.playerHeight - 10;

        int vx = 14;
        int vy = -4;
        int size = 10;

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

        try { AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/wpn_blade_fire.wav", 0.25f); }
        catch (Throwable ignored) {}
    }

    private List<Bullet> spawnShrapnel(int cx, int cy) {
        final int shards = 5;
        final int size = 6;
        final int dmg = 1;
        final int speed = 6;

        List<Bullet> out = new ArrayList<>(shards);
        for (int i = 0; i < shards; i++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            int vx = (int) Math.round(Math.cos(angle) * speed);
            int vy = (int) Math.round(Math.sin(angle) * speed);
            out.add(new Bullet(cx, cy, vx, vy, size, dmg, Bullet.BulletKind.BASIC));
        }

        try { AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/exp_ord_rocket_small01.wav", 0.66f); }
        catch (Throwable ignored) {}

        return out;
    }

    @Override
    public void update(double dtMillis) {
        nowMs = System.currentTimeMillis();
        int dtMs = (int) Math.max(1, Math.round(dtMillis));

        if (missionComplete || missionFailed) return;

        // blade cooldown
        if (bladeCooldownMs > 0) bladeCooldownMs -= dtMs;

        // movement
        int step = (player != null) ? player.speedPx : 8;
        if (moveLeft)  state.playerX -= step;
        if (moveRight) state.playerX += step;
        state.playerX = Math.max(0, Math.min(state.playerX, state.width - state.playerWidth));

        // fire
        int muzzleX = state.playerX + state.playerWidth / 2;
        int muzzleY = state.height - state.playerHeight - 10;

        if (fire != null) {
            List<Bullet> spawned = fire.tick(nowMs, muzzleX, muzzleY, bullets, state.width, state.height);
            if (!spawned.isEmpty()) {
                for (Bullet b : spawned) {
                    try {
                        if (b.kind == Bullet.BulletKind.MISSILE) {
                            AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/wpn_ywing_torpedo_fire.wav", 0.18f);
                        } else {
                            AudioManager.get().playSfx("/spaceinvaders/resources/audio/sfx/ct_blaster_fire.wav", 0.14f);
                        }
                    } catch (Throwable ignored) {}
                }
                bullets.addAll(spawned);
            }
        }

        // spawn invaders (scripted)
        runWaveSpawner();

        // bullets update/move
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

        // invader update + leak rule
        for (Iterator<Invader> it = invaders.iterator(); it.hasNext();) {
            Invader inv = it.next();
            inv.update(dtMs, state.width, state.height);

            // if reaches bottom = leak
            if (inv.y > state.height + 40) {
                it.remove();
                int dmg = player.leakDamageFor(inv.kind);
                player.borderHp = Math.max(0, player.borderHp - dmg);
            }
        }

        // shooters fire enemy bullets
        InvaderAttackSystem.spawnShooterBullets(invaders, bullets);

        // bullet vs invader collisions + points
        CollisionSystem.bulletsVsInvaders(bullets, invaders, player, this::spawnShrapnel);

        // win/lose checks
        if (player.hp <= 0 || player.borderHp <= 0) {
            missionFailed = true;
            return;
        }

        // wave clear -> next wave
        if (remainingToSpawn <= 0 && invaders.isEmpty()) {
            if (wave < 3) startWave(wave + 1);
            else missionComplete = true;
        }
    }

    private void runWaveSpawner() {
        if (remainingToSpawn <= 0) return;
        if (nowMs < nextSpawnMs) return;

        int x = 120 + rng.nextInt(Math.max(1, state.width - 240));
        int y = -60;

        int baseGap = switch (wave) {
            case 1 -> 520;
            case 2 -> 480;
            case 3 -> 440;
            default -> 520;
        };
        nextSpawnMs = nowMs + baseGap + rng.nextInt(220);

        Invader inv;

        if (wave == 1) {
            inv = new Invader(x, y, 40, 40, Invader.InvaderKind.BASIC, null);
            inv.vy = 2;
            inv.hp = 1;
            inv.scoreValue = 8;

        } else if (wave == 2) {
            boolean shielded = rng.nextDouble() < 0.35;
            inv = new Invader(x, y, 40, 40, shielded ? Invader.InvaderKind.SHIELDED : Invader.InvaderKind.BASIC, null);
            inv.vy = 2;
            inv.hp = 1;
            inv.scoreValue = shielded ? 12 : 8;

        } else {
            if (remainingToSpawn == 3) {
                inv = new Invader(x, -90, 56, 56, Invader.InvaderKind.TANK, null);
                inv.hp = 6;
                inv.vy = 1;
                inv.scoreValue = 40;
                inv.touchDamage = 2;
                inv.armored = true;
            } else {
                double roll = rng.nextDouble();
                if (roll < 0.35) {
                    inv = new Invader(x, y, 40, 40, Invader.InvaderKind.SHOOTER, new ShooterZigZagPattern());
                    inv.vy = 1;
                    inv.hp = 1;
                    inv.scoreValue = 14;
                    inv.touchDamage = 1;
                } else if (roll < 0.55) {
                    inv = new Invader(x, y, 26, 26, Invader.InvaderKind.SWARMER, new SwarmerZigZagPattern());
                    inv.vy = 2;
                    inv.hp = 1;
                    inv.scoreValue = 10;
                } else {
                    inv = new Invader(x, y, 40, 40, Invader.InvaderKind.BASIC, null);
                    inv.vy = 2;
                    inv.hp = 1;
                    inv.scoreValue = 8;
                }
            }
        }

        invaders.add(inv);
        remainingToSpawn--;
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

        int px = state.playerX;
        int py = height - state.playerHeight - 10;
        if (state.playerImage != null) {
            g.drawImage(state.playerImage, px, py, state.playerWidth, state.playerHeight, null);
        } else {
            g.setColor(Color.CYAN);
            g.fillRect(px, py, state.playerWidth, state.playerHeight);
        }

        drawHud(g);

        if (missionComplete) drawOverlay(g, "MISSION 1 COMPLETE", "Press ENTER");
        if (missionFailed)   drawOverlay(g, "MISSION FAILED", "Press ENTER");
    }

    private void drawHud(Graphics2D g) {
        g.setFont(new Font("Consolas", Font.PLAIN, 22));
        g.setColor(new Color(255, 255, 255, 220));

        String line1 = "MISSION 1  |  Wave " + wave + "/3";
        String line2 = "HP " + player.hp + "/" + player.maxHp + "   Border " + player.borderHp + "/" + player.maxBorderHp;
        String line3 = "Points " + player.pointsEarned;

        g.drawString(line1, 24, 36);
        g.drawString(line2, 24, 64);
        g.drawString(line3, 24, 92);

        if (remainingToSpawn > 0) {
            g.drawString("Incoming: " + remainingToSpawn, 24, 120);
        }
    }

    private void drawOverlay(Graphics2D g, String title, String sub) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, state.width, state.height);
        g.setComposite(old);

        g.setFont(new Font("Consolas", Font.BOLD, 64));
        g.setColor(Color.WHITE);

        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (state.width - tw) / 2, state.height / 2 - 30);

        g.setFont(new Font("Consolas", Font.PLAIN, 30));
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, (state.width - sw) / 2, state.height / 2 + 30);
    }
}
