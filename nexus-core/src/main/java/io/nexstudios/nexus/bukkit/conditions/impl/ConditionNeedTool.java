package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import io.nexstudios.nexus.bukkit.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConditionNeedTool implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    @Override
    public boolean checkSync(NexusConditionContext context) {
        Player player = context.player();
        if (player == null) return false;

        ConditionData data = context.data();

        String slotStr = String.valueOf(data.getData().getOrDefault("equipment-slot", "mainhand")).toUpperCase();
        EquipmentSlot slot;
        try {
            slot = EquipmentSlot.valueOf(slotStr.replace("mainhand", "offhand"));
        } catch (IllegalArgumentException e) {
            slot = EquipmentSlot.HAND;
        }

        Object toolsObj = data.getData().get("tools");
        if (!(toolsObj instanceof List<?> toolList)) {
            return false;
        }

        ItemStack itemInSlot = player.getInventory().getItem(slot);
        Material currentMaterial = itemInSlot.getType();
        String currentCustomId = StringUtils.getCustomId(itemInSlot);

        for (Object obj : toolList) {
            String toolId = String.valueOf(obj);

            // 1. Check für Custom Items (ID Vergleich)
            if (currentCustomId != null && currentCustomId.equalsIgnoreCase(toolId)) {
                return true;
            }

            // 2. Check für Vanilla / Material
            ItemStack targetStack = StringUtils.parseItem(toolId);
            if (targetStack == null) continue;
            Material targetMaterial = targetStack.getType();

            // 3. Air -> Hand
            if (targetMaterial.isAir() && currentMaterial.isAir()) {
                return true;
            }

            if (currentMaterial == targetMaterial && currentCustomId == null) {
                return true;
            }
        }

        return false;
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