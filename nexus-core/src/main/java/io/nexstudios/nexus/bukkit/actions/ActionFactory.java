package io.nexstudios.nexus.bukkit.actions;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.actions.impl.*;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ActionFactory {

    private final HashMap<String, NexusAction> availableActions;

    public ActionFactory() {
        this.availableActions = new HashMap<>();
        registerInternalActions();
    }

    private void registerInternalActions() {
        this.availableActions.put("send-message", new ActionSendMessage());
        this.availableActions.put("vault-add", new ActionVaultAdd());
        this.availableActions.put("command", new ActionCommand());
        this.availableActions.put("play-sound", new ActionSound());
        this.availableActions.put("add-exp", new ActionEXP());
    }

    public boolean registerAction(String id, NexusAction action) {

        if (this.availableActions.containsKey(id)) {
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

    public ExecutionBuilder newExecution() {
        return new ExecutionBuilder(this);
    }

    public static final class ExecutionBuilder {

        private final ActionFactory factory;

        private LivingEntity actor;
        private Location targetLocation;
        private List<Map<String, Object>> actions;
        private NexParams params = NexParams.empty();

        private ExecutionBuilder(ActionFactory factory) {
            this.factory = factory;
        }

        public ExecutionBuilder actor(LivingEntity actor) {
            this.actor = actor;
            return this;
        }

        public ExecutionBuilder targetLocation(@Nullable Location location) {
            this.targetLocation = location;
            return this;
        }

        public ExecutionBuilder actions(List<Map<String, Object>> actions) {
            this.actions = (actions == null || actions.isEmpty()) ? List.of() : List.copyOf(actions);
            return this;
        }

        public ExecutionBuilder actionsFromSection(ConfigurationSection section, String key) {
            if (section == null || key == null || key.isBlank()) {
                this.actions = List.of();
                return this;
            }
            List<?> rawList = section.getMapList(key);
            this.actions = castListOfMap(rawList);
            return this;
        }

        public ExecutionBuilder params(NexParams params) {
            this.params = (params == null) ? NexParams.empty() : params;
            return this;
        }

        public ExecutionBuilder params(Map<String, String> raw) {
            if (raw == null || raw.isEmpty()) {
                this.params = NexParams.empty();
            } else {
                this.params = NexParams.of(raw);
            }
            return this;
        }

        public void execute() {
            if (actor == null) {
                throw new IllegalStateException("ActionExecutionBuilder: actor must be set before execute()");
            }
            if (actions == null || actions.isEmpty()) {
                return;
            }

            factory.executeInternal(actor, targetLocation, actions, params);
        }
    }

    void executeInternal(LivingEntity actor,
                         @Nullable Location targetLocation,
                         List<Map<String, Object>> actions,
                         NexParams params) {

        if (actions == null || actions.isEmpty()) {
            return;
        }

        NexParams nonNullParams = (params == null) ? NexParams.empty() : params;

        for (Map<String, Object> actionDataMap : actions) {
            Object idObj = actionDataMap.get("id");
            if (!(idObj instanceof String actionId)) {
                NexusPlugin.nexusLogger.warning("Action entry without valid 'id' in Action Section: " + actionDataMap);
                continue;
            }

            NexusAction action = getAction(actionId);

            if (action == null) {
                NexusPlugin.nexusLogger.warning("Unknown action: " + actionId + " in Action Section");
                continue;
            }

            ActionData actionData = new ActionData();
            actionData.getData().putAll(actionDataMap);

            NexusActionContext context = new NexusActionContext(
                    actor,
                    targetLocation,
                    actionData,
                    nonNullParams
            );

            Object delayObj = actionDataMap.get("delay");
            int delay = 0;
            if (delayObj instanceof Number n) {
                delay = n.intValue();
            }

            if (delay > 0) {
                NexusPlugin.getInstance().getServer().getScheduler().runTaskLater(
                        NexusPlugin.getInstance(),
                        () -> executeAction(action, context, actionId),
                        delay
                );
            } else {
                executeAction(action, context, actionId);
            }
        }
    }

    private void executeAction(NexusAction action, NexusActionContext context, String actionId) {
        try {
            action.execute(context);
        } catch (Exception e) {
            NexusPlugin.nexusLogger.error("Failed to execute action: " + actionId);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castListOfMap(Object o) {
        if (o instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object e : list) {
                if (e instanceof Map<?, ?> m) {
                    out.add((Map<String, Object>) m);
                }
            }
            return out;
        }
        return List.of();
    }
}