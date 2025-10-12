package io.nexstudios.nexus.bukkit.effects.filters;

import io.nexstudios.nexus.bukkit.effects.filters.impl.HasPermissionFilter;
import io.nexstudios.nexus.bukkit.effects.filters.impl.InWorldFilter;
import io.nexstudios.nexus.bukkit.effects.filters.impl.InventoryContainsFilter;
import io.nexstudios.nexus.bukkit.effects.filters.impl.MatchItemFilter;

import java.util.*;
import java.util.function.Function;

public final class FilterFactory {

    private FilterFactory() {}

    // Builder-Maps je Kontext-Art
    private static final Map<String, Function<Map<String, Object>, NexusFilter<PlayerContext>>> PLAYER_BUILDERS = new HashMap<>();
    private static final Map<String, Function<Map<String, Object>, NexusFilter<WorldContext>>>  WORLD_BUILDERS  = new HashMap<>();

    private static volatile boolean INITIALIZED = false;

    private static synchronized void initIfNeeded() {
        if (INITIALIZED) return;

        // Player-basierte Filter
        registerPlayerFilter("match-item-hand", MatchItemFilter::fromConfig);
        registerPlayerFilter("match-item-inventory", InventoryContainsFilter::fromConfig);
        registerPlayerFilter("has-permission", HasPermissionFilter::fromConfig);

        // World-basierte Filter
        registerWorldFilter("in-world", InWorldFilter::fromConfig);

        INITIALIZED = true;
    }

    private static void registerPlayerFilter(String id, Function<Map<String, Object>, NexusFilter<PlayerContext>> builder) {
        PLAYER_BUILDERS.put(id.toLowerCase(Locale.ROOT), builder);
    }

    private static void registerWorldFilter(String id, Function<Map<String, Object>, NexusFilter<WorldContext>> builder) {
        WORLD_BUILDERS.put(id.toLowerCase(Locale.ROOT), builder);
    }

    /**
     * Generische Kompilierung: wandelt YAML-Filter in konkrete Filter für Ziel-Context T um.
     * Adapter bestimmen, wie Player-/World-Filter auf T angewendet werden.
     */
    private static <T extends TriggerContext> List<NexusFilter<T>> compile(
            Object rawFilters,
            Function<NexusFilter<PlayerContext>, NexusFilter<T>> adaptPlayer,
            Function<NexusFilter<WorldContext>, NexusFilter<T>> adaptWorld
    ) {
        if (!(rawFilters instanceof List<?> list) || list.isEmpty()) return List.of();

        initIfNeeded();

        List<NexusFilter<T>> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> map)) continue;

            Object rid = map.get("id");
            if (rid == null) continue;
            String id = String.valueOf(rid).toLowerCase(Locale.ROOT);

            // Config als String->Object map spiegeln
            Map<String, Object> cfg = new HashMap<>();
            map.forEach((k, v) -> cfg.put(String.valueOf(k), v));

            // Player-Filter?
            Function<Map<String, Object>, NexusFilter<PlayerContext>> pBuilder = PLAYER_BUILDERS.get(id);
            if (pBuilder != null) {
                NexusFilter<PlayerContext> f = pBuilder.apply(cfg);
                if (f != null) out.add(adaptPlayer.apply(f));
                continue;
            }

            // World-Filter?
            Function<Map<String, Object>, NexusFilter<WorldContext>> wBuilder = WORLD_BUILDERS.get(id);
            if (wBuilder != null) {
                NexusFilter<WorldContext> f = wBuilder.apply(cfg);
                if (f != null) out.add(adaptWorld.apply(f));
                continue;
            }

            // Unbekannt: wurde i. d. R. bereits durch Registry/Loader geblockt; hier ignorieren
        }
        return out;
    }

    // Damage-Pipeline: DamageContext implementiert PlayerContext & WorldContext -> direkte Adapter
    public static List<NexusFilter<DamageContext>> compileForDamage(Object rawFilters) {
        return compile(
                rawFilters,
                playerFilter -> playerFilter::test,
                worldFilter  -> worldFilter::test
        );
    }

    // Beispiel für zukünftige Trigger (KillContext):
    // public static List<NexusFilter<KillContext>> compileForKill(Object rawFilters) {
    //     return compile(
    //             rawFilters,
    //             playerFilter -> (KillContext kc) -> playerFilter.test(kc),
    //             worldFilter  -> (KillContext kc) -> worldFilter.test(kc)
    //     );
    // }
}






