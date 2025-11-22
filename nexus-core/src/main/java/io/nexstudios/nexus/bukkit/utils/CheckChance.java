package io.nexstudios.nexus.bukkit.utils;


import java.util.Random;

public class CheckChance {
    public static boolean check(double chance) {
        return random() <= chance;
    }

    public static double random() {
        Random random = new Random();
        return random.nextDouble() * 100;
    }
}
