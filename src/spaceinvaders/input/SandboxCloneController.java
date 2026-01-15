package spaceinvaders.input;

import java.util.List;
import java.util.Random;
import spaceinvaders.core.entities.Blade;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Missile;
import spaceinvaders.core.entities.Player;

/**
 * Sandbox clone AI:
 * - Independent random movement (no player-follow)
 * - Bounces off walls
 * - Auto-fires blaster
 * - Optionally fires missiles/blades if enabled via Player clone upgrades
 *
 * NOTE: This controller ONLY appends bullets. Scene owns adding/updating/removal.
 */
public final class SandboxCloneController {

    private final Random rng;

    private int x;
    private int y;

    private int dx; // -1 or +1
    private int dy; // -1 or +1

    private final int speed = 2;

    // direction change timing (like your title clone)
    private int dirMs = 0;
    private int dirNextMs;

    // blaster firing timing
    private int fireMs = 0;
    private int fireNextMs;

    // missile tap timer
    private int missileMs = 0;
    private int missileNextMs;

    // blade firing timing (optional)
    private int bladeMs = 0;
    private int bladeNextMs;

    public SandboxCloneController(int startX, int startY, long seed) {
        this.x = startX;
        this.y = startY;

        this.rng = new Random(seed);

        this.dx = rng.nextBoolean() ? 1 : -1;
        this.dy = rng.nextBoolean() ? 1 : -1;

        this.dirNextMs = 1000 + rng.nextInt(5001); // 1000–6000

        // blaster: feels like title demo
        this.fireNextMs = 180 + rng.nextInt(181); // 180–360

        // missiles: slower, "tap"
        this.missileNextMs = 900 + rng.nextInt(700); // 900–1600

        // blades: slower still
        this.bladeNextMs = 1200 + rng.nextInt(900); // 1200–2100

        // desync
        this.dirMs = rng.nextInt(dirNextMs);
        this.fireMs = rng.nextInt(fireNextMs);
        this.missileMs = rng.nextInt(missileNextMs);
        this.bladeMs = rng.nextInt(bladeNextMs);
    }

    /**
     * @param frameMs delta time in ms
     * @param worldW  virtual width
     * @param worldH  virtual height
     * @param spriteW clone sprite width
     * @param spriteH clone sprite height
     * @param outBullets bullets list to append into
     */
    public void tick(
            int frameMs,
            int worldW,
            int worldH,
            int spriteW,
            int spriteH,
            Player player,
            List<Bullet> outBullets
    ) {
        // --- direction change ---
        dirMs += frameMs;
        if (dirMs >= dirNextMs) {
            dx = rng.nextBoolean() ? 1 : -1;
            dy = rng.nextBoolean() ? 1 : -1;
            dirMs = 0;
            dirNextMs = 1000 + rng.nextInt(5001);
        }

        // --- movement + bounce ---
        int maxX = Math.max(0, worldW - spriteW);
        int maxY = Math.max(0, worldH - spriteH);

        x += dx * speed;
        y += dy * speed;

        if (x <= 0) { x = 0; dx = 1; }
        else if (x >= maxX) { x = maxX; dx = -1; }

        if (y <= 0) { y = 0; dy = 1; }
        else if (y >= maxY) { y = maxY; dy = -1; }

        // --- blaster auto-fire (always when clone exists) ---
        fireMs += frameMs;
        if (fireMs >= fireNextMs) {
            final int size = 8;
            int muzzleCenterX = x + spriteW / 2;
            int muzzleY = y; // shoot upward from clone body top area

            Bullet b = new Bullet(
                    muzzleCenterX - size / 2,
                    muzzleY,
                    0,
                    -10,
                    size,
                    1,
                    Bullet.BulletKind.BASIC
            );

            // If clone is allowed to use basic upgrades, apply the same flags
            if (player != null && player.upCloneUsesBasicUpgrades) {
                b.smartTargeting = player.upBasicSmart;
                b.armorPiercing = player.upBasicArmorPierce;
                b.pierce = player.basicExtraPierce();
                b.damage = player.basicDamage(1);
            }

            outBullets.add(b);

            fireMs = 0;
            fireNextMs = 180 + rng.nextInt(181);
        }

        // --- missile arm (optional) ---
        if (player != null && player.upCloneMissileArm) {
            missileMs += frameMs;
            if (missileMs >= missileNextMs) {
                final int size = 12;
                int muzzleCenterX = x + spriteW / 2;
                int muzzleY = y;

                int dmg = 2;
                boolean straight = false;

                if (player.upCloneUsesMissileUpgrades) {
                    dmg = player.missileDamage(2);
                    straight = player.upMissileStraight;
                }

                Missile m = new Missile(
                        muzzleCenterX - size / 2,
                        muzzleY,
                        0,
                        -10,
                        size,
                        dmg
                );
                m.straightFlight = straight;

                // smart missile targeting is handled by SmartTargetingSystem later (flag here)
                // If you want the clones to benefit from smart missile upgrade:
                m.smartTargeting = player.upCloneUsesMissileUpgrades && player.upMissileSmart;

                outBullets.add(m);

                missileMs = 0;
                missileNextMs = 900 + rng.nextInt(700);
            }
        }

        // --- blade arm (optional) ---
        if (player != null && player.upCloneBladeArm) {
            bladeMs += frameMs;
            if (bladeMs >= bladeNextMs) {
                int muzzleCenterX = x + spriteW / 2;
                int muzzleY = y;

                int bounces = 3;
                int pierce = 3;
                boolean split = false;
                boolean verticalBounce = false;
                boolean ap = false;

                if (player.upCloneUsesBladeUpgrades) {
                    bounces = 3 + player.bladeExtraBounces();
                    pierce = 3 + player.bladeExtraPierce();
                    split = player.upBladeLegendarySplit;
                    verticalBounce = player.upBladeVerticalBounce;
                    ap = player.upBladeArmorPen;
                }

                // two angled blades like your sandbox
                outBullets.add(new Blade(
                        muzzleCenterX, muzzleY,
                        -14, -4,
                        10,
                        bounces,
                        pierce,
                        split,
                        verticalBounce,
                        ap
                ));
                outBullets.add(new Blade(
                        muzzleCenterX, muzzleY,
                        +14, -4,
                        10,
                        bounces,
                        pierce,
                        split,
                        verticalBounce,
                        ap
                ));

                bladeMs = 0;
                bladeNextMs = 1200 + rng.nextInt(900);
            }
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
