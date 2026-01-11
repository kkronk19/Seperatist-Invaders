package spaceinvaders.core.entities;

/** Represents a single enemy. Pure data + simple motion hook. */
public class Invader {

    // Position & size (px)
    public int x, y;
    public int width, height;

    // Velocity (px per tick)
    public int vx, vy;

    // Gameplay
    public int hp;              // hit points
    public int touchDamage;     // damage to player on collision
    public int scoreValue;      // points awarded on death
    public InvaderKind kind;

    // Shielding: number of incoming HITS to absorb regardless of damage
    public int shieldHits;

    // Loot / drops
    public double dropChance;   // 0.0–1.0
    public LootKind dropKind;

    // Pattern / AI (strategy)
    public MovementPattern pattern;

    // Timing for pattern math
    public long ageMs = 0;
    public int spawnX;

    /** Result of a hit attempt */
    public enum HitResult {
        ABSORBED,   // shield absorbed the hit, no HP damage
        DAMAGED,    // HP reduced but still alive
        KILLED      // HP reduced to zero or below
    }

    public Invader(int x, int y, int w, int h, InvaderKind kind, MovementPattern pattern) {
        this.x = this.spawnX = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.kind = kind;
        this.pattern = pattern;

        // defaults
        this.vx = 0;
        this.vy = 2;
        this.hp = 1;
        this.touchDamage = 1;
        this.scoreValue = 10;

        // Shielded enemies absorb exactly one hit
        this.shieldHits = (kind == InvaderKind.SHIELDED) ? 1 : 0;

        this.dropChance = 0.0;
        this.dropKind = LootKind.NONE;
    }

    /**
     * Apply a hit and report what happened.
     * Shield absorption does NOT deal HP damage.
     */
    public HitResult takeHit(int damage) {
        if (damage <= 0) {
            return HitResult.ABSORBED;
        }

        // Shield absorbs entire hit
        if (shieldHits > 0) {
            shieldHits--;
            return HitResult.ABSORBED;
        }

        // No shield → deal damage
        hp -= damage;

        if (hp <= 0) {
            return HitResult.KILLED;
        }

        return HitResult.DAMAGED;
    }

    /** Advance using movement pattern (or fallback motion). */
    public void update(long dtMs, int panelW, int panelH) {
        ageMs += dtMs;

        if (pattern != null) {
            pattern.update(this, dtMs, panelW, panelH);
        } else {
            y += vy;
            x += vx;
        }
    }

    public boolean isOffScreen(int panelH) {
        return y > panelH;
    }

    public enum InvaderKind { BASIC, SHIELDED, TANK, SWARMER, SHOOTER, BOSS_01, BOSS_02, BOSS_03, FINAL_BOSS }
    public enum LootKind { NONE, AMMO, UPGRADE, HEALTH, MISSILE, DISC }
}
