package io.nexstudios.nexus.bukkit.levels.impl;

import io.nexstudios.nexus.bukkit.levels.LevelProgress;
import io.nexstudios.nexus.bukkit.levels.LevelService;
import io.nexstudios.nexus.bukkit.levels.NexLevel;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DefaultLevelService implements LevelService {

    private final NexLevel core;

    public DefaultLevelService(NexLevel core) {
        this.core = core;
    }

    @Override
    public void registerLevel(String namespace, String key, List<Double> neededExp) {
        core.registerLevel(namespace, key, neededExp);
    }

    @Override
    public LevelProgress getProgress(UUID playerId, String namespace, String key) {
        return core.getProgress(playerId, namespace, key);
    }

    @Override
    public LevelProgress addXp(UUID playerId, String namespace, String key, double deltaXp) {
        return core.addXp(playerId, namespace, key, deltaXp);
    }

    @Override
    public LevelProgress removeXp(UUID playerId, String namespace, String key, double deltaXp) {
        return core.removeXp(playerId, namespace, key, deltaXp);
    }

    @Override
    public LevelProgress addLevel(UUID playerId, String namespace, String key, int deltaLevels) {
        return core.addLevel(playerId, namespace, key, deltaLevels);
    }

    @Override
    public LevelProgress setLevel(UUID playerId, String namespace, String key, int newLevel) {
        return core.setLevel(playerId, namespace, key, newLevel);
    }

    @Override
    public LevelProgress setXp(UUID playerId, String namespace, String key, double newXp) {
        return core.setXp(playerId, namespace, key, newXp);
    }

    // Cache/Flush
    @Override
    public void safeToDatabase(UUID playerId) {
        core.flushPlayer(playerId);
    }

    @Override
    public void safeAllToDatabase() {
        core.flushAll();
    }

    @Override
    public void invalidate(UUID playerId, String namespace, String key) {
        core.invalidate(playerId, namespace, key);
    }

    @Override
    public void preloadAllAsync() {
        core.preloadAllAsync();
    }

    @Override
    public void preloadAllSync() {
        core.preloadAllSync();
    }

    @Override
    public double getRequiredXp(String namespace, String key, int level) {
        return core.getRequiredXp(namespace, key, level);
    }

    @Override
    public void preloadAllForPlayer(UUID playerId) {
        core.preloadAllForPlayer(playerId);
    }

    @Override
    public void preloadForAllPlayers(String namespace, String key) {
        core.preloadForAllPlayers(namespace, key);
    }

    @Override
    public CompletableFuture<Boolean> resetPlayer(UUID playerId) {
        return core.resetPlayer(playerId);
    }

    @Override
    public CompletableFuture<Boolean> resetPlayerForType(UUID playerId, String namespace, String key) {
        return core.resetPlayerForType(playerId, namespace, key);
    }

    @Override
    public CompletableFuture<Boolean> resetAllPlayersForType(String namespace, String key) {
        return core.resetAllPlayersForType(namespace, key);
    }

    @Override
    public CompletableFuture<Boolean> resetAllPlayers() {
        return core.resetAllPlayers();
    }
}