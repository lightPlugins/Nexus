package io.nexstudios.nexus.bukkit.levels;

import java.util.Collections;
import java.util.List;

/**
 * @param neededExp Index 0 => Level 1 Requirement
 */
public record LevelDefinition(List<Double> neededExp) {
    public LevelDefinition(List<Double> neededExp) {
        if (neededExp == null || neededExp.isEmpty()) {
            throw new IllegalArgumentException("neededExp darf nicht leer sein");
        }
        // defensive copy + unmodifiable
        this.neededExp = Collections.unmodifiableList(neededExp);
    }

    public int maxLevel() {
        return neededExp.size();
    }

    public double requirementFor(int level) {
        // level ist 1-basiert; 1..maxLevel
        if (level < 1 || level > maxLevel()) {
            throw new IllegalArgumentException("Level außerhalb des gültigen Bereichs: " + level);
        }
        return neededExp.get(level - 1);
    }
}