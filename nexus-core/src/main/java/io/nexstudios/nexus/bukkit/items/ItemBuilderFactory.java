package io.nexstudios.nexus.bukkit.items;

import org.bukkit.Bukkit;
import org.bukkit.Material;

/**
 * A factory class for providing instances of the {@link ItemBuilder} interface.
 * <p>
 * This factory dynamically loads an appropriate implementation of the {@link ItemBuilder}
 * interface based on the current Minecraft server version. The implementation is expected
 * to reside within the package structure derived from the version.
 * <p>
 * If no compatible implementation is found for the given Minecraft version or if an
 * error occurs during the instantiation process, exceptions will be thrown.
 */
public class ItemBuilderFactory {

    /**
     * Retrieves an instance of the {@link ItemBuilder} interface, dynamically loading
     * the appropriate implementation based on the current Minecraft server version.
     *
     * The implementation class must follow a package naming convention that includes
     * the Minecraft version as part of its path.
     *
     * @return An instance of {@link ItemBuilder} dynamically loaded based on the Minecraft version.
     * @throws IllegalStateException if no compatible implementation is found or if an error occurs
     *                                during the instantiation process.
     */
    public static ItemBuilder getItemBuilder(Material material) {
        String minecraftVersion = Bukkit.getMinecraftVersion();

        try {
            String className = "io.nexstudios.internal.nms.v"
                            + minecraftVersion.replace(".", "_")
                            + ".items.ItemBuilderImpl";
            Class<?> clazz = Class.forName(className);
            return (ItemBuilder) clazz.getDeclaredConstructor(Material.class).newInstance(material);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "No compatible ItemBuilder implementation found for Minecraft version: " + minecraftVersion, e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error loading ItemBuilder implementation for Minecraft version: " + minecraftVersion, e);
        }
    }
}



