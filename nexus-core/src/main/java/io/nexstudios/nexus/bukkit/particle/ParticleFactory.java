package io.nexstudios.nexus.bukkit.particle;

import org.bukkit.Bukkit;
import org.bukkit.Material;

public class ParticleFactory {

    public static ParticleBuilder getParticleBuilder() {
        String minecraftVersion = Bukkit.getMinecraftVersion();

        try {
            String className = "io.nexstudios.internal.nms.v"
                    + minecraftVersion.replace(".", "_")
                    + ".packets.ClientParticles";
            Class<?> clazz = Class.forName(className);
            return (ParticleBuilder) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "No compatible ItemBuilder implementation found for Minecraft version: " + minecraftVersion, e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error loading ItemBuilder implementation for Minecraft version: " + minecraftVersion, e);
        }
    }
}
