package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class ConditionPermission implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public boolean checkSync(NexusConditionContext context) {
        ConditionData data = context.data();
        Object permObj = data.getData().get("permission");

        if (permObj == null || !data.validate(permObj, String.class)) {
            NexusPlugin.nexusLogger.error("Invalid permission data.");
            NexusPlugin.nexusLogger.error("Missing 'permission' parameter for condition");
            return false;
        }

        Player player = context.player();
        if (player == null) {
            // Subjekt ist nur OfflinePlayer (UUID), Bukkit-Permissions sind nur für Online-Player verfügbar
            return false;
        }

        String permission = (String) permObj;
        if (permission.isEmpty()) {
            permission = "no.permission.found";
        }

        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(NexusConditionContext context) {
        ConditionData data = context.data();

        boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);
        boolean asActionBar = (boolean) data.getData().getOrDefault("as-actionbar", false);

        if (!sendMessage) return;

        Player player = context.player();
        if (player == null) {
            // Niemand online, dem wir die Nachricht schicken können
            return;
        }

        if (asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}
