package io.nexstudios.nexus.bukkit.hooks.worldguard.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.nexstudios.nexus.bukkit.hooks.worldguard.flags.WoodStripFlag;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class WoodStripListener implements Listener {

    private final RegionQuery regionQuery;

    public WoodStripListener(WorldGuard worldGuard) {
        this.regionQuery = worldGuard.getPlatform().getRegionContainer().createQuery();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onStripLog(EntityChangeBlockEvent event) {
        // Nur Spieler
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if(player.getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }

        Block block = event.getBlock();
        Material from = block.getType();
        Material to = event.getTo();

        // Optional: nur Logs
        if (!Tag.LOGS.isTagged(from)) {
            return;
        }

        // Strip-Vorgang erkennen: aus normalem Log wird ein Stripped-Log
        if (!from.name().startsWith("STRIPPED_") && to.name().startsWith("STRIPPED_")) {

            // WorldGuard-Abfrage an dieser Block-Position
            var weLoc = BukkitAdapter.adapt(block.getLocation());
            var localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

            Boolean flagValue = regionQuery.queryValue(
                    weLoc,
                    localPlayer,
                    WoodStripFlag.NEXUS_DISABLE_WOOD_STRIPPING
            );

            // Wenn Flag in dieser Region aktiv ist -> Stripping verhindern
            if (Boolean.TRUE.equals(flagValue)) {
                event.setCancelled(true);
            }
        }
    }
}