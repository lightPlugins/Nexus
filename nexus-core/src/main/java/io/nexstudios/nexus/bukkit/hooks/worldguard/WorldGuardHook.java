package io.nexstudios.nexus.bukkit.hooks.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.hooks.worldguard.flags.WoodStripFlag;
import io.nexstudios.nexus.bukkit.hooks.worldguard.listener.WoodStripListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Listener;

public class WorldGuardHook implements Listener {

    public WorldGuardHook() {

        // eigene Flags registrieren
        WoodStripFlag.registerFlags();


        // ... existing code ...
    }

    public void registerEvents() {
        // Listener für Wood-Stripping registrieren
        Bukkit.getPluginManager().registerEvents(
                new WoodStripListener(WorldGuard.getInstance()),
                NexusPlugin.getInstance()
        );

    }

    public boolean isInRegion(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        ApplicableRegionSet set = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
        return !set.getRegions().isEmpty();
    }

    public boolean isInRegion(Location location, String region) {
        if (location == null || region == null) {
            return false;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        ApplicableRegionSet set = container.createQuery()
                .getApplicableRegions(BukkitAdapter.adapt(location));

        // Prüfen, ob eine der Regionen die gewünschte ID hat
        return set.getRegions().stream()
                .anyMatch(r -> r.getId().equalsIgnoreCase(region));
    }


}
