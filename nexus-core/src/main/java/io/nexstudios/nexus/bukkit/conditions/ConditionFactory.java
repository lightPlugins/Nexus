package io.nexstudios.nexus.bukkit.conditions;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.conditions.impl.*;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ConditionFactory {

    private final HashMap<String, NexusCondition> availableConditions;

    public ConditionFactory() {
        this.availableConditions = new HashMap<>();
        registerInternalConditions();
    }

    private void registerInternalConditions() {
        availableConditions.put("permission", new ConditionPermission());
        availableConditions.put("above-block", new ConditionAboveBlock());
        availableConditions.put("in-world", new ConditionInWorld());
        availableConditions.put("has-age", new ConditionHasAge());

        if(NexusPlugin.getInstance().getWorldGuardHook() != null) {
            availableConditions.put("in-region", new ConditionInRegion());
        }

    }


    public boolean registerCondition(String id, NexusCondition condition) {
        if(this.availableConditions.containsKey(id)) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Third party plugin tried to register an condition with id: " + id,
                    "Condition with id " + id + " already exists!"
            ));
            return false;
        }

        this.availableConditions.put(id, condition);
        return true;
    }

    public NexusCondition getCondition(String conditionID) {
        return this.availableConditions.get(conditionID);
    }

    public boolean checkConditions(Player player, @Nullable Location targetLocation, List<Map<String, Object>> conditions) {

        if(conditions == null || conditions.isEmpty()) {
            return true;
        }

        ConditionFactory conditionFactory = NexusPlugin.getInstance().getConditionFactory();
        boolean anyConditionChecked = false;

        for(Map<String, Object> conditionMap : conditions) {
            String conditionID = (String) conditionMap.get("id");
            NexusCondition condition = conditionFactory.getCondition(conditionID);

            if(condition == null) {
                NexusPlugin.nexusLogger.warning("Unknown condition: " + conditionID + " in Condition Section");
                continue;
            }

            ConditionData conditionData = new ConditionData();
            conditionData.getData().putAll(conditionMap);
            anyConditionChecked = true;

            try {
                if(!condition.checkCondition(player, conditionData, targetLocation)) {
                    condition.sendMessage(player, conditionData);
                    return false;
                }
            } catch (Exception e) {
                NexusPlugin.nexusLogger.error(List.of(
                        "Error while checking condition: " + conditionID,
                        "Error: " + e.getMessage()
                ));
                e.printStackTrace();
                return false;
            }
        }

        return anyConditionChecked;
    }
}
