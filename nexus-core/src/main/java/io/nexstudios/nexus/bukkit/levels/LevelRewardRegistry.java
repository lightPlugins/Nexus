package io.nexstudios.nexus.bukkit.levels;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class LevelRewardRegistry {

    private static final Map<String, LevelRewardConfig> REGISTRY = new ConcurrentHashMap<>();

    private LevelRewardRegistry() {
    }

    private static String key(String namespace, String key) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(key, "key");
        return namespace.toLowerCase() + ":" + key.toLowerCase();
    }

    public static void register(String namespace, String key, LevelRewardConfig config) {
        Objects.requireNonNull(config, "config");
        String idx = key(namespace, key);
        REGISTRY.put(idx, config);
    }

    public static LevelRewardConfig registerFromSection(String namespace,
                                                        String key,
                                                        ConfigurationSection root) {
        LevelRewardConfig cfg = LevelRewardConfig.fromStandardSection(root);
        register(namespace, key, cfg);
        return cfg;
    }

    public static LevelRewardConfig registerFromSection(String namespace,
                                                        String key,
                                                        ConfigurationSection root,
                                                        String levelsPath,
                                                        String actionsPath) {
        LevelRewardConfig cfg = LevelRewardConfig.fromSection(root, levelsPath, actionsPath);
        register(namespace, key, cfg);
        return cfg;
    }

    public static LevelRewardConfig get(String namespace, String key) {
        return REGISTRY.get(key(namespace, key));
    }

    public static boolean isRegistered(String namespace, String key) {
        return get(namespace, key) != null;
    }
}