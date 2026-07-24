package spaceinvaders.features.campaign;

/** XP ledger with explicit one-level-at-a-time consumption for upgrade screens. */
public final class ExperienceProgress {
    private int xp;
    private int completedLevels;

    public void add(int amount) { xp += Math.max(0, amount); }
    public int xp() { return xp; }
    public int completedLevels() { return completedLevels; }
    /** Original curve was 100 + 50*level; campaign pacing uses round(75%). */
    public int nextRequirement() { return Math.max(1, (int) Math.round((100 + completedLevels * 50) * .75)); }

    /** Keeps overflow for later calls, allowing each level-up to show one card set. */
    public boolean consumeLevelUp() {
        if (xp < nextRequirement()) return false;
        xp -= nextRequirement();
        completedLevels++;
        return true;
    }
}
