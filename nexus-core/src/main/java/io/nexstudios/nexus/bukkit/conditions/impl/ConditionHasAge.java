package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConditionHasAge implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    /**
     * Synchrone Prüfung:
     * - nutzt nur targetLocation + ConditionData
     */
    @Override
    public boolean checkSync(NexusConditionContext context) {
        ConditionData data = context.data();
        Location targetLocation = context.location();

        if (targetLocation == null) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Could not check provided condition",
                    "Condition 'has_age' needs a target location to work"
            ));
            return false;
        }

        Object ageObj = data.getData().get("age");
        if (ageObj == null || !data.validate(ageObj, String.class)) {
            NexusPlugin.nexusLogger.error("Invalid target age data.");
            NexusPlugin.nexusLogger.error("Missing 'age' parameter for condition has-age");
            return false;
        }

        int requiredAge;
        if (ageObj instanceof Number n) {
            requiredAge = n.intValue();
        } else {
            try {
                requiredAge = Integer.parseInt(String.valueOf(ageObj));
            } catch (NumberFormatException e) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Invalid age value for condition has-age: " + ageObj,
                        "Expected an integer age value."
                ));
                return false;
            }
        }

        Block block = targetLocation.getBlock();
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Invalid target block data for condition has_age.",
                    "Block must have Ageable block data!"
            ));
            return false;
        }

        return ageable.getAge() == requiredAge;
    }

    @Override
    public void sendMessage(NexusConditionContext context) {
        ConditionData data = context.data();

        boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);
        boolean asActionBar = (boolean) data.getData().getOrDefault("as-actionbar", false);

        if (!sendMessage) return;

        Player player = context.player();
        if (player == null) {
            // Niemand online verfügbar, um die Nachricht zu empfangen
            return;
        }

        if (asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}