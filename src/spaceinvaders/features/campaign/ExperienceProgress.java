package spaceinvaders.features.campaign;

/** XP ledger with explicit one-level-at-a-time consumption for upgrade screens. */
public final class ExperienceProgress {
    private int xp;
    private int completedLevels;

    public void add(int amount) { xp += Math.max(0, amount); }
    public int xp() { return xp; }
    public int completedLevels() { return completedLevels; }
    public int nextRequirement() { return 100 + completedLevels * 50; }

    /** Keeps overflow for later calls, allowing each level-up to show one card set. */
    public boolean consumeLevelUp() {
        if (xp < nextRequirement()) return false;
        xp -= nextRequirement();
        completedLevels++;
        return true;
    }
}
