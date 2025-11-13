package io.nexstudios.nexus.bukkit.placeholder;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.placeholder.internal.NexusPlayerNamePlaceholder;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
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
        boolean papiAvailable = NexusPlaceholderRegistry.enablePAPIIntegration();
        if (papiAvailable) {
            nexusPlugin.getLogger().info("PlaceholderAPI integration enabled for NexusPlaceholders");
        }

        // Default TTL configurable at registration time; e.g. 1 second as requested.
        long defaultTtlMillis = Duration.ofSeconds(1).toMillis();

        // Example: no non-cacheable keys and no per-key overrides for now.
        NexusPlaceholderRegistry.CachePolicy policy = new NexusPlaceholderRegistry.CachePolicy(
                defaultTtlMillis,
                Set.of(/* non-cacheable keys here, lower-case e.g. "somekey" */),
                Map.of(/* per-key ttl overrides, e.g. "playername", 500L */)
        );

        boolean registered = NexusPlaceholderRegistry.register(
                nexusPlugin,
                "nexus",
                new NexusPlayerNamePlaceholder(),
                policy
        );

        if (registered && papiAvailable) {
            NexusPlugin.nexusLogger.info("Enabled PAPI support for Nexus placeholders.");
        } else if (registered) {
            NexusPlugin.nexusLogger.info(List.of(
                    "PAPI not found. If you want to use nexus placeholders please use this Syntax",
                    "-> #nexus:placeholder#"
            ));
        }
    }
}