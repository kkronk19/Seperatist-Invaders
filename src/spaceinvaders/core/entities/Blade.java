package spaceinvaders.core.entities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import spaceinvaders.services.audio.AudioManager;

public class Blade extends Bullet {

    // ---- visual FX state ----
    public static final class Ghost {
        public int x, y;
        public float a; // alpha 0..1
        public Ghost(int x, int y, float a) { this.x = x; this.y = y; this.a = a; }
    }

    /** Recent positions for a ghost trail (render-only). */
    public final List<Ghost> trail = new ArrayList<>();

    /** White flash timer in ms (render-only). */
    public int flashMs = 0;

    /** Bounce SFX cooldown in ms (prevents spam). */
    private int bounceSfxCdMs = 0;

    // ---- gameplay state ----
    private int bouncesRemaining;
    private int pierceRemaining;

    private final boolean legendarySplitEnabled;
    private boolean splitUsed = false;

    /** If true, this blade ignores "armored" resistance rules. */
    public final boolean armorPiercing;

    /** If true, blade can bounce off top/bottom instead of dying. */
    private final boolean verticalBounceEnabled;

    private int lifeMs = 0;
    private static final int MAX_LIFE_MS = 9000; // safety

    public Blade(
            int x, int y,
            int vx, int vy,
            int size,
            int bouncesRemaining,
            int pierceRemaining,
            boolean legendarySplitEnabled,
            boolean verticalBounceEnabled,
            boolean armorPiercing
    ) {
        super(x, y, vx, vy, size, 1, BulletKind.BLADE);
        this.bouncesRemaining = bouncesRemaining;
        this.pierceRemaining = pierceRemaining;
        this.legendarySplitEnabled = legendarySplitEnabled;
        this.verticalBounceEnabled = verticalBounceEnabled;
        this.armorPiercing = armorPiercing;
    }

    /** Consume all remaining pierce (used when armor stops the blade). */
    public void consumeAllPierce() {
        this.pierceRemaining = 0;
    }

    /**
     * Update movement + bounces.
     * Returns any spawned blades (legendary split) as Bullets so caller can add safely.
     */
    public List<Bullet> updateBlade(int dtMs, int worldW, int worldH) {
        lifeMs += dtMs;

        // timers
        if (flashMs > 0) flashMs -= dtMs;
        if (bounceSfxCdMs > 0) bounceSfxCdMs -= dtMs;

        // --- ghost trail: record BEFORE move so ghosts lag behind ---
        trail.add(new Ghost(this.x, this.y, 0.45f)); // start alpha

        // fade + cap
        for (Iterator<Ghost> it = trail.iterator(); it.hasNext();) {
            Ghost g = it.next();
            g.a *= 0.86f; // fade rate
            if (g.a < 0.06f) it.remove();
        }
        while (trail.size() > 10) trail.remove(0);

        // move
        super.update();

        List<Bullet> spawned = new ArrayList<>();

        int half = Math.max(1, this.size / 2);

        boolean hitLeft  = (this.x - half) <= 0;
        boolean hitRight = (this.x + half) >= worldW;

        if (hitLeft || hitRight) {
            // clamp
            if (hitLeft)  this.x = half;
            if (hitRight) this.x = worldW - half;

            // reflect X
            this.vx = -this.vx;

            // widen rebound slightly
            this.vx += (this.vx > 0 ? 1 : -1);

            bouncesRemaining--;

            // white flash on bounce
            flashMs = 110;

            // bounce sound (gated)
            playBounceSfx();

            // legendary split ON FIRST bounce
            if (legendarySplitEnabled && !splitUsed) {
                splitUsed = true;

                int childVY = this.vy; // keep trend
                int childSpeedX = Math.max(2, Math.abs(this.vx) + 2);

                spawned.add(new Blade(
                        this.x, this.y,
                        -childSpeedX, childVY,
                        this.size,
                        Math.max(0, bouncesRemaining - 1),
                        Math.max(1, pierceRemaining - 1),
                        false, // no recursion
                        verticalBounceEnabled,
                        this.armorPiercing
                ));

                spawned.add(new Blade(
                        this.x, this.y,
                        +childSpeedX, childVY,
                        this.size,
                        Math.max(0, bouncesRemaining - 1),
                        Math.max(1, pierceRemaining - 1),
                        false,
                        verticalBounceEnabled,
                        this.armorPiercing
                ));
            }
        }

        boolean hitTop    = (this.y - half) <= 0;
        boolean hitBottom = (this.y + half) >= worldH;

        if (hitTop || hitBottom) {
            if (!verticalBounceEnabled) {
                pierceRemaining = 0; // die
            } else {
                if (hitTop) this.y = half;
                if (hitBottom) this.y = worldH - half;

                this.vy = -this.vy;
                flashMs = 110;
                playBounceSfx();
            }
        }

        if (lifeMs >= MAX_LIFE_MS) pierceRemaining = 0;
        if (bouncesRemaining < 0) pierceRemaining = 0;

        return spawned;
    }

    private void playBounceSfx() {
        if (bounceSfxCdMs > 0) return;
        bounceSfxCdMs = 90;
        try {
            AudioManager.get().playRandomSfx(
                    0.20f,
                    "/spaceinvaders/resources/audio/sfx/imp_ricco_02.wav",
                    "/spaceinvaders/resources/audio/sfx/imp_ricco_03.wav",
                    "/spaceinvaders/resources/audio/sfx/imp_ricco_04.wav",
                    "/spaceinvaders/resources/audio/sfx/imp_ricco_06.wav",
                    "/spaceinvaders/resources/audio/sfx/imp_ricco_08.wav",
                    "/spaceinvaders/resources/audio/sfx/imp_ricco_12.wav"
            );
        } catch (Throwable ignored) {}
    }

    /** Call when blade hits an invader. Returns true if it should despawn now. */
    public boolean onHitInvader() {
        pierceRemaining--;
        return pierceRemaining <= 0;
    }

    public boolean isDead() { return pierceRemaining <= 0; }
}
