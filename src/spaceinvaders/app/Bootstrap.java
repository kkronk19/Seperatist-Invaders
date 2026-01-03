package spaceinvaders.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import spaceinvaders.core.GameState;
import spaceinvaders.core.SceneManager;
import spaceinvaders.ui.view.GamePanel;
import spaceinvaders.ui.menu.GameMenuBar;
import spaceinvaders.ui.menu.StartMenuPanel;

public final class Bootstrap {
    private Bootstrap() {}

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            // --- Base window size (max cap) ---
            final int BASE_W = 1920;
            final int BASE_H = 1080;

            GameState state = new GameState();
            state.width  = BASE_W;
            state.height = BASE_H;

            SceneManager scenes = new SceneManager();

            JFrame frame = new JFrame("Space Invaders");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            GamePanel panel = new GamePanel(state, scenes);

            GameMenuBar menuBar = new GameMenuBar(state, panel);
            frame.setJMenuBar(menuBar);
            panel.attachMenuBar(menuBar);

            StartMenuPanel startMenu = new StartMenuPanel(state, panel, scenes);
            startMenu.setOpaque(false);
            frame.setGlassPane(startMenu);
            startMenu.setVisible(true);

            frame.setContentPane(panel);

            // Windowed, can shrink but never exceed BASE_W x BASE_H
            frame.pack();
            frame.setResizable(true);
            frame.setLocationRelativeTo(null);
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int w = Math.min(frame.getWidth(),  BASE_W);
                    int h = Math.min(frame.getHeight(), BASE_H);
                    if (w != frame.getWidth() || h != frame.getHeight()) {
                        frame.setSize(w, h);
                    }
                }
            });
            frame.setVisible(true);

            panel.start();
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    panel.stop();
                }
            });
            panel.requestFocusInWindow();
        });
    }
}
