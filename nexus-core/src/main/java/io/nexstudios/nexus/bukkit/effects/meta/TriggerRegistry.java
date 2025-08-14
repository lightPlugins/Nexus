package io.nexstudios.nexus.bukkit.effects.meta;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerRegistry {
    private static final Set<String> TYPES = ConcurrentHashMap.newKeySet();

    private TriggerRegistry() {}

    public static String normalize(String id) {
        return id == null ? null : id.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean register(String id) {
        String key = normalize(id);
        if (key == null || key.isEmpty()) return false;
        return TYPES.add(key);
    }

    public static boolean isRegistered(String id) {
        String key = normalize(id);
        return key != null && TYPES.contains(key);
    }

    public static Set<String> snapshot() {
        return Set.copyOf(TYPES);
    }
}
