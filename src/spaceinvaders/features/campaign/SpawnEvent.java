package spaceinvaders.features.campaign;

/** One deterministic group that is spawned exactly once when its time is reached. */
public record SpawnEvent(int timeMs, EnemyType enemyType, int count, String formation) { }
