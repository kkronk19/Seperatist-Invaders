package spaceinvaders.features.campaign;

import java.util.List;

/** Chapter composition is intentionally data, not random gameplay code. */
public final class CampaignDefinitions {
    private CampaignDefinitions() { }
    public static MissionDefinition mission(int chapter, int mission) {
        return switch (chapter + ":" + mission) {
            case "1:1" -> d(1, 1, e(0, EnemyType.B1, 5, "line"), e(3000, EnemyType.B1, 5, "stagger"));
            case "1:2" -> d(1, 2, e(0, EnemyType.B1, 5, "line"), e(3000, EnemyType.B1, 5, "stagger"), e(6000, EnemyType.B1, 5, "v"), e(9000, EnemyType.B1, 5, "alternate"), e(12000, EnemyType.B1, 5, "line"));
            case "1:3" -> d(1, 3, e(0, EnemyType.B2, 5, "frontline"), e(2000, EnemyType.B1, 5, "stagger"), e(4000, EnemyType.B2, 5, "frontline"), e(6000, EnemyType.B1, 5, "line"), e(8000, EnemyType.B1, 5, "v"), e(10000, EnemyType.B1, 5, "alternate"), e(12000, EnemyType.B1, 5, "line"));
            case "2:1" -> d(2, 1, e(0, EnemyType.B2, 5, "frontline"), e(2000, EnemyType.B1, 5, "line"), e(4000, EnemyType.B1, 5, "stagger"), e(6000, EnemyType.B2, 5, "frontline"), e(8000, EnemyType.B1, 5, "v"), e(10000, EnemyType.B1, 5, "alternate"), e(12000, EnemyType.B1, 5, "line"), e(14000, EnemyType.B1, 5, "stagger"), e(16000, EnemyType.B1, 5, "line"));
            case "2:2" -> d(2, 2, e(0, EnemyType.B1, 5, "line"), e(3000, EnemyType.DROIDEKA, 5, "frontline"), e(6000, EnemyType.B1, 5, "stagger"), e(9000, EnemyType.B2, 5, "frontline"), e(12000, EnemyType.DROIDEKA, 5, "line"), e(15000, EnemyType.B1, 5, "v"));
            case "2:3" -> d(2, 3, e(0, EnemyType.B1, 5, "line"), e(3000, EnemyType.HAILFIRE, 1, "left"), e(5000, EnemyType.B1, 5, "stagger"), e(8000, EnemyType.HAILFIRE, 2, "alternate"), e(10000, EnemyType.B1, 5, "line"), e(13000, EnemyType.HAILFIRE, 2, "alternate"), e(15000, EnemyType.B1, 5, "v"));
            default -> throw new IllegalArgumentException("Unknown campaign mission " + chapter + ":" + mission);
        };
    }
    private static MissionDefinition d(int chapter, int mission, SpawnEvent... events) { return new MissionDefinition(chapter, mission, List.of(events)); }
    private static SpawnEvent e(int ms, EnemyType type, int count, String formation) { return new SpawnEvent(ms, type, count, formation); }
}
