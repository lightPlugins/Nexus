package io.nexstudios.nexus.bukkit.levels;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    void safeToDatabase(UUID playerId);
    void safeAllToDatabase();
    void invalidate(UUID playerId, String namespace, String key);

    double getRequiredXp(String namespace, String key, int level);

    default double getRequiredXpForCurrentLevel(UUID playerId, String namespace, String key) {
        LevelProgress progress = getProgress(playerId, namespace, key);
        if (progress == null) {
            throw new IllegalStateException(
                    "Could not find LevelProgress for player " + playerId + " / " + namespace + ":" + key
            );
        }

        int currentLevel = progress.getLevel();
        int nextLevel = currentLevel + 1;
        if (nextLevel <= 0) {
            nextLevel = 1;
        }

        // Optional: Wenn schon über maxLevel, letzte bekannte Schwelle zurückgeben
        try {
            return getRequiredXp(namespace, key, nextLevel);
        } catch (IllegalArgumentException ex) {
            // nextLevel > maxLevel -> Fallback: maxLevel-Schwelle
            return getRequiredXp(namespace, key, currentLevel);
        }
    }

    void preloadAllAsync();
    void preloadAllSync();

    void preloadAllForPlayer(UUID playerId);
    void preloadForAllPlayers(String namespace, String key);

    CompletableFuture<Boolean> resetPlayer(UUID playerId);
    CompletableFuture<Boolean> resetPlayerForType(UUID playerId, String namespace, String key);
    CompletableFuture<Boolean> resetAllPlayers();
    CompletableFuture<Boolean> resetAllPlayersForType(String namespace, String key);

}