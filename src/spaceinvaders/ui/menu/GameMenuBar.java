package spaceinvaders.ui.menu;

import java.awt.Image;
import java.io.File;
import java.net.URI;
import javax.imageio.ImageIO;
import javax.swing.*;
import spaceinvaders.core.GameState;
import spaceinvaders.services.audio.MusicPlayer;
import spaceinvaders.services.exceptions.GameExceptions;
import spaceinvaders.services.loading.AssetLoader;

/** Menubar that configures images, bullet type, music, and SANDBOX upgrades. */
public class GameMenuBar extends JMenuBar {
    private final GameState state;              // writes directly to the model
    private final JComponent repaintTarget;     // call repaint() after changes
    private final MusicPlayer music = new MusicPlayer();

    public GameMenuBar(GameState state, JComponent repaintTarget) {
        this.state = state;
        this.repaintTarget = repaintTarget;

        add(buildPlayerMenu());
        add(buildInvaderMenu());
        add(buildBulletMenu());
        add(buildMusicMenu());

        // NEW
        add(buildUpgradesMenu());
    }

    /* ================= Player Image ================= */

    private JMenu buildPlayerMenu() {
        JMenu m = new JMenu("Player Image");
        m.add(item("Jango Fett", () -> setPlayer(fromRes("image/jango.png"))));
        m.add(item("Master Chief", () -> setPlayer(fromRes("image/chief.png"))));
        m.add(item("Captain Qwark", () -> setPlayer(fromRes("image/qwark.png"))));
        m.addSeparator();
        m.add(item("Custom…", () -> chooseCustomImage(this::setPlayer, "Choose player image")));
        return m;
    }

    private void setPlayer(Image img) {
        if (img == null) {
            GameExceptions.showErrorDialog("Could not load player image.");
            return;
        }
        state.playerImage = img;
        repaintTarget.repaint();
    }

    /* ================= Invader Image ================= */

    private JMenu buildInvaderMenu() {
        JMenu m = new JMenu("Invader Image");
        m.add(item("Master Yoda", () -> setInvader(fromRes("image/yoda.png"))));
        m.add(item("B1 Battle Droid", () -> setInvader(fromRes("image/b1_droid.png"))));
        m.add(item("Phase-1 Clone", () -> setInvader(fromRes("image/p1_clone.png"))));
        m.addSeparator();
        m.add(item("Custom…", () -> chooseCustomImage(this::setInvader, "Choose invader image")));
        return m;
    }

    private void setInvader(Image img) {
        if (img == null) {
            GameExceptions.showErrorDialog("Could not load invader image.");
            return;
        }
        state.invaderImage = img;
        state.invaderImageBasic = img;
        repaintTarget.repaint();
    }

    /* ================= Bullet Type ================= */

    private JMenu buildBulletMenu() {
        JMenu m = new JMenu("Bullet Type");
        m.add(item("Triangle", () -> { state.bulletType = GameState.BulletType.TRIANGLE; repaintTarget.repaint(); }));
        m.add(item("Circle",   () -> { state.bulletType = GameState.BulletType.CIRCLE;   repaintTarget.repaint(); }));
        m.add(item("Square",   () -> { state.bulletType = GameState.BulletType.SQUARE;   repaintTarget.repaint(); }));
        m.addSeparator();
        m.add(item("Blaster Bolt (image)", () -> {
            Image img = fromRes("image/blaster_bolt.png");
            if (img != null) {
                state.bulletImage = img;
                state.bulletType = GameState.BulletType.IMAGE;
                repaintTarget.repaint();
            } else {
                GameExceptions.showErrorDialog("Could not load image/blaster_bolt.png");
            }
        }));
        m.add(item("Custom Image…", () ->
                chooseCustomImage(img -> {
                    if (img != null) {
                        state.bulletImage = img;
                        state.bulletType = GameState.BulletType.IMAGE;
                        repaintTarget.repaint();
                    }
                }, "Choose bullet image")));
        return m;
    }

    /* ================= Music ================= */

    private JMenu buildMusicMenu() {
        JMenu m = new JMenu("Music");
        m.add(item("Play \"The Heist\"", () -> playRes("/spaceinvaders/resources/audio/the_heist.wav")));
        m.add(item("Play \"New Directive\"", () -> playRes("/spaceinvaders/resources/audio/new_directive.wav")));
        m.add(item("Play \"End of Line\"", () -> playRes("/spaceinvaders/resources/audio/end_of_line.wav")));
        m.add(item("Play \"Droid March\"", () -> playRes("/spaceinvaders/resources/audio/droid_march.wav")));
        m.add(item("Play \"This is War\"", () -> playRes("/spaceinvaders/resources/audio/this_is_war.wav")));
        m.add(item("Play \"The Battle in the Snow\"", () -> playRes("/spaceinvaders/resources/audio/the_battle_in_the_snow.wav")));
        m.addSeparator();
        m.add(item("Play from URL…", this::playFromUrl));
        m.add(item("Play local file…", this::playFromFile));
        m.addSeparator();
        m.add(item("Stop", music::stop));
        return m;
    }

    private void playRes(String resourcePath) {
        try { music.playLoopFromResource(resourcePath); }
        catch (Exception ex) { GameExceptions.showErrorDialog("Music error: " + ex.getMessage()); }
    }

    private void playFromUrl() {
        String url = JOptionPane.showInputDialog(null, "Enter WAV/MP3 URL:");
        if (url == null || url.isBlank()) return;
        try { music.playLoopFromUrl(url); }
        catch (Exception ex) { GameExceptions.showErrorDialog("Music URL error: " + ex.getMessage()); }
    }

    private void playFromFile() {
        JFileChooser ch = new JFileChooser();
        if (ch.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File f = ch.getSelectedFile();
            try { music.playLoopFromFile(f); }
            catch (Exception ex) { GameExceptions.showErrorDialog("Music file error: " + ex.getMessage()); }
        }
    }

    /* ================= Upgrades ================= */

    private JMenu buildUpgradesMenu() {
        JMenu root = new JMenu("Upgrades");

        root.add(buildBasicShotMenu());
        root.add(buildMissileMenu());
        root.add(buildBladeMenu());
        root.add(buildPlayerUpgradesMenu());
        root.add(buildCloneMenu());

        root.addSeparator();
        root.add(item("Recompute Core Stats", () -> {
            state.player.recomputeCoreStats();
            repaintTarget.repaint();
        }));

        root.add(item("Reset ALL Upgrades", () -> {
            resetAllUpgrades();
            state.player.recomputeCoreStats();
            repaintTarget.repaint();
        }));

        return root;
    }

    /* ----- Basic shot ----- */

    private JMenu buildBasicShotMenu() {
        JMenu m = new JMenu("Blaster");

        m.add(levelMenu("Damage +100% (ranks)", 2,
                () -> state.player.upBasicDmg100,
                v -> state.player.upBasicDmg100 = v));

        m.add(levelMenu("Pierce +3 (ranks)", 3,
                () -> state.player.upBasicPiercePlus3,
                v -> state.player.upBasicPiercePlus3 = v));

        m.add(levelMenu("Fire Rate +25% (ranks)", 2,
                () -> state.player.upBasicFireRate25,
                v -> state.player.upBasicFireRate25 = v));

        m.add(levelMenu("+1 Projectile (ranks)", 2,
                () -> state.player.upBasicPlus1Bullets,
                v -> state.player.upBasicPlus1Bullets = v));

        m.add(toggle("Armor Piercing", () -> state.player.upBasicArmorPierce,
                v -> state.player.upBasicArmorPierce = v));

        m.addSeparator();

        // legendary flags
        m.add(toggle("Smart Targeting", () -> state.player.upBasicSmart,
                v -> state.player.upBasicSmart = v));

        m.add(levelMenu("Overtuned 6% proc (ranks)", 2,
                () -> state.player.upBasicOvertuned6,
                v -> state.player.upBasicOvertuned6 = v));

        m.add(toggle("Money Shot", () -> state.player.upBasicMoneyShot,
                v -> state.player.upBasicMoneyShot = v));

        m.add(toggle("Systems Fried", () -> state.player.upBasicSystemsFried,
                v -> state.player.upBasicSystemsFried = v));

        return m;
    }

    /* ----- Missiles ----- */

    private JMenu buildMissileMenu() {
        JMenu m = new JMenu("Missile");

        m.add(toggle("Straight Flight", () -> state.player.upMissileStraight,
                v -> state.player.upMissileStraight = v));

        m.add(levelMenu("Damage +100% (ranks)", 4,
                () -> state.player.upMissileDmg100,
                v -> state.player.upMissileDmg100 = v));

        m.add(levelMenu("+1 Missile (ranks)", 2,
                () -> state.player.upMissilePlus1,
                v -> state.player.upMissilePlus1 = v));

        m.add(levelMenu("Shrapnel +3 (ranks)", 3,
                () -> state.player.upMissilePlus3Shrapnel,
                v -> state.player.upMissilePlus3Shrapnel = v));

        m.add(levelMenu("Shrapnel Dmg +2 (ranks)", 2,
                () -> state.player.upMissileShrapnelDmgPlus2,
                v -> state.player.upMissileShrapnelDmgPlus2 = v));

        m.add(levelMenu("Fire Rate +25% (ranks)", 2,
                () -> state.player.upMissileFireRate25,
                v -> state.player.upMissileFireRate25 = v));

        m.addSeparator();

        // legendary
        m.add(toggle("Missile Smart", () -> state.player.upMissileSmart,
                v -> state.player.upMissileSmart = v));

        m.add(toggle("Shrapnel Smart", () -> state.player.upShrapnelSmart,
                v -> state.player.upShrapnelSmart = v));

        m.add(levelMenu("Shrapnel Armor Pierce (ranks)", 2,
                () -> state.player.upShrapnelArmorPierce,
                v -> state.player.upShrapnelArmorPierce = v));

        m.add(toggle("Impact AOE", () -> state.player.upMissileImpactAOE,
                v -> state.player.upMissileImpactAOE = v));

        m.add(levelMenu("Secret Tech Missile (0/1)", 1,
                () -> state.player.secretTechMissile,
                v -> state.player.secretTechMissile = v));

        return m;
    }

    /* ----- Blades ----- */

    private JMenu buildBladeMenu() {
        JMenu m = new JMenu("Blade");

        m.add(levelMenu("North Ricochet (0/1)", 1,
                () -> state.player.upBladeNorthRicochet,
                v -> state.player.upBladeNorthRicochet = v));

        m.add(levelMenu("South Ricochet (0/1)", 1,
                () -> state.player.upBladeSouthRicochet,
                v -> state.player.upBladeSouthRicochet = v));

        m.add(levelMenu("Damage +100% (ranks)", 2,
                () -> state.player.upBladeDmg100,
                v -> state.player.upBladeDmg100 = v));

        m.add(levelMenu("Bounces +3 (ranks)", 3,
                () -> state.player.upBladeBouncesPlus3,
                v -> state.player.upBladeBouncesPlus3 = v));

        m.add(levelMenu("Pierce +3 (ranks)", 3,
                () -> state.player.upBladePiercePlus3,
                v -> state.player.upBladePiercePlus3 = v));

        m.add(levelMenu("Fire Rate +25% (ranks)", 2,
                () -> state.player.upBladeFireRate25,
                v -> state.player.upBladeFireRate25 = v));

        m.add(toggle("Armor Pen", () -> state.player.upBladeArmorPen,
                v -> state.player.upBladeArmorPen = v));

        m.add(toggle("Shield Null", () -> state.player.upBladeShieldNull,
                v -> state.player.upBladeShieldNull = v));

        // convenience flags you had up top too
        m.addSeparator();
        m.add(toggle("Vertical Bounce (alt flag)", () -> state.player.upBladeVerticalBounce,
                v -> state.player.upBladeVerticalBounce = v));
        m.add(toggle("Legendary Split (alt flag)", () -> state.player.upBladeLegendarySplit,
                v -> state.player.upBladeLegendarySplit = v));

        m.addSeparator();

        // legendary
        m.add(toggle("Initial Split", () -> state.player.upBladeInitialSplit,
                v -> state.player.upBladeInitialSplit = v));

        m.add(toggle("Blade Smart", () -> state.player.upBladeSmart,
                v -> state.player.upBladeSmart = v));

        m.add(toggle("Energy Trail", () -> state.player.upBladeEnergyTrail,
                v -> state.player.upBladeEnergyTrail = v));

        m.add(levelMenu("Secret Tech Blade (0/1)", 1,
                () -> state.player.secretTechBlade,
                v -> state.player.secretTechBlade = v));

        return m;
    }

    /* ----- Player ----- */

    private JMenu buildPlayerUpgradesMenu() {
        JMenu m = new JMenu("Player");

        m.add(levelMenu("Speed +35% (ranks)", 3,
                () -> state.player.upPlayerSpeed35,
                v -> { state.player.upPlayerSpeed35 = v; state.player.recomputeCoreStats(); }));

        m.add(levelMenu("HP +15 / Border +25 (ranks)", 3,
                () -> state.player.upPlayerHp15_Border25,
                v -> { state.player.upPlayerHp15_Border25 = v; state.player.recomputeCoreStats(); }));

        m.add(levelMenu("Money +45 (ranks)", 2,
                () -> state.player.upMoney45,
                v -> state.player.upMoney45 = v));

        m.add(toggle("Overclock +10%", () -> state.player.upOverclock10,
                v -> state.player.upOverclock10 = v));

        m.add(toggle("Sixth Sense", () -> state.player.upSixthSense,
                v -> state.player.upSixthSense = v));

        m.addSeparator();

        // legendary
        m.add(toggle("Hybrid Weapon Core", () -> state.player.upHybridWeaponCore,
                v -> state.player.upHybridWeaponCore = v));

        m.add(levelMenu("Y-Wing Support (ranks)", 3,
                () -> state.player.upYwingSupport,
                v -> state.player.upYwingSupport = v));

        m.add(toggle("Smarter Targeting", () -> state.player.upSmarterTargeting,
                v -> state.player.upSmarterTargeting = v));

        m.add(toggle("Personal Shield", () -> state.player.upPersonalShield,
                v -> state.player.upPersonalShield = v));

        return m;
    }

    /* ----- Clone support ----- */

    private JMenu buildCloneMenu() {
        JMenu m = new JMenu("Clone Support");

        m.add(levelMenu("Reinforcement (ranks)", 5,
                () -> state.player.upCloneReinforcement,
                v -> state.player.upCloneReinforcement = v));

        m.add(toggle("Clone Missile Arm", () -> state.player.upCloneMissileArm,
                v -> state.player.upCloneMissileArm = v));

        m.add(toggle("Clone Blade Arm", () -> state.player.upCloneBladeArm,
                v -> state.player.upCloneBladeArm = v));

        m.add(levelMenu("Cooling +35% (ranks)", 2,
                () -> state.player.upCloneCooling35,
                v -> state.player.upCloneCooling35 = v));

        m.addSeparator();

        // legendary
        m.add(toggle("Clone Uses Basic Upgrades", () -> state.player.upCloneUsesBasicUpgrades,
                v -> state.player.upCloneUsesBasicUpgrades = v));

        m.add(toggle("Clone Uses Missile Upgrades", () -> state.player.upCloneUsesMissileUpgrades,
                v -> state.player.upCloneUsesMissileUpgrades = v));

        m.add(toggle("Clone Uses Blade Upgrades", () -> state.player.upCloneUsesBladeUpgrades,
                v -> state.player.upCloneUsesBladeUpgrades = v));

        return m;
    }

    private void resetAllUpgrades() {
        // Missile (basic)
        state.player.upMissileStraight = false;
        state.player.upMissileDmg100 = 0;
        state.player.upMissilePlus1 = 0;
        state.player.upMissilePlus3Shrapnel = 0;
        state.player.upMissileShrapnelDmgPlus2 = 0;
        state.player.upMissileFireRate25 = 0;

        // Missile (legendary)
        state.player.upMissileSmart = false;
        state.player.upShrapnelSmart = false;
        state.player.upShrapnelArmorPierce = 0;
        state.player.upMissileImpactAOE = false;
        state.player.secretTechMissile = 0;

        // Blade (basic)
        state.player.upBladeNorthRicochet = 0;
        state.player.upBladeSouthRicochet = 0;
        state.player.upBladeDmg100 = 0;
        state.player.upBladeBouncesPlus3 = 0;
        state.player.upBladePiercePlus3 = 0;
        state.player.upBladeFireRate25 = 0;
        state.player.upBladeArmorPen = false;
        state.player.upBladeShieldNull = false;

        // Blade (extra flags)
        state.player.upBladeVerticalBounce = false;
        state.player.upBladeLegendarySplit = false;

        // Blade (legendary)
        state.player.upBladeInitialSplit = false;
        state.player.upBladeSmart = false;
        state.player.upBladeEnergyTrail = false;
        state.player.secretTechBlade = 0;

        // Basic shot (basic)
        state.player.upBasicDmg100 = 0;
        state.player.upBasicPiercePlus3 = 0;
        state.player.upBasicFireRate25 = 0;
        state.player.upBasicPlus1Bullets = 0;
        state.player.upBasicArmorPierce = false;

        // Basic shot (legendary)
        state.player.upBasicSmart = false;
        state.player.upBasicOvertuned6 = 0;
        state.player.upBasicMoneyShot = false;
        state.player.upBasicSystemsFried = false;

        // Player (basic)
        state.player.upPlayerSpeed35 = 0;
        state.player.upPlayerHp15_Border25 = 0;
        state.player.upMoney45 = 0;
        state.player.upOverclock10 = false;
        state.player.upSixthSense = false;

        // Player (legendary)
        state.player.upHybridWeaponCore = false;
        state.player.upYwingSupport = 0;
        state.player.upSmarterTargeting = false;
        state.player.upPersonalShield = false;

        // Clone support (basic)
        state.player.upCloneReinforcement = 0;
        state.player.upCloneMissileArm = false;
        state.player.upCloneBladeArm = false;
        state.player.upCloneCooling35 = 0;

        // Clone support (legendary)
        state.player.upCloneUsesBasicUpgrades = false;
        state.player.upCloneUsesMissileUpgrades = false;
        state.player.upCloneUsesBladeUpgrades = false;
    }

    /* ================= Helpers ================= */

    private JMenuItem item(String label, Runnable action) {
        return new JMenuItem(new AbstractAction(label) {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
        });
    }

    private JMenuItem toggle(String label,
                             java.util.function.BooleanSupplier getter,
                             java.util.function.Consumer<Boolean> setter) {
        JCheckBoxMenuItem it = new JCheckBoxMenuItem(label);
        it.setState(getter.getAsBoolean());
        it.addActionListener(e -> {
            setter.accept(it.getState());
            repaintTarget.repaint();
        });
        return it;
    }

    private JMenu levelMenu(String label, int max,
                            java.util.function.IntSupplier getter,
                            java.util.function.IntConsumer setter) {
        JMenu m = new JMenu(label);
        ButtonGroup g = new ButtonGroup();
        int current = getter.getAsInt();

        for (int i = 0; i <= max; i++) {
            final int lvl = i;
            JRadioButtonMenuItem it = new JRadioButtonMenuItem(String.valueOf(lvl), lvl == current);
            g.add(it);
            it.addActionListener(e -> {
                setter.accept(lvl);
                repaintTarget.repaint();
            });
            m.add(it);
        }
        return m;
    }

    private Image fromRes(String relPath) {
        try { return AssetLoader.imageFromResource(relPath); }
        catch (Exception ex) {
            GameExceptions.showErrorDialog("Resource load error: " + ex.getMessage());
            return null;
        }
    }

    /** Offer local file vs URL and deliver the Image to the consumer. */
    private void chooseCustomImage(java.util.function.Consumer<Image> consumer, String title) {
        String[] opts = { "Pick Local File…", "Enter URL…", "Cancel" };
        int choice = JOptionPane.showOptionDialog(null, "Choose image source", title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
        try {
            if (choice == 0) {
                JFileChooser ch = new JFileChooser();
                if (ch.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    Image img = ImageIO.read(ch.getSelectedFile());
                    if (img == null) throw new IllegalArgumentException("Selected file is not an image.");
                    consumer.accept(img);
                }
            } else if (choice == 1) {
                String url = JOptionPane.showInputDialog(null, "Enter image URL (png/jpg):");
                if (url != null && !url.isBlank()) {
                    Image img = ImageIO.read(new URI(url).toURL());
                    if (img == null) throw new IllegalArgumentException("URL did not return an image.");
                    consumer.accept(img);
                }
            }
        } catch (Exception ex) {
            GameExceptions.showErrorDialog("Failed to load image: " + ex.getMessage());
        }
    }
}
