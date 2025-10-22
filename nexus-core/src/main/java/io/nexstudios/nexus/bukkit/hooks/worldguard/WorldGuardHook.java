package io.nexstudios.nexus.bukkit.hooks.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class WorldGuardHook implements Listener {


    @EventHandler
    public void onMove(PlayerMoveEvent e) {

        String targetRegionId = "time-night"; // deine Region-ID (kleinbuchstaben in WG)

        // Nur reagieren, wenn sich der Block ändert (Noise reduzieren)
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        Location from = e.getFrom();
        Location to   = e.getTo();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        ApplicableRegionSet fromSet = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(from));
        ApplicableRegionSet toSet   = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(to));

        // Wenn sich die Regionsmenge nicht ändert, nichts tun
        if (fromSet.getRegions().equals(toSet.getRegions())) return;

        boolean wasIn = fromSet.getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(targetRegionId));
        boolean isIn  = toSet.getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(targetRegionId));

        if (!wasIn && isIn) {
            // BETRETEN
            e.getPlayer().setPlayerTime(16000L, false);
        } else if (wasIn && !isIn) {
            // VERLASSEN
            e.getPlayer().setPlayerTime(4000L, false);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {

        String targetRegionId = "time-night";

        Location from = e.getFrom();
        Location to   = e.getTo();

        if (from.getWorld() == null || to.getWorld() == null) return;

        // Wenn der Spieler sich gar nicht in eine andere Position/Welt teleportiert, abbrechen
        if (from.getWorld().equals(to.getWorld())
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        ApplicableRegionSet fromSet = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(from));
        ApplicableRegionSet toSet   = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(to));

        if (fromSet.getRegions().equals(toSet.getRegions())) return; // keine Regionsänderung

        boolean wasIn = fromSet.getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(targetRegionId));
        boolean isIn  = toSet.getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(targetRegionId));

        if (!wasIn && isIn) {
            e.getPlayer().setPlayerTime(16000L, false);
        } else if (wasIn && !isIn) {
            e.getPlayer().setPlayerTime(4000L, false);
        }

    }


}
