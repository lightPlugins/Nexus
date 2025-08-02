package io.nexstudios.nexus.bukkit.utils;

import java.util.Random;

public class CheckChance {
    public static boolean check(double chance) {
        Random random = new java.util.Random();
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException("Chance must be between 0 and 100, inclusive. You gave " + chance);
        }
        return random.nextDouble() * 100 < chance;
    }
}
