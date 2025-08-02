package io.nexstudios.nexus.bukkit.utils;

public class CalculateMultiplier {

    public static int calculate(double result) {
        int baseMultiplier = (int) result;

        double fractionalChance = result - baseMultiplier;

        if (CheckChance.check(fractionalChance * 100)) {
            baseMultiplier++;
        }

        return baseMultiplier;
    }
}
