package io.nexstudios.nexus.bukkit.levels;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public final class LevelProgress {

    private final UUID playerId;
    private final LevelKey key;

    @Setter
    private int level;

    @Setter
    private double xp;

    @Setter
    private double totalXp;

    @Setter
    private int lastAppliedLevel;

    public int levelInt() {
        return level;
    }
}