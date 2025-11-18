package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class ConditionAboveBlock implements NexusCondition {
    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data) {
        NexusPlugin.nexusLogger.error(List.of(
                "Condition above_block need a target location to work"
        ));
        return false;
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data, Location targetLocation) {

        String notBlock = (String) data.getData().getOrDefault("not-block", "not_found");
        int offset = (int) data.getData().getOrDefault("offset", 1);

        if(!data.validate(data.getData().get("target-block"), String.class)) {
            NexusPlugin.nexusLogger.error("Invalid target block data.");
            NexusPlugin.nexusLogger.error("Missing 'target-block' parameter for condition above_block");
            return false;
        }

        String targetBlock = (String) data.getData().getOrDefault("target-block", "air");
        Material targetMaterial = Material.valueOf(targetBlock.toUpperCase());

        if(!targetMaterial.isBlock()) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Invalid block data in condition above_blocks",
                    "The 'target-block' material must be a valid block!"
            ));
            return false;
        }

        if(targetLocation.getBlock().getType() != targetMaterial)  {
            return false;
        }

        if(!notBlock.equalsIgnoreCase("not_found")) {
            Material notMaterial = Material.valueOf(data.getData().get("not-block").toString().toUpperCase());
            if(!notMaterial.isBlock()) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid block data in condition above_blocks",
                        "The 'not-block' material must be a valid block!"
                ));
                return false;
            }

            Location aboveBlockLocation = targetLocation.clone().add(0, offset, 0);
            return aboveBlockLocation.getBlock().getType() != notMaterial;
        } else {
            Material material = Material.valueOf(data.getData().get("block").toString().toUpperCase());
            if(!material.isBlock()) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid block data in condition above_blocks",
                        "The 'block' must be a valid block!"
                ));
                return false;
            }

            Location aboveBlockLocation = targetLocation.clone().add(0, offset, 0);
            return aboveBlockLocation.getBlock().getType() == material;
        }
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data, Location targetLocation, Map<String, Object> params) {
        return false;
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
