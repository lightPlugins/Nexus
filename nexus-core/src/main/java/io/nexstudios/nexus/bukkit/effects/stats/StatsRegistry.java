package io.nexstudios.nexus.bukkit.effects.stats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsRegistry {
    private static final Map<String, Map<String, NexusStat>> REGISTRY = new ConcurrentHashMap<>();
    private StatsRegistry() {}

    public static void put(NexusStat stat) {
        REGISTRY
                .computeIfAbsent(stat.namespace(), ns -> new ConcurrentHashMap<>())
                .put(stat.id(), stat);
    }

    public static void putAll(Collection<? extends NexusStat> stats) {
        if (stats == null) return;
        for (NexusStat s : stats) put(s);
    }

    public static NexusStat get(String namespace, String statId) {
        if (namespace == null || statId == null) return null;
        Map<String, NexusStat> byNs = REGISTRY.get(namespace.toLowerCase(Locale.ROOT));
        if (byNs == null) return null;
        return byNs.get(statId.toLowerCase(Locale.ROOT));
    }

    public static NexusStat getAny(String statId) {
        if (statId == null) return null;
        String id = statId.toLowerCase(Locale.ROOT);
        for (Map<String, NexusStat> byNs : REGISTRY.values()) {
            NexusStat s = byNs.get(id);
            if (s != null) return s;
        }
        return null;
    }

    public static List<NexusStat> getAllById(String statId) {
        if (statId == null) return List.of();
        String id = statId.toLowerCase(Locale.ROOT);
        List<NexusStat> out = new ArrayList<>();
        for (Map<String, NexusStat> byNs : REGISTRY.values()) {
            NexusStat s = byNs.get(id);
            if (s != null) out.add(s);
        }
        return Collections.unmodifiableList(out);
    }

    public static Collection<NexusStat> all() {
        List<NexusStat> out = new ArrayList<>();
        for (var map : REGISTRY.values()) out.addAll(map.values());
        return Collections.unmodifiableList(out);
    }

    public static Collection<NexusStat> byNamespace(String namespace) {
        Map<String, NexusStat> map = REGISTRY.get(namespace == null ? "" : namespace.toLowerCase(Locale.ROOT));
        return map == null ? List.of() : Collections.unmodifiableCollection(map.values());
    }

    public static void removeNamespace(String namespace) {
        if (namespace == null) return;
        REGISTRY.remove(namespace.toLowerCase(Locale.ROOT));
    }

    // Neu: Globale Existenz-Prüfung
    public static boolean containsId(String statId) {
        return getAny(statId) != null;
    }
}


