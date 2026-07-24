package spaceinvaders.features.campaign;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/** Mutable upgrade levels owned by one campaign run only. */
public final class RunUpgrades {
    private final EnumMap<UpgradeId, Integer> levels = new EnumMap<>(UpgradeId.class);
    private final EnumSet<UpgradePath> favoredPaths = EnumSet.noneOf(UpgradePath.class);

    public int level(UpgradeId id) { return levels.getOrDefault(id, 0); }

    public boolean canUpgrade(UpgradeId id) { return level(id) < id.maxLevel(); }
    public boolean isEligible(UpgradeId id) {
        if (!canUpgrade(id)) return false;
        return switch (id) {
            // Internal support-slot key; the Protocol card is the first selectable clone-path node.
            case CLONE_REINFORCEMENTS -> false;
            case MULTI_SHOT -> level(UpgradeId.FIRE_RATE) >= 2;
            case PIERCING -> level(UpgradeId.BLASTER_DAMAGE) >= 2;
            case SMART_BULLETS_UNLOCK -> level(UpgradeId.BOLT_SPEED) >= 2 || level(UpgradeId.BLASTER_DAMAGE) >= 3;
            case SMART_BULLET_TRACKING, SMART_BULLET_RANGE -> level(UpgradeId.SMART_BULLETS_UNLOCK) > 0;
            case MISSILE_DAMAGE, MISSILE_COOLDOWN, MISSILE_SPEED, EXPLOSION_RADIUS, MISSILE_COUNT -> level(UpgradeId.MISSILE_LAUNCHER) > 0;
            case SMART_MISSILE_UNLOCK -> level(UpgradeId.MISSILE_LAUNCHER)>0 && level(UpgradeId.MISSILE_SPEED)>=2;
            case SMART_MISSILE_TRACKING, SMART_MISSILE_TURN_RATE -> level(UpgradeId.SMART_MISSILE_UNLOCK)>0;
            case SHRAPNEL_UNLOCK -> level(UpgradeId.EXPLOSION_RADIUS)>=2;
            case SHRAPNEL_COUNT, SHRAPNEL_DAMAGE, SHRAPNEL_SPEED -> level(UpgradeId.SHRAPNEL_UNLOCK)>0;
            case BLADE_DAMAGE, BLADE_COUNT, BLADE_THROW_SPEED, BLADE_LIFETIME, BLADE_BOUNCE_COUNT, BLADE_SIZE, BLADE_PENETRATION, BLADE_RECALL_SPEED -> level(UpgradeId.BLADE_UNLOCK)>0;
            case ADDITIONAL_CLONE, CLONE_HEALTH, CLONE_DAMAGE, CLONE_FIRE_RATE, CLONE_ACCURACY, CLONE_ARMOR, CLONE_REVIVAL, CLONE_DISCIPLINE -> level(UpgradeId.CLONE_SUPPORT_UNLOCK)>0;
            case HEAVY_REINFORCEMENT_AUTHORIZATION -> level(UpgradeId.CLONE_SUPPORT_UNLOCK) > 0 && level(UpgradeId.CLONE_HEALTH) >= 2 && level(UpgradeId.CLONE_DAMAGE) >= 2;
            case TANK_SUPPORT_UNLOCK -> level(UpgradeId.HEAVY_REINFORCEMENT_AUTHORIZATION) > 0;
            case TANK_ARMOR, TANK_WEAPON_DAMAGE, TANK_SUPPORT_CHARGES -> level(UpgradeId.TANK_SUPPORT_UNLOCK) > 0;
            case ADVANCED_REPUBLIC_ARMOR -> level(UpgradeId.TANK_ARMOR) >= 2 && level(UpgradeId.TANK_WEAPON_DAMAGE) >= 2;
            default -> true;
        };
    }

    /** Returns false instead of allowing a card to exceed its defined cap. */
    public boolean apply(UpgradeId id) {
        if (!canUpgrade(id)) return false;
        levels.put(id, level(id) + 1);
        return true;
    }

    /** A path receives its 1.5x appearance weight only once per campaign run. */
    public void favor(UpgradePath path) { favoredPaths.add(path); }
    public boolean isFavored(UpgradePath path) { return favoredPaths.contains(path); }
    public double cardWeight(UpgradeId id) { return isFavored(id.path()) ? 1.5 : 1.0; }

    public List<UpgradeId> available() {
        List<UpgradeId> result = new ArrayList<>();
        for (UpgradeId id : UpgradeId.values()) if (isEligible(id)) result.add(id);
        return result;
    }
}
