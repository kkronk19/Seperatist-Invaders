package spaceinvaders.core.entities;

/**
 * Source of truth for all player stats + upgrade state.
 * Other systems/weapons READ this, they do not store permanent upgrade state.
 */
public class Player {

    /* -------------------- Core stats -------------------- */

    /** Horizontal movement speed in px per tick/frame (your code uses int steps). */
    public int speedPx = 8;

    /** Player health (dying from being shot / hit). */
    public int hp = 10;
    public int maxHp = 10;

    /** "Border / leak health" (enemies slipping past southern border). */
    public int borderHp = 25;
    public int maxBorderHp = 25;

    /* -------------------- Economy -------------------- */

    /** Points currently banked toward next upgrade offer. */
    public int pointsBanked = 0;

    /** Total points earned lifetime (run). */
    public int pointsEarned = 0;

    /** Total points spent (for metrics / balancing). */
    public int pointsSpent = 0;

    /** Next threshold required to offer upgrade. You can tune this later. */
    public int nextUpgradeCost = 100;

    /** Earn points (kill reward, etc.). Returns true if you should offer upgrades now. */
    public boolean addPoints(int amount) {
        if (amount <= 0) return false;
        pointsEarned += amount;
        pointsBanked += amount;
        return pointsBanked >= nextUpgradeCost;
    }

    /** Spend points for an upgrade offer selection. Keeps overlap. */
    public void consumeUpgradeCost() {
        int cost = Math.max(1, nextUpgradeCost);
        pointsBanked = Math.max(0, pointsBanked - cost);
        pointsSpent += cost;

        // Example scaling (100 -> 110 -> 121...). Tune as you like.
        nextUpgradeCost = (int) Math.ceil(nextUpgradeCost * 1.10);
    }

    /* -------------------- Leak damage rules -------------------- */

    /** Damage to BORDER when an invader escapes past southern border. */
    public int leakDamageBasic    = 1;
    public int leakDamageShielded = 2;
    public int leakDamageSwarmer  = 1;
    public int leakDamageShooter  = 2;
    public int leakDamageTank     = 4;

    public int leakDamageFor(Invader.InvaderKind kind) {
        if (kind == null) return 1;
        switch (kind) {
            case TANK:     return leakDamageTank;
            case SHOOTER:  return leakDamageShooter;
            case SHIELDED: return leakDamageShielded;
            case SWARMER:  return leakDamageSwarmer;
            case BASIC:
            default:       return leakDamageBasic;
        }
    }

    /* -------------------- Upgrade state -------------------- */

    // -------- Missile (basic) --------
    public boolean upMissileStraight = false;   // 1
    public int upMissileDmg100 = 0;             // 4 ranks
    public int upMissilePlus1 = 0;              // 2 ranks (each rank +1 missile)
    public int upMissilePlus3Shrapnel = 0;      // 3 ranks (each rank +3)
    public int upMissileShrapnelDmgPlus2 = 0;   // 2 ranks (each +2)
    public int upMissileFireRate25 = 0;         // 2 ranks (each +25%)

    // -------- Missile (legendary) --------
    public boolean upMissileSmart = false;
    public boolean upShrapnelSmart = false;
    public int upShrapnelArmorPierce = 0;       // 2 ranks (each rank: +3 penetrations, you’ll wire in shrapnel bullet logic later)
    public boolean upMissileImpactAOE = false;
    public int secretTechMissile = 0;           // 0/1 (your “??? then reveals later” flow)

    // -------- Blade (basic) --------
    public int upBladeNorthRicochet = 0;        // 1
    public int upBladeSouthRicochet = 0;        // 1 (requires north to show, but data exists)
    public int upBladeDmg100 = 0;               // 2 ranks
    public int upBladeBouncesPlus3 = 0;         // 3 ranks (each +3)
    public int upBladePiercePlus3 = 0;          // 3 ranks (each +3)
    public int upBladeFireRate25 = 0;           // 2 ranks
    public boolean upBladeArmorPen = false;     // 1
    public boolean upBladeShieldNull = false;   // 1

    // -------- Blade (legendary) --------
    public boolean upBladeInitialSplit = false;
    public boolean upBladeSmart = false;
    public boolean upBladeEnergyTrail = false;
    public int secretTechBlade = 0;

    // -------- Basic shot (basic) --------
    public int upBasicDmg100 = 0;               // 2
    public int upBasicPiercePlus3 = 0;          // 3
    public int upBasicFireRate25 = 0;           // 2
    public int upBasicPlus1Bullets = 0;         // 2 (each +1 projectile)
    public boolean upBasicArmorPierce = false;  // 1

    // -------- Basic shot (legendary) --------
    public boolean upBasicSmart = false;
    public int upBasicOvertuned6 = 0;           // 2 ranks
    public boolean upBasicMoneyShot = false;
    public boolean upBasicSystemsFried = false;

    // -------- Player (basic) --------
    public int upPlayerSpeed35 = 0;             // 3
    public int upPlayerHp15_Border25 = 0;       // 3
    public int upMoney45 = 0;                   // 2
    public boolean upOverclock10 = false;       // 1
    public boolean upSixthSense = false;        // 1

    // -------- Player (legendary) --------
    public boolean upHybridWeaponCore = false;  // only shows if both secret techs selected later
    public int upYwingSupport = 0;              // 3 ranks
    public boolean upSmarterTargeting = false;
    public boolean upPersonalShield = false;

    // -------- Clone support (basic) --------
    public int upCloneReinforcement = 0;        // 5
    public boolean upCloneMissileArm = false;
    public boolean upCloneBladeArm = false;
    public int upCloneCooling35 = 0;            // 2

    // -------- Clone support (legendary) --------
    public boolean upCloneUsesBasicUpgrades = false;
    public boolean upCloneUsesMissileUpgrades = false;
    public boolean upCloneUsesBladeUpgrades = false;

    /* -------------------- Derived helpers used by weapons -------------------- */

    /** Multiplicative fire-rate multiplier (1.0 = unchanged). */
    public double globalFireRateMult() {
        double mult = 1.0;
        if (upOverclock10) mult *= 1.10;
        return mult;
    }

    /** Missile fire-rate multiplier from its tree. */
    public double missileFireRateMult() {
        return globalFireRateMult() * (1.0 + 0.25 * upMissileFireRate25);
    }

    public int missileCount() {
        // base 1 missile, each rank adds +1, max 3 total by your design
        int count = 1 + Math.max(0, upMissilePlus1);
        return Math.max(1, Math.min(3, count));
    }

    public int missileDamage(int base) {
        // each rank doubles damage (+100%): base * (1 + ranks)
        return base * (1 + Math.max(0, upMissileDmg100));
    }

    public int missileShrapnelCount(int base) {
        return base + 3 * Math.max(0, upMissilePlus3Shrapnel);
    }

    public int missileShrapnelDamage(int base) {
        return base + 2 * Math.max(0, upMissileShrapnelDmgPlus2);
    }

    public int bladeDamage(int base) {
        return base * (1 + Math.max(0, upBladeDmg100));
    }

    public int bladeExtraBounces() {
        return 3 * Math.max(0, upBladeBouncesPlus3);
    }

    public int bladeExtraPierce() {
        return 3 * Math.max(0, upBladePiercePlus3);
    }

    public double bladeFireRateMult() {
        return globalFireRateMult() * (1.0 + 0.25 * upBladeFireRate25);
    }

    public int basicDamage(int base) {
        return base * (1 + Math.max(0, upBasicDmg100));
    }

    public int basicExtraPierce() {
        return 3 * Math.max(0, upBasicPiercePlus3);
    }

    public int basicProjectilesPerShot() {
        return 1 + Math.max(0, upBasicPlus1Bullets);
    }

    public double basicFireRateMult() {
        return globalFireRateMult() * (1.0 + 0.25 * upBasicFireRate25);
    }

    /** Apply player upgrades that scale core stats. Call this after selecting upgrades. */
    public void recomputeCoreStats() {
        // speed
        speedPx = (int) Math.round(8 * (1.0 + 0.35 * upPlayerSpeed35));

        // max hp/border
        maxHp = 10 + 15 * upPlayerHp15_Border25;
        maxBorderHp = 25 + 25 * upPlayerHp15_Border25;

        // keep current, clamp to max (change to "hp = maxHp" if you want upgrades to refill)
        hp = Math.min(hp, maxHp);
        borderHp = Math.min(borderHp, maxBorderHp);
    }
}
