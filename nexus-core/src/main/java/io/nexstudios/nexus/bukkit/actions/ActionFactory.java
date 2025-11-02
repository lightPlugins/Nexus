package io.nexstudios.nexus.bukkit.actions;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.handler.*;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class ActionFactory {

    private final HashMap<String, NexusAction> availableActions;

    public ActionFactory() {
        this.availableActions = new HashMap<>();
        registerInternalActions();
    }

    private void registerInternalActions() {
        this.availableActions.put("message", new ActionSendMessage());
        this.availableActions.put("vault-add", new ActionVaultAdd());
        this.availableActions.put("command", new ActionCommand());
        this.availableActions.put("sound", new ActionSound());
        this.availableActions.put("exp", new ActionEXP());
    }

    public boolean registerAction(String id, NexusAction action) {

        if(this.availableActions.containsKey(id)) {
            NexusPlugin.nexusLogger.error("Third party plugin tried to register an action with id: " + id);
            NexusPlugin.nexusLogger.error("Action with id " + id + " already exists!");
            return false;
        }

        this.availableActions.put(id, action);
        return true;
    }

    @Nullable
    public NexusAction getAction(String actionID) {
        return this.availableActions.get(actionID);
    }

    public void executeActions(Player player, @Nullable Location targetLocation, List<Map<String, Object>> actions) {

        if (actions == null || actions.isEmpty()) {
            return;
        }

        // Hole die ActionFactory vom Plugin
        ActionFactory actionFactory = NexusPlugin.getInstance().getActionFactory();

        for (Map<String, Object> actionDataMap : actions) {
            String actionId = (String) actionDataMap.get("id");
            NexusAction action = actionFactory.getAction(actionId);

            // Überprüfen, ob die Aktion registriert ist
            if (action == null) {
                NexusPlugin.nexusLogger.warning("Unknown action: " + actionId + " in Action Section");
                continue;
            }

            // Erstelle ein ActionData-Objekt basierend auf den YAML-Daten
            ActionData actionData = new ActionData();
            actionData.getData().putAll(actionDataMap);

            try {
                // Führe die Aktion aus
                action.execute(player, actionData, targetLocation);
            } catch (Exception e) {
                // Fehler beim Ausführen der Aktion abfangen und protokollieren
                NexusPlugin.nexusLogger.error("Failed to execute action: " + actionId);
                e.printStackTrace();
            }
        }
    }
}
