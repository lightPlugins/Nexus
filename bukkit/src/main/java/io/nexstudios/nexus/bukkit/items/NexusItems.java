package io.nexstudios.nexus.bukkit.items;

import io.nexstudios.nexus.common.files.NexusFileReader;
import io.nexstudios.nexus.common.logging.NexusLogger;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Getter
public class NexusItems {

    private final NexusFileReader itemFiles;
    private final NexusLogger nexusLogger;
    private Map<String, ItemStack> loadedItems = new HashMap<>();

    public NexusItems(NexusLogger nexusLogger, NexusFileReader itemFiles) {
        this.nexusLogger = nexusLogger;
        this.itemFiles = itemFiles;

        if(itemFiles.getFiles().isEmpty()) {
            nexusLogger.warning("Nexus items files were empty! Skipping item loading.");
            return;
        }
        float startTime = System.currentTimeMillis();
        itemFiles.getFiles().forEach(this::readItems);
        float endTime = System.currentTimeMillis();
        nexusLogger.info("(Re)Loaded <yellow>" + loadedItems.size() + "<reset> items in <yellow>" + (endTime - startTime) + "<reset>ms.");

    }

    private void readItems(File file) {

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // the "unique" item name is the file name without the .yml extension
        String itemID = file.getName().replace(".yml", "");

        if(loadedItems.containsKey(itemID)) {
            nexusLogger.warning("Item file name <yellow>" + itemID + "<reset> is already loaded. Skipping this item.");
            return;
        }

        String itemType = config.getString("id");

        if(itemType == null) {
            nexusLogger.warning("Item file <yellow>" + itemID + "<reset> does not contain an 'id' field. Skipping this item.");
            return;
        }

        switch (itemType) {
            case "vanilla": readVanillaItem(config, itemID); break;
            case "mmoitems": nexusLogger.info("MMOItems are not supported yet. Skipping item loading for <yellow>" + itemID + "<reset>."); break;
            default: {
                nexusLogger.warning("Unknown item type <yellow>" + itemType + "<reset> in file <yellow>" + itemID + "<reset>. Skipping item loading.");
                break;
            }
        }
    }

    private void readVanillaItem(FileConfiguration config, String fileName) {
        // Read vanilla item configuration

    }
}
