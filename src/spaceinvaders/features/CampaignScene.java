package spaceinvaders.features;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.Set;
import spaceinvaders.core.GameState;
import spaceinvaders.core.Scene;
import spaceinvaders.core.SceneManager;
import spaceinvaders.core.entities.Blade;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.EnemyBullet;
import spaceinvaders.core.entities.Invader;
import spaceinvaders.core.entities.Missile;
import spaceinvaders.core.entities.ShooterZigZagPattern;
import spaceinvaders.core.entities.SwarmerZigZagPattern;
import spaceinvaders.core.entities.TankPattern;
import spaceinvaders.core.entities.HailfirePattern;
import spaceinvaders.core.entities.HailfireRocket;
import spaceinvaders.core.render.BulletRenderers;
import spaceinvaders.core.render.InvaderRenderer;
import spaceinvaders.features.campaign.RunUpgrades;
import spaceinvaders.features.campaign.ExperienceProgress;
import spaceinvaders.features.campaign.CampaignDefinitions;
import spaceinvaders.features.campaign.EnemyType;
import spaceinvaders.features.campaign.MissionDefinition;
import spaceinvaders.features.campaign.SpawnEvent;
import spaceinvaders.features.campaign.UpgradeId;
import spaceinvaders.services.loading.AssetLoader;
import spaceinvaders.services.scores.HighScoreService;

/**
 * Chapter one. It owns only per-run data; shared menu configuration remains in
 * {@link GameState}, and entity rendering continues to use the existing renderers.
 */
public final class CampaignScene implements Scene {
    private static final int FLOOR_MARGIN = 42;
    private static final int INITIAL_HEALTH = 15;
    private static final int PLAYER_W = 58;
    private static final int PLAYER_H = 70;
    private static final int BOSS_MAX_HEALTH = 220;

    private enum Phase { ANNOUNCEMENT, RUNNING, PAUSED, COLLECTION, UPGRADE, SUPPORT_WHEEL, NUKE_SEQUENCE, CHAPTER_COMPLETE, GAME_OVER, COMPLETE }

    private final GameState state;
    private final SceneManager scenes;
    private final HighScoreService scores = new HighScoreService();
    private final Random random = new Random();
    private final RunUpgrades upgrades = new RunUpgrades();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Invader> invaders = new ArrayList<>();
    private final List<UpgradeId> cards = new ArrayList<>(3);
    private final List<SupportClone> clones = new ArrayList<>();
    private final EnumMap<UpgradeId, Integer> supportCharges = new EnumMap<>(UpgradeId.class);
    private final Set<Integer> heldSupportKeys = new HashSet<>();
    private final Map<Invader, Long> hailfireNextBarrage = new HashMap<>();

    private Phase phase;
    private Phase collectionReturn;
    private int wave;
    private int chapter = 1;
    private int pendingStage;
    private MissionDefinition activeMission;
    private int nextSpawnEvent;
    private double missionElapsedMs;
    private boolean transitionPending;
    private boolean pendingComplete;
    private boolean clearEnemyProjectiles;
    private double phaseMs;
    private String announcement;
    private boolean left, right, firing;
    private boolean scoreRecorded;
    private boolean emergencySaveUsed;

    private int health;
    private int maxHealth;
    private int score;
    private final ExperienceProgress experience = new ExperienceProgress();
    private int invulnerableMs;
    private int hitFlashMs;
    private double regenerationBank;
    private double blasterCooldown;
    private double missileCooldown;
    private double bladeCooldown;

    private Invader boss;
    private int bossMaxHealth;
    private double sweepTimer;
    private double sweepAngle;
    private double cannonTimer;
    private double cannonWarningMs;
    private int cannonLane;
    private double spiderBeamMs, spiderBeamWarmupMs, spiderBeamCooldown, spiderBeamDamageBank, spiderBeamAngle;
    private int bossSupportSpawnIndex;
    private double cloneReinforcementMs, cloneReinforcementCooldown, orbitalCooldown, airstrikeCooldown, nukeCooldown, supportFlashMs;
    private int supportLane;
    private double enemyMotionMs;
    private double nukeActivationMs;
    private static final boolean DEBUG = Boolean.getBoolean("separatist.debug");

    public CampaignScene(GameState state, SceneManager scenes) {
        this.state = state;
        this.scenes = scenes;
    }

    @Override public void onEnter() {
        state.mode = GameState.AppMode.PLAY;
        state.playerWidth = PLAYER_W;
        state.playerHeight = PLAYER_H;
        state.playerX = state.width / 2 - PLAYER_W / 2;
        loadImages();
        health = maxHealth = INITIAL_HEALTH;
        phase = Phase.ANNOUNCEMENT;
        phaseMs = 1800;
        announcement = "CHAPTER 1 — MISSION 1";
        wave = 1;
    }

    @Override public void onExit() {
        bullets.clear();
        invaders.clear();
        boss = null;
    }

    private void loadImages() {
        try { if (state.playerImage == null) state.playerImage = AssetLoader.imageFromResource("image/p1_clone.png"); } catch (Exception ignored) { }
        try { state.invaderImageBasic = AssetLoader.imageFromResource("image/b1_droid.png"); } catch (Exception ignored) { }
        try { state.invaderImageB2 = AssetLoader.imageFromResource("image/b2_droid.png"); } catch (Exception ignored) { }
        try { state.invaderImageTank = AssetLoader.imageFromResource("image/aat.png"); } catch (Exception ignored) { }
        try { state.invaderImageShielded = AssetLoader.imageFromResource("image/droideka.png"); } catch (Exception ignored) { }
        try { state.invaderImageShooter = AssetLoader.imageFromResource("image/bx_commando_droid.png"); } catch (Exception ignored) { }
        try { state.invaderImageSwarmer = AssetLoader.imageFromResource("image/buzz_droid.png"); } catch (Exception ignored) { }
    }

    @Override public void handleKeyPressed(int key) {
        if (phase == Phase.UPGRADE) {
            if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_3) chooseCard(key - KeyEvent.VK_1);
            return;
        }
        if (phase == Phase.SUPPORT_WHEEL) {
            if (key == KeyEvent.VK_R || key == KeyEvent.VK_ESCAPE) phase = Phase.RUNNING;
            return;
        }
        if (phase == Phase.NUKE_SEQUENCE) return;
        if (phase == Phase.CHAPTER_COMPLETE) {
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_R) startChapterTwo();
            if (key == KeyEvent.VK_M || key == KeyEvent.VK_ESCAPE) returnToMenu();
            return;
        }
        if (phase == Phase.GAME_OVER || phase == Phase.COMPLETE) {
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_R) scenes.set(new CampaignScene(state, scenes));
            if (key == KeyEvent.VK_M || key == KeyEvent.VK_ESCAPE) returnToMenu();
            return;
        }
        if (phase == Phase.COLLECTION) {
            if (key == KeyEvent.VK_U || key == KeyEvent.VK_ESCAPE) phase = collectionReturn;
            return;
        }
        if (key == KeyEvent.VK_P || key == KeyEvent.VK_ESCAPE) {
            phase = phase == Phase.PAUSED ? Phase.RUNNING : Phase.PAUSED;
            return;
        }
        if (key == KeyEvent.VK_U) {
            collectionReturn = phase;
            phase = Phase.COLLECTION;
            return;
        }
        if (phase != Phase.RUNNING) return;
        switch (key) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> left = true;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = true;
            case KeyEvent.VK_SPACE -> firing = true;
            case KeyEvent.VK_R -> openSupportWheel();
            case KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                 KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD4 -> {
                int slot = supportSlot(key);
                if (slot >= 0 && heldSupportKeys.add(key)) activateSupport(slot);
            }
            default -> { }
        }
    }

    private int supportSlot(int key) {
        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_4) return key - KeyEvent.VK_1;
        if (key >= KeyEvent.VK_NUMPAD1 && key <= KeyEvent.VK_NUMPAD4) return key - KeyEvent.VK_NUMPAD1;
        return -1;
    }

    private void openSupportWheel() {
        phase = Phase.SUPPORT_WHEEL;
    }

    private void activateSupport(int slot) {
        UpgradeId id = switch (slot) { case 1 -> UpgradeId.ORBITAL_STRIKE; case 2 -> UpgradeId.AIRSTRIKE; case 3 -> UpgradeId.NUKE; default -> UpgradeId.CLONE_REINFORCEMENTS; };
        if (upgrades.level(id) == 0 || chargesFor(id) <= 0) return;
        if (slot == 0 && cloneReinforcementCooldown <= 0) { cloneReinforcementMs = 14000; cloneReinforcementCooldown = 22000; }
        else if (slot == 1 && orbitalCooldown <= 0) { supportLane = state.playerX + state.playerWidth / 2; damageLane(supportLane, 18); supportFlashMs = 450; orbitalCooldown = 12000; }
        else if (slot == 2 && airstrikeCooldown <= 0) { supportLane = state.playerX + state.playerWidth / 2; damageLane(supportLane, 12); supportFlashMs = 800; airstrikeCooldown = 16000; }
        else if (slot == 3 && nukeCooldown <= 0) {
            consumeCharge(id);
            nukeActivationMs = 1000;
            supportFlashMs = 1000;
            phase = Phase.NUKE_SEQUENCE;
            debug("nuke activation started; active=" + invaders.size());
            return;
        }
        else return;
        consumeCharge(id);
        phase = Phase.RUNNING;
    }

    private int chargesFor(UpgradeId id) {
        if (upgrades.level(id) <= 0) return 0;
        return supportCharges.computeIfAbsent(id, unused -> 3);
    }

    private void consumeCharge(UpgradeId id) {
        supportCharges.put(id, Math.max(0, chargesFor(id) - 1));
    }

    private void damageLane(int lane, int damage) {
        for (Iterator<Invader> it = invaders.iterator(); it.hasNext();) {
            Invader enemy = it.next();
            if (Math.abs(enemy.x + enemy.width / 2 - lane) < 140) { enemy.hp -= damage; if (enemy.hp <= 0) { score += enemy.scoreValue; grantXp(enemyXp(enemy)); it.remove(); } }
        }
        if (boss != null && Math.abs(boss.x + boss.width / 2 - lane) < 250) { boss.hp -= damage; if (boss.hp <= 0) defeatBoss(); }
    }

    /** Resolves only enemies already on the field; future mission events remain pending. */
    private void executeNuke() {
        int resolved = 0;
        for (Iterator<Invader> it = invaders.iterator(); it.hasNext();) {
            Invader enemy = it.next();
            it.remove();
            score += enemy.scoreValue;
            grantXp(enemyXp(enemy));
            resolved++;
        }
        if (boss != null) {
            boss.hp -= 25;
            if (boss.hp <= 0) defeatBoss();
        }
        bullets.removeIf(bullet -> bullet instanceof EnemyBullet);
        nukeCooldown = 90000;
        supportFlashMs = 750;
        if (phase != Phase.UPGRADE && !pendingComplete) phase = Phase.RUNNING;
        debug("nuke detonated; resolved=" + resolved + " pendingEvents="
                + (activeMission == null ? 0 : activeMission.spawns().size() - nextSpawnEvent));
    }

    @Override public void handleKeyReleased(int key) {
        switch (key) {
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> left = false;
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> right = false;
            case KeyEvent.VK_SPACE -> firing = false;
            case KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                 KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD3, KeyEvent.VK_NUMPAD4 -> heldSupportKeys.remove(key);
            default -> { }
        }
    }

    @Override public void update(double dtMillis) {
        int dt = Math.max(1, (int) Math.round(dtMillis));
        if (phase == Phase.ANNOUNCEMENT) {
            phaseMs -= dt;
            if (phaseMs <= 0) startStage(pendingStage == 0 ? wave : pendingStage);
            return;
        }
        if (phase == Phase.NUKE_SEQUENCE) {
            nukeActivationMs -= dt;
            if (nukeActivationMs <= 0) executeNuke();
            return;
        }
        if (phase != Phase.RUNNING) return;

        invulnerableMs = Math.max(0, invulnerableMs - dt);
        hitFlashMs = Math.max(0, hitFlashMs - dt);
        blasterCooldown = Math.max(0, blasterCooldown - dt);
        missileCooldown = Math.max(0, missileCooldown - dt);
        bladeCooldown = Math.max(0, bladeCooldown - dt);
        cloneReinforcementMs = Math.max(0, cloneReinforcementMs - dt);
        cloneReinforcementCooldown = Math.max(0, cloneReinforcementCooldown - dt);
        orbitalCooldown = Math.max(0, orbitalCooldown - dt);
        airstrikeCooldown = Math.max(0, airstrikeCooldown - dt);
        nukeCooldown = Math.max(0, nukeCooldown - dt);
        supportFlashMs = Math.max(0, supportFlashMs - dt);
        updatePlayer(dt);
        updateMissionSchedule(dt);
        updateWeapons();
        updateBullets(dt);
        updateClones(dt);
        updateInvaders(dt);
        updateBoss(dt);
        resolveCollisions();
        if (clearEnemyProjectiles) {
            bullets.removeIf(b -> b instanceof EnemyBullet);
            clearEnemyProjectiles = false;
        }
        regenerate(dt);
        if (health <= 0) endRun(false);
        // A final kill may have opened an upgrade screen during collision handling;
        // still queue the stage transition so the selected card resumes correctly.
        if (boss == null && activeMission != null && nextSpawnEvent >= activeMission.spawns().size() && invaders.isEmpty() && !pendingComplete
                && (phase == Phase.RUNNING || phase == Phase.UPGRADE)) finishStage();
    }

    private void updatePlayer(int dt) {
        double speed = 3.2 + upgrades.level(UpgradeId.MOVEMENT) * 0.45;
        if (left) state.playerX -= (int) Math.round(speed * dt / 16.0);
        if (right) state.playerX += (int) Math.round(speed * dt / 16.0);
        state.playerX = Math.max(0, Math.min(state.width - state.playerWidth, state.playerX));
    }

    /** Advances only while PLAYING; upgrade cards deliberately freeze this clock. */
    private void updateMissionSchedule(int dt) {
        if (activeMission == null) return;
        missionElapsedMs += dt;
        while (nextSpawnEvent < activeMission.spawns().size()
                && missionElapsedMs >= activeMission.spawns().get(nextSpawnEvent).timeMs()) {
            spawnEvent(activeMission.spawns().get(nextSpawnEvent));
            nextSpawnEvent++;
        }
    }

    private void spawnEvent(SpawnEvent event) {
        for (int i = 0; i < event.count(); i++) {
            int x = formationX(event.formation(), i, event.count());
            int y = -70 - (i % 2) * 22; // every campaign enemy enters from above the top edge
            invaders.add(createCampaignEnemy(event.enemyType(), x, y));
        }
        debug("spawn chapter=" + chapter + " mission=" + wave + " t=" + (int) missionElapsedMs
                + " type=" + event.enemyType() + " count=" + event.count()
                + " pending=" + (activeMission.spawns().size() - nextSpawnEvent - 1) + " active=" + invaders.size());
    }

    private int formationX(String formation, int index, int count) {
        int spacing = Math.max(1, state.width / (count + 1));
        int x = spacing * (index + 1) - 30;
        if ("stagger".equals(formation)) x += (index % 2 == 0 ? 45 : -45);
        if ("v".equals(formation)) x += Math.abs(index - (count - 1) / 2) * 32;
        if ("alternate".equals(formation)) x = 130 + (index * 310) % (state.width - 260);
        if ("left".equals(formation)) x = 150;
        return Math.max(0, Math.min(state.width - 90, x));
    }

    private Invader createCampaignEnemy(EnemyType type, int x, int y) {
        return switch (type) {
            case B1 -> configure(new Invader(x, y, 52, 52, Invader.InvaderKind.BASIC, null), 1, 1, 10);
            case B2 -> configure(new Invader(x, y, 58, 58, Invader.InvaderKind.B2, null), 6, 1, 30);
            case DROIDEKA -> configure(new Invader(x, y, 62, 62, Invader.InvaderKind.SHIELDED, null), 3, 1, 30);
            case HAILFIRE -> configure(new Invader(x, y, 90, 70, Invader.InvaderKind.HAILFIRE, new HailfirePattern(x)), 10, 1, 70);
        };
    }

    private void updateWeapons() {
        if (!firing) return;
        if (blasterCooldown <= 0) {
            int damage = 1 + upgrades.level(UpgradeId.BLASTER_DAMAGE);
            int speed = 9 + upgrades.level(UpgradeId.BOLT_SPEED) * 2;
            int count = 1 + upgrades.level(UpgradeId.MULTI_SHOT);
            int mx = state.playerX + state.playerWidth / 2;
            int my = playerY() - 8;
            for (int i = 0; i < count; i++) {
                int offset = (i - (count - 1) / 2) * 14;
                bullets.add(new Bullet(mx + offset - 4, my, offset / 7, -speed, 8, damage, Bullet.BulletKind.BASIC));
            }
            blasterCooldown = Math.max(180, 390 - upgrades.level(UpgradeId.FIRE_RATE) * 45);
        }
        if (upgrades.level(UpgradeId.MISSILE_LAUNCHER) > 0 && missileCooldown <= 0) {
            int count = 1 + upgrades.level(UpgradeId.MISSILE_COUNT);
            for (int i = 0; i < count; i++) tryFireMissile(i - (count - 1) / 2);
        }
        if (upgrades.level(UpgradeId.BLADE_UNLOCK) > 0 && bladeCooldown <= 0) tryFireBlade();
    }

    private void tryFireMissile(int laneOffset) {
        if (missileCooldown > 0) return;
        int damage = 5 + upgrades.level(UpgradeId.MISSILE_DAMAGE) * 2;
        int speed = 8 + upgrades.level(UpgradeId.MISSILE_SPEED) * 2;
        bullets.add(new Missile(state.playerX + state.playerWidth / 2 - 7 + laneOffset * 18, playerY() - 10, laneOffset * 2, -speed, 14, damage));
        missileCooldown = Math.max(700, 1700 - upgrades.level(UpgradeId.MISSILE_COOLDOWN) * 200);
    }

    private void tryFireBlade() {
        if (bladeCooldown > 0) return;
        int damage = 1 + upgrades.level(UpgradeId.BLADE_DAMAGE);
        int pierce = 1 + upgrades.level(UpgradeId.BLADE_PENETRATION);
        int mx = state.playerX + state.playerWidth / 2;
        int count = 1 + upgrades.level(UpgradeId.BLADE_COUNT);
        for (int i = 0; i < count; i++) {
            int vx = count == 1 ? 0 : (i - (count - 1) / 2) * (8 + upgrades.level(UpgradeId.BLADE_THROW_SPEED) * 2);
            Blade blade = new Blade(mx, playerY(), vx, -5 - upgrades.level(UpgradeId.BLADE_THROW_SPEED), 13 + upgrades.level(UpgradeId.BLADE_SIZE) * 3, 1 + upgrades.level(UpgradeId.BLADE_BOUNCE_COUNT), pierce, false, false, false);
            blade.setMaxLifeMs(9000 + upgrades.level(UpgradeId.BLADE_LIFETIME) * 2000);
            blade.damage = damage;
            bullets.add(blade);
        }
        bladeCooldown = Math.max(600, 1300 - upgrades.level(UpgradeId.BLADE_RECALL_SPEED) * 220);
    }

    private void updateBullets(int dt) {
        for (Iterator<Bullet> it = bullets.iterator(); it.hasNext();) {
            Bullet bullet = it.next();
            if (bullet instanceof Blade blade) {
                bulletsAddSafely(blade.updateBlade(dt, state.width, state.height));
                if (blade.isDead()) it.remove();
                continue;
            }
            if (!(bullet instanceof EnemyBullet) && !(bullet instanceof spaceinvaders.core.entities.CloneBullet)) steerSmartProjectile(bullet);
            bullet.update();
            if (bullet.isOffScreen(state.width, state.height)) it.remove();
        }
    }

    /* Blade split is intentionally rare; append after an iterator cycle to avoid modification errors. */
    private final List<Bullet> deferredBullets = new ArrayList<>();
    private void bulletsAddSafely(List<Bullet> additions) {
        if (additions != null && !additions.isEmpty()) deferredBullets.addAll(additions);
    }

    private void steerSmartProjectile(Bullet bullet) {
        boolean missile = bullet instanceof Missile;
        if ((!missile && upgrades.level(UpgradeId.SMART_BULLETS_UNLOCK) == 0)
                || (missile && upgrades.level(UpgradeId.SMART_MISSILE_UNLOCK) == 0)) return;
        Invader target = nearestTarget(bullet.x, bullet.y);
        if (target == null) return;
        int range = 400 + (missile ? upgrades.level(UpgradeId.SMART_MISSILE_TRACKING) * 180 : upgrades.level(UpgradeId.SMART_BULLET_RANGE) * 250);
        if (distanceSquared(bullet.x, bullet.y, target.x, target.y) > (long) range * range) return;
        int targetX = target.x + target.width / 2;
        int desired = Integer.compare(targetX, bullet.x + bullet.size / 2) * (missile ? 2 + upgrades.level(UpgradeId.SMART_MISSILE_TURN_RATE) : 1 + upgrades.level(UpgradeId.SMART_BULLET_TRACKING));
        bullet.vx += Integer.compare(desired, bullet.vx);
        bullet.vx = Math.max(-6, Math.min(6, bullet.vx));
    }

    private Invader nearestTarget(int x, int y) {
        Invader best = boss;
        long bestDistance = boss == null ? Long.MAX_VALUE : distanceSquared(x, y, boss.x, boss.y);
        for (Invader enemy : invaders) {
            long distance = distanceSquared(x, y, enemy.x, enemy.y);
            if (distance < bestDistance) { best = enemy; bestDistance = distance; }
        }
        return best;
    }

    private static long distanceSquared(int x1, int y1, int x2, int y2) {
        long dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private void updateInvaders(int dt) {
        enemyMotionMs += dt;
        if (enemyMotionMs < 64) return; // quarter-speed descent without changing shared enemy patterns
        int movementDt = (int) enemyMotionMs;
        enemyMotionMs = 0;
        Rectangle player = playerBounds();
        for (Iterator<Invader> it = invaders.iterator(); it.hasNext();) {
            Invader enemy = it.next();
            enemy.update(movementDt, state.width, state.height);
            if (enemy.kind == Invader.InvaderKind.SHOOTER && enemy.firePending) {
                enemy.firePending = false;
                fireEnemy(enemy.x + enemy.width / 2, enemy.y + enemy.height, 0, 6 + wave, 1);
            }
            if (enemy.kind != Invader.InvaderKind.SHOOTER && enemy.kind != Invader.InvaderKind.HAILFIRE && random.nextDouble() < 0.0015 * wave) {
                fireEnemy(enemy.x + enemy.width / 2, enemy.y + enemy.height, 0, 5 + wave, 1);
            }
            if (enemy.kind == Invader.InvaderKind.HAILFIRE) updateHailfireBarrage(enemy);
            if (new Rectangle(enemy.x, enemy.y, enemy.width, enemy.height).intersects(player)) {
                it.remove();
                damagePlayer(enemy.touchDamage);
            } else if (enemy.y + enemy.height >= defensiveLine()) {
                it.remove();
                damagePlayer(Math.max(0, 2 - upgrades.level(UpgradeId.REINFORCED_BARRIER) - upgrades.level(UpgradeId.DEFENSIVE_LINE_INTEGRITY) / 2 - upgrades.level(UpgradeId.AUTO_REPAIR) / 2));
            }
        }
        if (!deferredBullets.isEmpty()) { bullets.addAll(deferredBullets); deferredBullets.clear(); }
    }

    private void updateHailfireBarrage(Invader hailfire) {
        if (hailfire.y < 0) return;
        Long due = hailfireNextBarrage.get(hailfire);
        if (due == null) {
            due = hailfire.ageMs + 1000 + Math.floorMod(hailfire.spawnX, 2001);
            hailfireNextBarrage.put(hailfire, due);
            debug("hailfire entered; first barrage at=" + due);
        }
        if (hailfire.ageMs < due) return;
        int count = 4 + Math.floorMod(hailfire.spawnX + (int) (hailfire.ageMs / 1000), 5);
        for (int i = 0; i < count; i++) {
            int offset = -hailfire.width / 3 + i * (hailfire.width * 2 / 3) / Math.max(1, count - 1);
            int vx = (i % 3) - 1;
            bullets.add(new HailfireRocket(hailfire.x + hailfire.width / 2 + offset, hailfire.y + hailfire.height, vx, 6 + (i % 2), i * 0.9));
        }
        long next = hailfire.ageMs + 10000 + Math.floorMod(hailfire.spawnX + count, 2001);
        hailfireNextBarrage.put(hailfire, next);
        debug("hailfire barrage count=" + count + " hostileProjectiles=" + bullets.size() + " next=" + next);
    }

    private void updateClones(int dt) {
        int desired = upgrades.level(UpgradeId.CLONE_SUPPORT_UNLOCK) == 0 ? 0 : 1 + upgrades.level(UpgradeId.ADDITIONAL_CLONE);
        if (cloneReinforcementMs > 0) desired = Math.min(3, desired + 1);
        while (clones.size() < desired) clones.add(new SupportClone(160 + clones.size() * 150));
        while (clones.size() > desired) clones.remove(clones.size() - 1);
        for (SupportClone clone : clones) {
            if (clone.hp <= 0) {
                if (upgrades.level(UpgradeId.CLONE_REVIVAL) == 0) continue;
                clone.reviveMs -= dt;
                if (clone.reviveMs <= 0) clone.hp = clone.maxHealth();
                continue;
            }
            clone.x += clone.direction * 2;
            if (clone.x < 30 || clone.x > state.width - 70) clone.direction *= -1;
            clone.cooldown -= dt;
            Invader target = nearestTarget(clone.x, clone.y());
            if (target != null && clone.cooldown <= 0) {
                int vx = upgrades.level(UpgradeId.CLONE_ACCURACY) >= 2 ? Integer.compare(target.x, clone.x) : random.nextInt(3) - 1;
                bullets.add(new spaceinvaders.core.entities.CloneBullet(clone.x + 15, clone.y(), vx, -8, 7, 1 + upgrades.level(UpgradeId.CLONE_DAMAGE)));
                clone.cooldown = Math.max(300, 760 - upgrades.level(UpgradeId.CLONE_FIRE_RATE) * 120);
            }
        }
    }

    private SupportClone cloneHit(Rectangle hitbox) {
        for (SupportClone clone : clones) {
            if (clone.hp > 0 && hitbox.intersects(new Rectangle(clone.x, clone.y(), 36, 48))) return clone;
        }
        return null;
    }

    private void updateBoss(int dt) {
        if (boss == null) return;
        if (chapter == 2) {
            spiderBeamCooldown += dt;
            if (spiderBeamWarmupMs > 0) {
                spiderBeamWarmupMs -= dt;
                if (spiderBeamWarmupMs <= 0) { spiderBeamMs = 3500; spiderBeamDamageBank = 0; }
                return;
            }
            if (spiderBeamMs > 0) {
                spiderBeamMs -= dt;
                double desired = Math.atan2(playerY() + state.playerHeight / 2.0 - (boss.y + boss.height / 2.0), state.playerX + state.playerWidth / 2.0 - (boss.x + boss.width / 2.0));
                spiderBeamAngle = moveTowardAngle(spiderBeamAngle, desired, Math.toRadians(12) * dt / 1000.0);
                if (playerTouchesSpiderBeam()) {
                    spiderBeamDamageBank += 4.0 * dt / 1000.0;
                    int damage = (int) spiderBeamDamageBank;
                    if (damage > 0) { health = Math.max(0, health - damage); spiderBeamDamageBank -= damage; }
                }
                return;
            }
            if (spiderBeamCooldown >= 7200) {
                spiderBeamCooldown = 0; spiderBeamWarmupMs = 1400;
                spiderBeamAngle = Math.atan2(playerY() + state.playerHeight / 2.0 - (boss.y + boss.height / 2.0), state.playerX + state.playerWidth / 2.0 - (boss.x + boss.width / 2.0));
                long activeHailfire = invaders.stream().filter(enemy -> enemy.kind == Invader.InvaderKind.HAILFIRE).count();
                if (activeHailfire < 3) {
                    int x = 150 + (bossSupportSpawnIndex++ % 5) * 320;
                    invaders.add(configure(new Invader(x, -80, 90, 70, Invader.InvaderKind.HAILFIRE, new HailfirePattern(x)), 10, 1, 70));
                }
            }
            return;
        }
        double aggression = boss.hp * 2 < bossMaxHealth ? 0.72 : 1.0;
        cannonTimer += dt;
        sweepTimer += dt;
        if (cannonWarningMs > 0) {
            cannonWarningMs -= dt;
            if (cannonWarningMs <= 0) {
                fireEnemy(cannonLane - 15, boss.y + boss.height - 5, 0, 16, 4);
                cannonTimer = 0;
            }
        } else if (cannonTimer >= 3300 * aggression) {
            cannonLane = Math.max(40, Math.min(state.width - 40, state.playerX + state.playerWidth / 2));
            cannonWarningMs = 950 * aggression;
        }
        if (sweepTimer >= 155 * aggression) {
            sweepTimer = 0;
            double vx = Math.sin(sweepAngle) * 7;
            double vy = Math.cos(sweepAngle) * 6 + 3;
            fireEnemy(boss.x + boss.width / 2, boss.y + boss.height - 8, (int) Math.round(vx), (int) Math.round(vy), 1);
            sweepAngle += 0.12;
            if (sweepAngle > Math.PI * 0.85) sweepAngle = -Math.PI * 0.85;
        }
    }

    private void fireEnemy(int x, int y, int vx, int vy, int damage) {
        bullets.add(new EnemyBullet(x - 6, y, vx, vy, 12, damage));
    }

    private static double moveTowardAngle(double current, double target, double maximumStep) {
        double delta = Math.atan2(Math.sin(target - current), Math.cos(target - current));
        return current + Math.max(-maximumStep, Math.min(maximumStep, delta));
    }

    private boolean playerTouchesSpiderBeam() {
        double ox = boss.x + boss.width / 2.0, oy = boss.y + boss.height / 2.0;
        double ex = ox + Math.cos(spiderBeamAngle) * state.height;
        double ey = oy + Math.sin(spiderBeamAngle) * state.height;
        double px = state.playerX + state.playerWidth / 2.0, py = playerY() + state.playerHeight / 2.0;
        double dx = ex - ox, dy = ey - oy;
        double t = Math.max(0, Math.min(1, ((px - ox) * dx + (py - oy) * dy) / (dx * dx + dy * dy)));
        double lx = ox + t * dx, ly = oy + t * dy;
        return distanceSquared((int) px, (int) py, (int) lx, (int) ly) <= 26L * 26L;
    }

    private void resolveCollisions() {
        Rectangle player = playerBounds();
        for (Iterator<Bullet> it = bullets.iterator(); it.hasNext();) {
            Bullet bullet = it.next();
            Rectangle hitbox = new Rectangle(bullet.x, bullet.y, bullet.size, bullet.size);
            if (bullet instanceof EnemyBullet) {
                SupportClone clone = cloneHit(hitbox);
                if (clone != null) {
                    it.remove();
                    clone.hp -= Math.max(1, bullet.damage - upgrades.level(UpgradeId.CLONE_ARMOR));
                    if (clone.hp <= 0) clone.reviveMs = 6000;
                } else if (hitbox.intersects(player)) { it.remove(); damagePlayer(bullet.damage); }
                continue;
            }
            if (boss != null && hitbox.intersects(new Rectangle(boss.x, boss.y, boss.width, boss.height))) {
                boss.hp -= Math.max(1, bullet.damage);
                if (bullet instanceof Missile) explodeMissile(bullet);
                consumePlayerBullet(bullet, it);
                if (boss.hp <= 0) defeatBoss();
                continue;
            }
            for (Iterator<Invader> enemies = invaders.iterator(); enemies.hasNext();) {
                Invader enemy = enemies.next();
                if (!hitbox.intersects(new Rectangle(enemy.x, enemy.y, enemy.width, enemy.height))) continue;
                Invader.HitResult result = enemy.takeHit(Math.max(1, bullet.damage));
                if (bullet instanceof Missile) explodeMissile(bullet);
                consumePlayerBullet(bullet, it);
                if (result == Invader.HitResult.KILLED) {
                    enemies.remove();
                    score += enemy.scoreValue;
                    grantXp(enemyXp(enemy));
                }
                break;
            }
        }
    }

    private void explodeMissile(Bullet missile) {
        int radius = 35 + upgrades.level(UpgradeId.EXPLOSION_RADIUS) * 30;
        for (Invader enemy : invaders) {
            if (distanceSquared(missile.x, missile.y, enemy.x, enemy.y) < (long) radius * radius) enemy.hp -= Math.max(1, missile.damage / 2);
        }
        if (upgrades.level(UpgradeId.SHRAPNEL_UNLOCK) > 0) {
            int count = 4 + upgrades.level(UpgradeId.SHRAPNEL_COUNT) * 3;
            int speed = 3 + upgrades.level(UpgradeId.SHRAPNEL_SPEED) * 2;
            int damage = 1 + upgrades.level(UpgradeId.SHRAPNEL_DAMAGE);
            for (int i = 0; i < count; i++) { double a = Math.PI * 2 * i / count; deferredBullets.add(new Bullet(missile.x, missile.y, (int)(Math.cos(a)*speed), (int)(Math.sin(a)*speed), 5, damage, Bullet.BulletKind.BASIC)); }
        }
    }

    private void consumePlayerBullet(Bullet bullet, Iterator<Bullet> iterator) {
        if (bullet instanceof Blade blade) { if (blade.onHitInvader()) iterator.remove(); }
        else iterator.remove();
    }

    private int enemyXp(Invader enemy) {
        return switch (enemy.kind) {
            case BASIC -> 1;
            case B2, SHIELDED, SHOOTER -> 3;
            case TANK, HAILFIRE -> 8;
            default -> 1;
        };
    }

    private void damagePlayer(int amount) {
        if (amount <= 0 || invulnerableMs > 0) return;
        amount = Math.max(1, amount - upgrades.level(UpgradeId.ARMOR));
        if (health - amount <= 0 && upgrades.level(UpgradeId.EMERGENCY_SAVE) > 0 && !emergencySaveUsed) {
            health = 1;
            emergencySaveUsed = true;
        } else health = Math.max(0, health - amount);
        invulnerableMs = 650;
        hitFlashMs = 220;
    }

    private void regenerate(int dt) {
        int rank = upgrades.level(UpgradeId.RECOVERY);
        if (rank == 0 || health >= maxHealth) return;
        regenerationBank += rank * dt / 3500.0;
        int restored = (int) regenerationBank;
        if (restored > 0) { health = Math.min(maxHealth, health + restored); regenerationBank -= restored; }
    }

    private void startStage(int stage) {
        pendingStage = 0;
        if (stage == 4) { activeMission = null; startBoss(); return; }
        wave = stage;
        activeMission = CampaignDefinitions.mission(chapter, wave);
        nextSpawnEvent = 0;
        missionElapsedMs = 0;
        invaders.clear();
        phase = Phase.RUNNING;
        debug("mission initialized chapter=" + chapter + " mission=" + wave + " events=" + activeMission.spawns().size());
        updateMissionSchedule(0); // only the explicit time-zero group is due
    }

    private void spawnWave(int number) {
        invaders.clear();
        if (chapter == 2) { spawnChapterTwoWave(number); return; }
        int startX = 300;
        if (number == 1) {
            for (int i = 0; i < 10; i++) invaders.add(configure(new Invader(startX + (i % 5) * 250, 120 + (i / 5) * 110, 52, 52, Invader.InvaderKind.BASIC, null), 1, 2, 10));
        } else if (number == 2) {
            for (int i = 0; i < 8; i++) invaders.add(configure(new Invader(190 + (i % 8) * 190, 110, 52, 52, Invader.InvaderKind.BASIC, null), 2, 3, 12));
            for (int i = 0; i < 5; i++) invaders.add(configure(new Invader(320 + i * 260, 230, 56, 56, Invader.InvaderKind.SHOOTER, new ShooterZigZagPattern()), 3, 2, 25));
            for (int i = 0; i < 4; i++) invaders.add(configure(new Invader(420 + i * 300, 330, 60, 60, Invader.InvaderKind.SHIELDED, null), 3, 2, 24));
        } else {
            for (int i = 0; i < 10; i++) invaders.add(configure(new Invader(120 + (i % 10) * 170, 95, 54, 54, Invader.InvaderKind.SWARMER, new SwarmerZigZagPattern()), 2, 3, 16));
            for (int i = 0; i < 7; i++) invaders.add(configure(new Invader(300 + i * 190, 220, 58, 58, Invader.InvaderKind.SHOOTER, new ShooterZigZagPattern()), 4, 2, 30));
            for (int i = 0; i < 5; i++) invaders.add(configure(new Invader(410 + i * 270, 340, 65, 65, Invader.InvaderKind.SHIELDED, null), 5, 2, 32));
            for (int i = 0; i < 3; i++) invaders.add(configure(new Invader(450 + i * 430, 430, 82, 70, Invader.InvaderKind.TANK, new TankPattern()), 9, 1, 60));
        }
    }

    private void spawnChapterTwoWave(int number) {
        if (number == 1) {
            for (int i = 0; i < 45; i++) invaders.add(configure(new Invader(80 + (i % 15) * 120, 70 + (i / 15) * 85, 48, 48, Invader.InvaderKind.BASIC, null), i % 5 == 0 ? 6 : 1, 1, i % 5 == 0 ? 30 : 10));
        } else if (number == 2) {
            for (int i = 0; i < 10; i++) invaders.add(configure(new Invader(90 + i * 175, 100, 58, 58, Invader.InvaderKind.SHIELDED, null), 3, 1, 25));
            for (int i = 0; i < 15; i++) invaders.add(configure(new Invader(80 + (i % 15) * 120, 220, 48, 48, Invader.InvaderKind.BASIC, null), 1, 1, 10));
            for (int i = 0; i < 5; i++) invaders.add(configure(new Invader(250 + i * 310, 330, 64, 64, Invader.InvaderKind.TANK, new TankPattern()), 6, 1, 35));
        } else {
            for (int i = 0; i < 5; i++) invaders.add(configure(new Invader(120 + i * 355, 120, 90, 70, Invader.InvaderKind.TANK, new TankPattern()), 10, 1, 70));
            for (int i = 0; i < 20; i++) invaders.add(configure(new Invader(80 + (i % 10) * 180, 260 + (i / 10) * 85, 48, 48, Invader.InvaderKind.BASIC, null), 1, 1, 10));
        }
    }

    private Invader configure(Invader enemy, int hp, int speed, int points) {
        enemy.hp = hp;
        enemy.vy = speed;
        enemy.touchDamage = switch (enemy.kind) {
            case HAILFIRE -> 5;
            case TANK -> 3;
            default -> 1;
        };
        enemy.scoreValue = points;
        return enemy;
    }

    private void startBoss() {
        int hp = chapter == 1 ? BOSS_MAX_HEALTH : 340;
        boss = configure(new Invader(state.width / 2 - 170, 90, 340, 190, Invader.InvaderKind.TANK, null), hp, 0, 1500);
        boss.armored = false;
        bossMaxHealth = hp;
        sweepAngle = -Math.PI * .85;
        sweepTimer = cannonTimer = 0;
        cannonWarningMs = 0;
        phase = Phase.RUNNING;
    }

    private void finishStage() {
        pendingStage = wave + 1;
        transitionPending = true;
        activeMission = null;
        debug("mission complete chapter=" + chapter + " mission=" + wave + " next=" + pendingStage);
        int reward = 35 + wave * 20;
        // Keep an already-visible card set intact. Its completion will consume any
        // additional overflow and then begin this queued stage.
        if (phase == Phase.UPGRADE) {
            experience.add(reward);
            return;
        }
        if (!grantXp(reward)) startTransition();
    }

    private void defeatBoss() {
        boss = null;
        cannonWarningMs = 0;
        // Collision resolution is currently iterating this list, so defer cleanup.
        clearEnemyProjectiles = true;
        score += 1500;
        pendingComplete = true;
        if (!grantXp(420)) completeAfterUpgrades();
    }

    private boolean grantXp(int amount) {
        experience.add((int) Math.ceil(amount * (1 + upgrades.level(UpgradeId.XP_GAIN) * .15)));
        if (!experience.consumeLevelUp()) return false;
        rollCards();
        phase = Phase.UPGRADE;
        return true;
    }

    private void rollCards() {
        cards.clear();
        List<UpgradeId> available = upgrades.available();
        while (cards.size() < 3 && !available.isEmpty()) cards.add(available.remove(random.nextInt(available.size())));
    }

    private void chooseCard(int index) {
        if (index < 0 || index >= cards.size()) return;
        UpgradeId chosen = cards.get(index);
        if (!upgrades.apply(chosen)) return;
        debug("upgrade selected " + chosen + " transitionPending=" + transitionPending + " chapter=" + chapter + " mission=" + wave);
        if (chosen == UpgradeId.MAX_HEALTH) { maxHealth += 3; health = Math.min(maxHealth, health + 3); }
        if (experience.consumeLevelUp()) { rollCards(); return; }
        if (pendingComplete) completeAfterUpgrades();
        else if (transitionPending) startTransition();
        else { phase = Phase.RUNNING; debug("upgrade overlay closed; resumed active mission"); }
    }

    private void startTransition() {
        transitionPending = false;
        phase = Phase.ANNOUNCEMENT;
        phaseMs = 1700;
        announcement = pendingStage == 4 ? "BOSS INCOMING" : "CHAPTER " + chapter + " — MISSION " + pendingStage;
    }

    private void completeAfterUpgrades() {
        pendingComplete = false;
        if (chapter == 1) phase = Phase.CHAPTER_COMPLETE; else endRun(true);
    }

    private void debug(String message) {
        if (DEBUG) System.out.println("[Campaign] " + message);
    }

    private void startChapterTwo() {
        chapter = 2; wave = 1; pendingStage = 1; announcement = "CHAPTER 2 — GEONOSIS"; phaseMs = 2200; phase = Phase.ANNOUNCEMENT;
    }

    private void endRun(boolean won) {
        if (!scoreRecorded) { scores.recordOnce(score, won ? "Chapter 1 Complete" : "Chapter 1"); scoreRecorded = true; }
        phase = won ? Phase.COMPLETE : Phase.GAME_OVER;
        firing = left = right = false;
    }

    private void returnToMenu() {
        if (state.showStartMenu != null) state.showStartMenu.run();
        else { scenes.clear(); state.mode = GameState.AppMode.START_MENU; }
    }

    private int playerY() { return state.height - state.playerHeight - FLOOR_MARGIN; }
    private int defensiveLine() { return playerY() + state.playerHeight / 2; }
    private Rectangle playerBounds() { return new Rectangle(state.playerX, playerY(), state.playerWidth, state.playerHeight); }

    @Override public void render(Graphics2D g, int width, int height) {
        renderBackground(g, width, height);
        drawDefensiveLine(g);
        for (Invader enemy : invaders) InvaderRenderer.render(g, enemy, state);
        if (boss != null) InvaderRenderer.render(g, boss, state);
        if (cannonWarningMs > 0) drawCannonWarning(g);
        if ((spiderBeamWarmupMs > 0 || spiderBeamMs > 0) && boss != null) drawSpiderBeam(g);
        if (supportFlashMs > 0) { g.setColor(new Color(255, 235, 130, 110)); g.fillRect(supportLane - 45, 0, 90, defensiveLine()); }
        for (Bullet bullet : bullets) renderBullet(g, bullet);
        drawClones(g);
        drawPlayer(g);
        drawHud(g);
        switch (phase) {
            case ANNOUNCEMENT -> drawCenteredMessage(g, announcement, "Prepare yourself", new Color(120, 210, 255));
            case PAUSED -> drawCenteredMessage(g, "PAUSED", "P: resume    U: upgrade collection", Color.WHITE);
            case COLLECTION -> drawCollection(g);
            case UPGRADE -> drawUpgradeCards(g);
            case SUPPORT_WHEEL -> drawSupportWheel(g);
            case CHAPTER_COMPLETE -> drawCenteredMessage(g, "CHAPTER ONE COMPLETE", "Enter/R: deploy to Chapter 2    M/Esc: main menu", new Color(130, 255, 170));
            case GAME_OVER -> drawCenteredMessage(g, "MISSION FAILED", "Enter/R: retry    M/Esc: main menu", new Color(255, 110, 110));
            case COMPLETE -> drawCenteredMessage(g, "CHAPTER ONE COMPLETE", "Enter/R: replay    M/Esc: main menu", new Color(130, 255, 170));
            default -> { }
        }
    }

    private void renderBackground(Graphics2D g, int width, int height) {
        // Theme is conveyed by sparse stars/terrain colors, never a global green wash.
        g.setColor(chapter == 1 ? new Color(10, 18, 29) : new Color(44, 18, 18)); g.fillRect(0, 0, width, height);
        g.setColor(chapter == 1 ? new Color(180, 205, 220, 110) : new Color(240, 140, 85, 110));
        for (int i = 0; i < 75; i++) {
            int x = (i * 283) % width, y = (i * 151) % height;
            g.fillRect(x, y, 2, 2);
        }
    }

    private void drawDefensiveLine(Graphics2D g) {
        int y = defensiveLine();
        g.setColor(new Color(100, 185, 255, 100)); g.fillRect(0, y, state.width, 4);
        g.setColor(new Color(160, 220, 255)); g.drawString("DEFENSIVE BOUNDARY", 20, y - 8);
    }

    private void renderBullet(Graphics2D g, Bullet bullet) {
        if (bullet instanceof EnemyBullet) {
            if (bullet instanceof HailfireRocket) {
                g.setColor(new Color(100, 45, 165, 130));
                g.fillOval(bullet.x - 5, bullet.y + 5, bullet.size + 10, bullet.size + 10);
                g.setColor(Color.BLACK);
            } else g.setColor(bullet.damage >= 4 ? new Color(255, 130, 50) : new Color(255, 70, 70));
            g.fillOval(bullet.x, bullet.y, bullet.size, bullet.size);
        } else BulletRenderers.render(g, bullet, state);
    }

    private void drawPlayer(Graphics2D g) {
        if (hitFlashMs > 0 && (hitFlashMs / 45) % 2 == 0) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .35f));
        Image image = state.playerImage;
        if (image != null) g.drawImage(image, state.playerX, playerY(), state.playerWidth, state.playerHeight, null);
        else { g.setColor(Color.CYAN); g.fillRect(state.playerX, playerY(), state.playerWidth, state.playerHeight); }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void drawClones(Graphics2D g) {
        Image image = state.playerImage;
        for (SupportClone clone : clones) {
            if (image != null) g.drawImage(image, clone.x, clone.y(), 36, 48, null);
            else { g.setColor(new Color(110, 210, 255)); g.fillRect(clone.x, clone.y(), 36, 48); }
            g.setColor(Color.WHITE); g.drawString("SUPPORT", clone.x - 5, clone.y() - 5);
        }
    }

    private void drawHud(Graphics2D g) {
        drawBar(g, 30, 28, 350, 24, health, maxHealth, new Color(220, 65, 65), "HEALTH " + health + "/" + maxHealth);
        drawBar(g, 30, 62, 350, 16, experience.xp(), experience.nextRequirement(), new Color(95, 150, 255), "XP  L" + (experience.completedLevels() + 1));
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("SCORE " + score, state.width - 230, 46);
        g.drawString(boss == null ? "CH " + chapter + "  MISSION " + wave : (chapter == 1 ? "AAT TANK" : "OG-9 SPIDER"), state.width - 300, 76);
        if (boss != null) drawBar(g, state.width / 2 - 360, 24, 720, 22, Math.max(0, boss.hp), bossMaxHealth, new Color(230, 165, 50), chapter == 1 ? "AAT TANK" : "OG-9 HOMING SPIDER DROID");
        g.setFont(new Font("SansSerif", Font.PLAIN, 16)); g.setColor(new Color(210, 225, 245));
        g.drawString("A/D move   Space: all unlocked weapons   R: weapons & support info   P pause   U upgrades", 30, state.height - 18);
        drawSupportHotbar(g);
    }

    private void drawSupportHotbar(Graphics2D g) {
        UpgradeId[] ids = { UpgradeId.CLONE_REINFORCEMENTS, UpgradeId.ORBITAL_STRIKE, UpgradeId.AIRSTRIKE, UpgradeId.NUKE };
        String[] names = { "CLONES", "ORBITAL", "AIRSTRIKE", "NUKE" };
        int x = state.width / 2 - 360;
        for (int i = 0; i < ids.length; i++) {
            boolean unlocked = upgrades.level(ids[i]) > 0;
            int charges = unlocked ? chargesFor(ids[i]) : 0;
            g.setColor(unlocked && charges > 0 ? new Color(35, 75, 112, 220) : new Color(25, 25, 30, 220));
            g.fillRoundRect(x + i * 185, state.height - 90, 170, 48, 10, 10);
            g.setColor(unlocked ? Color.WHITE : new Color(110, 110, 110));
            g.drawRoundRect(x + i * 185, state.height - 90, 170, 48, 10, 10);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString((i + 1) + " " + (unlocked ? names[i] : "LOCKED"), x + 10 + i * 185, state.height - 68);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString(unlocked ? "x" + charges : "???", x + 10 + i * 185, state.height - 50);
        }
    }

    private void drawBar(Graphics2D g, int x, int y, int w, int h, int value, int max, Color color, String label) {
        g.setColor(new Color(0, 0, 0, 180)); g.fillRect(x, y, w, h);
        g.setColor(color); g.fillRect(x + 2, y + 2, Math.max(0, Math.min(w - 4, (w - 4) * value / Math.max(1, max))), h - 4);
        g.setColor(Color.WHITE); g.drawRect(x, y, w, h); g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.drawString(label, x + 8, y + h - 6);
    }

    private void drawCannonWarning(Graphics2D g) {
        float alpha = (float) (.25 + .45 * Math.abs(Math.sin(cannonWarningMs / 80.0)));
        g.setColor(new Color(255, 80, 50, (int) (alpha * 255)));
        g.fillRect(cannonLane - 30, boss.y + boss.height - 10, 60, defensiveLine() - boss.y - boss.height + 10);
        g.setColor(Color.ORANGE); g.drawString("CANNON LOCK", cannonLane - 45, boss.y + boss.height + 24);
    }

    private void drawSpiderBeam(Graphics2D g) {
        int ox = boss.x + boss.width / 2, oy = boss.y + boss.height / 2;
        int ex = ox + (int) (Math.cos(spiderBeamAngle) * state.height);
        int ey = oy + (int) (Math.sin(spiderBeamAngle) * state.height);
        boolean active = spiderBeamMs > 0;
        g.setColor(active ? new Color(255, 70, 45, 220) : new Color(255, 180, 70, 90));
        g.setStroke(new BasicStroke(active ? 18f : 4f));
        g.drawLine(ox, oy, ex, ey);
        g.setColor(Color.ORANGE); g.drawString(active ? "TRACKING BEAM" : "BEAM LOCK", ox - 50, oy + 22);
    }

    private void drawCenteredMessage(Graphics2D g, String title, String subtitle, Color color) {
        overlay(g);
        g.setColor(color); g.setFont(new Font("SansSerif", Font.BOLD, 48)); center(g, title, state.height / 2 - 20);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.PLAIN, 24)); center(g, subtitle, state.height / 2 + 30);
    }

    private void drawUpgradeCards(Graphics2D g) {
        overlay(g);
        g.setColor(new Color(255, 220, 120)); g.setFont(new Font("SansSerif", Font.BOLD, 38)); center(g, "LEVEL UP — CHOOSE ONE", 180);
        for (int i = 0; i < cards.size(); i++) {
            UpgradeId id = cards.get(i);
            int x = 180 + i * 530, y = 270, w = 450, h = 330;
            g.setColor(new Color(13, 25, 50, 245)); g.fillRoundRect(x, y, w, h, 18, 18);
            g.setColor(new Color(100, 185, 255)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(x, y, w, h, 18, 18);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 28)); g.drawString((i + 1) + ". " + id.title(), x + 24, y + 58);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20)); drawWrapped(g, id.description(), x + 24, y + 105, w - 48, 28);
            g.setColor(new Color(255, 220, 120)); g.drawString(upgrades.level(id) == 0 ? "NEW" : "LEVEL " + upgrades.level(id) + " → " + (upgrades.level(id) + 1), x + 24, y + h - 34);
        }
    }

    private void drawSupportWheel(Graphics2D g) {
        overlay(g);
        g.setColor(new Color(255, 220, 120)); g.setFont(new Font("SansSerif", Font.BOLD, 34)); center(g, "WEAPONS & SUPPORT", 150);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        center(g, "Blaster: reliable and accurate — base damage 1", 180);
        center(g, "Missiles: wavy, high damage 5, deliberately less accurate", 202);
        center(g, "Blades: diagonal, piercing, wall-bouncing — base damage 1", 224);
        String[] names = { "1. Clone Reinforcements", "2. Orbital Strike", "3. Airstrike", "4. Nuke" };
        UpgradeId[] ids = { UpgradeId.CLONE_REINFORCEMENTS, UpgradeId.ORBITAL_STRIKE, UpgradeId.AIRSTRIKE, UpgradeId.NUKE };
        for (int i = 0; i < names.length; i++) {
            int x = i < 2 ? 350 : 1040, y = i % 2 == 0 ? 300 : 470;
            boolean unlocked = upgrades.level(ids[i]) > 0;
            g.setColor(unlocked ? new Color(45, 82, 120) : new Color(40, 40, 45)); g.fillRoundRect(x, y, 520, 105, 16, 16);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 21)); g.drawString(unlocked ? names[i] : (i + 1) + ". ???", x + 24, y + 40);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16)); g.drawString(unlocked ? ids[i].description() + "  Key " + (i + 1) + "  x" + chargesFor(ids[i]) : ids[i].hint(), x + 24, y + 75);
        }
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.PLAIN, 18)); center(g, "Use 1–4 during gameplay to activate. R or Esc closes this information screen.", 675);
    }

    private void drawCollection(Graphics2D g) {
        overlay(g);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 34)); center(g, "UPGRADE COLLECTION", 105);
        int col = 0, row = 0;
        for (UpgradeId id : UpgradeId.values()) {
            int x = 140 + col * 830, y = 150 + row * 125;
            boolean unlocked = upgrades.level(id) > 0;
            g.setColor(unlocked ? new Color(20, 55, 85) : new Color(25, 25, 32)); g.fillRoundRect(x, y, 760, 100, 12, 12);
            g.setColor(unlocked ? new Color(100, 210, 255) : new Color(80, 80, 88)); g.drawRoundRect(x, y, 760, 100, 12, 12);
            g.setFont(new Font("SansSerif", Font.BOLD, 19)); g.setColor(unlocked ? Color.WHITE : new Color(155, 155, 165));
            g.drawString(unlocked ? id.title() + " — Level " + upgrades.level(id) + "/" + id.maxLevel() : "???", x + 18, y + 30);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16)); drawWrapped(g, unlocked ? id.description() : id.hint(), x + 18, y + 58, 720, 21);
            row++; if (row == 6) { row = 0; col++; }
        }
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.PLAIN, 19)); center(g, "U or Esc to return", state.height - 35);
    }

    private void overlay(Graphics2D g) { g.setColor(new Color(0, 0, 0, 185)); g.fillRect(0, 0, state.width, state.height); }
    private void center(Graphics2D g, String text, int y) { g.drawString(text, (state.width - g.getFontMetrics().stringWidth(text)) / 2, y); }
    private void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight) {
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (String word : text.split(" ")) {
            if (g.getFontMetrics().stringWidth(line + word) > width) { g.drawString(line.toString(), x, currentY); currentY += lineHeight; line.setLength(0); }
            if (!line.isEmpty()) line.append(' '); line.append(word);
        }
        if (!line.isEmpty()) g.drawString(line.toString(), x, currentY);
    }

    private final class SupportClone {
        int x, direction = 1, hp, reviveMs; double cooldown;
        SupportClone(int x) { this.x = x; this.hp = maxHealth(); }
        int maxHealth() { return 5 + upgrades.level(UpgradeId.CLONE_HEALTH) * 3; }
        int y() { return playerY() + 10; }
    }
}
