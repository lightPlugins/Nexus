package io.nexstudios.nexus.bukkit.platform;

public interface NmsServiceProvider {
    void registerServices(VersionedServiceRegistry registry) throws Exception;
}

