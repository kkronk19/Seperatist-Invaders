package spaceinvaders.core.systems;

import java.util.List;

import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.EnemyBullet;
import spaceinvaders.core.entities.Invader;
import spaceinvaders.services.audio.AudioManager;

public final class InvaderAttackSystem {

    private InvaderAttackSystem() {}

    /**
     * Converts inv.firePending into actual enemy bullets.
     * Call once per frame AFTER inv.update(...).
     */
    public static void spawnShooterBullets(List<Invader> invaders, List<Bullet> bullets) {
        for (Invader inv : invaders) {
            if (inv.kind != Invader.InvaderKind.SHOOTER) continue;
            if (!inv.firePending) continue;

            inv.firePending = false;

            int vx = 0;
            int vy = 6;     // downward toward player
            int size = 8;
            int dmg = 1;

            // spawn bullet from invader "gun" (center bottom)
            int bx = inv.x + (inv.width  / 2) - (size / 2);
            int by = inv.y + inv.height - 2;

            bullets.add(new EnemyBullet(bx, by, vx, vy, size, dmg));

            try {
                AudioManager.get().playSfx(
                    "/spaceinvaders/resources/audio/sfx/wpn_cis_blaster_fire.wav",
                    0.25f
                );
            } catch (Throwable ignored) {}
        }
    }
}
