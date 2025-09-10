package io.nexstudios.nexus.bukkit.placeholder;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for placeholder providers.
 *
 * Features:
 * - One provider per namespace (namespace == plugin name or custom).
 * - Auto-unregister on PluginDisableEvent.
 * - Per-namespace cache policy with default TTL and per-key override.
 * - Fine-grained cache invalidation (namespace+key, optionally per player).
 *
 * Notes:
 * - All logs are deliberately quiet (no spam). Only critical issues are logged once.
 * - Comments are in English.
 */
public final class NexusPlaceholderRegistry {

    private NexusPlaceholderRegistry() {}

    /**
     * @param nonCacheableKeys exact keys (lowercase)
     * @param perKeyTtlMillis  exact keys -> ttl (lowercase)
     */
    public record CachePolicy(long defaultTtlMillis, Set<String> nonCacheableKeys, Map<String, Long> perKeyTtlMillis) {
            public CachePolicy(long defaultTtlMillis,
                               Set<String> nonCacheableKeys,
                               Map<String, Long> perKeyTtlMillis) {
                this.defaultTtlMillis = Math.max(0L, defaultTtlMillis);
                this.nonCacheableKeys = Objects.requireNonNullElse(nonCacheableKeys, Set.of());
                this.perKeyTtlMillis = Objects.requireNonNullElse(perKeyTtlMillis, Map.of());
            }

            public boolean isNonCacheable(String key) {
                return nonCacheableKeys.contains(key);
            }

            public @Nullable Long perKeyTtlMillis(String key) {
                return perKeyTtlMillis.get(key);
            }

            public static CachePolicy of(long defaultTtlMillis) {
                return new CachePolicy(defaultTtlMillis, Set.of(), Map.of());
            }
        }

    public record Registration(Plugin owner, String namespace, PlaceholderProvider provider, CachePolicy cachePolicy) { }

    private static final Map<String, Registration> PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<CacheKey, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private static volatile boolean listenerRegistered = false;

    private record CacheKey(String namespace, String key, @Nullable UUID playerId) {}

    private static final class CacheEntry {
        final @Nullable String stringValue;
        final @Nullable Component componentValue;
        volatile long expiresAtMillis;

        CacheEntry(@Nullable String stringValue, @Nullable Component componentValue, long expiresAtMillis) {
            this.stringValue = stringValue;
            this.componentValue = componentValue;
            this.expiresAtMillis = expiresAtMillis;
        }

        boolean isExpired(long now) {
            return expiresAtMillis < now;
        }
    }

    public static boolean register(Plugin owner,
                                   String namespace,
                                   PlaceholderProvider provider,
                                   CachePolicy cachePolicy) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(cachePolicy, "cachePolicy");

        ensureDisableListenerInstalled();

        String ns = namespace.toLowerCase(Locale.ROOT).trim();
        Registration reg = new Registration(owner, ns, provider, cachePolicy);
        Registration existing = PROVIDERS.putIfAbsent(ns, reg);
        if (existing != null) {
            // Intentionally quiet: allow caller to decide how to proceed.
            return false;
        }
        return true;
    }

    public static boolean unregister(String namespace) {
        if (namespace == null) return false;
        Registration removed = PROVIDERS.remove(namespace.toLowerCase(Locale.ROOT).trim());
        if (removed != null) {
            // remove all cache entries for that namespace
            CACHE.keySet().removeIf(k -> k.namespace.equals(removed.namespace));
            return true;
        }
        return false;
    }

    public static void unregisterFor(Plugin owner) {
        if (owner == null) return;
        String pluginName = owner.getName().toLowerCase(Locale.ROOT);
        PROVIDERS.entrySet().removeIf(e -> {
            boolean remove = e.getValue().owner == owner || e.getKey().equals(pluginName);
            if (remove) {
                CACHE.keySet().removeIf(k -> k.namespace.equals(e.getKey()));
            }
            return remove;
        });
    }

    private static void ensureDisableListenerInstalled() {
        if (listenerRegistered) return;
        synchronized (NexusPlaceholderRegistry.class) {
            if (listenerRegistered) return;
            // Self-register a lightweight listener to auto-unregister providers on plugin disable.
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onPluginDisable(PluginDisableEvent event) {
                    unregisterFor(event.getPlugin());
                }
            }, getOwningPluginSafe());
            listenerRegistered = true;
        }
    }

    /**
     * Attempts to find an owning plugin to attach the listener to.
     * As we are inside Nexus, we try to use the first enabled plugin with our package present.
     * Fallback to the first enabled plugin if necessary.
     */
    private static Plugin getOwningPluginSafe() {
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (p.isEnabled() && p.getName().toLowerCase(Locale.ROOT).contains("nexus")) {
                return p;
            }
        }
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (p.isEnabled()) return p;
        }
        throw new IllegalStateException("No enabled plugin found to register events for NexusPlaceholderRegistry.");
    }

    static Optional<Registration> getRegistration(String namespace) {
        if (namespace == null) return Optional.empty();
        return Optional.ofNullable(PROVIDERS.get(namespace.toLowerCase(Locale.ROOT).trim()));
    }

    // ---- Cache helpers ----

    static @Nullable PlaceholderValue getCached(String namespace, String key, @Nullable UUID playerId, long nowMillis) {
        CacheEntry e = CACHE.get(new CacheKey(namespace, key, playerId));
        if (e == null) return null;
        if (e.isExpired(nowMillis)) {
            CACHE.remove(new CacheKey(namespace, key, playerId));
            return null;
        }
        if (e.stringValue == null && e.componentValue == null) return null;
        if (e.stringValue != null && e.componentValue != null) {
            return PlaceholderValue.of(e.stringValue, e.componentValue);
        } else if (e.stringValue != null) {
            return PlaceholderValue.ofString(e.stringValue);
        } else {
            return PlaceholderValue.ofComponent(e.componentValue);
        }
    }

    static void putCached(String namespace, String key, @Nullable UUID playerId,
                          @Nullable String stringValue, @Nullable Component componentValue,
                          long ttlMillis, long nowMillis) {
        long exp = nowMillis + Math.max(0, ttlMillis);
        CACHE.put(new CacheKey(namespace, key, playerId), new CacheEntry(stringValue, componentValue, exp));
    }

    public static void invalidate(String namespace, String key) {
        if (namespace == null || key == null) return;
        String ns = namespace.toLowerCase(Locale.ROOT).trim();
        String k = key.toLowerCase(Locale.ROOT).trim();
        CACHE.keySet().removeIf(ck -> ck.namespace.equals(ns) && ck.key.equals(k));
    }

    public static void invalidate(String namespace, String key, UUID playerId) {
        if (namespace == null || key == null || playerId == null) return;
        String ns = namespace.toLowerCase(Locale.ROOT).trim();
        String k = key.toLowerCase(Locale.ROOT).trim();
        CACHE.remove(new CacheKey(ns, k, playerId));
    }

    /**
     * Returns how many namespaces (providers) are currently registered.
     */
    public static int countNamespaces() {
        return PROVIDERS.size();
    }

    /**
     * Returns how many keys are advertised for the given namespace.
     * If the provider does not implement PlaceholderIntrospector, returns 0.
     */
    public static int countKeys(String namespace) {
        if (namespace == null) return 0;
        var reg = PROVIDERS.get(namespace.toLowerCase(Locale.ROOT).trim());
        if (reg == null) return 0;
        if (reg.provider() instanceof PlaceholderIntrospector pi) {
            var keys = pi.advertisedKeys();
            return keys == null ? 0 : keys.size();
        }
        return 0;
    }

    /**
     * Returns the sum of all advertised keys across all registered namespaces.
     * Providers that do not implement PlaceholderIntrospector contribute 0.
     */
    public static int countAllKeys() {
        int total = 0;
        for (var reg : PROVIDERS.values()) {
            if (reg.provider() instanceof PlaceholderIntrospector pi) {
                var keys = pi.advertisedKeys();
                if (keys != null) total += keys.size();
            }
        }
        return total;
    }


}