package spaceinvaders.ui.view;

import spaceinvaders.core.GameState;
import spaceinvaders.core.SceneManager;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.features.StartMenuDemo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.locks.LockSupport;

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

        setPreferredSize(new Dimension(1280, 720)); // safe default window
        setBackground(Color.BLACK);
        setFocusable(true);
        setLayout(new GridBagLayout());

        // Start menu uses virtual size ONLY
        demo.init(GameState.VIRTUAL_W, GameState.VIRTUAL_H, leftFrac, rightFrac);

        setupKeyBindings();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        bind(im, am, "LEFT",  KeyEvent.VK_LEFT);
        bind(im, am, "A",     KeyEvent.VK_A);
        bind(im, am, "RIGHT", KeyEvent.VK_RIGHT);
        bind(im, am, "D",     KeyEvent.VK_D);
        bind(im, am, "SPACE", KeyEvent.VK_SPACE);
        bind(im, am, "R",     KeyEvent.VK_R);
    }

    private void bind(InputMap im, ActionMap am, String key, int vk) {
        im.put(KeyStroke.getKeyStroke("pressed " + key), key + "-p");
        im.put(KeyStroke.getKeyStroke("released " + key), key + "-r");

        am.put(key + "-p", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current() != null) scenes.current().handleKeyPressed(vk);
            }
        });
        am.put(key + "-r", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current() != null) scenes.current().handleKeyReleased(vk);
            }
        });
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
            demo.update(FRAME_TIME, GameState.VIRTUAL_W, GameState.VIRTUAL_H);
            if (lastMode != state.mode) {
                lastMode = state.mode;
                updateMenuVisibility();
            }
            return;
        }

        // Fallback legacy logic
        state.bullets.removeIf(b -> b.y < 0);
        state.bullets.forEach(b -> b.y -= 10);

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

        int viewW = (int)(GameState.VIRTUAL_W * scale);
        int viewH = (int)(GameState.VIRTUAL_H * scale);

        offX = (pw - viewW) / 2;
        offY = (ph - viewH) / 2;

        Graphics2D g2 = (Graphics2D) g.create();

        // Letterbox
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, pw, ph);

        g2.translate(offX, offY);
        g2.scale(scale, scale);

        // WORLD SIZE NEVER CHANGES
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

        Image bg = demo.starsImage();
        if (bg != null) {
            int iw = bg.getWidth(this);
            int ih = bg.getHeight(this);
            if (iw > 0 && ih > 0) {
                double s = Math.max(
                    GameState.VIRTUAL_W / (double) iw,
                    GameState.VIRTUAL_H / (double) ih
                );
                int dw = (int)(iw * s);
                int dh = (int)(ih * s);
                int dx = (GameState.VIRTUAL_W - dw) / 2;
                int dy = (GameState.VIRTUAL_H - dh) / 2;

                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                g.drawImage(bg, dx, dy, dw, dh, this);
                g.setComposite(old);
            }
        }

        int y = demo.floorY();
        Image player = demo.playerImage();

        if (player != null) {
            g.drawImage(player, demo.leftX(),  y, demo.playerW(), demo.playerH(), this);
            g.drawImage(player, demo.rightX(), y, demo.playerW(), demo.playerH(), this);
        }

        Image inv = demo.invaderImage();
        for (GameState.Invader i : demo.invadersSnapshot()) {
            if (inv != null)
                g.drawImage(inv, i.x, i.y, i.size, i.size, this);
        }

        Image bullet = demo.bulletImage();
        for (Bullet b : demo.bulletsSnapshot()) {
            if (bullet != null)
                g.drawImage(bullet, b.x - 6, b.y - 6, 12, 12, this);
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
