package spaceinvaders.input;

import spaceinvaders.core.entities.Bullet;
import spaceinvaders.weapons.Weapon;

import java.util.ArrayList;
import java.util.List;

public class FireController {
    private final Weapon primary;    // Space (hold)
    private final Weapon secondary;  // R (tap)

    private boolean primaryHeld = false;
    private boolean pendingSecondaryTap = false;

    public FireController(Weapon primary, Weapon secondary) {
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
        if (secondary != null) {
            if (pendingSecondaryTap) {
                secondary.tryFire(nowMs, mx, my, false, spawned);
            }
        }
        pendingSecondaryTap = false;

        // Make a combined view so updateBullets sees brand-new missiles this frame
        List<Bullet> allPlus = (all.isEmpty() && spawned.isEmpty())
                ? all
                : new ArrayList<>(all);
        if (!spawned.isEmpty() && allPlus != all) allPlus.addAll(spawned);

        if (primary != null)   primary.updateBullets(nowMs, allPlus, w, h);
        if (secondary != null) secondary.updateBullets(nowMs, allPlus, w, h);

        return spawned; // Scene still owns adding them to its 'bullets' list
    }
}
