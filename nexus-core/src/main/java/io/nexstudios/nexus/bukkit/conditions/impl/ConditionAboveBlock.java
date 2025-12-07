package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConditionAboveBlock implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    /**
     * Synchrone Prüfung:
     * - nutzt nur targetLocation + ConditionData
     * - Player/OfflinePlayer sind hier egal
     */
    @Override
    public boolean checkSync(NexusConditionContext context) {
        ConditionData data = context.data();
        Location targetLocation = context.location();

        if (targetLocation == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Condition 'above-block' needs a target location to work"
            ));
            return false;
        }

        String notBlock = String.valueOf(data.getData().getOrDefault("not-block", "not_found"));

        Object offsetObj = data.getData().getOrDefault("offset", 1);
        int offset;
        if (offsetObj instanceof Number n) {
            offset = n.intValue();
        } else {
            try {
                offset = Integer.parseInt(String.valueOf(offsetObj));
            } catch (NumberFormatException e) {
                offset = 1;
            }
        }

        if (!data.validate(data.getData().get("target-block"), String.class)) {
            NexusPlugin.nexusLogger.error("Invalid target block data.");
            NexusPlugin.nexusLogger.error("Missing 'target-block' parameter for condition above-block");
            return false;
        }

        String targetBlock = (String) data.getData().getOrDefault("target-block", "air");
        Material targetMaterial;
        try {
            targetMaterial = Material.valueOf(targetBlock.toUpperCase());
        } catch (IllegalArgumentException ex) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Invalid material in condition above-block: '" + targetBlock + "'",
                    "The 'target-block' material must be a valid block!"
            ));
            return false;
        }

        if (!targetMaterial.isBlock()) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Invalid block data in condition above-block",
                    "The 'target-block' material must be a valid block!"
            ));
            return false;
        }

        if (targetLocation.getBlock().getType() != targetMaterial) {
            return false;
        }

        Location aboveBlockLocation = targetLocation.clone().add(0, offset, 0);

        if (!notBlock.equalsIgnoreCase("not_found")) {
            // Variante: NOT-BLOCK darf NICHT über dem Ziel stehen
            Material notMaterial;
            try {
                notMaterial = Material.valueOf(notBlock.toUpperCase());
            } catch (IllegalArgumentException ex) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid block data in condition above-block",
                        "The 'not-block' material must be a valid block!"
                ));
                return false;
            }

            if (!notMaterial.isBlock()) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid block data in condition above-block",
                        "The 'not-block' material must be a valid block!"
                ));
                return false;
            }

            return aboveBlockLocation.getBlock().getType() != notMaterial;
        } else {
            // Variante: ein bestimmter BLOCK MUSS über dem Ziel stehen
            Object blockObj = data.getData().get("block");
            if (!(blockObj instanceof String blockStr)) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid block data in condition above-block",
                        "Missing or non-string 'block' parameter while 'not-block' is not set"
                ));
                return false;
            }

            Material material;
            try {
                material = Material.valueOf(blockStr.toUpperCase());
            } catch (IllegalArgumentException ex) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid block data in condition above-block",
                        "The 'block' must be a valid block!"
                ));
                return false;
            }

            if (!material.isBlock()) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid block data in condition above-block",
                        "The 'block' must be a valid block!"
                ));
                return false;
            }

            return aboveBlockLocation.getBlock().getType() == material;
        }
    }

    @Override
    public void sendMessage(NexusConditionContext context) {
        ConditionData data = context.data();

        boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);
        boolean asActionBar = (boolean) data.getData().getOrDefault("as-actionbar", false);

        if (!sendMessage) return;

        Player player = context.player();
        if (player == null) {
            // Subjekt ist offline / kein Online-Player vorhanden, niemand zum Ansprechen
            return;
        }

        if (asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}