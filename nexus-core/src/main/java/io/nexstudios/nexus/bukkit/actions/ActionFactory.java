package io.nexstudios.nexus.bukkit.actions;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.handler.ActionCommand;
import io.nexstudios.nexus.bukkit.actions.handler.ActionSendMessage;
import io.nexstudios.nexus.bukkit.actions.handler.ActionVaultAdd;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.HashMap;

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
}
