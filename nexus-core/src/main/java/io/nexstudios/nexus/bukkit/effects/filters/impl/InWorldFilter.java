package io.nexstudios.nexus.bukkit.effects.filters.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.filters.NexusFilter;
import io.nexstudios.nexus.bukkit.effects.filters.WorldContext;
import org.bukkit.World;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InWorldFilter implements NexusFilter<WorldContext> {

    private final Set<String> worldNamesLower; // Vergleich über Namen (case-insensitive)

    private InWorldFilter(Set<String> worldNamesLower) {
        this.worldNamesLower = worldNamesLower;
    }

    @Override
    public boolean test(WorldContext ctx) {
        World w = ctx.world();
        if (w == null) return false;
        if (worldNamesLower.isEmpty()) return false;
        return worldNamesLower.contains(w.getName().toLowerCase());
    }

    public static InWorldFilter fromConfig(Map<String, Object> cfg) {
        if (cfg == null) return null;

        Set<String> names = new HashSet<>();

        Object worlds = cfg.get("worlds");
        if (worlds instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                String s = String.valueOf(o).trim();
                if (!s.isEmpty()) names.add(s.toLowerCase());
            }
        }

        // Abwärtskompatibel: world: “world” -> single world
        Object single = cfg.get("world");
        if ((worlds == null || names.isEmpty()) && single != null) {
            String s = String.valueOf(single).trim();
            if (!s.isEmpty()) names.add(s.toLowerCase());
        }

        if (names.isEmpty()) {
            NexusPlugin.nexusLogger.warning("in-world: Keine gültigen Einträge gefunden (keys: 'worlds' oder 'world'). Filter wird ignoriert.");
            return null;
        }

        return new InWorldFilter(names);
    }
}

