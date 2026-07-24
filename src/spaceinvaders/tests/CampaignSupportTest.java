package spaceinvaders.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import spaceinvaders.core.GameState;
import spaceinvaders.core.SceneManager;
import spaceinvaders.features.CampaignScene;
import spaceinvaders.features.campaign.ExperienceProgress;
import spaceinvaders.features.campaign.RunUpgrades;
import spaceinvaders.features.campaign.UpgradeId;
import spaceinvaders.core.entities.Blade;
import spaceinvaders.core.entities.Invader;
import spaceinvaders.core.entities.HailfireRocket;
import spaceinvaders.core.entities.MttTransport;
import spaceinvaders.core.entities.WavyMissile;
import spaceinvaders.features.campaign.CampaignDefinitions;
import spaceinvaders.features.campaign.EnemyType;
import spaceinvaders.services.scores.HighScoreService;

/** Dependency-free smoke tests runnable with plain javac/java. */
public final class CampaignSupportTest {
    private CampaignSupportTest() { }

    public static void main(String[] args) throws Exception {
        ExperienceProgress xp = new ExperienceProgress();
        xp.add(360);
        require(xp.consumeLevelUp(), "first XP level should trigger");
        require(xp.consumeLevelUp(), "overflow should trigger a second level");
        require(xp.nextRequirement() == 150 && xp.completedLevels() == 2 && xp.xp() == 172, "XP curve or overflow was not retained");
        require(new ExperienceProgress().nextRequirement() == 75, "initial XP threshold was not reduced by 25 percent");

        RunUpgrades upgrades = new RunUpgrades();
        require(upgrades.apply(UpgradeId.BLASTER_DAMAGE), "first upgrade should apply");
        require(upgrades.apply(UpgradeId.BLASTER_DAMAGE), "stacking upgrade should apply");
        require(upgrades.level(UpgradeId.BLASTER_DAMAGE) == 2, "upgrade level was not stacked");
        require(upgrades.cardWeight(UpgradeId.MISSILE_DAMAGE) == 1.0, "unfavored path did not start at base weight");
        upgrades.favor(UpgradeId.MISSILE_LAUNCHER.path());
        upgrades.favor(UpgradeId.MISSILE_DAMAGE.path());
        require(upgrades.cardWeight(UpgradeId.MISSILE_DAMAGE) == 1.5, "path affinity was not capped at 1.5x");

        WavyMissile wavy = new WavyMissile(100, 200, -8, 5, 0);
        wavy.update(); wavy.update(); wavy.update();
        require(wavy.y < 200 && wavy.x != 100, "base missile lost its smooth wavy flight");

        Blade blade = new Blade(-2, 40, -7, -2, 12, 3, 1, false, false, false);
        blade.updateBlade(16, 100, 100);
        require(blade.x == 0 && blade.vx > 0, "wall bounce did not clamp and reflect safely");
        blade.updateBlade(16, 100, 100);
        require(blade.x > 0, "blade remained embedded in the wall");

        require(CampaignDefinitions.mission(1, 1).totalEnemies() == 10, "chapter 1 mission 1 total is wrong");
        require(CampaignDefinitions.mission(1, 1).spawns().get(1).timeMs() == 3000, "chapter 1 mission 1 second group timing is wrong");
        require(CampaignDefinitions.mission(1, 2).count(EnemyType.B1) == 25, "chapter 1 mission 2 composition is wrong");
        require(CampaignDefinitions.mission(1, 3).count(EnemyType.B1) == 25 && CampaignDefinitions.mission(1, 3).count(EnemyType.B2) == 10, "chapter 1 mission 3 composition is wrong");
        require(CampaignDefinitions.mission(2, 1).count(EnemyType.B1) == 35 && CampaignDefinitions.mission(2, 1).count(EnemyType.B2) == 10, "chapter 2 mission 1 composition is wrong");
        require(CampaignDefinitions.mission(2, 2).count(EnemyType.DROIDEKA) == 10 && CampaignDefinitions.mission(2, 2).totalEnemies() == 30, "chapter 2 mission 2 composition is wrong");
        require(CampaignDefinitions.mission(2, 3).count(EnemyType.HAILFIRE) == 5 && CampaignDefinitions.mission(2, 3).count(EnemyType.B1) == 20, "chapter 2 mission 3 composition is wrong");
        require(CampaignDefinitions.mission(3, 1).count(EnemyType.B1) == 15 && CampaignDefinitions.mission(3, 1).count(EnemyType.HAILFIRE) == 14 && CampaignDefinitions.mission(3, 1).totalEnemies() == 29, "chapter 3 mission 1 composition is wrong");
        require(CampaignDefinitions.mission(3, 2).count(EnemyType.MTT) == 2 && CampaignDefinitions.mission(3, 2).totalEnemies() == 2, "chapter 3 mission 2 composition is wrong");
        require(CampaignDefinitions.mission(3, 3).count(EnemyType.MTT) == 2 && CampaignDefinitions.mission(3, 3).count(EnemyType.HAILFIRE) == 10, "chapter 3 mission 3 composition is wrong");

        String oldHome = System.getProperty("user.home");
        Path temporaryHome = Files.createTempDirectory("separatist-invaders-test");
        System.setProperty("user.home", temporaryHome.toString());
        HighScoreService scores = new HighScoreService();
        scores.recordOnce(120, "Chapter 1");
        scores.recordOnce(900, "Chapter 1 Complete");
        scores.recordOnce(900, "Chapter 1 Complete");
        require(scores.load().size() == 2, "duplicate high score was stored");
        require(scores.load().get(0).score() == 900, "scores were not sorted descending");
        System.setProperty("user.home", oldHome);

        verifyCampaignOverlayAndSchedule();
        System.out.println("Campaign support tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Regression coverage for the previously broken upgrade/mission lifecycle. */
    @SuppressWarnings("unchecked")
    private static void verifyCampaignOverlayAndSchedule() throws Exception {
        CampaignScene scene = new CampaignScene(new GameState(), new SceneManager());
        scene.onEnter();
        scene.update(1800); // announcement ends; only the time-zero event is due
        List<Object> enemies = (List<Object>) field(scene, "invaders");
        require(enemies.size() == 5, "Mission 1 spawned more than its time-zero group");
        scene.update(2999);
        require(enemies.size() == 5, "Mission 1 second group spawned before three seconds");
        scene.update(1);
        require(enemies.size() == 10, "Mission 1 second group did not spawn at three seconds");

        Object experience = field(scene, "experience");
        setField(experience, "xp", 99);
        invoke(scene, "grantXp", new Class<?>[] { int.class }, 1); // opens an upgrade while Mission 1 remains active
        int mission = (int) field(scene, "wave");
        int spawnCursor = (int) field(scene, "nextSpawnEvent");
        double elapsed = (double) field(scene, "missionElapsedMs");
        int active = enemies.size();
        scene.handleKeyPressed(KeyEvent.VK_1);
        require((int) field(scene, "wave") == mission && (int) field(scene, "nextSpawnEvent") == spawnCursor,
                "in-mission upgrade changed mission or replayed spawn events");
        require(enemies.size() == active && (double) field(scene, "missionElapsedMs") == elapsed,
                "in-mission upgrade reset active enemies or mission time");

        setField(experience, "xp", 99);
        enemies.clear();
        scene.update(1); // completion reward opens the card overlay
        scene.handleKeyPressed(KeyEvent.VK_1);
        scene.update(1700); // next-mission transition
        require((int) field(scene, "wave") == 2 && enemies.size() == 5,
                "Mission 1 reward did not advance to Mission 2's first scheduled group");

        Object upgrades = field(scene, "upgrades");
        upgrades.getClass().getMethod("apply", UpgradeId.class).invoke(upgrades, UpgradeId.NUKE);
        scene.handleKeyPressed(KeyEvent.VK_4);
        require((double) field(scene, "supportGlobalCooldownMs") == 1000, "support activation did not start the shared one-second cooldown");
        scene.update(1000);
        require(enemies.isEmpty() && (double) field(scene, "nukeCooldown") > 0,
                "top-row 4 did not activate and detonate Nuke");
        require(((java.util.Map<?, ?>) field(scene, "supportCharges")).get(UpgradeId.NUKE).equals(2),
                "Nuke did not consume exactly one support charge");

        CampaignScene numpadScene = new CampaignScene(new GameState(), new SceneManager());
        numpadScene.onEnter(); numpadScene.update(1800);
        Object numpadUpgrades = field(numpadScene, "upgrades");
        numpadUpgrades.getClass().getMethod("apply", UpgradeId.class).invoke(numpadUpgrades, UpgradeId.NUKE);
        numpadScene.handleKeyPressed(KeyEvent.VK_NUMPAD4);
        numpadScene.update(1000);
        require(((List<Object>) field(numpadScene, "invaders")).isEmpty(), "numpad 4 did not activate Nuke");

        Invader hailfire = (Invader) invoke(numpadScene, "createCampaignEnemy", new Class<?>[] { EnemyType.class, int.class, int.class }, EnemyType.HAILFIRE, 400, 1);
        invoke(numpadScene, "updateHailfireBarrage", new Class<?>[] { Invader.class }, hailfire);
        hailfire.ageMs = 4000;
        for (int shot = 0; shot < 8; shot++) {
            invoke(numpadScene, "updateHailfireBarrage", new Class<?>[] { Invader.class }, hailfire);
            hailfire.ageMs += 200;
        }
        long rockets = ((List<Object>) field(numpadScene, "bullets")).stream().filter(HailfireRocket.class::isInstance).count();
        require(rockets >= 4 && rockets <= 8, "Hailfire first barrage was not a 4–8 missile barrage");

        CampaignScene bossScene = new CampaignScene(new GameState(), new SceneManager());
        bossScene.onEnter();
        setField(bossScene, "chapter", 2);
        invoke(bossScene, "startBoss", new Class<?>[0]);
        int beamHealth = (int) field(bossScene, "health");
        bossScene.update(7200); // starts locked warm-up
        bossScene.update(1400); // transitions to active beam, still no warm-up damage
        require((int) field(bossScene, "health") == beamHealth, "OG-9 warm-up beam dealt damage");
        bossScene.update(1000);
        require((int) field(bossScene, "health") < beamHealth, "OG-9 active beam did not use delta-time damage");

        MttTransport mtt = (MttTransport) invoke(bossScene, "createCampaignEnemy", new Class<?>[] { EnemyType.class, int.class, int.class }, EnemyType.MTT, 300, 500);
        invoke(bossScene, "updateMtt", new Class<?>[] { MttTransport.class, int.class }, mtt, 256);
        require(mtt.state == MttTransport.State.DEPLOYMENT_PAUSE, "MTT did not stop at the calculated halfway point");
        invoke(bossScene, "updateMtt", new Class<?>[] { MttTransport.class, int.class }, mtt, 2000);
        invoke(bossScene, "updateMtt", new Class<?>[] { MttTransport.class, int.class }, mtt, 3000);
        require(mtt.deployed == 10, "MTT did not deploy exactly ten B1s");
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void setField(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static Object invoke(Object object, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(object, arguments);
    }
}
