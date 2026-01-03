package spaceinvaders.features.title;

/** Lightweight visual-only bullet for the title screen. */
public final class MenuBullet {
    public int x, y;
    private static final int SPEED = 12;

    public MenuBullet(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update() {
        y -= SPEED;
    }

    public boolean isOffscreen() {
        return y < -20;
    }
}
