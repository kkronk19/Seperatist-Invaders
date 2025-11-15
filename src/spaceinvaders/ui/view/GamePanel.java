package spaceinvaders.ui.view;

import spaceinvaders.core.GameState;
import spaceinvaders.core.SceneManager;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.features.StartMenuDemo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.locks.LockSupport;

public class GamePanel extends JPanel implements Runnable {

    private final GameState state;
    private final Painter painter;
    private Thread gameThread;
    private boolean running = false;

    private JMenuBar menuBar;
    private GameState.AppMode lastMode = null;

    // Scene system (SRP + OCP)
    private final SceneManager scenes;

    // Start-menu demo (now thread-safe snapshots)
    private final StartMenuDemo demo = new StartMenuDemo();
    private double leftFrac  = 0.05;
    private double rightFrac = 0.90;

    private static final int FPS = 60;
    private static final int FRAME_TIME = 1000 / FPS;

    public GamePanel(GameState state, SceneManager scenes) {
        this.state = state;
        this.scenes = scenes;
        this.painter = new Painter();

        setPreferredSize(new Dimension(state.width, state.height));
        setBackground(Color.BLACK);
        setFocusable(true);
        setLayout(new GridBagLayout()); // keeps overlay menu centered

        // init demo with current size
        demo.init(state.width, state.height, leftFrac, rightFrac);

        // resize listener
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                demo.onResize(getWidth(), getHeight(), leftFrac, rightFrac);
            }
        });

        // --- key bindings that work even when menus are focused ---
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("pressed LEFT"),  "left-pressed");
        im.put(KeyStroke.getKeyStroke("released LEFT"), "left-released");
        im.put(KeyStroke.getKeyStroke("pressed A"),     "a-pressed");
        im.put(KeyStroke.getKeyStroke("released A"),    "a-released");

        im.put(KeyStroke.getKeyStroke("pressed RIGHT"),  "right-pressed");
        im.put(KeyStroke.getKeyStroke("released RIGHT"), "right-released");
        im.put(KeyStroke.getKeyStroke("pressed D"),      "d-pressed");
        im.put(KeyStroke.getKeyStroke("released D"),     "d-released");

        im.put(KeyStroke.getKeyStroke("pressed SPACE"),  "space-pressed");
        im.put(KeyStroke.getKeyStroke("released SPACE"), "space-released");

        // --- NEW: R key for missile tap (pressed/released) ---
        im.put(KeyStroke.getKeyStroke("pressed R"),  "r-pressed");
        im.put(KeyStroke.getKeyStroke("released R"), "r-released");

        am.put("left-pressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyPressed(KeyEvent.VK_LEFT);
            }
        });
        am.put("left-released", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyReleased(KeyEvent.VK_LEFT);
            }
        });
        am.put("a-pressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyPressed(KeyEvent.VK_A);
            }
        });
        am.put("a-released", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyReleased(KeyEvent.VK_A);
            }
        });

        am.put("right-pressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyPressed(KeyEvent.VK_RIGHT);
            }
        });
        am.put("right-released", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyReleased(KeyEvent.VK_RIGHT);
            }
        });
        am.put("d-pressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyPressed(KeyEvent.VK_D);
            }
        });
        am.put("d-released", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyReleased(KeyEvent.VK_D);
            }
        });

        am.put("space-pressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyPressed(KeyEvent.VK_SPACE);
            }
        });
        am.put("space-released", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyReleased(KeyEvent.VK_SPACE);
            }
        });

        // --- NEW: forward R to active scene ---
        am.put("r-pressed", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyPressed(KeyEvent.VK_R);
            }
        });
        am.put("r-released", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (scenes.current()!=null) scenes.current().handleKeyReleased(KeyEvent.VK_R);
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

                // if we fell behind by more than one frame, catch up gracefully
                if (now - next > frameNanos) {
                    next = now;
                }
            } else {
                long sleepNanos = next - now;
                if (sleepNanos > 0L) {
                    LockSupport.parkNanos(sleepNanos);
                }
            }
        }
    }

    /** Handles game logic (movement, collisions, etc.) */
    private void updateState() {
        // if a scene is active, let it handle updates
        if (scenes.current() != null) {
            scenes.current().update(FRAME_TIME);
            updateMenuVisibility();
            return;
        }

        // existing Start Menu update logic
        if (state.mode == GameState.AppMode.START_MENU) {
            demo.update(FRAME_TIME, getWidth(), getHeight());
            if (lastMode != state.mode) { lastMode = state.mode; updateMenuVisibility(); }
            return;
        }

        // fallback logic for other modes (old behavior)
        state.bullets.removeIf(b -> b.y < 0);
        state.bullets.forEach(b -> b.y -= 10);

        if (state.moveLeft)  state.playerX -= 8;
        if (state.moveRight) state.playerX += 8;

        state.playerX = Math.max(0, Math.min(state.playerX, getWidth() - state.playerWidth));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        state.width  = getWidth();
        state.height = getHeight();

        // if a scene is active, let it handle drawing
        if (scenes.current() != null) {
            scenes.current().render((Graphics2D) g, getWidth(), getHeight());
            return;
        }

        // START MENU draw (uses snapshots)
        if (state.mode == GameState.AppMode.START_MENU) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());

            Image bg = demo.starsImage();
            if (bg != null) {
                int w = getWidth(), h = getHeight();
                int iw = bg.getWidth(this), ih = bg.getHeight(this);
                if (iw > 0 && ih > 0) {
                    double scale = Math.max(w / (double) iw, h / (double) ih);
                    int dw = (int) Math.round(iw * scale);
                    int dh = (int) Math.round(ih * scale);
                    int dx = (w - dw) / 2;
                    int dy = (h - dh) / 2;

                    Composite old = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.60f));
                    g2.drawImage(bg, dx, dy, dw, dh, this);
                    g2.setComposite(old);
                }
            }
            g2.dispose();

            int y = demo.floorY();
            Image player = demo.playerImage();
            if (player != null) {
                g.drawImage(player, demo.leftX(),  y, demo.playerW(), demo.playerH(), this);
                g.drawImage(player, demo.rightX(), y, demo.playerW(), demo.playerH(), this);
            } else {
                g.setColor(Color.WHITE);
                g.fillRect(demo.leftX(),  y, demo.playerW(), demo.playerH());
                g.fillRect(demo.rightX(), y, demo.playerW(), demo.playerH());
            }

            // --- invaders (thread-safe snapshot) ---
            Image invaderImg = demo.invaderImage();
            for (GameState.Invader inv : demo.invadersSnapshot()) {
                if (invaderImg != null) {
                    g.drawImage(invaderImg, inv.x, inv.y, inv.size, inv.size, this);
                } else {
                    g.setColor(Color.GREEN);
                    g.fillRect(inv.x, inv.y, inv.size, inv.size);
                }
            }

            // --- bullets (thread-safe snapshot) ---
            Image bulletImg = demo.bulletImage();
            for (Bullet b : demo.bulletsSnapshot()) {
                if (bulletImg != null) {
                    g.drawImage(bulletImg, b.x - 6, b.y - 6, 12, 12, this);
                } else {
                    g.setColor(Color.YELLOW);
                    int[] xs = { b.x, b.x - 5, b.x + 5 };
                    int[] ys = { b.y, b.y + 10, b.y + 10 };
                    g.fillPolygon(xs, ys, 3);
                }
            }

            return; // END start-menu drawing
        }

        // Regular game render
        painter.paintAll(g, state, this);
    }

    public void stop() {
        running = false;
        if (gameThread != null) {
            try { gameThread.join(); } catch (InterruptedException ignored) {}
        }
    }

    public void attachMenuBar(JMenuBar bar) {
        this.menuBar = bar;
        updateMenuVisibility();
    }

    private void updateMenuVisibility() {
        if (menuBar == null) return;

        boolean show =
            (state.mode == GameState.AppMode.SANDBOX) ||
            (scenes != null && scenes.current() instanceof spaceinvaders.features.SandboxScene);

        if (menuBar.isVisible() != show) menuBar.setVisible(show);
    }
}
