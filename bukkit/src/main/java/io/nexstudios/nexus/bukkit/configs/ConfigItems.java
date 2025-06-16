package io.nexstudios.nexus.bukkit.configs;

import org.bukkit.configuration.ConfigurationSection;

public class ConfigItems {

    private final ConfigurationSection actionSection;
    private final String fileName;

    public ConfigItems(ConfigurationSection actionSection, String fileName) {
        this.actionSection = actionSection;
        this.fileName = fileName;
    }



}
