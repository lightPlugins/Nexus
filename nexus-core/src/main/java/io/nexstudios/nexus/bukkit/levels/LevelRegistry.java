package io.nexstudios.nexus.bukkit.levels;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    /**
     * Liefert eine Momentaufnahme aller registrierten LevelKeys.
     * Wird u.a. von preloadAllForPlayer genutzt.
     */
    public Set<LevelKey> getAllKeys() {
        return new HashSet<>(defs.keySet());
    }
}