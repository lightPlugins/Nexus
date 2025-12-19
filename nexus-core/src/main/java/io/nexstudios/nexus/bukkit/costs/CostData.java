package io.nexstudios.nexus.bukkit.costs;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class CostData {

    private final HashMap<String, Object> data;

    public CostData() {
        this.data = new HashMap<>();
    }

    /**
     * Fills this data object from a configuration section.
     */
    public CostData createData(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            this.data.put(key, section.get(key));
        }
        return this;
    }

    /**
     * Adds a custom typed data entry.
     */
    public CostData addCustomData(String key, Object value, Class<?> expectedType) {
        if (!validate(value, expectedType)) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Wrong data type for cost data value: " + value,
                    "Expected " + expectedType.getSimpleName() + ", got: " + value.getClass().getSimpleName()
            ));
            return null;
        }
        this.data.put(key, value);
        return this;
    }

    /**
     * Simple type check helper.
     */
    public boolean validate(Object value, Class<?> type) {
        return type.isInstance(value);
    }

    /**
     * Convenience: reads a map list from "success-actions" / "fail-actions".
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getActionList(String key) {
        Object raw = data.get(key);
        if (raw instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }
}