package io.nexstudios.nexus.bukkit.placeholder;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Provider for a specific namespace. One provider per namespace.
 *
 * Responsibilities:
 * - Resolve a key (with or without Player context).
 * - Optionally define per-key fallback strings when the key is not known or cannot be resolved.
 * - Optionally influence caching by marking values as non-cacheable or by providing ttl overrides.
 * - Optionally control PlaceholderAPI availability for the entire provider.
 *
 * Contract:
 * - Namespace and keys are lower-case.
 * - Keys may contain multiple ':' segments (everything after the first ':' is the key).
 * - Return null from resolve* to indicate "unknown/not-resolved".
 * - Fallbacks are used only when resolve* returns null.
 * - All methods must be thread-safe.
 *
 * Logging and comments must be in English.
 */
public interface NexPlaceholderProvider {

    /**
     * Resolve a key without player context.
     * Return null if this provider does not know the key or cannot resolve it.
     */
    @Nullable PlaceholderValue resolve(String key);

    /**
     * Resolve a key with player context.
     * Return null if this provider does not know the key or cannot resolve it.
     */
    @Nullable PlaceholderValue resolve(Player player, String key);

    /**
     * Optional per-key fallback string when a key cannot be resolved.
     * Return null if no fallback is defined for this key.
     *
     * Note:
     * - You may use the player context for personalized fallbacks.
     */
    default @Nullable String fallback(@Nullable Player player, String key) {
        return null;
    }

    /**
     * Optional hint: whether a given key is cacheable.
     * Called when no ttl override is provided by the returned PlaceholderValue.
     * Default true.
     */
    default boolean isCacheable(String key) {
        return true;
    }

    /**
     * Optional per-key TTL override in milliseconds.
     * Return null to use the namespace default TTL configured during registration.
     */
    default @Nullable Long ttlMillis(String key) {
        return null;
    }

    /**
     * Optional: Control whether this provider should be available through PlaceholderAPI.
     *
     * If false, ALL keys in this provider will only work with NexusPlaceholder syntax (#namespace:key#)
     * and will NOT be available through PlaceholderAPI (%namespace_key%).
     *
     * Default: true (provider is available in both systems)
     *
     * @return true if this provider should be exposed to PlaceholderAPI, false otherwise
     */
    default boolean papiSupport() {
        return true;
    }

    /**
     * Optional: If the provider can produce a pure component value for a key without Player context.
     * Default: return null and string value will be used (if any).
     */
    default @Nullable Component componentOnly(String key) {
        return null;
    }

    /**
     * Optional: If the provider can produce a pure component value for a key with Player context.
     * Default: return null and string value will be used (if any).
     */
    default @Nullable Component componentOnly(Player player, String key) {
        return null;
    }
}