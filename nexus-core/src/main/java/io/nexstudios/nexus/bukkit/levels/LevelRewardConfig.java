package io.nexstudios.nexus.bukkit.levels;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Repräsentiert die Level-Definition + die konfigurierten Level-Up-Actions
 * für einen bestimmten Level-Typ (z. B. "slayer").
 *
 * Erwartete Standard-YAML-Struktur (für fromStandardSection):
 *
 * levels:
 *   - 50
 *   - 100
 *   - 300
 *   - 500
 *   - 1000
 *   - 15000
 *
 * level-up-actions:
 *   - level: 1
 *     actions:
 *       - id: vault-add
 *         amount: '2000'
 *         multiplier: '1'
 *   - level: 2
 *     actions:
 *       - id: vault-add
 *         amount: '2000'
 *         multiplier: '1'
 *   # ...
 */
public final class LevelRewardConfig {

    private final List<Double> requiredXpPerLevel;
    private final Map<Integer, List<Map<String, Object>>> actionsPerLevel;

    public LevelRewardConfig(List<Double> requiredXpPerLevel,
                             Map<Integer, List<Map<String, Object>>> actionsPerLevel) {
        if (requiredXpPerLevel == null || requiredXpPerLevel.isEmpty()) {
            throw new IllegalArgumentException("requiredXpPerLevel darf nicht null oder leer sein");
        }
        this.requiredXpPerLevel = List.copyOf(requiredXpPerLevel);
        this.actionsPerLevel = (actionsPerLevel == null || actionsPerLevel.isEmpty())
                ? Map.of()
                : Map.copyOf(actionsPerLevel);
    }

    public List<Double> requiredXpPerLevel() {
        return requiredXpPerLevel;
    }

    public List<Map<String, Object>> actionsForLevel(int level) {
        List<Map<String, Object>> list = actionsPerLevel.get(level);
        return (list == null || list.isEmpty()) ? List.of() : list;
    }

    public static LevelRewardConfig fromConfig(FileConfiguration cfg,
                                               String levelsPath,
                                               String actionsPath) {
        if (cfg == null) {
            throw new IllegalArgumentException("cfg darf nicht null sein");
        }
        return fromSection(cfg, levelsPath, actionsPath);
    }

    public static LevelRewardConfig fromStandardSection(ConfigurationSection root) {
        return fromSection(root, "levels", "level-up-actions");
    }

    public static LevelRewardConfig fromSection(ConfigurationSection root,
                                                String levelsKey,
                                                String actionsKey) {
        if (root == null) {
            throw new IllegalArgumentException("root darf nicht null sein");
        }
        if (levelsKey == null || levelsKey.isBlank()) {
            throw new IllegalArgumentException("levelsKey darf nicht leer sein");
        }
        if (actionsKey == null || actionsKey.isBlank()) {
            throw new IllegalArgumentException("actionsKey darf nicht leer sein");
        }

        // 1) XP-Requirements laden
        List<Double> required = root.getDoubleList(levelsKey);
        if (required.isEmpty()) {
            throw new IllegalStateException("Keine Level-XP-Liste unter Pfad '" + levelsKey + "' gefunden.");
        }

        // 2) Level-Up-Actions laden
        Map<Integer, List<Map<String, Object>>> actionsPerLevel = new HashMap<>();

        List<Map<?, ?>> rawList = root.getMapList(actionsKey);
        if (!rawList.isEmpty()) {
            for (Map<?, ?> entry : rawList) {
                if (entry == null || entry.isEmpty()) continue;

                // level
                Object lvlObj = entry.get("level");
                if (lvlObj == null) {
                    continue;
                }

                int level = parseLevelNumber(lvlObj);
                if (level <= 0) {
                    continue;
                }

                // actions
                Object actionsObj = entry.get("actions");
                if (!(actionsObj instanceof List<?> list)) {
                    continue;
                }

                List<Map<String, Object>> castedActions = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typed = (Map<String, Object>) m;
                        castedActions.add(typed);
                    }
                }

                if (!castedActions.isEmpty()) {
                    actionsPerLevel.put(level, castedActions);
                }
            }
        }

        return new LevelRewardConfig(required, actionsPerLevel);
    }

    private static int parseLevelNumber(Object lvlObj) {
        if (lvlObj instanceof Number n) {
            return n.intValue();
        }
        if (lvlObj instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }
}