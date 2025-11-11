package io.nexstudios.nexus.bukkit.levels;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode
public class LevelKey {
    String namespace;
    String key;

    @Override
    public String toString() {
        return namespace + ":" + key;
    }
}