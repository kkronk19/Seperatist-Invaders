package spaceinvaders.core;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import spaceinvaders.core.entities.Bullet;

public class GameState {

    public enum AppMode { START_MENU, PLAY, SANDBOX }
    public AppMode mode = AppMode.START_MENU;

    // Virtual dimensions (fixed, used for game logic and rendering scale)
    public static final int VIRTUAL_W = 1920;
    public static final int VIRTUAL_H = 1080;

    public int width  = VIRTUAL_W;
    public int height = VIRTUAL_H;

    // player
    public int playerX = 200;
    public int playerWidth = 50;
    public int playerHeight = 60;
    public Image playerImage;   // set by menus

    // ------------------------------------------------------------------
    // LEGACY MENU/DEMO INVADERS (StartMenuDemo + Painter still use these)
    // ------------------------------------------------------------------
    public static final class Invader {
        public int x, y, size;
        public Invader(int x, int y, int size){ this.x=x; this.y=y; this.size=size; }
    }
    public final List<Invader> invaders = new ArrayList<>();
    public Image invaderImage;   // legacy "one invader image" used by Painter/menu

    // ------------------------------------------------------------------
    // NEW: per-kind images (used by core.render.InvaderRenderer)
    // ------------------------------------------------------------------
    public Image invaderImageBasic;     // b1_droid.png
    public Image invaderImageB2;        // b2_droid.png
    public Image invaderImageTank;      // aat.png
    public Image invaderImageShielded;  // droideka.png
    public Image invaderImageShooter;   // bx_commando_droid.png
    public Image invaderImageSwarmer;   // buzz_droid.png

    // ------------------------------------------------------------------
    // bullets
    // ------------------------------------------------------------------
    public final List<Bullet> bullets = new ArrayList<>();

    public enum BulletType { TRIANGLE, CIRCLE, SQUARE, IMAGE }
    public BulletType bulletType = BulletType.TRIANGLE;
    public Image bulletImage;    // used when bulletType == IMAGE

    // Input flags
    public boolean moveLeft = false;
    public boolean moveRight = false;

    /** Bridge used by scenes to return to the Swing title overlay without owning it. */
    public Runnable showStartMenu;
}
