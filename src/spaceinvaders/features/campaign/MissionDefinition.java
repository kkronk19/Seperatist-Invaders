package spaceinvaders.features.campaign;

import java.util.List;

/** Immutable, inspectable campaign mission schedule. */
public record MissionDefinition(int chapter, int mission, List<SpawnEvent> spawns) {
    public int totalEnemies() { return spawns.stream().mapToInt(SpawnEvent::count).sum(); }
    public long count(EnemyType type) { return spawns.stream().filter(e -> e.enemyType() == type).mapToLong(SpawnEvent::count).sum(); }
}
