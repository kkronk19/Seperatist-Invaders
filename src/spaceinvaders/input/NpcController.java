package spaceinvaders.input;

import spaceinvaders.core.GameState;

public class NpcController {
    private final GameState s;
    private int timer;

    public NpcController(GameState s){ this.s = s; }

    /** Call from GamePanel.updateState() if you’re in TITLE mode. */
    public void tick() {
        // wiggle left/right
        if ((timer / 30) % 2 == 0) s.moveLeft = true; else s.moveLeft = false;
        if ((timer / 30) % 2 == 1) s.moveRight = true; else s.moveRight = false;

        // fire periodically
        if (timer % 20 == 0) s.bullets.add(new GameState.Bullet(s.playerX + s.playerWidth/2, s.height - s.playerHeight));

        timer++;
    }
}
