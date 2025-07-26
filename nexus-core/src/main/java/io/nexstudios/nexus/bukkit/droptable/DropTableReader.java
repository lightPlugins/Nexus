package io.nexstudios.nexus.bukkit.droptable;

import io.nexstudios.nexus.bukkit.NexusPlugin;
import io.nexstudios.nexus.bukkit.droptable.models.DropTable;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropTableReader {

    private final List<File> files;
    @Getter private final HashMap<String, DropTable> dropTables;

    public DropTableReader(List<File> files) {
        this.files = files;
        dropTables = new HashMap<>();
    }

    public void read() {
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            DropTable dropTable = new DropTable();

            String fileID = file.getName().replace(".yml", "");
            dropTable.setId(fileID);
            dropTable.setConfiguration(config);

            dropTable.setConditions(readDropConditions(config, dropTable));
            dropTable.setStyle(readDropStyle(config, dropTable));
            dropTable.setDrops(readDrop(config, dropTable));

            dropTables.put(fileID, dropTable);
        }

        NexusPlugin.nexusLogger.info("Successfully read " + dropTables.size() + " droptables");
    }

    public Vector refreshVelocity(YamlConfiguration config, DropTable dropTable) {
        DropTable.DropStyle dropStyle = readDropStyle(config, dropTable);
        if(dropStyle == null) {
            NexusPlugin.nexusLogger.error("Could not find 'drop-style' section in droptable " + dropTable.getId() + ". Fallback to default velocity (0, 0, 0).");
            return new Vector(0, 0, 0);
        }
        return dropStyle.getFancyDropStyle().getVelocity();

    }

    @SuppressWarnings("unchecked")
    private List<DropTable.Drop> readDrop(YamlConfiguration config, DropTable dropTable) {
        List<Map<?, ?>> dropsConfig = config.getMapList("drop-items");

        if (dropsConfig.isEmpty()) {
            NexusPlugin.nexusLogger.error("Could not find 'drop-items' section in droptable " + dropTable.getId() + ". Skipping.");
            return null;
        }

        List<DropTable.Drop> drops = new ArrayList<>();

        for (Map<?, ?> dropDataRaw : dropsConfig) {
            Map<String, Object> dropData = (Map<String, Object>) dropDataRaw;

            DropTable.Drop drop = new DropTable.Drop();
            try {
                String item = (String) dropData.getOrDefault("item", "stone"); // Sicherer Zugriff
                int amount = ((Number) dropData.getOrDefault("amount", 1)).intValue(); // Wert wird als Number interpretiert
                double chance = ((Number) dropData.getOrDefault("chance", 100.0)).doubleValue(); // Wert wird ebenfalls als Number interpretiert

                drop.setItem(item);
                drop.setAmount(amount);
                drop.setChance(String.valueOf(chance));

                if (dropData.containsKey("settings")) {
                    ConfigurationSection settingsSection = config.createSection("settings", (Map<?, ?>) dropData.get("settings"));
                    drop.setSettings(readDropSettings(settingsSection, dropTable));
                }

                drops.add(drop);

            } catch (Exception e) {
                NexusPlugin.nexusLogger.error("Failed to parse drop for DropTable: " + dropTable.getId() + " Error: " + e.getMessage());
            }
        }

        return drops;
    }


    private DropTable.Drop.DropSettings readDropSettings(ConfigurationSection section, DropTable dropTable) {
        DropTable.Drop.DropSettings dropSettings = new DropTable.Drop.DropSettings();

        if (section == null) {
            NexusPlugin.nexusLogger.error("Configuration for drop-settings is null in droptable " + dropTable.getId() + ". Skipping.");
            return null;
        }

        dropSettings.setPickUpOwner(section.getBoolean("pick-up-only-owner", false));
        dropSettings.setVisibleOwner(section.getBoolean("visible-only-owner", false));
        dropSettings.setItemName(MiniMessage.miniMessage().deserialize(section.getString("item-name", "<red>Name not found")));

        TextColor textColor = TextColor.fromHexString(section.getString("glow-color", "RED"));
        dropSettings.setGlowColor((textColor != null) ? NamedTextColor.nearestTo(textColor) : NamedTextColor.GREEN);

        dropSettings.setTrailColor(section.getString("fancy-drop-color", "#FFFFFF"));

        return dropSettings;
    }

    private DropTable.DropStyle readDropStyle(YamlConfiguration config, DropTable dropTable) {
        ConfigurationSection section = config.getConfigurationSection("drop-style");

        if (section == null) {
            NexusPlugin.nexusLogger.error("Could not find 'drop-style' section in droptable " + dropTable.getId() + ". Skipping.");
            return null;
        }

        DropTable.DropStyle style = new DropTable.DropStyle();
        String styleType = section.getString("id");

        if (styleType == null) {
            NexusPlugin.nexusLogger.error("Could not find style id in 'drop-style' section for droptable " + dropTable.getId() + ". Skipping.");
            return null;
        }

        if ("fancy-drop".equalsIgnoreCase(styleType)) {
            style.setFancyDropStyle(readFancyDropStyle(section, dropTable));
        }

        return style;
    }

    private DropTable.DropStyle.FancyDropStyle readFancyDropStyle(ConfigurationSection section, DropTable dropTable) {
        DropTable.DropStyle.FancyDropStyle fancyDropStyle = new DropTable.DropStyle.FancyDropStyle();

        ConfigurationSection velocitySection = section.getConfigurationSection("velocity");
        if (velocitySection == null) {
            NexusPlugin.nexusLogger.error("Missing 'velocity' section in 'fancy-drop' style for droptable " + dropTable.getId() + ". Skipping fancy-drop.");
            return null;
        }

        double randomX = (Math.random() * 0.4) - 0.2;
        double xMin = velocitySection.getDouble("in-x.min", 0.2);
        double xMax = velocitySection.getDouble("in-x.max", 0.5);
        double yMin = velocitySection.getDouble("in-y.min", 0.5);
        double yMax = velocitySection.getDouble("in-y.max", 0.9);
        double zMin = velocitySection.getDouble("in-z.min", 0.2);
        double zMax = velocitySection.getDouble("in-z.max", 0.5);

        // Randomly generate vector within bounds
        double x = xMin + (Math.random() * (xMax - xMin));
        double y = yMin + (Math.random() * (yMax - yMin));
        double z = zMin + (Math.random() * (zMax - zMin));

        fancyDropStyle.setVelocity(new Vector(x, y, z));

        return fancyDropStyle;
    }

    private DropTable.DropConditions readDropConditions(YamlConfiguration config, DropTable dropTable) {
        List<Map<?, ?>> conditionsConfig = config.getMapList("drop-conditions");

        if (conditionsConfig.isEmpty()) {
            NexusPlugin.nexusLogger.warning("No 'drop-conditions' section defined for droptable " + dropTable.getId() + ". Defaulting to empty conditions.");
            return new DropTable.DropConditions();
        }

        DropTable.DropConditions conditions = new DropTable.DropConditions();

        for (Map<?, ?> conditionData : conditionsConfig) {
            String id = (String) conditionData.get("id");

            if ("kill".equalsIgnoreCase(id)) {
                conditions.setKillCondition(readKillCondition(config.createSection("kill-condition", conditionData)));
            } else if ("mine".equalsIgnoreCase(id)) {
                conditions.setMineCondition(readMineCondition(config.createSection("mine-condition", conditionData)));
            } else {
                NexusPlugin.nexusLogger.warning("Unknown condition id '" + id + "' in droptable " + dropTable.getId() + ". Skipping.");
            }
        }

        return conditions;
    }

    private DropTable.DropConditions.KillCondition readKillCondition(ConfigurationSection section) {
        DropTable.DropConditions.KillCondition condition = new DropTable.DropConditions.KillCondition();
        condition.setEntityTypes(section.getStringList("types"));
        return condition;
    }

    private DropTable.DropConditions.MineCondition readMineCondition(ConfigurationSection section) {
        DropTable.DropConditions.MineCondition condition = new DropTable.DropConditions.MineCondition();
        condition.setBlockTypes(section.getStringList("blocks"));
        return condition;
    }
}