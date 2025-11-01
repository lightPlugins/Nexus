package io.nexstudios.nexus.bukkit.actions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public interface NexusAction {
    JavaPlugin getPlugin();
    void execute(Player player, ActionData data, Location targetLocation);

}
