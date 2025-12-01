package io.nexstudios.nexus.bukkit.levels.handler;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.levels.NexLevel;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class LevelCheckOnJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        NexLevel levelSystem = NexLevel.getInstance();
        if (levelSystem == null) {
            NexusPlugin.nexusLogger.error("Level system is null, cannot apply missing level ups for player: " + event.getPlayer().getName());
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();

        // WICHTIG:
        // - berechnet aus totalXp das aktuelle theoretische Level
        // - schaut auf lastAppliedLevel
        // - feuert fehlende NexLevelUpEvents (inkl. Actions)
        // - aktualisiert lastAppliedLevel und speichert es
        Bukkit.getScheduler().runTaskLater(NexusPlugin.getInstance(), () -> levelSystem.applyMissingLevelUps(playerId), 20 * 2);
    }
}