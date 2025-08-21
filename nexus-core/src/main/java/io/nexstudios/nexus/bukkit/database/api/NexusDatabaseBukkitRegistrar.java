package io.nexstudios.nexus.bukkit.database.api;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

public final class NexusDatabaseBukkitRegistrar {
    private NexusDatabaseBukkitRegistrar() {}

    public static void register(Plugin owner, NexusDatabaseService service) {
        if (owner == null || service == null) return;
        ServicesManager sm = owner.getServer().getServicesManager();
        sm.register(NexusDatabaseService.class, service, owner, ServicePriority.Normal);
    }

    public static void unregister(Plugin owner, NexusDatabaseService service) {
        if (owner == null || service == null) return;
        ServicesManager sm = owner.getServer().getServicesManager();
        sm.unregister(NexusDatabaseService.class, service);
    }
}