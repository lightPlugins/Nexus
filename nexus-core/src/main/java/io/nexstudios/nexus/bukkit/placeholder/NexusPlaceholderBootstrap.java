package io.nexstudios.nexus.bukkit.placeholder;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.placeholder.internal.NexusCorePlaceholderProvider;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convenience bootstrap to register built-in Nexus placeholders.
 *
 * Call this from {@link io.nexstudios.nexus.bukkit.NexusPlugin#onEnable()}.
 *
 * Responsibilities:
 * <ul>
 *     <li>Enable PlaceholderAPI integration (if available)</li>
 *     <li>Register internal Nexus placeholder providers into {@link NexusPlaceholderRegistry}</li>
 * </ul>
 *
 * Currently registered namespaces:
 * <ul>
 *     <li><b>nexus</b> – {@link NexusCorePlaceholderProvider}</li>
 * </ul>
 */
public final class NexusPlaceholderBootstrap {

    private NexusPlaceholderBootstrap() { }

    /**
     * Registers all internal Nexus placeholder providers.
     *
     * @param nexusPlugin the Nexus {@link Plugin} instance
     */
    public static void registerNexusPlaceholders(Plugin nexusPlugin) {
        // Try to enable PlaceholderAPI integration once.
        boolean papiAvailable = NexusPlaceholderRegistry.enablePAPIIntegration();
        if (papiAvailable) {
            nexusPlugin.getLogger().info("PlaceholderAPI integration enabled for NexusPlaceholders");
        }

        // Default TTL: 1 second, can be tuned later (per key etc.).
        long defaultTtlMillis = Duration.ofSeconds(1).toMillis();

        NexusPlaceholderRegistry.CachePolicy corePolicy = new NexusPlaceholderRegistry.CachePolicy(
                defaultTtlMillis,
                Set.of(
                        // Example: mark keys as non-cacheable if they change very frequently.
                        // "level_*_*_current_level",
                        // "level_*_*_current_exp",
                        // "level_*_*_required_exp"
                ),
                Map.of(
                        // Example: override TTL for specific keys:
                        // "playername", 500L
                )
        );

        // Register "nexus" namespace with the combined core provider
        boolean registeredCore = NexusPlaceholderRegistry.register(
                nexusPlugin,
                "nexus",
                new NexusCorePlaceholderProvider(),
                corePolicy
        );

        if (registeredCore && papiAvailable) {
            NexusPlugin.nexusLogger.info("Enabled PAPI support for 'nexus' placeholders.");
        } else if (registeredCore) {
            NexusPlugin.nexusLogger.info(List.of(
                    "PAPI not found. If you want to use nexus placeholders please use this syntax:",
                    "-> #nexus:placeholder#"
            ));
        }

        // In the future, additional internal namespaces could be registered here, e.g.:
        // NexusPlaceholderRegistry.register(nexusPlugin, "nexus_items", new NexusItemsPlaceholderProvider(), somePolicy);
    }
}