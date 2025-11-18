package io.nexstudios.nexus.bukkit.conditions;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;

@Getter
public class ConditionData {

    private final HashMap<String, Object> data;

    public ConditionData() {
        this.data = new HashMap<>();
    }

    public ConditionData createData(ConfigurationSection section) {
        for(String key : section.getKeys(false)) {
            this.data.put(key, section.get(key));
        }

        return this;
    }

    public ConditionData addCustomData(String key, Object value, Class<?> expectedType) {
        if(!value.getClass().equals(expectedType)) {
            NexusPlugin.nexusLogger.error(List.of(
                    "Wrong data type for action data value: " + value,
                    "Expected " + expectedType.getSimpleName() + ", got: " + value.getClass().getSimpleName()
            ));
            return null;
        }

        this.data.put(key, value);
        return this;
    }

    public boolean validate(Object value, Class<?> type) {
        return value.getClass().equals(type);
    }

}
