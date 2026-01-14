package spaceinvaders.weapons;

import java.util.List;
import java.util.Random;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Player;

public class BlasterWeapon implements Weapon {

    private final Player player;
    private final Random rng = new Random();

    // base stats
    private final int baseCooldownMs = 180;
    private final int baseDamage = 1;
    private final int bulletSize = 8;
    private final int bulletSpeed = -10;

    private long lastShot = 0L;

    public BlasterWeapon(Player player) {
        this.player = player;
    }

    @Override
    public void tryFire(long nowMs, int mx, int my, boolean isHeld, List<Bullet> out) {
        if (!isHeld) return;

        int cd = effectiveCooldownMs();
        if (nowMs - lastShot < cd) return;
        lastShot = nowMs;

        int bullets = player.basicProjectilesPerShot();
        int damage  = player.basicDamage(baseDamage);
        int pierce  = player.basicExtraPierce();

        boolean armorPiercing = player.upBasicArmorPierce;

        // spacing for multi-shot
        int spacing = 12;
        int startOffset = -(bullets - 1) * spacing / 2;

        for (int i = 0; i < bullets; i++) {
            int ox = startOffset + i * spacing;

            Bullet b = new Bullet(
                    mx - bulletSize / 2 + ox,
                    my,
                    0,
                    bulletSpeed,
                    bulletSize,
                    damage,
                    Bullet.BulletKind.BASIC
            );

            b.pierce = pierce;
            b.armorPiercing = armorPiercing;

            // ---- LEGENDARY FLAGS (logic handled elsewhere later) ----
            b.smartTargeting = player.upBasicSmart;
            b.moneyShot = player.upBasicMoneyShot;
            b.systemsFried = player.upBasicSystemsFried;

            // Overtuned bolt chance (split handled later)
            if (player.upBasicOvertuned6 > 0) {
                double chance = 0.06 * player.upBasicOvertuned6; // 6% per rank
                b.overtuned = rng.nextDouble() < chance;
            }

            out.add(b);
        }
    }

    private int effectiveCooldownMs() {
        double mult = player.basicFireRateMult();
        return (int) Math.max(60, Math.round(baseCooldownMs / mult));
    }

    @Override
    public void updateBullets(long nowMs, List<Bullet> bullets, int w, int h) {
        // Basic bullets currently have no per-frame weapon-side behavior.
        // (smart targeting / overtuned splitting can be implemented later in a system)
    }
}
