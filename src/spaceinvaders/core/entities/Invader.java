package spaceinvaders.core.entities;

/** Represents a single enemy. Pure data + simple motion hook. */
public class Invader {
    // Position & size (px)
    public int x, y;
    public int width, height;

    // Velocity (px per tick) — used by simple patterns
    public int vx, vy;

    // Gameplay
    public int hp;              // hit points
    public int touchDamage;     // damage to player on collision 
    public int scoreValue;      // points awarded on death by shooter
    public InvaderKind kind;

    // Loot / drops
    public double dropChance;   // 0.0–1.0
    public LootKind dropKind;   // optional (ammo, upgrade, etc.)

    // Pattern / AI (strategy)
    public MovementPattern pattern;

    // Timing for pattern math
    public long ageMs = 0;      // lifespan in ms (for pattern deployment logic)
    public int spawnX;          // original x for strategic deployment variance

    public Invader(int x, int y, int w, int h, InvaderKind kind, MovementPattern pattern) {
        this.x = this.spawnX = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.kind = kind;
        this.pattern = pattern;

        // sensible defaults; tune per kind
        this.vx = 0; this.vy = 2;
        this.hp = 1;
        this.touchDamage = 1;
        this.scoreValue = 10;
        this.dropChance = 0.0;
        this.dropKind = LootKind.NONE;
    }

    /** Advance by dt milliseconds using the movement pattern. */
    public void update(long dtMs, int panelW, int panelH) {
        ageMs += dtMs;
        if (pattern != null) {
            pattern.update(this, dtMs, panelW, panelH);
        } else {
            // fallback: straight down using vy per tick (~frame)
            y += vy;
            x += vx;
        }
    }

    public boolean isOffScreen(int panelH) {
        return y > panelH;
    }

    public enum InvaderKind { BASIC, TANK, SWARMER, SHOOTER, BOSS }
    public enum LootKind { NONE, AMMO, UPGRADE, HEALTH, MISSILE, DISC }
}
