package spaceinvaders.core.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Blade = ricochet + penetration projectile.
 * Stored inside the normal Bullet list so existing systems can render it easily.
 *
 * Lifetime ends when:
 * - bouncesRemaining hits 0, OR
 * - pierceRemaining hits 0, OR
 * - it hits top/bottom (unless verticalBounceEnabled), OR
 * - optional failsafe maxLifeMs.
 */
public final class Blade extends Bullet {

    private int bouncesRemaining;
    private int pierceRemaining;

    private boolean legendarySplitEnabled;
    private boolean splitUsed = false;

    private boolean verticalBounceEnabled;

    private int lifeMs = 0;
    private final int maxLifeMs = 4500; // safety

    // Size is used by rendering/hitbox assumptions
    public Blade(int x, int y, int vx, int vy, int size,
                 int bouncesRemaining,
                 int pierceRemaining,
                 boolean legendarySplitEnabled,
                 boolean verticalBounceEnabled) {

        super(x, y, vx, vy, size, 1, BulletKind.BLADE);

        this.bouncesRemaining = bouncesRemaining;
        this.pierceRemaining = pierceRemaining;
        this.legendarySplitEnabled = legendarySplitEnabled;
        this.verticalBounceEnabled = verticalBounceEnabled;
    }

    /** Update movement and handle screen-bound bounces. Returns any spawned blades (legendary split). */
    public List<Blade> updateBlade(int dtMs, int worldW, int worldH) {
        lifeMs += dtMs;
        super.update();

        List<Blade> spawned = new ArrayList<>();

        // --- left/right walls bounce ---
        // Bullet x,y seems to be treated like center in some places; treat it as center for bounds.
        int half = Math.max(1, this.size / 2);

        boolean hitLeft  = (this.x - half) <= 0;
        boolean hitRight = (this.x + half) >= worldW;

        if (hitLeft || hitRight) {
            // clamp inside
            if (hitLeft)  this.x = half;
            if (hitRight) this.x = worldW - half;

            // reflect horizontal velocity
            this.vx = -this.vx;

            // bounce budget
            bouncesRemaining--;

            // Legendary: on FIRST side bounce, spawn 2 additional blades
            if (legendarySplitEnabled && !splitUsed) {
                splitUsed = true;

                // Spawn two new blades from the bounce point:
                // one with a slightly steeper left, one steeper right.
                // Keep them moving upward overall (vy stays negative).
                int childVY = this.vy; // already negative
                int childSpeedX = Math.max(2, Math.abs(this.vx) + 2);

                // child 1: up-left
                spawned.add(new Blade(
                        this.x, this.y,
                        -childSpeedX, childVY,
                        this.size,
                        Math.max(0, bouncesRemaining - 1),
                        Math.max(1, pierceRemaining - 1),
                        false, // children do NOT split again (prevents recursion insanity)
                        verticalBounceEnabled
                ));

                // child 2: up-right
                spawned.add(new Blade(
                        this.x, this.y,
                        +childSpeedX, childVY,
                        this.size,
                        Math.max(0, bouncesRemaining - 1),
                        Math.max(1, pierceRemaining - 1),
                        false,
                        verticalBounceEnabled
                ));
            }
        }

        // --- top/bottom handling ---
        boolean hitTop = (this.y - half) <= 0;
        boolean hitBottom = (this.y + half) >= worldH;

        if (hitTop || hitBottom) {
            if (!verticalBounceEnabled) {
                // base behavior: despawn on top/bottom
                this.pierceRemaining = 0; // mark dead
            } else {
                // bounce vertically
                if (hitTop) this.y = half;
                if (hitBottom) this.y = worldH - half;
                this.vy = -this.vy;

                // Optional: keep the "blade identity" as mostly upward:
                // If you truly want it to keep trending up even after bottom bounce,
                // uncomment this:
                // this.vy = -Math.abs(this.vy);
            }
        }

        // failsafe
        if (lifeMs >= maxLifeMs) {
            this.pierceRemaining = 0;
        }

        return spawned;
    }

    /** Call when this blade hits an invader. Returns true if blade should despawn. */
    public boolean onHitInvader() {
        pierceRemaining--;
        return pierceRemaining <= 0;
    }

    public boolean isDead() {
        return (pierceRemaining <= 0) || (bouncesRemaining < 0);
    }

    public int getPierceRemaining() { return pierceRemaining; }
    public int getBouncesRemaining() { return bouncesRemaining; }
}
