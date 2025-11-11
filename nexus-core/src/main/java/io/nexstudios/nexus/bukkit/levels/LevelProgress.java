package io.nexstudios.nexus.bukkit.levels;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public final class LevelProgress {
    private final UUID playerId;
    private final LevelKey key;
    private int level;
    private double xp;

    public int levelInt() {
        return level;
    }
}