package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class ConditionInRegion implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data) {

        List<String> regionsConfig = (List<String>) data.getData().getOrDefault("regions", List.of());
        String regionConfig = (String) data.getData().getOrDefault("region", "no_region_found");

        if (regionsConfig.isEmpty()) {
            return NexusPlugin.getInstance()
                    .getWorldGuardHook()
                    .isInRegion(player.getLocation(), regionConfig);
        }

        for (String region : regionsConfig) {
            if (NexusPlugin.getInstance().getWorldGuardHook().isInRegion(player.getLocation(), region)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean checkCondition(Player player, ConditionData data, Location targetLocation) {

        List<String> regionsConfig = (List<String>) data.getData().getOrDefault("regions", List.of());
        String regionConfig = (String) data.getData().getOrDefault("region", "no_region_found");

        if (targetLocation == null) {
            return false;
        }

        // Keine Region angegeben → irgendeine WG‑Region am Zielort reicht
        if (regionsConfig.isEmpty()) {
            return NexusPlugin.getInstance()
                    .getWorldGuardHook()
                    .isInRegion(targetLocation, regionConfig);
        }

        // Mindestens eine der angegebenen Regionen muss am Zielort zutreffen
        for (String region : regionsConfig) {
            if (NexusPlugin.getInstance().getWorldGuardHook().isInRegion(targetLocation, region)) {
                return true;
            }
        }

        return false;
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
