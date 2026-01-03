package spaceinvaders.core;
import spaceinvaders.core.entities.Bullet;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;


public class GameState {

    public enum AppMode { START_MENU, PLAY, SANDBOX }
    public AppMode mode = AppMode.START_MENU;

    // Virtual dimensions (fixed, used for game logic and rendering scale)
    public static final int VIRTUAL_W = 1920;
    public static final int VIRTUAL_H = 1080;

    // Virtual world dimensions (do not change at runtime)
    public int width  = VIRTUAL_W;
    public int height = VIRTUAL_H;

    // player
    public int playerX = 200;
    public int playerWidth = 50;
    public int playerHeight = 60;
    public Image playerImage;   // set by menus

    // Invaders
    public static final class Invader {
        public int x, y, size;
        public Invader(int x, int y, int size){ this.x=x; this.y=y; this.size=size; }
    }
    public final List<Invader> invaders = new ArrayList<>();
    public Image invaderImage;   // set by menus

    public final List<Bullet> bullets = new ArrayList<>();

    public enum BulletType { TRIANGLE, CIRCLE, SQUARE, IMAGE }
    public BulletType bulletType = BulletType.TRIANGLE;
    public Image bulletImage;    // used when bulletType == IMAGE

    // Input flags
    public boolean moveLeft = false;
    public boolean moveRight = false;

}
