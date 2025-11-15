package spaceinvaders.core;

import java.awt.Graphics2D;

public interface Scene {
    /** Called once when this scene becomes active. */
    void onEnter();

    /** Called once when leaving this scene. */
    void onExit();

    /** Handle key press events. */
    void handleKeyPressed(int keyCode);

    /** Handle key release events. */
    void handleKeyReleased(int keyCode);

    /** Update logic — dtMillis is time since last frame. */
    void update(double dtMillis);

    /** Draw everything for this scene. */
    void render(Graphics2D g, int width, int height);
}
