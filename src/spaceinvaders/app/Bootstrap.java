package spaceinvaders.app;

import javax.swing.*;
import java.awt.*;
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
            GameState state = new GameState();
            // World size is fixed (virtual)
            state.width  = GameState.VIRTUAL_W;
            state.height = GameState.VIRTUAL_H;

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

            // Pick a window size that fits the current display
            Rectangle bounds = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds();

            int targetW = GameState.VIRTUAL_W;
            int targetH = GameState.VIRTUAL_H;

            double s = Math.min(
                    (bounds.width  * 0.9) / targetW,
                    (bounds.height * 0.9) / targetH
            );

            if (s < 1.0) {
                targetW = (int) (targetW * s);
                targetH = (int) (targetH * s);
            }

            frame.setSize(targetW, targetH);
            frame.setResizable(true);
            frame.setLocationRelativeTo(null);
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
