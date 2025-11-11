package io.nexstudios.nexus.bukkit.levels;

import java.util.List;
import java.util.UUID;

public interface LevelService {
    // Registrierung
    void registerLevel(String namespace, String key, List<Double> neededExp);

    // Lesen/Mutationen
    LevelProgress getProgress(UUID playerId, String namespace, String key);
    LevelProgress addXp(UUID playerId, String namespace, String key, double deltaXp);
    LevelProgress removeXp(UUID playerId, String namespace, String key, double deltaXp);
    LevelProgress addLevel(UUID playerId, String namespace, String key, int deltaLevels);
    LevelProgress setLevel(UUID playerId, String namespace, String key, int newLevel);
    LevelProgress setXp(UUID playerId, String namespace, String key, double newXp);

    // Cache/Flush
    void flushPlayer(UUID playerId);
    void flushAll();
    void invalidate(UUID playerId, String namespace, String key);

    void preloadAllAsync();
    void preloadAllSync();

}