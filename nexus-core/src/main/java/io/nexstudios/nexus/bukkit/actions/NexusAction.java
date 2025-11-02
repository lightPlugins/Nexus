package io.nexstudios.nexus.bukkit.actions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public interface NexusAction {
    JavaPlugin getPlugin();
    void execute(Player player, ActionData data, Location targetLocation);
    void execute(Player player, ActionData data, Location targetLocation, Map<String, Object> params);

}
