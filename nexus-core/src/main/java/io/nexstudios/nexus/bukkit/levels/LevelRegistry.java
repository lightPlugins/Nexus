package io.nexstudios.nexus.bukkit.levels;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class LevelRegistry {
    private final Map<LevelKey, LevelDefinition> defs = new ConcurrentHashMap<>();

    public void register(LevelKey key, LevelDefinition def) {
        defs.put(key, def);
    }

    public boolean isRegistered(LevelKey key) {
        return defs.containsKey(key);
    }

    public Optional<LevelDefinition> get(LevelKey key) {
        return Optional.ofNullable(defs.get(key));
    }
}