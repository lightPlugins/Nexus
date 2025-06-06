package io.lightstudios.nexus.bukkit;

import io.lightstudios.nexus.common.logging.NexusLogger;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Nexus extends JavaPlugin {

    @Getter
    private static Nexus instance;
    public static NexusLogger logger;

    @Override
    public void onLoad() {
        // Plugin startup logic
        instance = this;
        logger = new NexusLogger("<reset>[<yellow>Nexus<reset>]", true, 99, "<yellow>");
        logger.info("Nexus plugin is loading...");
    }

    @Override
    public void onEnable() {

        // debug level: higher number = more verbose logging

        logger.info("Nexus plugin enabled");
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logger.info("Nexus plugin has been disabled!");
    }
}
