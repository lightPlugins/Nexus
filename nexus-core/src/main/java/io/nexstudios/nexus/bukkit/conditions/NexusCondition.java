package io.nexstudios.nexus.bukkit.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public interface NexusCondition {
    JavaPlugin getPlugin();
    boolean checkCondition(Player player, ConditionData data);
    boolean checkCondition(Player player, ConditionData data, Location targetLocation);
    boolean checkCondition(Player player, ConditionData data, Location targetLocation, Map<String, Object> params);
    void sendMessage(Player player, ConditionData data);

}
