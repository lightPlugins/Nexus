package io.nexstudios.nexus.bukkit.conditions.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.ConditionData;
import io.nexstudios.nexus.bukkit.conditions.NexusCondition;
import io.nexstudios.nexus.bukkit.conditions.NexusConditionContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConditionInRegion implements NexusCondition {

    @Override
    public JavaPlugin getPlugin() {
        return NexusPlugin.getInstance();
    }

    /**
     * Synchrone Prüfung:
     * - verwendet bevorzugt die targetLocation aus dem Kontext
     * - wenn keine targetLocation gesetzt ist, wird die Spieler-Position genutzt (falls online)
     * - Regions-Konfiguration:
     *   - "regions": List<String> → eine der Regionen muss matchen
     *   - "region": String → einzelne Region
     */
    @Override
    public boolean checkSync(NexusConditionContext context) {
        ConditionData data = context.data();

        // Zielposition: erst explizite Location, sonst Spielerposition
        Location location = context.location();
        if (location == null) {
            Player p = context.player();
            if (p != null) {
                location = p.getLocation();
            }
        }

        if (location == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<String> regionsConfig = (List<String>) data.getData().getOrDefault("regions", List.of());
        String regionConfig = (String) data.getData().getOrDefault("region", "no_region_found");

        // Keine Regionen-Liste → einzelne Region / "irgendeine Region" am Ort
        if (regionsConfig.isEmpty()) {
            return NexusPlugin.getInstance()
                    .getWorldGuardHook()
                    .isInRegion(location, regionConfig);
        }

        // Mindestens eine der angegebenen Regionen muss zutreffen
        for (String region : regionsConfig) {
            if (NexusPlugin.getInstance().getWorldGuardHook().isInRegion(location, region)) {
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
            // kein Online-Player vorhanden
            return;
        }

        if (asActionBar) {
            player.sendActionBar(Component.text("Condition not met"));
        } else {
            NexusPlugin.getInstance().getMessageSender().send(player, "general.condition-not-met");
        }
    }
}