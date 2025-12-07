package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConditionInWorld implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    /**
     * Synchrone Prüfung:
     * - nutzt bevorzugt context.location(), sonst Spieler-Position (falls online)
     * - Config:
     *   - "world": String (einzelne Welt, abwärtskompatibel)
     *   - "worlds": List<String> (eine der Welten muss matchen)
     */
    @Override
    public boolean checkSync(NexusConditionContext context) {
        ConditionData data = context.data();

        // Zielposition: zuerst explizite Location, sonst Spielerlocation
        Location location = context.location();
        if (location == null) {
            Player p = context.player();
            if (p != null) {
                location = p.getLocation();
            }
        }

        if (location == null) {
            NexusPlugin.nexusLogger.error("Condition 'in-world' could not determine a target location.");
            return false;
        }

        List<String> worldsList = new ArrayList<>();

        // Liste von Welten
        Object worldsObj = data.getData().get("worlds");
        if (worldsObj instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                String s = String.valueOf(o).trim();
                if (!s.isEmpty()) worldsList.add(s.toLowerCase());
            }
        }

        // Abwärtskompatibel: einzelnes "world" Feld
        Object singleWorldObj = data.getData().get("world");
        if (worldsList.isEmpty() && singleWorldObj instanceof String sw && !sw.isBlank()) {
            worldsList.add(sw.toLowerCase());
        }

        if (worldsList.isEmpty()) {
            NexusPlugin.nexusLogger.error("Invalid target world data for condition 'in-world'. Expected 'world' or 'worlds'.");
            return false;
        }

        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        String currentNameLower = world.getName().toLowerCase();
        return worldsList.contains(currentNameLower);
    }

    @Override
    public void sendMessage(NexusConditionContext context) {
        ConditionData data = context.data();

        boolean sendMessage = (boolean) data.getData().getOrDefault("send-message", true);
        boolean asActionBar = (boolean) data.getData().getOrDefault("as-actionbar", false);

        if (!sendMessage) return;

        Player player = context.player();
        if (player == null) {
            // kein Online-Player zum Benachrichtigen vorhanden
            return;
        }

        if (asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}