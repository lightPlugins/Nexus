package io.nexstudios.nexus.bukkit.actions;

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
    }

    public void registerAction(String id, NexusAction action) {
        this.availableActions.put(id, action);
    }

    @Nullable
    public NexusAction getAction(String actionID) {
        return this.availableActions.get(actionID);
    }
}
