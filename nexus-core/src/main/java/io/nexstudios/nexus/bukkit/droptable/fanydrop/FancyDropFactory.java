package io.nexstudios.nexus.bukkit.droptable.fanydrop;

import org.bukkit.Bukkit;

public class FancyDropFactory {

    public static FancyDropBuilder getFancyDropBuilder() {
        String minecraftVersion = Bukkit.getMinecraftVersion();

        try {
            String className = "io.nexstudios.internal.nms.v"
                    + minecraftVersion.replace(".", "_")
                    + ".fancydrop.FancyDropBuilderImpl";
            Class<?> clazz = Class.forName(className);
            return (FancyDropBuilder) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "No compatible FancyDropBuilder implementation found for Minecraft version: " + minecraftVersion, e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error loading FancyDropBuilder implementation for Minecraft version: " + minecraftVersion, e);
        }
    }
}
