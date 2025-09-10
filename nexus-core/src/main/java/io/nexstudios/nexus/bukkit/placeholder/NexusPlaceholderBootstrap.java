package io.nexstudios.nexus.bukkit.placeholder;

import io.nexstudios.nexus.bukkit.placeholder.internal.NexusPlayerNamePlaceholder;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Convenience bootstrap to register example "nexus" placeholders.
 * Call this from your plugin's onEnable().
 *
 * Logging and comments are in English.
 */
public final class NexusPlaceholderBootstrap {

    private NexusPlaceholderBootstrap() {}

    public static void registerNexusPlaceholders(Plugin nexusPlugin) {
        // Default TTL configurable at registration time; e.g. 1 second as requested.
        long defaultTtlMillis = Duration.ofSeconds(1).toMillis();

        // Example: no non-cacheable keys and no per-key overrides for now.
        NexusPlaceholderRegistry.CachePolicy policy = new NexusPlaceholderRegistry.CachePolicy(
                defaultTtlMillis,
                Set.of(/* non-cacheable keys here, lower-case e.g. "somekey" */),
                Map.of(/* per-key ttl overrides, e.g. "playername", 500L */)
        );

        NexusPlaceholderRegistry.register(
                nexusPlugin,
                "nexus",
                new NexusPlayerNamePlaceholder(),
                policy
        );
    }
}