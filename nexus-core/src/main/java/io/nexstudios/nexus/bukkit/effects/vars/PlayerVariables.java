package io.nexstudios.nexus.bukkit.effects.vars;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The PlayerVariables class provides a thread-safe mechanism for managing
 * key-value pair storage associated with individual players, identified by their UUIDs.
 * Each player has an independent storage space for their variables, with additional support
 * for a versioning system to track updates.
 *
 * This utility class includes methods for setting, retrieving, and snapshotting variables,
 * as well as checking the version of a player's stored variables. It also ensures safe
 * concurrent access for managing player-specific data.
 *
 * This class is designed to work in scenarios where dynamic, player-specific
 * data needs to be managed, and is particularly useful in games or
 * systems requiring real-time variable updates and retrievals.
 */
public final class PlayerVariables {
    private static final Map<UUID, Map<String, String>> STORE = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicLong> VERSION = new ConcurrentHashMap<>();

    private PlayerVariables() {}

    /**
     * Sets a key-value pair for the specified player's variable storage.
     * If the player's storage does not exist, it will be initialized.
     * Simultaneously increments the version counter for the player's variable storage.
     *
     * @param playerId the unique identifier of the player
     * @param key the key under which the value will be stored
     * @param value the value to be stored associated with the provided key
     */
    public static void set(UUID playerId, String key, String value) {
        STORE.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>()).put(key, value);
        VERSION.computeIfAbsent(playerId, id -> new AtomicLong()).incrementAndGet();
    }

    /**
     * Retrieves the value associated with the specified key for the given player's variable storage.
     * If the player's storage does not exist or the key is not found, the default value is returned.
     *
     * @param playerId the unique identifier of the player
     * @param key the key whose associated value is to be returned
     * @param def the default value to return if the key is not found
     * @return the value associated with the specified key, or the default value if the key does not exist
     */
    public static String get(UUID playerId, String key, String def) {
        Map<String, String> m = STORE.get(playerId);
        if (m == null) return def;
        return m.getOrDefault(key, def);
    }

    /**
     * Returns an unmodifiable snapshot of the variable storage for the specified player.
     * If no variables are stored for the player, an empty map is returned.
     *
     * @param playerId the unique identifier of the player
     * @return an unmodifiable map containing the player's variable storage, or an empty map if no variables exist
     */
    public static Map<String, String> snapshot(UUID playerId) {
        Map<String, String> m = STORE.get(playerId);
        return m == null ? Map.of() : Map.copyOf(m);
    }

    /**
     * Retrieves the current version number of the variable storage for the specified player.
     * If the player's storage does not exist, the version is considered to be 0.
     *
     * @param playerId the unique identifier of the player
     * @return the current version number of the player's variable storage
     */
    public static long version(UUID playerId) {
        AtomicLong v = VERSION.get(playerId);
        return v == null ? 0L : v.get();
    }
}


