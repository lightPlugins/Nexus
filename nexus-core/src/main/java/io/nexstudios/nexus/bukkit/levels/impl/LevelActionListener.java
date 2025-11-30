package io.nexstudios.nexus.bukkit.levels.impl;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.ActionFactory;
import io.nexstudios.nexus.bukkit.levels.LevelKey;
import io.nexstudios.nexus.bukkit.levels.LevelRewardConfig;
import io.nexstudios.nexus.bukkit.levels.LevelRewardRegistry;
import io.nexstudios.nexus.bukkit.levels.handler.NexLevelUpEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Zentraler Listener für ALLE Level-Typen, die über LevelRewardRegistry registriert wurden.
 *
 * Ablauf:
 * - Bei jedem NexLevelUpEvent wird geprüft, ob für (namespace, key) eine
 *   LevelRewardConfig in LevelRewardRegistry registriert ist.
 * - Falls ja, werden für alle neu erreichten Level die Actions über das
 *   Nexus-ActionSystem (ActionFactory) ausgeführt.
 *
 * Andere Plugins müssen nur:
 *  - LevelRewardRegistry.register(...) bzw. registerFromSection(...) aufrufen
 *  - den Level-Typ im LevelService registrieren.
 */
public final class LevelActionListener implements Listener {

    private final ActionFactory actionFactory;

    public LevelActionListener() {
        this.actionFactory = NexusPlugin.getInstance().getActionFactory();
    }

    @EventHandler
    public void onLevelUp(NexLevelUpEvent event) {
        LevelKey lk = event.getKey();

        String namespace = lk.getNamespace();
        String key = lk.getKey();

        // Gibt es für diesen Level-Typ überhaupt Rewards?
        LevelRewardConfig rewardConfig = LevelRewardRegistry.get(namespace, key);
        if (rewardConfig == null) {
            return;
        }

        UUID playerId = event.getPlayerId();
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            // Spieler offline -> aktuell keine Actions
            return;
        }

        int oldLevel = event.getOldLevel();
        int newLevel = event.getNewLevel();

        // Falls mehrere Level auf einmal erreicht wurden, alle Rewards dazwischen abfeuern
        for (int reached = oldLevel + 1; reached <= newLevel; reached++) {

            List<Map<String, Object>> actions = rewardConfig.actionsForLevel(reached);
            if (actions.isEmpty()) {
                continue;
            }

            Location loc = player.getLocation();

            try {
                actionFactory
                        .newExecution()
                        .actor(player)
                        .targetLocation(loc)
                        .actions(actions)
                        // ein paar Standard-Parameter bereitstellen
                        .params(Map.of(
                                "level", String.valueOf(reached),
                                "old_level", String.valueOf(oldLevel),
                                "new_level", String.valueOf(newLevel),
                                "namespace", namespace,
                                "key", key
                        ))
                        .execute();
            } catch (Exception ex) {
                NexusPlugin.nexusLogger.error("Failed to execute level-up actions for "
                        + namespace + ":" + key + " Level " + reached + " for player " + player.getName());
                ex.printStackTrace();
            }
        }
    }
}