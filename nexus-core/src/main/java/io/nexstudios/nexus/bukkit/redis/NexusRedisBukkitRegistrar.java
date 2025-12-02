package io.nexstudios.nexus.bukkit.redis;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

/**
 * Helper class to register/unregister NexusRedisService
 * with Bukkit's ServicesManager.
 *
 * This mirrors the pattern used for NexusDatabaseService.
 */
public final class NexusRedisBukkitRegistrar {

    private NexusRedisBukkitRegistrar() {
    }

    /**
     * Registers the given NexusRedisService instance as a Bukkit service.
     *
     * @param owner   the owning plugin (usually NexusPlugin)
     * @param service the service implementation to expose
     */
    public static void register(Plugin owner, NexusRedisService service) {
        if (owner == null || service == null) return;
        ServicesManager sm = owner.getServer().getServicesManager();
        sm.register(NexusRedisService.class, service, owner, ServicePriority.Normal);
    }

    /**
     * Unregisters the given NexusRedisService from the Bukkit ServicesManager.
     *
     * @param owner   the owning plugin (same as used in register)
     * @param service the previously registered service instance
     */
    public static void unregister(Plugin owner, NexusRedisService service) {
        if (owner == null || service == null) return;
        ServicesManager sm = owner.getServer().getServicesManager();
        sm.unregister(NexusRedisService.class, service);
    }
}