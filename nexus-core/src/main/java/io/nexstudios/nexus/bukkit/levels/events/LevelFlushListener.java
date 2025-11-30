package io.nexstudios.nexus.bukkit.levels.events;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.levels.LevelService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LevelFlushListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        LevelService svc = NexusPlugin.getInstance().getLevelService();
        if (svc != null) svc.safeToDatabase(e.getPlayer().getUniqueId());
    }

}
