package io.lightstudios.nexus.bukkit;

import io.lightstudios.nexus.common.logging.NexusLogger;
import io.lightstudios.nexus.common.logging.files.NexusFile;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Nexus extends JavaPlugin {

    @Getter
    private static Nexus instance;
    public static NexusLogger logger;
    public NexusFile settingsFile;

    @Override
    public void onLoad() {
        // Plugin startup logic
        instance = this;
        logger = new NexusLogger("<reset>[<yellow>Nexus<reset>]", true, 99, "<yellow>");
        logger.info("Nexus plugin is loading...");
        onReload();
    }

    @Override
    public void onEnable() {

        logger.info("Nexus plugin enabled");
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logger.info("Nexus plugin has been disabled!");
    }

    public void onReload() {
        logger.info("Reloading Nexus plugin...");
        loadNexusFiles();
        logger.info("Nexus plugin reloaded successfully.");
    }

    private void loadNexusFiles() {
        settingsFile = new NexusFile(this, "settings.yml", logger, true);
        logger.setDebugEnabled(settingsFile.getBoolean("logging.debug.enable", true));
        logger.setDebugLevel(settingsFile.getInt("logging.debug.level", 3));
        logger.info("All Nexus files have been loaded successfully.");
    }
}
