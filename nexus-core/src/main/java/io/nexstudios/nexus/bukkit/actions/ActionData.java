package io.nexstudios.nexus.bukkit.actions;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;

@Getter
public class ActionData {

    private final HashMap<String, Object> data;

    public ActionData() {
        this.data = new HashMap<>();
    }

    public ActionData createData(ConfigurationSection section) {
        for(String key : section.getKeys(false)) {
            this.data.put(key, section.get(key));
        }
        return this;
    }

    public ActionData addCustomData(String key, Object value, Class<?> expectedType) {
        if(!validate(value, expectedType)) {
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
