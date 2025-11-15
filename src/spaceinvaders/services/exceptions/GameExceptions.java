package spaceinvaders.services.exceptions;

import javax.swing.JOptionPane;

/** Central place for showing error dialogs and throwing game-specific errors. */
public final class GameExceptions {

    private GameExceptions() {} // utility class, not instantiable

    /** Show an error dialog safely from anywhere in the app. */
    public static void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
