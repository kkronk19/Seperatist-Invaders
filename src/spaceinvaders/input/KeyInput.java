package spaceinvaders.input;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import spaceinvaders.core.SceneManager;

/** Handles keyboard bindings and forwards events to the active scene. */
public final class KeyInput {

    private KeyInput() {}

    public static void install(JComponent target, SceneManager scenes) {
        InputMap im = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = target.getActionMap();

        bind(im, am, "LEFT",  KeyEvent.VK_LEFT, scenes);
        bind(im, am, "A",     KeyEvent.VK_A,    scenes);
        bind(im, am, "RIGHT", KeyEvent.VK_RIGHT,scenes);
        bind(im, am, "D",     KeyEvent.VK_D,    scenes);
        bind(im, am, "SPACE", KeyEvent.VK_SPACE,scenes);
        bind(im, am, "R",     KeyEvent.VK_R,    scenes);
    }

    private static void bind(InputMap im, ActionMap am, String key, int vk, SceneManager scenes) {
        String pressed  = key + "-p";
        String released = key + "-r";

        im.put(KeyStroke.getKeyStroke("pressed " + key),  pressed);
        im.put(KeyStroke.getKeyStroke("released " + key), released);

        am.put(pressed, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current() != null) scenes.current().handleKeyPressed(vk);
            }
        });

        am.put(released, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current() != null) scenes.current().handleKeyReleased(vk);
            }
        });
    }
}
