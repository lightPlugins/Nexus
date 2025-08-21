package io.nexstudios.nexus.bukkit.effects.stats;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.effects.EffectFactory;
import io.nexstudios.nexus.bukkit.effects.NexusEffectsApi;
import io.nexstudios.nexus.bukkit.effects.load.EffectsLoader;
import io.nexstudios.nexus.bukkit.effects.stats.events.NexusStatRegisteredEvent;
import io.nexstudios.nexus.bukkit.effects.stats.events.PlayerStatLevelChangeEvent;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariableResolver;
import io.nexstudios.nexus.bukkit.effects.vars.PlayerVariables;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class StatsApi {

    private StatsApi() {}

    private static final String LEVEL_KEY_PREFIX = "stats:";
    private static final String LEVEL_KEY_SUFFIX = ":level";

    private static final Map<String, Set<String>> STATS_BY_NAMESPACE = new ConcurrentHashMap<>();


    public static List<NexusStat> registerStats(Plugin owner, List<File> statFiles) {
        Objects.requireNonNull(owner, "owner");
        if (statFiles == null || statFiles.isEmpty()) return List.of();

        String ns = owner.getName().toLowerCase(Locale.ROOT);
        Set<String> nsStats = STATS_BY_NAMESPACE.computeIfAbsent(ns, k -> ConcurrentHashMap.newKeySet());

        List<NexusStat> created = new ArrayList<>();
        for (File f : statFiles) {
            if (f == null || !f.exists() || !f.isFile()) continue;

            String statId = toStatId(f.getName());

            // Globale Eindeutigkeit erzwingen
            if (StatsRegistry.containsId(statId)) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Duplicate stat-id detected. Skipping registration.",
                        "Stat id: " + statId,
                        "Registered by: " + ns,
                        "File: " + f.getAbsolutePath()
                ));
                continue;
            }
            if (nsStats.contains(statId)) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Duplicate stat-id in namespace '" + ns + "'. Skipping registration.",
                        "Stat id: " + statId,
                        "Registered by: " + ns,
                        "File: " + f.getAbsolutePath()
                ));
                continue;
            }

            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);

            EffectFactory statFactory = buildFactoryForStat(statId);
            var loaded = EffectsLoader.load(cfg, statFactory);

            if (!loaded.isEmpty()) {
                NexusEffectsApi.addBindings(owner, loaded);
                nsStats.add(statId);

                NexusStat stat = new DefaultNexusStat(ns, statId, f);
                StatsRegistry.put(stat);
                created.add(stat);
                callSync(new NexusStatRegisteredEvent(stat));
            } else {
                NexusPlugin.nexusLogger.warning("No effects loaded for stat '" + statId + "' from file: " + f.getAbsolutePath());
            }
        }

        if (!created.isEmpty()) {
            NexusPlugin.nexusLogger.info("Registered " + created.size() + " stats for namespace '" + ns + "': " +
                    created.stream().map(NexusStat::id).toList());
        }
        return List.copyOf(created);
    }

    public static List<NexusStat> reloadStats(Plugin owner, List<File> statFiles) {
        Objects.requireNonNull(owner, "owner");
        String ns = owner.getName().toLowerCase(Locale.ROOT);

        NexusEffectsApi.removeExternalNamespace(owner);
        STATS_BY_NAMESPACE.remove(ns);
        StatsRegistry.removeNamespace(ns);

        return registerStats(owner, statFiles);
    }

    public static Set<String> getAllStatIds() {
        return STATS_BY_NAMESPACE.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Collection<NexusStat> getAllStats() {
        return StatsRegistry.all();
    }

    public static Collection<NexusStat> getStatsForNamespace(Plugin owner) {
        return owner == null ? List.of() : StatsRegistry.byNamespace(owner.getName());
    }

    // Über alle Namespaces suchen
    public static NexusStat findStat(String statId) {
        return StatsRegistry.getAny(statId);
    }

    public static List<NexusStat> findStatsById(String statId) {
        return StatsRegistry.getAllById(statId);
    }

    public static void unregisterStatsForNamespace(Plugin owner) {
        if (owner == null) return;
        String ns = owner.getName().toLowerCase(Locale.ROOT);
        STATS_BY_NAMESPACE.remove(ns);
        StatsRegistry.removeNamespace(ns);
    }

    public static void setPlayerStatLevel(UUID playerId, String statId, int level) {
        if (playerId == null || statId == null) return;
        // alten Wert lesen
        int old = getPlayerStatLevel(playerId, statId, Integer.MIN_VALUE);
        PlayerVariables.set(playerId, levelKey(statId), String.valueOf(level));
        // Event nur bei Änderung
        if (old != level) {
            NexusStat stat = findStat(statId);
            callSync(new PlayerStatLevelChangeEvent(playerId, stat, old == Integer.MIN_VALUE ? 0 : old, level));
        }
    }


    public static int getPlayerStatLevel(UUID playerId, String statId, int def) {
        if (playerId == null || statId == null) return def;
        String s = PlayerVariables.get(playerId, levelKey(statId), String.valueOf(def));
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    public static void setPlayerStatLevel(UUID playerId, NexusStat stat, int level) {
        if (playerId == null || stat == null) return;
        int old = getPlayerStatLevel(playerId, stat, Integer.MIN_VALUE);
        PlayerVariables.set(playerId, stat.keyLevel(), String.valueOf(level));
        if (old != level) {
            callSync(new PlayerStatLevelChangeEvent(playerId, stat, old == Integer.MIN_VALUE ? 0 : old, level));
        }
    }


    public static int getPlayerStatLevel(UUID playerId, NexusStat stat, int def) {
        if (playerId == null || stat == null) return def;
        String s = PlayerVariables.get(playerId, stat.keyLevel(), String.valueOf(def));
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private static EffectFactory buildFactoryForStat(String statId) {
        PlayerVariableResolver statLevelResolver = PlayerVariableResolver.ofConstant(
                "stat-level",
                (player, key) -> PlayerVariables.get(player.getUniqueId(), levelKey(statId), "0")
        );

        PlayerVariableResolver composite = PlayerVariableResolver.composite(
                PlayerVariableResolver.ofStore(),
                statLevelResolver
        );

        // WICHTIG: contextKey pro Stat an die Factory übergeben
        String contextKey = "stat:" + statId.toLowerCase(Locale.ROOT);
        return new EffectFactory(composite, contextKey);
    }


    private static String toStatId(String fileName) {
        if (fileName == null) return "unknown";
        String n = fileName.toLowerCase(Locale.ROOT);
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static String levelKey(String statId) {
        return LEVEL_KEY_PREFIX + statId.toLowerCase(Locale.ROOT).trim() + LEVEL_KEY_SUFFIX;
    }


    private static void callSync(org.bukkit.event.Event event) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event);
        } else {
            Bukkit.getScheduler().runTask(NexusPlugin.getInstance(), () -> Bukkit.getPluginManager().callEvent(event));
        }
    }

}





