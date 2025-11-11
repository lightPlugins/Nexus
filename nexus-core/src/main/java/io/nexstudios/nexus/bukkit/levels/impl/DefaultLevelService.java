package io.nexstudios.nexus.bukkit.levels.impl;

import io.nexstudios.nexus.bukkit.levels.LevelProgress;
import io.nexstudios.nexus.bukkit.levels.LevelService;
import io.nexstudios.nexus.bukkit.levels.NexLevel;

import java.util.List;
import java.util.UUID;

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

    @Override
    public void flushPlayer(UUID playerId) {
        core.flushPlayer(playerId);
    }

    @Override
    public void flushAll() {
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

}