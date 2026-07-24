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
        bind(im, am, "Q",     KeyEvent.VK_Q,    scenes);
        bind(im, am, "W",     KeyEvent.VK_W,    scenes);
        bind(im, am, "E",     KeyEvent.VK_E,    scenes);
        bind(im, am, "RIGHT", KeyEvent.VK_RIGHT,scenes);
        bind(im, am, "D",     KeyEvent.VK_D,    scenes);
        bind(im, am, "SPACE", KeyEvent.VK_SPACE,scenes);
        bind(im, am, "R",     KeyEvent.VK_R,    scenes);
        bind(im, am, "F", KeyEvent.VK_F, scenes);
        bind(im, am, "P", KeyEvent.VK_P, scenes);
        bind(im, am, "U", KeyEvent.VK_U, scenes);
        bind(im, am, "M", KeyEvent.VK_M, scenes);
        bind(im, am, "ESCAPE", KeyEvent.VK_ESCAPE, scenes);
        bind(im, am, "ENTER", KeyEvent.VK_ENTER, scenes);
        bind(im, am, "1", KeyEvent.VK_1, scenes);
        bind(im, am, "2", KeyEvent.VK_2, scenes);
        bind(im, am, "3", KeyEvent.VK_3, scenes);
        bind(im, am, "4", KeyEvent.VK_4, scenes);
        bind(im, am, "NUMPAD4", KeyEvent.VK_NUMPAD4, scenes);
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
