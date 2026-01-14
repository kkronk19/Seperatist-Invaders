package spaceinvaders.ui.menu;

import java.awt.*;
import javax.swing.*;
import spaceinvaders.core.GameState;
import spaceinvaders.core.SceneManager;
import spaceinvaders.features.Mission1Scene;
import spaceinvaders.features.SandboxScene;
import spaceinvaders.services.audio.AudioManager;
import spaceinvaders.ui.view.GamePanel;

public class StartMenuPanel extends JPanel {

    private final SceneManager scenes;
    private final JLabel titleLabel = new JLabel();
    private Image baseLogo; // original unscaled logo image

    public StartMenuPanel(GameState state, GamePanel panel, SceneManager sceneMgr) {
        this.scenes = sceneMgr;
        setLayout(new GridBagLayout());  // center vertically & horizontally
        setOpaque(false); // transparent over background

        // --- Load title image (safe if missing) ---
        try {
            baseLogo = new ImageIcon(
                    getClass().getResource("/spaceinvaders/resources/image/title_logo.png")
            ).getImage();
            titleLabel.setIcon(new ImageIcon(baseLogo.getScaledInstance(700, -1, Image.SCALE_SMOOTH)));
        } catch (Throwable t) {
            titleLabel.setText("SEPARATIST INVADERS");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 42));
            titleLabel.setForeground(new Color(100, 180, 255));
        }
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Buttons ---
        JButton play    = makeButton("Play",    new Color(100, 180, 255), Color.BLACK);
        JButton sandbox = makeButton("Sandbox", new Color(100, 180, 255), Color.BLACK);
        JButton quit    = makeButton("Quit",    new Color(100, 180, 255), Color.BLACK);

        // --- Actions ---
        play.addActionListener(e -> {
            try { AudioManager.get().stopLoop("menu"); } catch (Throwable ignored) {}
            scenes.set(new Mission1Scene(state, scenes));
            setVisible(false);
            panel.requestFocusInWindow();
        });


        sandbox.addActionListener(e -> {
            scenes.set(new SandboxScene(state, scenes));
            setVisible(false);
            panel.requestFocusInWindow();
            try { AudioManager.get().stopLoop("menu"); } catch (Throwable ignored) {}
        });

        quit.addActionListener(e -> System.exit(0));

        // --- Stack vertically ---
        Box box = Box.createVerticalBox();
        box.add(titleLabel);
        box.add(Box.createVerticalStrut(50));
        box.add(play);
        box.add(Box.createVerticalStrut(15));
        box.add(sandbox);
        box.add(Box.createVerticalStrut(15));
        box.add(quit);

        add(box, new GridBagConstraints());

        // --- Resize listener for responsive logo ---
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                resizeLogo();
            }
        });
    }

    /** Scales the logo dynamically to fit window while keeping buttons visible */
    private void resizeLogo() {
        if (baseLogo == null) return;

        int w = getWidth()  > 0 ? getWidth()  : 1200;
        int h = getHeight() > 0 ? getHeight() : 720;

        // keep 20% margin and room for buttons
        int maxLogoW = (int) (w * 0.8);
        int maxLogoH = Math.max(160, h - 320);

        // scale proportionally
        int targetW = Math.min(baseLogo.getWidth(null), maxLogoW);
        int targetH = (int) (baseLogo.getHeight(null) * (targetW / (double) baseLogo.getWidth(null)));
        if (targetH > maxLogoH) {
            targetH = maxLogoH;
            targetW = (int) (baseLogo.getWidth(null) * (targetH / (double) baseLogo.getHeight(null)));
        }

        Image scaled = baseLogo.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        titleLabel.setIcon(new ImageIcon(scaled));
        revalidate();
        repaint();
    }

    // --- Helper: themed button creator ---
    private JButton makeButton(String text, Color fg, Color glowColor) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 18));
        b.setForeground(fg);
        b.setBackground(new Color(0, 0, 0, 180)); // translucent black
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(glowColor, 2, true),
                BorderFactory.createEmptyBorder(10, 30, 10, 30)
        ));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // hover effect
        b.addChangeListener(e -> {
            if (b.getModel().isRollover()) b.setForeground(Color.WHITE);
            else                           b.setForeground(fg);
        });
        return b;
    }
}
