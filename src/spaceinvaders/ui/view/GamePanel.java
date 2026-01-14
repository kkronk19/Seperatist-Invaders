package spaceinvaders.ui.view;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import javax.swing.*;
import spaceinvaders.core.GameState;
import spaceinvaders.core.SceneManager;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.features.StartMenuDemo;
import spaceinvaders.input.KeyInput;
import spaceinvaders.input.NpcController;

public class GamePanel extends JPanel implements Runnable {

    private final GameState state;
    private final Painter painter;
    private final SceneManager scenes;

    private Thread gameThread;
    private boolean running = false;

    private JMenuBar menuBar;
    private GameState.AppMode lastMode = null;

    // Start menu demo (VIRTUAL SPACE)
    private final StartMenuDemo demo = new StartMenuDemo();
    private final double leftFrac = 0.05;
    private final double rightFrac = 0.90;

    // Title clones (two independent controllers) + title bullets
    private final List<NpcController> titleClones = new ArrayList<>();
    private final List<Bullet> titleBullets = new ArrayList<>();

    // Rendering transform
    private double scale = 1.0;
    private int offX = 0;
    private int offY = 0;

    private static final int FPS = 60;
    private static final int FRAME_TIME = 1000 / FPS;

    public GamePanel(GameState state, SceneManager scenes) {
        this.state = state;
        this.scenes = scenes;
        this.painter = new Painter();

        // FIXED WORLD SIZE
        state.width  = GameState.VIRTUAL_W;
        state.height = GameState.VIRTUAL_H;

        setPreferredSize(new Dimension(1280, 720));
        setBackground(Color.BLACK);
        setFocusable(true);
        setLayout(new GridBagLayout());

        // Demo uses virtual size ONLY
        demo.init(GameState.VIRTUAL_W, GameState.VIRTUAL_H, leftFrac, rightFrac);

        // Spawn TWO clones for title screen with different seeds (desync)
        spawnTitleClones();

        // Input elsewhere (SRP)
        KeyInput.install(this, scenes);
    }

    private void spawnTitleClones() {
        titleClones.clear();
        titleBullets.clear();

        // Use demo's anchor points
        int leftX  = demo.leftX();
        int rightX = demo.rightX();

        // Different seeds => different direction/firing timing
        titleClones.add(new NpcController(leftX,  System.nanoTime()));
        titleClones.add(new NpcController(rightX, System.nanoTime() ^ 0x9E3779B97F4A7C15L));
    }

    public void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void run() {
        final long frameNanos = 1_000_000_000L / FPS;
        long next = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            if (now >= next) {
                updateState();
                repaint();
                next += frameNanos;
                if (now - next > frameNanos) next = now;
            } else {
                LockSupport.parkNanos(next - now);
            }
        }
    }

    private void updateState() {
        if (scenes.current() != null) {
            scenes.current().update(FRAME_TIME);
            updateMenuVisibility();
            return;
        }

        if (state.mode == GameState.AppMode.START_MENU) {
            // background/invader ambiance (and their fall movement)
            demo.update(FRAME_TIME, GameState.VIRTUAL_W, GameState.VIRTUAL_H);

            int floorY = demo.floorY();
            int playerW = demo.playerW();

            // Two independent clones with independent fire timers + SFX
            for (NpcController npc : titleClones) {
                npc.tick(FRAME_TIME, GameState.VIRTUAL_W, playerW, floorY, titleBullets);
            }

            // Update title bullets + cull
            for (int i = 0; i < titleBullets.size(); i++) {
                titleBullets.get(i).update();
            }
            for (int i = titleBullets.size() - 1; i >= 0; i--) {
                if (titleBullets.get(i).isOffScreen(GameState.VIRTUAL_W, GameState.VIRTUAL_H)) {
                    titleBullets.remove(i);
                }
            }

            // Collision + death SFX using StartMenuDemo's invader list
            demo.handleTitleCollisions(titleBullets);

            if (lastMode != state.mode) {
                lastMode = state.mode;
                updateMenuVisibility();
            }
            return;
        }

        // non-title fallback
        if (state.moveLeft)  state.playerX -= 8;
        if (state.moveRight) state.playerX += 8;

        state.playerX = Math.max(
            0,
            Math.min(state.playerX, GameState.VIRTUAL_W - state.playerWidth)
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int pw = getWidth();
        int ph = getHeight();

        scale = Math.min(
            pw / (double) GameState.VIRTUAL_W,
            ph / (double) GameState.VIRTUAL_H
        );

        int viewW = (int) (GameState.VIRTUAL_W * scale);
        int viewH = (int) (GameState.VIRTUAL_H * scale);

        offX = (pw - viewW) / 2;
        offY = (ph - viewH) / 2;

        Graphics2D g2 = (Graphics2D) g.create();

        // Letterbox
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, pw, ph);

        g2.translate(offX, offY);
        g2.scale(scale, scale);

        // world stays virtual
        state.width  = GameState.VIRTUAL_W;
        state.height = GameState.VIRTUAL_H;

        if (scenes.current() != null) {
            scenes.current().render(g2, GameState.VIRTUAL_W, GameState.VIRTUAL_H);
            g2.dispose();
            return;
        }

        if (state.mode == GameState.AppMode.START_MENU) {
            renderStartMenu(g2);
            g2.dispose();
            return;
        }

        painter.paintAll(g2, state, this);
        g2.dispose();
    }

    private void renderStartMenu(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, GameState.VIRTUAL_W, GameState.VIRTUAL_H);

        // background
        Image bg = demo.starsImage();
        if (bg != null) {
            int iw = bg.getWidth(this);
            int ih = bg.getHeight(this);
            if (iw > 0 && ih > 0) {
                double s = Math.max(
                    GameState.VIRTUAL_W / (double) iw,
                    GameState.VIRTUAL_H / (double) ih
                );
                int dw = (int) (iw * s);
                int dh = (int) (ih * s);
                int dx = (GameState.VIRTUAL_W - dw) / 2;
                int dy = (GameState.VIRTUAL_H - dh) / 2;

                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                g.drawImage(bg, dx, dy, dw, dh, this);
                g.setComposite(old);
            }
        }

        // clones driven by controllers
        int y = demo.floorY();
        int w = demo.playerW();
        int h = demo.playerH();
        Image player = demo.playerImage();

        for (NpcController npc : titleClones) {
            int x = npc.getX();
            if (player != null) g.drawImage(player, x, y, w, h, this);
            else {
                g.setColor(Color.WHITE);
                g.fillRect(x, y, w, h);
            }
        }

        // invaders from demo snapshots
        Image inv = demo.invaderImage();
        for (GameState.Invader i : demo.invadersSnapshot()) {
            if (inv != null) g.drawImage(inv, i.x, i.y, i.size, i.size, this);
            else {
                g.setColor(Color.GREEN);
                g.fillRect(i.x, i.y, i.size, i.size);
            }
        }

        // bullets from titleBullets (Bullet uses TOP-LEFT coords now)
        Image bulletImg = demo.bulletImage();
        for (Bullet b : titleBullets) {
            int s = Math.max(6, b.size);

            if (bulletImg != null) {
                g.drawImage(bulletImg, b.x, b.y, s, s, this);
            } else {
                g.setColor(Color.YELLOW);
                g.fillRect(b.x, b.y, s, s);
            }
        }
    }

    public void stop() {
        running = false;
        try { if (gameThread != null) gameThread.join(); }
        catch (InterruptedException ignored) {}
    }

    public void attachMenuBar(JMenuBar bar) {
        this.menuBar = bar;
        updateMenuVisibility();
    }

    private void updateMenuVisibility() {
        if (menuBar == null) return;
        boolean show =
            state.mode == GameState.AppMode.SANDBOX ||
            (scenes.current() instanceof spaceinvaders.features.SandboxScene);
        menuBar.setVisible(show);
    }
}
