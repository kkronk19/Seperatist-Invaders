package spaceinvaders.input;

import java.awt.event.*;
import spaceinvaders.core.GameState;
import spaceinvaders.ui.view.GamePanel;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Bullet.BulletKind;

public class GameController implements KeyListener {
    private final GameState state;

    @SuppressWarnings("unused") // kept for future focus/attachment if needed
    private final GamePanel panel;

    public GameController(GameState state, GamePanel panel) {
        this.state = state;
        this.panel = panel;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT)  state.moveLeft  = true;
        if (key == KeyEvent.VK_RIGHT) state.moveRight = true;

        if (key == KeyEvent.VK_SPACE) {
            int playerX = state.playerX;
            int playerWidth = state.playerWidth;
            int playerHeight = state.playerHeight;

            // Use the top-level Bullet type (vx, vy, size, damage, kind)
            state.bullets.add(new Bullet(
                playerX + playerWidth / 2,
                state.height - playerHeight,
                0, -10,
                6,
                1,
                BulletKind.BASIC
            ));
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT)  state.moveLeft  = false;
        if (key == KeyEvent.VK_RIGHT) state.moveRight = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
