package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ConditionPermission implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data) {
        if(!data.validate(data.getData().get("permission"), String.class)) {
            NexusPlugin.nexusLogger.error("Invalid permission data.");
            NexusPlugin.nexusLogger.error("Missing 'permission' parameter for action");
            return false;
        }

        String permission = (String) data.getData().getOrDefault("permission", "no.permission.found");
        return player.hasPermission(permission);

    }

    @Override
    public boolean checkCondition(Player player, ConditionData data, Location targetLocation) {
        return checkCondition(player, data);
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data, Location targetLocation, Map<String, Object> params) {
        return checkCondition(player, data);
    }

    @Override
    public void sendMessage(Player player, ConditionData data) {

        boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);
        boolean asActionBar = (boolean) data.getData().getOrDefault("as-actionbar", false);

        if(!sendMessage) return;
        if(asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}
