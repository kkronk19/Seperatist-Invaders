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

/** Menubar that configures images, bullet type, and music. */
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
    }

    /* ================= Player ================= */

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
        state.playerImage = img;    // changed from playerImage
        repaintTarget.repaint();
    }

    /* ================= Invader ================= */

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

    /* ================= Bullets ================= */

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

    /* ================= Helpers ================= */

    private JMenuItem item(String label, Runnable action) {
        return new JMenuItem(new AbstractAction(label) {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
        });
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
