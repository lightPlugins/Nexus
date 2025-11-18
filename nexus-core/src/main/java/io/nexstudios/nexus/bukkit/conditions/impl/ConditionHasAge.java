package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class ConditionHasAge implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data) {
        NexusPlugin.nexusLogger.error(List.of(
                "Could not check provided condition",
                "Condition 'has_age' need a target location to work"
        ));
        return false;
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data, Location targetLocation) {

        if(!data.validate(data.getData().get("age"), String.class)) {
            NexusPlugin.nexusLogger.error("Invalid target age data.");
            NexusPlugin.nexusLogger.error("Missing 'age' parameter for condition has-age");
            return false;
        }

        int age = (int) data.getData().getOrDefault("age", 0);
        Block block = targetLocation.getBlock();

        if(!(block instanceof Ageable ageable)) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Invalid target block data for condition has_age.",
                    "Block must be an instance of Ageable!"
            ));
            return false;
        }

        return ageable.getAge() == age;
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data, Location targetLocation, Map<String, Object> params) {
        return checkCondition(player, data, targetLocation);
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
