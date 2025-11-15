package spaceinvaders.ui.view;

import javax.swing.*;
import java.awt.*;
import spaceinvaders.app.AppState;

public class TitleScreenPanel extends JPanel {
    public TitleScreenPanel(AppState app, Runnable onPlay, Runnable onSandbox, Runnable onQuit) {
        setLayout(new GridBagLayout());
        var box = new JPanel();
        box.setLayout(new GridLayout(3,1,8,8));

        var play    = new JButton("Play");
        var sandbox = new JButton("Sandbox");
        var quit    = new JButton("Quit");

        play.addActionListener(e -> onPlay.run());
        sandbox.addActionListener(e -> onSandbox.run());
        quit.addActionListener(e -> onQuit.run());

        box.add(play);
        box.add(sandbox);
        box.add(quit);

        add(box, new GridBagConstraints());
        setPreferredSize(new Dimension(600, 700));
    }
}
