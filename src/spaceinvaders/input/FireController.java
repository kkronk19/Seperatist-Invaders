package spaceinvaders.input;

import java.util.ArrayList;
import java.util.List;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Player;
import spaceinvaders.weapons.Weapon;

/**
 * Central input->weapon dispatcher.
 * Owns weapon instances; weapons read Player for upgrades.
 */
public class FireController {
    private final Weapon primary;    // Space (hold)
    private final Weapon secondary;  // R (tap)

    private final Player player;

    private boolean primaryHeld = false;
    private boolean pendingSecondaryTap = false;

    public FireController(Player player, Weapon primary, Weapon secondary) {
        this.player = player;
        this.primary = primary;
        this.secondary = secondary;
    }

    public void setPrimaryHeld(boolean held) { primaryHeld = held; }
    public void triggerSecondaryTap() { pendingSecondaryTap = true; }

    public List<Bullet> tick(long nowMs, int mx, int my, List<Bullet> all, int w, int h) {
        List<Bullet> spawned = new ArrayList<>();

        if (primary != null) {
            primary.tryFire(nowMs, mx, my, primaryHeld, spawned);
        }
        if (secondary != null && pendingSecondaryTap) {
            secondary.tryFire(nowMs, mx, my, false, spawned);
        }
        pendingSecondaryTap = false;

        // Make a combined view so updateBullets sees brand-new missiles this frame
        List<Bullet> allPlus = (all.isEmpty() && spawned.isEmpty())
                ? all
                : new ArrayList<>(all);
        if (!spawned.isEmpty() && allPlus != all) allPlus.addAll(spawned);

        if (primary != null)   primary.updateBullets(nowMs, allPlus, w, h);
        if (secondary != null) secondary.updateBullets(nowMs, allPlus, w, h);

        // OPTIONAL (next step): if player points reached threshold, your scene can pause + show upgrade menu.
        // if (player != null && player.pointsBanked >= player.nextUpgradeCost) { ... }

        return spawned; // Scene still owns adding them to its 'bullets' list
    }

    public Player getPlayer() { return player; }
}
