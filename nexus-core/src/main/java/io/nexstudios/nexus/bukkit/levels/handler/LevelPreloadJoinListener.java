package io.nexstudios.nexus.bukkit.levels.handler;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.levels.LevelService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class LevelPreloadJoinListener implements Listener {

    private final LevelService levelService;

    public LevelPreloadJoinListener(NexusPlugin plugin) {
        this.levelService = plugin.getLevelService();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (levelService == null) {
            return;
        }
        levelService.preloadAllForPlayer(event.getPlayer().getUniqueId());
    }
}